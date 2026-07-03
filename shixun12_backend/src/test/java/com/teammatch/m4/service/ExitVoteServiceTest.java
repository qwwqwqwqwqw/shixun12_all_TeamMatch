package com.teammatch.m4.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.teammatch.m4.dto.ExitVoteCreateDTO;
import com.teammatch.m4.dto.ExitVoteSubmitDTO;
import com.teammatch.m4.dto.ExitVoteVO;
import com.teammatch.m4.entity.ExitVote;
import com.teammatch.m4.entity.ExitVoteRecord;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.mapper.M4ExitVoteMapper;
import com.teammatch.m4.mapper.M4ExitVoteRecordMapper;
import com.teammatch.m4.mapper.M4TeamMemberMapper;
import com.teammatch.m4.service.impl.ExitVoteServiceImpl;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * ExitVoteService 单元测试
 * 覆盖 T-136 selfExit / T-137 initiateVote / T-139 submitVote / T-140/T-141 closeVote
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("退出流程服务测试")
class ExitVoteServiceTest {

    @Mock
    private M4ExitVoteMapper exitVoteMapper;

    @Mock
    private M4ExitVoteRecordMapper exitVoteRecordMapper;

    @Mock
    private TeamMemberService teamMemberService;

    @Mock
    private ProjectService projectService;

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private M4TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private ExitVoteServiceImpl service;

    private static final Long PROJECT_ID = 1L;
    private static final Long LEADER_ID  = 10L;
    private static final Long MEMBER_ID  = 20L;
    private static final Long VOTE_ID    = 100L;

    private TeamMember leaderMember;
    private TeamMember regularMember;
    private ExitVote openVote;
    private Project inProgressProject;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 字段不会被 @InjectMocks 自动注入（在父类），需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", exitVoteMapper);

        leaderMember = new TeamMember();
        leaderMember.setId(1L);
        leaderMember.setProjectId(PROJECT_ID);
        leaderMember.setUserId(LEADER_ID);
        leaderMember.setRole("leader");
        leaderMember.setStatus("active");

        regularMember = new TeamMember();
        regularMember.setId(2L);
        regularMember.setProjectId(PROJECT_ID);
        regularMember.setUserId(MEMBER_ID);
        regularMember.setRole("member");
        regularMember.setStatus("active");

        openVote = new ExitVote();
        openVote.setId(VOTE_ID);
        openVote.setProjectId(PROJECT_ID);
        openVote.setInitiatorId(LEADER_ID);
        openVote.setTargetUserId(MEMBER_ID);
        openVote.setStatus("voting");
        openVote.setAgreeCount(0);
        openVote.setDisagreeCount(0);
        openVote.setTotalVoters(3);

