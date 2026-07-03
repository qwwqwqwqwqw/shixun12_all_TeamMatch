package com.teammatch.m6.service;

import com.teammatch.entity.CreditChange;
import com.teammatch.entity.User;
import com.teammatch.common.ReasonCode;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.m6.constants.CreditChangeType;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.mapper.PenaltyMapper;
import com.teammatch.m6.service.impl.PenaltyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PenaltyService 单元测试
 * 使用 Mockito Mock 数据库操作，专注于 Service 业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("处罚服务单元测试")
class PenaltyServiceTest {

    @Mock
    private PenaltyMapper penaltyMapper;

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PenaltyServiceImpl service;

    private static final Long PENALTY_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long ADMIN_ID = 999L;
    private static final Long REPORT_ID = 50L;

    private Penalty activeCreditPenalty;
    private Penalty activeFunctionPenalty;
    private Penalty revokedPenalty;

    @BeforeEach
    void setUp() {
        // 必须设置 baseMapper，因为 ServiceImpl 依赖它
        ReflectionTestUtils.setField(service, "baseMapper", penaltyMapper);

        activeCreditPenalty = new Penalty();
        activeCreditPenalty.setId(PENALTY_ID);
        activeCreditPenalty.setUserId(USER_ID);
        activeCreditPenalty.setType("credit_deduct");
        activeCreditPenalty.setCreditDeductValue(10);
        activeCreditPenalty.setReason("违规操作");
        activeCreditPenalty.setAdminId(ADMIN_ID);
        activeCreditPenalty.setRelatedReportId(REPORT_ID);
        activeCreditPenalty.setStatus("active");
        activeCreditPenalty.setCreatedAt(LocalDateTime.now().minusDays(1));
        activeCreditPenalty.setUpdatedAt(LocalDateTime.now().minusDays(1));

        activeFunctionPenalty = new Penalty();
        activeFunctionPenalty.setId(2L);
        activeFunctionPenalty.setUserId(USER_ID);
        activeFunctionPenalty.setType("function_limit");
        activeFunctionPenalty.setReason("恶意举报");
        activeFunctionPenalty.setAdminId(ADMIN_ID);
        activeFunctionPenalty.setStatus("active");
        activeFunctionPenalty.setCreatedAt(LocalDateTime.now().minusDays(2));
        activeFunctionPenalty.setUpdatedAt(LocalDateTime.now().minusDays(2));

        revokedPenalty = new Penalty();
        revokedPenalty.setId(3L);
        revokedPenalty.setUserId(USER_ID);
        revokedPenalty.setType("credit_deduct");
        revokedPenalty.setCreditDeductValue(5);
        revokedPenalty.setReason("临时处罚");
        revokedPenalty.setAdminId(ADMIN_ID);
        revokedPenalty.setStatus("revoked");
        revokedPenalty.setRevokedAt(LocalDateTime.now());
        revokedPenalty.setCreatedAt(LocalDateTime.now().minusDays(3));
        revokedPenalty.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== createPenalty ====================

    @Test
    @DisplayName("createPenalty: 不能对自己执行处罚")
    void createPenalty_selfPenalty_throws() {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(ADMIN_ID);
        dto.setType("function_limit");
        dto.setReason("测试自罚");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.PENALTY_SELF_NOT_ALLOWED));

