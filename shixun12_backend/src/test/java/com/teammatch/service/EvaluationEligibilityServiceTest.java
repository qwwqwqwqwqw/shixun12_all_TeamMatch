package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.EvaluatableMemberDTO;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.Project;
import com.teammatch.entity.TeamMember;
import com.teammatch.entity.User;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.mapper.TeamMemberMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.storage.OssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EvaluationEligibilityService 单元测试
 * 测试 M5-1A/B/C 三层业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("互评资格判断服务测试")
class EvaluationEligibilityServiceTest {

    // ==================== Mock 对象 ====================

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OssService ossService;

    @InjectMocks
    private EvaluationEligibilityService service;

    // ==================== 测试数据 ====================

    private Long userId;
    private Long projectId;
    private Project project;
    private TeamMember teamMember;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        userId = 1001L;
        projectId = 2001L;

        // 准备正常的项目数据
        project = new Project();
        project.setId(projectId);
        project.setStatus("ended");
        project.setEvalDeadline(LocalDateTime.now().plusDays(7));

        // OssService mock: resolveAvatarUrl 原样返回（单元测试不关注签名逻辑）
        lenient().when(ossService.resolveAvatarUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 准备正常的成员数据
        teamMember = new TeamMember();
        teamMember.setUserId(userId);
        teamMember.setProjectId(projectId);
        teamMember.setStatus("active");
    }

    // ==================== M5-1A：项目级互评入口判断 ====================
    // 等价类划分：
    // 1. 项目不存在 → PROJECT_NOT_FOUND
    // 2. 用户不是成员 → NOT_PROJECT_MEMBER
    // 3. 项目状态不是 ended → PROJECT_NOT_ENDED
    // 4. 超过互评截止时间 → EVAL_WINDOW_CLOSED
    // 5. 所有检查通过 → 00000

    @Test
    @DisplayName("M5-1A：所有检查通过，允许进入互评")
    void testCheckProjectEligibility_Success() {
        // Given：准备 Mock 数据
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When：调用方法
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then：验证结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.SUCCESS.getMessage());

