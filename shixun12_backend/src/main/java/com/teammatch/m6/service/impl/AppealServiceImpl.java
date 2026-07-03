package com.teammatch.m6.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AppealRestoreCommand;
import com.teammatch.entity.Appeal;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.Project;
import com.teammatch.exception.BusinessException;
import com.teammatch.exception.ValidationException;
import com.teammatch.mapper.AppealMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.m6.dto.AppealCreateDTO;
import com.teammatch.m6.dto.AppealHandleDTO;
import com.teammatch.m6.dto.AppealableEvaluationVO;
import com.teammatch.m6.dto.AppealablePenaltyVO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.mapper.PenaltyMapper;
import com.teammatch.m6.service.AppealService;
import com.teammatch.m6.service.PenaltyService;
import com.teammatch.service.AppealRestoreService;
import com.teammatch.service.storage.FileCategory;
import com.teammatch.service.storage.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 申诉 Service 实现类
 *
 * 根据详细设计文档 7.5 节定义实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealServiceImpl implements AppealService {

    private final AppealMapper appealMapper;
    private final EvaluationMapper evaluationMapper;
    private final PenaltyMapper penaltyMapper;
    private final ProjectMapper projectMapper;
    private final PenaltyService penaltyService;
    private final AppealRestoreService appealRestoreService;
    private final OssService ossService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appeal createAppeal(Long userId, AppealCreateDTO dto) {
        // 校验目标类型
        if (!"evaluation".equals(dto.getTargetType()) && !"penalty".equals(dto.getTargetType())) {
            throw new IllegalArgumentException("无效的申诉目标类型，必须是 evaluation 或 penalty");
        }

        List<String> evidenceUrls = ossService.normalizeStoredUrls(dto.getEvidenceUrls());
        ossService.validateEvidenceUrls(evidenceUrls, userId, FileCategory.APPEAL_EVIDENCE);

        // 校验目标是否存在且属于当前用户
        validateTargetExistsAndBelongsToUser(dto.getTargetType(), dto.getTargetId(), userId);

        // 同一用户对同一目标仅允许一条申诉（V2.1 §7.4 uk_appeal_target_user）
        Appeal existingAppeal = findAppealByTargetAndUser(
                dto.getTargetType(), dto.getTargetId(), userId);
        if (existingAppeal != null) {
            if ("pending".equals(existingAppeal.getStatus())) {
                throw new IllegalStateException("该目标已存在待处理的申诉，请勿重复提交");
            }
            throw new IllegalStateException("该目标已提交过申诉，请勿重复提交");
        }

        // 创建申诉实体
        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setTargetType(dto.getTargetType());
        appeal.setTargetId(dto.getTargetId());
        appeal.setReason(dto.getReason());
        appeal.setEvidenceUrls(evidenceUrls);
        appeal.setStatus("pending");
        appeal.setCreatedAt(LocalDateTime.now());
        appeal.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        appealMapper.insert(appeal);
        log.info("申诉创建成功: appealId={}, userId={}, targetType={}, targetId={}",
                appeal.getId(), userId, dto.getTargetType(), dto.getTargetId());

        return presignAppeal(appeal);
    }

    /**
     * 校验目标是否存在且属于当前用户、且满足申诉条件（与申诉页列表规则一致）
     */
    private void validateTargetExistsAndBelongsToUser(String targetType, Long targetId, Long userId) {
        if ("evaluation".equals(targetType)) {
            throwIfEvaluationNotAppealable(evaluationMapper.selectById(targetId), userId, targetId);
        } else if ("penalty".equals(targetType)) {
            throwIfPenaltyNotAppealable(penaltyMapper.selectById(targetId), userId, targetId);
        }
    }

    private void throwIfEvaluationNotAppealable(Evaluation evaluation, Long userId, Long targetId) {
        if (evaluation == null) {
            throw new IllegalArgumentException("评价记录不存在: " + targetId);
        }
        if (!Objects.equals(evaluation.getTargetId(), userId)) {
            throw new IllegalArgumentException("只能对自己的评价提交申诉");
        }
        if (Evaluation.STATUS_PENDING_REVIEW.equals(evaluation.getStatus())) {
            throw new IllegalStateException("待复核中的评价暂不可申诉，请等待管理员复核结果");
        }
        if (Evaluation.STATUS_VOIDED.equals(evaluation.getStatus())
                || Evaluation.STATUS_KEPT_NO_CREDIT.equals(evaluation.getStatus())) {
            throw new IllegalStateException("该评价已裁定，无法申诉");
        }
        if (!Evaluation.STATUS_NORMAL.equals(evaluation.getStatus())) {
            throw new IllegalStateException("仅可对已生效的正常评价提交申诉");
        }
    }

    private void throwIfPenaltyNotAppealable(Penalty penalty, Long userId, Long targetId) {
        if (penalty == null) {
            throw new IllegalArgumentException("处罚记录不存在: " + targetId);
        }
        if (!Objects.equals(penalty.getUserId(), userId)) {
            throw new IllegalArgumentException("只能对自己的处罚提交申诉");
        }
        if ("revoked".equals(penalty.getStatus())) {
            throw new IllegalStateException("该处罚已撤销，无需申诉");
        }
        if (!"active".equals(penalty.getStatus())) {
            throw new IllegalStateException("仅可对生效中的处罚提交申诉");
        }
    }

    private boolean hasAppealForTarget(String targetType, Long targetId, Long userId) {
        return findAppealByTargetAndUser(targetType, targetId, userId) != null;
    }

    @Override
    public List<AppealableEvaluationVO> listAppealableEvaluations(Long userId) {
        List<Evaluation> candidates = evaluationMapper.findNormalByTargetId(userId);
        Map<Long, String> projectTitleCache = new HashMap<>();
        List<AppealableEvaluationVO> result = new ArrayList<>();

        for (Evaluation evaluation : candidates) {
            if (hasAppealForTarget("evaluation", evaluation.getId(), userId)) {
                continue;
            }
            AppealableEvaluationVO vo = new AppealableEvaluationVO();
            vo.setEvaluationId(evaluation.getId());
            vo.setProjectId(evaluation.getProjectId());
            vo.setProjectTitle(resolveProjectTitle(evaluation.getProjectId(), projectTitleCache));
            vo.setCommunicationScore(evaluation.getCommunicationScore());
            vo.setTaskScore(evaluation.getTaskScore());
            vo.setSkillScore(evaluation.getSkillScore());
            vo.setResponsibilityScore(evaluation.getResponsibilityScore());
            vo.setAverageScore(evaluation.getAverageScore());
            vo.setComment(evaluation.getComment());
            vo.setCreatedAt(evaluation.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AppealablePenaltyVO> listAppealablePenalties(Long userId) {
        List<Penalty> activePenalties = penaltyMapper.selectActiveByUserId(userId);
        List<AppealablePenaltyVO> result = new ArrayList<>();

        for (Penalty penalty : activePenalties) {
            if (hasAppealForTarget("penalty", penalty.getId(), userId)) {
                continue;
            }
            AppealablePenaltyVO vo = new AppealablePenaltyVO();
            vo.setPenaltyId(penalty.getId());
            vo.setType(penalty.getType());
            vo.setCreditDeductValue(penalty.getCreditDeductValue());
            vo.setReason(penalty.getReason());
            vo.setStatus(penalty.getStatus());
            vo.setCreatedAt(penalty.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    private String resolveProjectTitle(Long projectId, Map<Long, String> cache) {
        if (projectId == null) {
            return null;
        }
        return cache.computeIfAbsent(projectId, id -> {
            Project project = projectMapper.selectById(id);
            return project != null ? project.getTitle() : null;
        });
    }

    @Override
    public List<Appeal> getMyAppeals(Long userId) {
        // 使用 BaseMapper.selectList 以正确反序列化 evidence_urls（@Select 不走 JacksonTypeHandler）
        return presignAppeals(appealMapper.selectList(
                new LambdaQueryWrapper<Appeal>()
                        .eq(Appeal::getUserId, userId)
                        .orderByDesc(Appeal::getCreatedAt)));
    }

    @Override
    public List<Appeal> getAppealList(String status, String targetType) {
        // 同上：带 evidence_urls 的列表查询须走 MyBatis-Plus 内置 SQL
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(Appeal::getTargetType, targetType);
        }
        return presignAppeals(appealMapper.selectList(wrapper.orderByDesc(Appeal::getCreatedAt)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appeal handleAppeal(Long appealId, Long handlerId, AppealHandleDTO dto) {
        // 检查申诉是否存在
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new IllegalArgumentException("申诉记录不存在: " + appealId);
        }

        // 检查状态是否为待处理
        if (!"pending".equals(appeal.getStatus())) {
            throw new IllegalStateException("申诉已处理，无法重复处理");
        }

        // 校验处理结果
        if (!"approved".equals(dto.getStatus()) && !"rejected".equals(dto.getStatus())) {
            throw new IllegalArgumentException("无效的处理结果，必须是 approved 或 rejected");
        }

        // 更新申诉状态
        appeal.setStatus(dto.getStatus());
        appeal.setHandlerId(handlerId);
        appeal.setHandleResult(dto.getHandleResult());
        appeal.setHandledAt(LocalDateTime.now());
        appeal.setUpdatedAt(LocalDateTime.now());

        appealMapper.updateById(appeal);

        // 如果批准申诉，执行相应的恢复操作
        if ("approved".equals(dto.getStatus())) {
            executeApprovedSideEffects(appeal, dto);
        }

        log.info("申诉处理完成: appealId={}, status={}, handlerId={}",
                appealId, dto.getStatus(), handlerId);

        return presignAppeal(appeal);
    }

    /**
     * 执行申诉批准的副作用
     * - evaluation 类型：调用 M5 AppealRestoreService 恢复信誉分
     * - penalty 类型：调用 PenaltyService.revokePenalty 撤销处罚
     */
    private void executeApprovedSideEffects(Appeal appeal, AppealHandleDTO dto) {
        if ("penalty".equals(appeal.getTargetType())) {
            Penalty penalty = penaltyMapper.selectById(appeal.getTargetId());
            if (penalty == null) {
                throw new IllegalArgumentException("处罚记录不存在: " + appeal.getTargetId());
            }
            if ("revoked".equals(penalty.getStatus())) {
                log.info("处罚已撤销，跳过重复撤销: appealId={}, penaltyId={}",
                        appeal.getId(), appeal.getTargetId());
                return;
            }

            PenaltyRevokeDTO revokeDTO = new PenaltyRevokeDTO();
            revokeDTO.setReason(dto.getHandleResult() != null ? dto.getHandleResult() : "经审核，撤销处罚");

            try {
                penaltyService.revokePenalty(appeal.getTargetId(), appeal.getHandlerId(), revokeDTO);
                log.info("处罚撤销成功: appealId={}, penaltyId={}", appeal.getId(), appeal.getTargetId());
            } catch (IllegalArgumentException | IllegalStateException | BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("处罚撤销失败: appealId={}, penaltyId={}", appeal.getId(), appeal.getTargetId(), e);
                throw new RuntimeException("处罚撤销失败: " + e.getMessage(), e);
            }
        } else if ("evaluation".equals(appeal.getTargetType())) {
            Result<?> restoreResult = appealRestoreService.restore(new AppealRestoreCommand(appeal.getId()));
            if (restoreResult.isFail()) {
                ReasonCode code = ReasonCode.fromCode(restoreResult.getCode());
                throw new BusinessException(code, restoreResult.getMessage());
            }
            log.info("申诉信誉恢复完成: appealId={}, targetId={}", appeal.getId(), appeal.getTargetId());
        }
    }

    @Override
    public Appeal getAppealById(Long appealId) {
        return presignAppeal(appealMapper.selectById(appealId));
    }

    @Override
    public Appeal getPendingAppeal(String targetType, Long targetId) {
        // selectById / selectList 才能应用 JacksonTypeHandler，@Select 自定义方法不行
        return presignAppeal(appealMapper.selectOne(
                new LambdaQueryWrapper<Appeal>()
                        .eq(Appeal::getTargetType, targetType)
                        .eq(Appeal::getTargetId, targetId)
                        .eq(Appeal::getStatus, "pending")
                        .last("LIMIT 1")));
    }

    private Appeal findAppealByTargetAndUser(String targetType, Long targetId, Long userId) {
        return appealMapper.selectOne(
                new LambdaQueryWrapper<Appeal>()
                        .eq(Appeal::getTargetType, targetType)
                        .eq(Appeal::getTargetId, targetId)
                        .eq(Appeal::getUserId, userId)
                        .last("LIMIT 1"));
    }

    private Appeal presignAppeal(Appeal appeal) {
        if (appeal == null) {
            return null;
        }
        List<String> evidenceUrls = appeal.getEvidenceUrls();
        if (evidenceUrls != null && !evidenceUrls.isEmpty()) {
            try {
                appeal.setEvidenceUrls(ossService.resolveAccessibleUrls(evidenceUrls));
            } catch (ValidationException e) {
                log.warn("申诉证据 presign 失败，返回原始 URL: appealId={}", appeal.getId());
            }
        }
        return appeal;
    }

    private List<Appeal> presignAppeals(List<Appeal> appeals) {
        if (appeals == null) {
            return null;
        }
        appeals.forEach(this::presignAppeal);
        return appeals;
    }
}