        verify(penaltyMapper, never()).insert(any(Penalty.class));
    }

    @Test
    @DisplayName("createPenalty: 正常创建credit_deduct处罚")
    void createPenalty_creditDeduct_success() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规操作");
        dto.setRelatedReportId(REPORT_ID);

        Penalty result = service.createPenalty(ADMIN_ID, dto);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getType()).isEqualTo("credit_deduct");
        assertThat(result.getCreditDeductValue()).isEqualTo(10);
        assertThat(result.getReason()).isEqualTo("违规操作");
        assertThat(result.getAdminId()).isEqualTo(ADMIN_ID);
        assertThat(result.getRelatedReportId()).isEqualTo(REPORT_ID);
        assertThat(result.getStatus()).isEqualTo("active");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(penaltyMapper).insert(any(Penalty.class));
        verify(creditChangeMapper).insert(any(CreditChange.class));
        verify(userMapper).updateCreditScore(USER_ID, -10);
        // credit_deduct 分支：不进入 function_limit 副作用
        verify(userMapper, never()).selectById(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("createPenalty: 正常创建function_limit处罚")
    void createPenalty_functionLimit_success() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        User user = new User();
        user.setId(USER_ID);
        user.setStatus("active");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("function_limit");
        dto.setReason("恶意举报");

        Penalty result = service.createPenalty(ADMIN_ID, dto);

        assertThat(result.getType()).isEqualTo("function_limit");
        assertThat(result.getCreditDeductValue()).isNull();
        assertThat(result.getReason()).isEqualTo("恶意举报");
        assertThat(result.getStatus()).isEqualTo("active");
        verify(userMapper).selectById(USER_ID);
        verify(userMapper).updateById(any(User.class));
        // function_limit 分支：不进入 credit_deduct 副作用
        verify(creditChangeMapper, never()).insert(any(CreditChange.class));
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("createPenalty: 非 credit_deduct/function_limit 类型仅落库无副作用")
    void createPenalty_unknownType_noSideEffects() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("legacy_type");
        dto.setReason("历史数据兼容");

        Penalty result = service.createPenalty(ADMIN_ID, dto);

        assertThat(result.getType()).isEqualTo("legacy_type");
        assertThat(result.getStatus()).isEqualTo("active");
        verify(penaltyMapper).insert(any(Penalty.class));
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
        verify(userMapper, never()).selectById(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("createPenalty: credit_deduct缺少扣分值抛出异常")
    void createPenalty_creditDeductMissingValue_throws() {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setReason("违规操作");
        // 未设置creditDeductValue

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit_deduct类型的处罚必须指定有效的扣分值");

        verify(penaltyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createPenalty: credit_deduct扣分值为0抛出异常")
    void createPenalty_creditDeductZeroValue_throws() {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(0);
        dto.setReason("违规操作");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit_deduct类型的处罚必须指定有效的扣分值");

        verify(penaltyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createPenalty: credit_deduct扣分值为负数抛出异常")
    void createPenalty_creditDeductNegativeValue_throws() {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(-5);
        dto.setReason("违规操作");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit_deduct类型的处罚必须指定有效的扣分值");

        verify(penaltyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createPenalty: credit_deduct副作用执行失败应回滚")
    void createPenalty_creditDeduct_sideEffectFail_throws() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(0); // 插入失败

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规操作");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("写入 credit_change 流水失败");
    }

    @Test
    @DisplayName("createPenalty: credit_deduct更新credit_score失败应回滚")
    void createPenalty_creditDeduct_updateScoreFail_throws() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(0); // 更新失败

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规操作");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("更新 user.credit_score 失败");
    }

    @Test
    @DisplayName("createPenalty: function_limit用户不存在抛出异常")
    void createPenalty_functionLimit_userNotFound_throws() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("function_limit");
        dto.setReason("恶意举报");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("createPenalty: function_limit更新用户状态失败应回滚")
    void createPenalty_functionLimit_updateFail_throws() {
        when(penaltyMapper.insert(any(Penalty.class))).thenReturn(1);
        User user = new User();
        user.setId(USER_ID);
        user.setStatus("active");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(0); // 更新失败

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("function_limit");
        dto.setReason("恶意举报");

        assertThatThrownBy(() -> service.createPenalty(ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("更新 user.status 失败");
    }

    // ==================== revokePenalty ====================

    @Test
    @DisplayName("revokePenalty: 正常撤销credit_deduct处罚")
    void revokePenalty_creditDeduct_success() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activeCreditPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过，撤销处罚");

        Penalty result = service.revokePenalty(PENALTY_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("revoked");
        assertThat(result.getRevokedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(penaltyMapper).updateById(any(Penalty.class));
        verify(creditChangeMapper).insert(argThat(cc ->
                CreditChangeType.PENALTY_RESTORE.equals(cc.getChangeType())
                        && cc.getChangeValue() == 10));
        verify(userMapper).updateCreditScore(USER_ID, 10); // 恢复10分
        // credit_deduct 撤销：不进入 function_limit 解封逻辑
        verify(penaltyMapper, never()).countActiveFunctionLimitByUserId(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("revokePenalty: 正常撤销function_limit处罚（无其它生效处罚时解封）")
    void revokePenalty_functionLimit_success() {
        when(penaltyMapper.selectById(2L)).thenReturn(activeFunctionPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(penaltyMapper.countActiveFunctionLimitByUserId(USER_ID)).thenReturn(0);
        User user = new User();
        user.setId(USER_ID);
        user.setStatus("banned");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        Penalty result = service.revokePenalty(2L, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("revoked");
        verify(penaltyMapper).countActiveFunctionLimitByUserId(USER_ID);
        verify(userMapper).selectById(USER_ID);
        verify(userMapper).updateById(any(User.class));
        // function_limit 撤销：不进入 credit_deduct 恢复流水
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("revokePenalty: 仍有其它active function_limit时不解封")
    void revokePenalty_functionLimit_otherActiveRemains_staysBanned() {
        when(penaltyMapper.selectById(2L)).thenReturn(activeFunctionPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(penaltyMapper.countActiveFunctionLimitByUserId(USER_ID)).thenReturn(1);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("仅撤销其中一条");

        Penalty result = service.revokePenalty(2L, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("revoked");
        verify(penaltyMapper).countActiveFunctionLimitByUserId(USER_ID);
        verify(userMapper, never()).selectById(any());
        verify(userMapper, never()).updateById(any(User.class));
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("revokePenalty: 非 credit_deduct/function_limit 类型仅改状态无副作用")
    void revokePenalty_unknownType_revokesOnlyNoSideEffects() {
        Penalty legacyPenalty = new Penalty();
        legacyPenalty.setId(4L);
        legacyPenalty.setUserId(USER_ID);
        legacyPenalty.setType("legacy_type");
        legacyPenalty.setReason("历史处罚");
        legacyPenalty.setAdminId(ADMIN_ID);
        legacyPenalty.setStatus("active");
        legacyPenalty.setCreatedAt(LocalDateTime.now());
        legacyPenalty.setUpdatedAt(LocalDateTime.now());

        when(penaltyMapper.selectById(4L)).thenReturn(legacyPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("纠正历史记录");

        Penalty result = service.revokePenalty(4L, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("revoked");
        verify(penaltyMapper).updateById(any(Penalty.class));
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
        verify(penaltyMapper, never()).countActiveFunctionLimitByUserId(any());
        verify(userMapper, never()).selectById(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("revokePenalty: 处罚不存在抛出异常")
    void revokePenalty_notFound_throws() {
        when(penaltyMapper.selectById(999L)).thenReturn(null);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        assertThatThrownBy(() -> service.revokePenalty(999L, ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("处罚记录不存在");
    }

    @Test
    @DisplayName("revokePenalty: 已撤销的处罚重复撤销抛出异常")
    void revokePenalty_alreadyRevoked_throws() {
        when(penaltyMapper.selectById(3L)).thenReturn(revokedPenalty);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("重复撤销");

        assertThatThrownBy(() -> service.revokePenalty(3L, ADMIN_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("处罚已撤销，无法重复撤销");
    }

    @Test
    @DisplayName("revokePenalty: credit_deduct写入恢复流水失败应回滚")
    void revokePenalty_creditDeduct_insertFail_throws() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activeCreditPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(0); // 插入失败

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        assertThatThrownBy(() -> service.revokePenalty(PENALTY_ID, ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("写入 credit_change 恢复流水失败");
    }

    @Test
    @DisplayName("revokePenalty: credit_deduct恢复credit_score失败应回滚")
    void revokePenalty_creditDeduct_restoreScoreFail_throws() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activeCreditPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(0); // 恢复失败

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        assertThatThrownBy(() -> service.revokePenalty(PENALTY_ID, ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("恢复 user.credit_score 失败");
    }

    @Test
    @DisplayName("revokePenalty: function_limit用户不存在抛出异常")
    void revokePenalty_functionLimit_userNotFound_throws() {
        when(penaltyMapper.selectById(2L)).thenReturn(activeFunctionPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(penaltyMapper.countActiveFunctionLimitByUserId(USER_ID)).thenReturn(0);
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        assertThatThrownBy(() -> service.revokePenalty(2L, ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("revokePenalty: function_limit恢复用户状态失败应回滚")
    void revokePenalty_functionLimit_restoreFail_throws() {
        when(penaltyMapper.selectById(2L)).thenReturn(activeFunctionPenalty);
        when(penaltyMapper.updateById(any(Penalty.class))).thenReturn(1);
        when(penaltyMapper.countActiveFunctionLimitByUserId(USER_ID)).thenReturn(0);
        User user = new User();
        user.setId(USER_ID);
        user.setStatus("banned");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(0); // 恢复失败

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        assertThatThrownBy(() -> service.revokePenalty(2L, ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("恢复 user.status 失败");
    }

    // ==================== getUserPenalties ====================

    @Test
    @DisplayName("getUserPenalties: 返回用户的所有处罚记录")
    void getUserPenalties_success() {
        when(penaltyMapper.selectByUserId(USER_ID))
                .thenReturn(Arrays.asList(activeCreditPenalty, activeFunctionPenalty, revokedPenalty));

        List<Penalty> result = service.getUserPenalties(USER_ID);

        assertThat(result).hasSize(3);
        verify(penaltyMapper).selectByUserId(USER_ID);
    }

    @Test
    @DisplayName("getUserPenalties: 无处罚记录返回空列表")
    void getUserPenalties_empty() {
        when(penaltyMapper.selectByUserId(USER_ID))
                .thenReturn(Collections.emptyList());

        List<Penalty> result = service.getUserPenalties(USER_ID);

        assertThat(result).isEmpty();
    }

    // ==================== getUserActivePenalties ====================

    @Test
    @DisplayName("getUserActivePenalties: 返回用户生效中的处罚")
    void getUserActivePenalties_success() {
        when(penaltyMapper.selectActiveByUserId(USER_ID))
                .thenReturn(Arrays.asList(activeCreditPenalty, activeFunctionPenalty));

        List<Penalty> result = service.getUserActivePenalties(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
        assertThat(result.get(1).getStatus()).isEqualTo("active");
    }

    // ==================== getPenaltyList ====================

    @Test
    @DisplayName("getPenaltyList: 查询全部处罚")
    void getPenaltyList_all() {
        when(penaltyMapper.selectList(any())).thenReturn(Arrays.asList(activeCreditPenalty, revokedPenalty));

        List<Penalty> result = service.getPenaltyList(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getPenaltyList: 按状态筛选active")
    void getPenaltyList_byStatusActive() {
        when(penaltyMapper.selectByStatus("active")).thenReturn(Arrays.asList(activeCreditPenalty, activeFunctionPenalty));

        List<Penalty> result = service.getPenaltyList("active", null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getPenaltyList: 按类型筛选credit_deduct")
    void getPenaltyList_byType() {
        when(penaltyMapper.selectByType("credit_deduct")).thenReturn(Arrays.asList(activeCreditPenalty, revokedPenalty));

        List<Penalty> result = service.getPenaltyList(null, "credit_deduct");

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getPenaltyList: 同时按状态和类型筛选")
    void getPenaltyList_byStatusAndType() {
        when(penaltyMapper.selectList(any())).thenReturn(Arrays.asList(activeCreditPenalty));

        List<Penalty> result = service.getPenaltyList("active", "credit_deduct");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
        assertThat(result.get(0).getType()).isEqualTo("credit_deduct");
    }

    @Test
    @DisplayName("getPenaltyList: 空字符串状态视为无筛选")
    void getPenaltyList_emptyStatus() {
        when(penaltyMapper.selectList(any())).thenReturn(Arrays.asList(activeCreditPenalty));

        List<Penalty> result = service.getPenaltyList("", null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPenaltyList: 空字符串类型视为无筛选")
    void getPenaltyList_emptyType() {
        when(penaltyMapper.selectList(any())).thenReturn(Arrays.asList(activeCreditPenalty));

        List<Penalty> result = service.getPenaltyList(null, "");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPenaltyList: 按状态筛选revoked")
    void getPenaltyList_byStatusRevoked() {
        when(penaltyMapper.selectByStatus("revoked")).thenReturn(Arrays.asList(revokedPenalty));

        List<Penalty> result = service.getPenaltyList("revoked", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("revoked");
    }

    @Test
    @DisplayName("getPenaltyList: status有值但type为空字符串时按status筛选")
    void getPenaltyList_statusWithEmptyType() {
        when(penaltyMapper.selectByStatus("active")).thenReturn(Arrays.asList(activeCreditPenalty, activeFunctionPenalty));

        List<Penalty> result = service.getPenaltyList("active", "");

        assertThat(result).hasSize(2);
        verify(penaltyMapper).selectByStatus("active");
    }

    // ==================== getPenaltyById ====================

    @Test
    @DisplayName("getPenaltyById: 查询存在的处罚")
    void getPenaltyById_exists() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activeCreditPenalty);

        Penalty result = service.getPenaltyById(PENALTY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PENALTY_ID);
    }

    @Test
    @DisplayName("getPenaltyById: 查询不存在的处罚返回null")
    void getPenaltyById_notExists() {
        when(penaltyMapper.selectById(999L)).thenReturn(null);

        Penalty result = service.getPenaltyById(999L);

        assertThat(result).isNull();
    }
}