        inProgressProject = new Project();
        inProgressProject.setId(PROJECT_ID);
        inProgressProject.setStatus("in_progress");
    }

    // ================================================================ T-136
    @Test
    @DisplayName("T-136 selfExit: 普通成员正常退出")
    void selfExit_normalMember_success() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.selfExit(PROJECT_ID, MEMBER_ID));

        verify(teamMemberMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("T-136 selfExit: 非项目成员退出应抛异常")
    void selfExit_notMember_throws() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.selfExit(PROJECT_ID, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("活跃成员");
    }

    @Test
    @DisplayName("T-136 selfExit: 队长不能主动退出")
    void selfExit_leader_throws() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(leaderMember);

        assertThatThrownBy(() -> service.selfExit(PROJECT_ID, LEADER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("队长");
    }

    // ================================================================ T-137
    @Test
    @DisplayName("T-137 initiateVote: 项目不是 in_progress 应抛异常")
    void initiateVote_notInProgress_throws() {
        Project recruitingProject = new Project();
        recruitingProject.setId(PROJECT_ID);
        recruitingProject.setStatus("recruiting");
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("进行中");
    }

    @Test
    @DisplayName("T-137 initiateVote: 队长对普通成员正常发起投票")
    void initiateVote_success() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)   // 第一次：校验发起人是队长
                .thenReturn(regularMember); // 第二次：校验目标成员 active
        when(exitVoteMapper.selectCount(any())).thenReturn(0L); // 无重复 open 投票
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L); // 其他成员数
        when(exitVoteMapper.insert(any())).thenReturn(1);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);
        dto.setReason("长期不活跃");
        dto.setPenaltyLevel("negotiated");

        ExitVoteVO result = service.initiateVote(PROJECT_ID, dto);

        assertThat(result.getStatus()).isEqualTo("voting");
        assertThat(result.getTotalVoters()).isEqualTo(2);
    }

    @Test
    @DisplayName("T-137 initiateVote: 非队长发起应抛异常")
    void initiateVote_notLeader_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(MEMBER_ID);
        dto.setTargetUserId(LEADER_ID);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("T-137 initiateVote: 已有 open 投票时重复发起应抛异常")
    void initiateVote_duplicate_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)
                .thenReturn(regularMember);
        when(exitVoteMapper.selectCount(any())).thenReturn(1L); // 已有进行中

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("进行中的退出投票");
    }

    // ================================================================ T-139
    @Test
    @DisplayName("T-139 submitVote: 正常投赞成票，agreeCount 加 1")
    void submitVote_agree_success() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(leaderMember); // voter 是 active member
        when(exitVoteRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(exitVoteRecordMapper.insert(any())).thenReturn(1);
        when(exitVoteMapper.updateById(any())).thenReturn(1);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        assertThatNoException().isThrownBy(() -> service.submitVote(VOTE_ID, dto));

        verify(exitVoteMapper).updateById(argThat(v -> v.getAgreeCount() == 1));
    }

    @Test
    @DisplayName("T-139 submitVote: 重复投票应抛 DUPLICATE_VOTE")
    void submitVote_duplicate_throws() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(leaderMember); // voter 是 active member
        when(exitVoteRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DUPLICATE_VOTE");
    }

    @Test
    @DisplayName("T-139 submitVote: 投票人不是 active member 应抛异常")
    void submitVote_voterNotActiveMember_throws() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null); // 不是成员

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(999L); // 非成员
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("活跃成员");
    }

    @Test
    @DisplayName("T-139 submitVote: 目标成员不能给自己投票")
    void submitVote_targetVotesSelf_throws() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(MEMBER_ID); // MEMBER_ID == targetUserId
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("目标成员");
    }

    @Test
    @DisplayName("T-139 submitVote: 投票已关闭应抛异常")
    void submitVote_voteClosed_throws() {
        openVote.setStatus("closed"); // 已关闭
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("关闭");
    }

    @Test
    @DisplayName("T-139 submitVote: deadline 已过, 自动懒关闭, 后续投票抛已关闭异常")
    void submitVote_deadlinePassed_autoClose() {
        openVote.setDeadlineAt(LocalDateTime.now().minusHours(1)); // 已超时
        openVote.setAgreeCount(2);
        openVote.setDisagreeCount(0);
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.update(isNull(), any())).thenReturn(1); // lazyClose CAS
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        // deadline 过后 lazyClose 自动关闭，投票方法应抛 已关闭
        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已关闭");
        // 自动关闭应更新投票和成员
        verify(exitVoteMapper).update(isNull(), any());
        verify(teamMemberMapper).update(isNull(), any(UpdateWrapper.class));
    }

    // ================================================================ T-140 / T-141
    @Test
    @DisplayName("T-140 closeVote: 赞成票多，结果为 approved，成员被踢出")
    void closeVote_approved() {
        openVote.setAgreeCount(3);
        openVote.setDisagreeCount(1);
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        ExitVoteVO result = service.closeVote(VOTE_ID, LEADER_ID);

        assertThat(result.getStatus()).isEqualTo("closed");
        assertThat(result.getResult()).isEqualTo("pass");
        verify(teamMemberMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("T-140 closeVote: 反对票多，结果为 rejected，成员保留")
    void closeVote_rejected() {
        openVote.setAgreeCount(1);
        openVote.setDisagreeCount(3);
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);

        ExitVoteVO result = service.closeVote(VOTE_ID, LEADER_ID);

        assertThat(result.getStatus()).isEqualTo("closed");
        assertThat(result.getResult()).isEqualTo("reject");
        verify(teamMemberMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("T-141 closeVote: 幂等——已关闭的投票重复调用直接返回，不修改数据")
    void closeVote_idempotent() {
        openVote.setStatus("closed"); // 已关闭
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);

        ExitVoteVO result = service.closeVote(VOTE_ID, LEADER_ID);

        assertThat(result.getStatus()).isEqualTo("closed");
        verify(exitVoteMapper, never()).updateById(any());
        verify(teamMemberMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("T-141 closeVote: 非发起人尝试关闭应抛异常")
    void closeVote_notInitiator_throws() {
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);

        assertThatThrownBy(() -> service.closeVote(VOTE_ID, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("发起人");
    }

    // ================================================================ T-136 T-146 信誉扣分验证
    @Test
    @DisplayName("T-146 selfExit: 正常退出后应写 credit_change -10 并更新 credit_score")
    void selfExit_writesCreditChange() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        service.selfExit(PROJECT_ID, MEMBER_ID);

        verify(creditChangeMapper).insert(argThat(cc ->
                cc.getChangeValue() == -10
                && "self_exit".equals(cc.getChangeType())
                && cc.getEffective()
                && MEMBER_ID.equals(cc.getUserId())
        ));
        verify(userMapper).updateCreditScore(MEMBER_ID, -10);
    }

    // ================================================================ T-137 initiateVote 更多分支
    @Test
    @DisplayName("T-137 initiateVote: 项目不存在应抛异常")
    void initiateVote_nullProject_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(null);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("项目不存在");
    }

    @Test
    @DisplayName("T-137 initiateVote: 发起人不是活跃成员（null）应抛异常")
    void initiateVote_nullInitiator_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null); // 发起人非成员

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(999L);
        dto.setTargetUserId(MEMBER_ID);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("T-137 initiateVote: 目标成员不存在应抛异常")
    void initiateVote_nullTarget_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)  // 发起人是队长
                .thenReturn(null);         // 目标成员不存在

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(999L);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("活跃成员");
    }

    @Test
    @DisplayName("T-137 initiateVote: 对队长发起投票应抛异常")
    void initiateVote_targetIsLeader_throws() {
        TeamMember anotherLeader = new TeamMember();
        anotherLeader.setId(3L);
        anotherLeader.setProjectId(PROJECT_ID);
        anotherLeader.setUserId(30L);
        anotherLeader.setRole("leader");
        anotherLeader.setStatus("active");

        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)   // 发起人
                .thenReturn(anotherLeader); // 目标是队长

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(30L);

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("T-137 initiateVote: 非法 penaltyLevel 应抛 INVALID_PENALTY_LEVEL")
    void initiateVote_invalidPenaltyLevel_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)
                .thenReturn(regularMember);
        when(exitVoteMapper.selectCount(any())).thenReturn(0L);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);
        dto.setPenaltyLevel("minor"); // 非法值

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("INVALID_PENALTY_LEVEL");
    }

    @Test
    @DisplayName("T-137 initiateVote: 缺失 penaltyLevel 应抛 INVALID_PENALTY_LEVEL")
    void initiateVote_missingPenaltyLevel_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(leaderMember)
                .thenReturn(regularMember);
        when(exitVoteMapper.selectCount(any())).thenReturn(0L);

        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(LEADER_ID);
        dto.setTargetUserId(MEMBER_ID);
        // 不设 penaltyLevel

        assertThatThrownBy(() -> service.initiateVote(PROJECT_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("INVALID_PENALTY_LEVEL");
    }

    // ================================================================ T-138 getVoteDetail 分支
    @Test
    @DisplayName("T-138 getVoteDetail: 投票不存在应抛异常")
    void getVoteDetail_notFound_throws() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getVoteDetail(VOTE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("T-138 getVoteDetail: deadline 已过时返回自动关闭后的投票")
    void getVoteDetail_deadlinePassedAutoClose() {
        openVote.setDeadlineAt(LocalDateTime.now().minusHours(2));
        openVote.setAgreeCount(2);
        openVote.setDisagreeCount(1);
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.update(isNull(), any())).thenReturn(1); // lazyClose CAS
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        ExitVoteVO result = service.getVoteDetail(VOTE_ID);

        assertThat(result.getStatus()).isEqualTo("closed");
        assertThat(result.getResult()).isEqualTo("pass");
    }

    // ================================================================ T-139 submitVote 更多分支
    @Test
    @DisplayName("T-139 submitVote: 投票不存在应抛异常")
    void submitVote_notFound_throws() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(null);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("T-139 submitVote: 投反对票时 disagreeCount 加 1")
    void submitVote_disagree_success() {
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(leaderMember);
        when(exitVoteRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(exitVoteRecordMapper.insert(any())).thenReturn(1);
        when(exitVoteMapper.updateById(any())).thenReturn(1);

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("disagree");

        assertThatNoException().isThrownBy(() -> service.submitVote(VOTE_ID, dto));

        verify(exitVoteMapper).updateById(argThat(v -> v.getDisagreeCount() == 1 && v.getAgreeCount() == 0));
    }

    @Test
    @DisplayName("T-139 submitVote: deadline 过后懒关闭结果为 reject 时应抛已关闭异常")
    void submitVote_deadlinePassed_rejectAutoClose() {
        openVote.setDeadlineAt(LocalDateTime.now().minusHours(1));
        openVote.setAgreeCount(0);
        openVote.setDisagreeCount(2); // 反对多
        when(exitVoteMapper.selectById(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.update(isNull(), any())).thenReturn(1); // lazyClose CAS
        // reject 时不调用 executePassedExitSideEffects

        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(LEADER_ID);
        dto.setChoice("agree");

        assertThatThrownBy(() -> service.submitVote(VOTE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已关闭");
        verify(exitVoteMapper).update(isNull(), any());
        verify(teamMemberMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    // ================================================================ T-140/T-141 closeVote 更多分支
    @Test
    @DisplayName("T-140 closeVote: 投票不存在应抛异常")
    void closeVote_notFound_throws() {
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.closeVote(VOTE_ID, LEADER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("T-140 closeVote: pass 但目标成员已不在 active 列表时，不更新成员且不写信誉流水")
    void closeVote_approved_noActiveMember() {
        openVote.setAgreeCount(3);
        openVote.setDisagreeCount(1);
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0); // CAS 失败

        ExitVoteVO result = service.closeVote(VOTE_ID, LEADER_ID);

        assertThat(result.getResult()).isEqualTo("pass");
        verify(creditChangeMapper, never()).insert(any());    // 不产生额外扣分流水
        verify(userMapper, never()).updateCreditScore(any(), anyInt());
    }

    @Test
    @DisplayName("T-146 closeVote_approved: 通过时应写 credit_change -10 并更新 credit_score")
    void closeVote_approved_writesCreditChange() {
        openVote.setAgreeCount(3);
        openVote.setDisagreeCount(1);
        openVote.setPenaltyLevel("malicious"); // 恶意退出，扣 -10
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        service.closeVote(VOTE_ID, LEADER_ID);

        verify(creditChangeMapper).insert(argThat(cc ->
                cc.getChangeValue() == -10
                && "exit_vote".equals(cc.getChangeType())
                && cc.getEffective()
                && MEMBER_ID.equals(cc.getUserId())
        ));
        verify(userMapper).updateCreditScore(MEMBER_ID, -10);
    }

    @Test
    @DisplayName("T-146 closeVote_approved: penaltyLevel=negotiated 时扣 -5")
    void closeVote_approved_negotiated_deductsFive() {
        openVote.setAgreeCount(3);
        openVote.setDisagreeCount(1);
        openVote.setPenaltyLevel("negotiated");
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        service.closeVote(VOTE_ID, LEADER_ID);

        verify(creditChangeMapper).insert(argThat(cc -> cc.getChangeValue() == -5));
        verify(userMapper).updateCreditScore(MEMBER_ID, -5);
    }

    @Test
    @DisplayName("T-146 closeVote_rejected: 结果为 reject 时不应扣减信誉分")
    void closeVote_rejected_noCreditChange() {
        openVote.setAgreeCount(1);
        openVote.setDisagreeCount(3); // 反对多 → reject
        when(exitVoteMapper.selectByIdForUpdate(VOTE_ID)).thenReturn(openVote);
        when(exitVoteMapper.updateById(any())).thenReturn(1);

        service.closeVote(VOTE_ID, LEADER_ID);

        verify(teamMemberMapper, never()).update(any(), any(UpdateWrapper.class));
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(any(), anyInt());
    }

    @Test
    @DisplayName("T-146 selfExit: 招募中项目退出，不写信誉流水")
    void selfExit_recruiting_noCreditChange() {
        Project recruitingProject = new Project();
        recruitingProject.setId(PROJECT_ID);
        recruitingProject.setStatus("recruiting");
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject);

        assertThatNoException().isThrownBy(() -> service.selfExit(PROJECT_ID, MEMBER_ID));

        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(any(), anyInt());
    }

    @Test
    @DisplayName("T-146 selfExit: 进行中项目退出，同事务关闭进行中的投票")
    void selfExit_closesActiveExitVote() {
        ExitVote activeVote = new ExitVote();
        activeVote.setId(200L);
        activeVote.setStatus("voting");

        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);
        when(exitVoteMapper.selectList(any())).thenReturn(List.of(activeVote));
        when(exitVoteMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(creditChangeMapper.insert(any())).thenReturn(1);
        when(userMapper.updateCreditScore(any(), anyInt())).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.selfExit(PROJECT_ID, MEMBER_ID));

        // 验证 active vote 被 CAS 关闭
        verify(exitVoteMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("T-146 selfExit: CAS 成员更新影响 0 行（并发抢占）应抛异常回滚")
    void selfExit_concurrentMemberRace_throws() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0); // CAS 失败
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);

        assertThatThrownBy(() -> service.selfExit(PROJECT_ID, MEMBER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TEAM_VOTE_CONFLICT");

        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(any(), anyInt());
    }

    @Test
    @DisplayName("T-146 selfExit: CAS 成员更新异常影响多行时应抛异常回滚")
    void selfExit_memberCasAffectsMultipleRows_throws() {
        when(teamMemberService.getOne(any(LambdaQueryWrapper.class))).thenReturn(regularMember);
        when(teamMemberMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(2); // 理论上受唯一约束保护，异常多行也应拦截
        when(projectService.getById(PROJECT_ID)).thenReturn(inProgressProject);

        assertThatThrownBy(() -> service.selfExit(PROJECT_ID, MEMBER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TEAM_VOTE_CONFLICT");

        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(any(), anyInt());
    }
}
