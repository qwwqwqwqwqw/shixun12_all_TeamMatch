package com.teammatch.m6.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.common.ReasonCode;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.User;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.m6.constants.CreditChangeType;
import com.teammatch.m6.constants.PenaltyType;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.mapper.PenaltyMapper;
import com.teammatch.m6.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 处罚记录 Service 实现类
 *
 * 根据详细设计文档 7.2 节、7.4 节、7.5 节定义实现。
 *
 * 【跨模块写表说明】
 * 1. credit_change — M5 账本，M6 写入 penalty / penalty_restore（V2.1 §6.9 冻结类型）
 * 2. user — M3 用户主体，M6 更新 credit_score 与 status
 *
 * 事务：{@link Transactional} 保证 penalty 主记录与副作用同事务提交或回滚。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyServiceImpl extends ServiceImpl<PenaltyMapper, Penalty> implements PenaltyService {

    private static final String PENALTY_STATUS_ACTIVE = "active";
    private static final String PENALTY_STATUS_REVOKED = "revoked";
    private static final String USER_STATUS_BANNED = "banned";
    private static final String USER_STATUS_ACTIVE = "active";

    private final CreditChangeMapper creditChangeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Penalty createPenalty(Long adminId, PenaltyCreateDTO dto) {
        if (Objects.equals(adminId, dto.getUserId())) {
            log.warn("管理员尝试对自己执行处罚: adminId={}, type={}, relatedReportId={}",
                    adminId, dto.getType(), dto.getRelatedReportId());
            throw new BusinessException(ReasonCode.PENALTY_SELF_NOT_ALLOWED);
        }

        if (PenaltyType.CREDIT_DEDUCT.equals(dto.getType())) {
            if (dto.getCreditDeductValue() == null || dto.getCreditDeductValue() <= 0) {
                throw new IllegalArgumentException("credit_deduct类型的处罚必须指定有效的扣分值（大于0）");
            }
        }

        Penalty penalty = new Penalty();
        penalty.setUserId(dto.getUserId());
        penalty.setType(dto.getType());
        penalty.setCreditDeductValue(dto.getCreditDeductValue());
        penalty.setReason(dto.getReason());
        penalty.setAdminId(adminId);
        penalty.setRelatedReportId(dto.getRelatedReportId());
        penalty.setStatus(PENALTY_STATUS_ACTIVE);
        penalty.setCreatedAt(LocalDateTime.now());
        penalty.setUpdatedAt(LocalDateTime.now());

        baseMapper.insert(penalty);
        executePenaltySideEffects(penalty, dto);
        return penalty;
    }

    /**
     * 创建处罚后的副作用（与 penalty 插入同事务）。
     * <ul>
     *   <li>credit_deduct — 写 penalty 流水并扣减 user.credit_score</li>
     *   <li>function_limit — 设 user.status=banned（多条处罚叠加时重复设为 banned 即可）</li>
     * </ul>
     */
    private void executePenaltySideEffects(Penalty penalty, PenaltyCreateDTO dto) {
        if (PenaltyType.CREDIT_DEDUCT.equals(dto.getType())) {
            CreditChange creditChange = new CreditChange();
            creditChange.setUserId(dto.getUserId());
            creditChange.setProjectId(null);
            creditChange.setChangeType(CreditChangeType.PENALTY);
            creditChange.setChangeValue(-dto.getCreditDeductValue());
            creditChange.setEffective(true);
            creditChange.setSourceType(CreditChangeType.SOURCE_TYPE_PENALTY);
            creditChange.setSourceId(penalty.getId());
            creditChange.setDescription(dto.getReason());
            creditChange.setCreatedAt(LocalDateTime.now());
            creditChange.setUpdatedAt(LocalDateTime.now());

            if (creditChangeMapper.insert(creditChange) != 1) {
                throw new RuntimeException("写入 credit_change 流水失败");
            }

            if (userMapper.updateCreditScore(dto.getUserId(), -dto.getCreditDeductValue()) != 1) {
                throw new RuntimeException("更新 user.credit_score 失败");
            }
        } else if (PenaltyType.FUNCTION_LIMIT.equals(dto.getType())) {
            User user = userMapper.selectById(dto.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在: " + dto.getUserId());
            }
            user.setStatus(USER_STATUS_BANNED);
            user.setUpdatedAt(LocalDateTime.now());
            if (userMapper.updateById(user) != 1) {
                throw new RuntimeException("更新 user.status 失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Penalty revokePenalty(Long penaltyId, Long adminId, PenaltyRevokeDTO dto) {
        Penalty penalty = baseMapper.selectById(penaltyId);
        if (penalty == null) {
            throw new IllegalArgumentException("处罚记录不存在: " + penaltyId);
        }
        if (PENALTY_STATUS_REVOKED.equals(penalty.getStatus())) {
            throw new IllegalStateException("处罚已撤销，无法重复撤销");
        }

        // 先将本条标记为 revoked，再执行副作用（function_limit 计数依赖此状态）
        penalty.setStatus(PENALTY_STATUS_REVOKED);
        penalty.setRevokedAt(LocalDateTime.now());
        penalty.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(penalty);

        executeRevokeSideEffects(penalty, dto);
        return penalty;
    }

    /**
     * 撤销处罚后的副作用（在 penalty.status 已更新为 revoked 之后调用）。
     * <ul>
     *   <li>credit_deduct — 写 penalty_restore 流水并回加 user.credit_score（§6.9 冻结类型）</li>
     *   <li>function_limit — 仅当该用户已无其它 active 的 function_limit 时才解封（§6.7）</li>
     * </ul>
     */
    private void executeRevokeSideEffects(Penalty penalty, PenaltyRevokeDTO dto) {
        if (PenaltyType.CREDIT_DEDUCT.equals(penalty.getType())) {
            // 不能使用 change_type=penalty：与扣分流水共用 uk(source_type,source_id,user_id,change_type) 会冲突
            CreditChange creditChange = new CreditChange();
            creditChange.setUserId(penalty.getUserId());
            creditChange.setProjectId(null);
            creditChange.setChangeType(CreditChangeType.PENALTY_RESTORE);
            creditChange.setChangeValue(penalty.getCreditDeductValue());
            creditChange.setEffective(true);
            creditChange.setSourceType(CreditChangeType.SOURCE_TYPE_PENALTY);
            creditChange.setSourceId(penalty.getId());
            creditChange.setDescription(dto.getReason());
            creditChange.setCreatedAt(LocalDateTime.now());
            creditChange.setUpdatedAt(LocalDateTime.now());

            if (creditChangeMapper.insert(creditChange) != 1) {
                throw new RuntimeException("写入 credit_change 恢复流水失败");
            }

            if (userMapper.updateCreditScore(penalty.getUserId(), penalty.getCreditDeductValue()) != 1) {
                throw new RuntimeException("恢复 user.credit_score 失败");
            }
        } else if (PenaltyType.FUNCTION_LIMIT.equals(penalty.getType())) {
            restoreUserStatusIfNoActiveFunctionLimit(penalty.getUserId());
        }
    }

    /**
     * 按剩余生效中的 function_limit 条数决定是否解封（V2.1 §6.7 function_limit 分支）。
     * <p>
     * 调用前当前 penalty 已为 revoked，故 count 不包含本条。
     * 若仍有其它 active 的 function_limit，保持 banned，避免「撤一条解封全部」。
     */
    private void restoreUserStatusIfNoActiveFunctionLimit(Long userId) {
        int remainingActive = baseMapper.countActiveFunctionLimitByUserId(userId);
        if (remainingActive > 0) {
            return;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        user.setStatus(USER_STATUS_ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateById(user) != 1) {
            throw new RuntimeException("恢复 user.status 失败");
        }
    }

    @Override
    public List<Penalty> getUserPenalties(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<Penalty> getUserActivePenalties(Long userId) {
        return baseMapper.selectActiveByUserId(userId);
    }

    @Override
    public List<Penalty> getPenaltyList(String status, String type) {
        if (status != null && !status.isEmpty() && type != null && !type.isEmpty()) {
            return baseMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Penalty>()
                            .eq(Penalty::getStatus, status)
                            .eq(Penalty::getType, type)
                            .orderByDesc(Penalty::getCreatedAt)
            );
        } else if (status != null && !status.isEmpty()) {
            return baseMapper.selectByStatus(status);
        } else if (type != null && !type.isEmpty()) {
            return baseMapper.selectByType(type);
        }

        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Penalty>()
                        .orderByDesc(Penalty::getCreatedAt)
        );
    }

    @Override
    public Penalty getPenaltyById(Long penaltyId) {
        return baseMapper.selectById(penaltyId);
    }
}