        // 验证 Mock 方法被调用
        verify(projectMapper).selectById(projectId);
        verify(teamMemberMapper).findActiveMember(userId, projectId);
    }

    @Test
    @DisplayName("M5-1A：项目不存在")
    void testCheckProjectEligibility_ProjectNotFound() {
        // Given：项目不存在
        when(projectMapper.selectById(projectId)).thenReturn(null);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PROJECT_NOT_FOUND.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.PROJECT_NOT_FOUND.getMessage());
        assertThat(result.getData()).isNull();

        // 验证后续方法没有被调用（短路）
        verify(projectMapper).selectById(projectId);
        verify(teamMemberMapper, never()).findActiveMember(any(), any());
    }

    @Test
    @DisplayName("M5-1A：用户不是项目成员")
    void testCheckProjectEligibility_NotProjectMember() {
        // Given
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(null);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.NOT_PROJECT_MEMBER.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.NOT_PROJECT_MEMBER.getMessage());

        // 验证调用顺序
        verify(projectMapper).selectById(projectId);
        verify(teamMemberMapper).findActiveMember(userId, projectId);
    }

    @Test
    @DisplayName("M5-1A：项目尚未结束")
    void testCheckProjectEligibility_ProjectNotEnded() {
        // Given：项目状态是 in_progress
        project.setStatus("in_progress");
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PROJECT_NOT_ENDED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.PROJECT_NOT_ENDED.getMessage());
        verify(projectMapper, never()).updateStatusToEvalClosed(any());
    }

    @Test
    @DisplayName("M5-1A：互评窗口已关闭（截止时间已过）")
    void testCheckProjectEligibility_EvalWindowClosed() {
        // Given：互评截止时间是昨天
        project.setEvalDeadline(LocalDateTime.now().minusDays(1));
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getMessage());
        verify(projectMapper).updateStatusToEvalClosed(projectId);
    }

    @Test
    @DisplayName("M5-1A：互评窗口已关闭（截止时间为 null）")
    void testCheckProjectEligibility_EvalDeadlineNull() {
        // Given：互评截止时间为 null
        project.setEvalDeadline(null);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getMessage());
        verify(projectMapper, never()).updateStatusToEvalClosed(any());
    }

    @Test
    @DisplayName("M5-1A：项目状态为 eval_closed 时直接短路返回窗口关闭")
    void testCheckProjectEligibility_StatusEvalClosed() {
        // Given：项目已经是 eval_closed 状态
        project.setStatus("eval_closed");
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When
        Result<Boolean> result = service.checkProjectEligibility(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getMessage());
        verify(projectMapper, never()).updateStatusToEvalClosed(any());
    }

    // ==================== M5-1B：待评价成员列表判断 ====================
    // 等价类划分：
    // 1. 项目级资格检查失败 → 返回对应错误码
    // 2. 项目只有自己一个成员 → 返回空列表
    // 3. 项目有多个成员，部分已评价 → 返回列表，标记已评价状态
    // 4. 项目有多个成员，全部已评价 → 返回列表，全部标记为已评价
    // 5. 用户信息不存在 → 显示"未知用户"

    @Test
    @DisplayName("M5-1B：返回可评价成员列表，标记已评价状态")
    void testGetEvaluatableMembers_Success() {
        // Given：准备测试数据
        // 1. 项目级资格检查通过
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // 2. 项目有 4 个 active 成员（包括自己）
        List<TeamMember> activeMembers = Arrays.asList(
            createTeamMember(userId),      // 1001 - 自己
            createTeamMember(1002L),       // 1002 - 张三
            createTeamMember(1003L),       // 1003 - 李四
            createTeamMember(1004L)        // 1004 - 王五
        );
        when(teamMemberMapper.findActiveMembers(projectId)).thenReturn(activeMembers);

        // 3. 已经评价过 1002
        when(evaluationMapper.findEvaluatedTargetIds(userId, projectId))
            .thenReturn(Arrays.asList(1002L));

        // 4. 批量查询用户信息
        List<User> users = Arrays.asList(
            createUser(1002L, "张三", "avatar1.jpg"),
            createUser(1003L, "李四", "avatar2.jpg"),
            createUser(1004L, "王五", "avatar3.jpg")
        );
        when(userMapper.findByIds(Arrays.asList(1002L, 1003L, 1004L)))
            .thenReturn(users);

        // When
        Result<List<EvaluatableMemberDTO>> result =
            service.getEvaluatableMembers(userId, projectId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(3); // 排除自己，剩 3 个

        // 验证第一个成员（张三，已评价）
        EvaluatableMemberDTO member1 = result.getData().get(0);
        assertThat(member1.getUserId()).isEqualTo(1002L);
        assertThat(member1.getNickname()).isEqualTo("张三");
        assertThat(member1.getAvatarUrl()).isEqualTo("avatar1.jpg");
        assertThat(member1.getEvaluated()).isTrue();

        // 验证第二个成员（李四，未评价）
        EvaluatableMemberDTO member2 = result.getData().get(1);
        assertThat(member2.getUserId()).isEqualTo(1003L);
        assertThat(member2.getNickname()).isEqualTo("李四");
        assertThat(member2.getEvaluated()).isFalse();

        // 验证第三个成员（王五，未评价）
        EvaluatableMemberDTO member3 = result.getData().get(2);
        assertThat(member3.getUserId()).isEqualTo(1004L);
        assertThat(member3.getEvaluated()).isFalse();

        // 验证批量查询被调用（性能优化）
        verify(userMapper).findByIds(Arrays.asList(1002L, 1003L, 1004L));
        verify(userMapper, times(1)).findByIds(any()); // 只调用一次
    }

    @Test
    @DisplayName("M5-1B：项目只有自己一个成员，返回空列表")
    void testGetEvaluatableMembers_OnlyOneself() {
        // Given
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // 项目只有自己一个成员
        List<TeamMember> activeMembers = Arrays.asList(createTeamMember(userId));
        when(teamMemberMapper.findActiveMembers(projectId)).thenReturn(activeMembers);

        when(evaluationMapper.findEvaluatedTargetIds(userId, projectId))
            .thenReturn(Collections.emptyList());

        // When
        Result<List<EvaluatableMemberDTO>> result =
            service.getEvaluatableMembers(userId, projectId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty(); // 空列表

        // 验证没有调用用户信息查询（因为没有其他成员，已优化跳过空列表查询）
        verify(userMapper, never()).findByIds(any());
    }

    @Test
    @DisplayName("M5-1B：用户信息不存在，显示未知用户")
    void testGetEvaluatableMembers_UserNotFound() {
        // Given
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        List<TeamMember> activeMembers = Arrays.asList(
            createTeamMember(userId),
            createTeamMember(1002L)
        );
        when(teamMemberMapper.findActiveMembers(projectId)).thenReturn(activeMembers);
        when(evaluationMapper.findEvaluatedTargetIds(userId, projectId))
            .thenReturn(Collections.emptyList());

        // 用户信息查询返回空列表（用户不存在）
        when(userMapper.findByIds(Arrays.asList(1002L)))
            .thenReturn(Collections.emptyList());

        // When
        Result<List<EvaluatableMemberDTO>> result =
            service.getEvaluatableMembers(userId, projectId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getNickname()).isEqualTo("未知用户");
        assertThat(result.getData().get(0).getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("M5-1B：项目级资格检查失败，返回错误码")
    void testGetEvaluatableMembers_ProjectNotEligible() {
        // Given：项目不存在
        when(projectMapper.selectById(projectId)).thenReturn(null);

        // When
        Result<List<EvaluatableMemberDTO>> result =
            service.getEvaluatableMembers(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PROJECT_NOT_FOUND.getCode());

        // 验证后续方法没有被调用（短路）
        verify(teamMemberMapper, never()).findActiveMembers(any());
        verify(evaluationMapper, never()).findEvaluatedTargetIds(any(), any());
        verify(userMapper, never()).findByIds(any());
    }

    @Test
    @DisplayName("M5-1B：互评窗口过期时通过资格检查路径触发项目懒关闭")
    void testGetEvaluatableMembers_EvalWindowClosedLazyClose() {
        // Given
        project.setEvalDeadline(LocalDateTime.now().minusDays(1));
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(teamMemberMapper.findActiveMember(userId, projectId)).thenReturn(teamMember);

        // When
        Result<List<EvaluatableMemberDTO>> result =
            service.getEvaluatableMembers(userId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        verify(projectMapper).updateStatusToEvalClosed(projectId);
        verify(teamMemberMapper, never()).findActiveMembers(any());
        verify(evaluationMapper, never()).findEvaluatedTargetIds(any(), any());
    }

    // ==================== M5-1C：最终提交前资格兜底校验 ====================
    // 等价类划分：
    // 1. 自评（evaluatorId == targetId） → SELF_EVALUATION
    // 2. 重复评价 → ALREADY_EVALUATED
    // 3. 评价人不是成员 → NOT_PROJECT_MEMBER
    // 4. 被评价人不是成员 → TARGET_NOT_PROJECT_MEMBER
    // 5. 项目级资格检查失败 → 返回对应错误码
    // 6. 所有检查通过 → 00000

    @Test
    @DisplayName("M5-1C：所有检查通过，允许提交")
    void testValidateSubmission_Success() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        // 1. 不是自评
        // 2. 没有重复评价
        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        // 3. 评价人是 active 成员
        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(teamMember);

        // 4. 被评价人是 active 成员
        TeamMember targetMember = createTeamMember(targetId);
        when(teamMemberMapper.findActiveMember(targetId, projectId))
            .thenReturn(targetMember);

        // 5. 项目级资格检查通过
        when(projectMapper.selectById(projectId)).thenReturn(project);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNull();

        // 验证所有检查都被执行
        verify(evaluationMapper).findEvaluation(evaluatorId, targetId, projectId);
        // 注意：findActiveMember(evaluatorId, projectId) 被调用 2 次
        // 第 1 次：validateSubmission 直接调用（检查评价人）
        // 第 2 次：checkProjectEligibility 内部调用（项目级资格检查）
        verify(teamMemberMapper, times(2)).findActiveMember(evaluatorId, projectId);
        verify(teamMemberMapper).findActiveMember(targetId, projectId);
        verify(projectMapper).selectById(projectId);
    }

    @Test
    @DisplayName("M5-1C：不允许自评")
    void testValidateSubmission_SelfEvaluation() {
        // Given：评价人和被评价人是同一个人
        Long evaluatorId = 1001L;
        Long targetId = 1001L;

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SELF_EVALUATION.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.SELF_EVALUATION.getMessage());

        // 验证后续检查没有执行（短路）
        verify(evaluationMapper, never()).findEvaluation(any(), any(), any());
    }

    @Test
    @DisplayName("M5-1C：重复评价")
    void testValidateSubmission_DuplicateEvaluation() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        // 已经存在评价记录
        Evaluation existingEvaluation = createEvaluation(evaluatorId, targetId);
        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(existingEvaluation);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ALREADY_EVALUATED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.ALREADY_EVALUATED.getMessage());

        // 验证后续检查没有执行
        verify(teamMemberMapper, never()).findActiveMember(any(), any());
    }

    @Test
    @DisplayName("M5-1C：评价人不是项目成员")
    void testValidateSubmission_EvaluatorNotMember() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        // 评价人不是成员
        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(null);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.NOT_PROJECT_MEMBER.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.NOT_PROJECT_MEMBER.getMessage());
    }

    @Test
    @DisplayName("M5-1C：被评价人不是项目成员")
    void testValidateSubmission_TargetNotMember() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(teamMember);

        // 被评价人不是成员
        when(teamMemberMapper.findActiveMember(targetId, projectId))
            .thenReturn(null);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.TARGET_NOT_PROJECT_MEMBER.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.TARGET_NOT_PROJECT_MEMBER.getMessage());
    }

    @Test
    @DisplayName("M5-1C：项目级资格检查失败（项目不存在）")
    void testValidateSubmission_ProjectNotEligible() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(teamMember);

        TeamMember targetMember = createTeamMember(targetId);
        when(teamMemberMapper.findActiveMember(targetId, projectId))
            .thenReturn(targetMember);

        // 项目不存在
        when(projectMapper.selectById(projectId)).thenReturn(null);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PROJECT_NOT_FOUND.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.PROJECT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("M5-1C：项目级资格检查失败（截止时间为 null）")
    void testValidateSubmission_EvalDeadlineNull() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(teamMember);

        TeamMember targetMember = createTeamMember(targetId);
        when(teamMemberMapper.findActiveMember(targetId, projectId))
            .thenReturn(targetMember);

        // 项目存在但截止时间为 null
        project.setEvalDeadline(null);
        when(projectMapper.selectById(projectId)).thenReturn(project);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getMessage());
        verify(projectMapper, never()).updateStatusToEvalClosed(any());
    }

    @Test
    @DisplayName("M5-1C：提交前兜底检查遇到窗口过期时触发项目懒关闭")
    void testValidateSubmission_EvalWindowClosedLazyClose() {
        // Given
        Long evaluatorId = 1001L;
        Long targetId = 1002L;

        when(evaluationMapper.findEvaluation(evaluatorId, targetId, projectId))
            .thenReturn(null);

        when(teamMemberMapper.findActiveMember(evaluatorId, projectId))
            .thenReturn(teamMember);

        TeamMember targetMember = createTeamMember(targetId);
        when(teamMemberMapper.findActiveMember(targetId, projectId))
            .thenReturn(targetMember);

        project.setEvalDeadline(LocalDateTime.now().minusDays(1));
        when(projectMapper.selectById(projectId)).thenReturn(project);

        // When
        Result<Void> result = service.validateSubmission(evaluatorId, targetId, projectId);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        assertThat(result.getMessage()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getMessage());
        verify(projectMapper).updateStatusToEvalClosed(projectId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的 TeamMember 对象
     */
    private TeamMember createTeamMember(Long userId) {
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setProjectId(projectId);
        member.setStatus("active");
        return member;
    }

    /**
     * 创建测试用的 User 对象
     */
    private User createUser(Long id, String nickname, String avatarUrl) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        return user;
    }

    /**
     * 创建测试用的 Evaluation 对象
     */
    private Evaluation createEvaluation(Long evaluatorId, Long targetId) {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setTargetId(targetId);
        evaluation.setProjectId(projectId);
        return evaluation;
    }
}
