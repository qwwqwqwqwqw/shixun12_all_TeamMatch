package com.teammatch.m3;

import com.teammatch.common.Result;
import com.teammatch.controller.MeController;
import com.teammatch.dto.UserBadgesVO;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.TeamMember;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.TeamMemberMapper;
import com.teammatch.m4.entity.ExitVote;
import com.teammatch.m4.entity.ExitVoteRecord;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.mapper.M4ExitVoteMapper;
import com.teammatch.m4.mapper.M4ExitVoteRecordMapper;
import com.teammatch.entity.Project;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.m4.mapper.M4TeamRequestMapper;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M3 MeController 角标接口单元测试
 */
@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Long USER_ID = 1L;

    @Mock
    private AuthUtil authUtil;

    @Mock
    private M4TeamRequestMapper teamRequestMapper;

    @Mock
    private M4ExitVoteMapper exitVoteMapper;

    @Mock
    private M4ExitVoteRecordMapper exitVoteRecordMapper;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private MeController meController;

    @BeforeEach
    void setUp() {
        when(authUtil.requireUserId(TOKEN)).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("无待处理事项时所有角标应为 0")
    void getBadges_shouldReturnZero_whenNothingPending() {
        // 无待处理邀请
        when(teamRequestMapper.selectCount(any())).thenReturn(0L);
        // 无活跃项目
        when(teamMemberMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<UserBadgesVO> result = meController.getBadges(TOKEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getPendingInvites()).isEqualTo(0);
        assertThat(result.getData().getPendingVotes()).isEqualTo(0);
        assertThat(result.getData().getPendingEvaluations()).isEqualTo(0);
        assertThat(result.getData().getTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("有待处理邀请时应正确计数")
    void getBadges_shouldCountPendingInvites() {
        when(teamRequestMapper.selectCount(any())).thenReturn(3L);
        when(teamMemberMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<UserBadgesVO> result = meController.getBadges(TOKEN);

        assertThat(result.getData().getPendingInvites()).isEqualTo(3);
        assertThat(result.getData().getTotal()).isEqualTo(3);
    }

    @Test
    @DisplayName("有活跃项目和待处理投票时应正确计数")
    void getBadges_shouldCountPendingVotes_whenActiveProject() {
        // 用户在一个活跃项目中
        TeamMember membership = new TeamMember();
        membership.setProjectId(100L);
        membership.setUserId(USER_ID);
        List<TeamMember> memberships = Collections.singletonList(membership);
        when(teamMemberMapper.selectList(any())).thenReturn(memberships);
        when(projectMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 有一个进行中的退出投票
        ExitVote activeVote = new ExitVote();
        activeVote.setId(10L);
        activeVote.setProjectId(100L);
        activeVote.setStatus("voting");
        when(exitVoteMapper.selectList(any())).thenReturn(Collections.singletonList(activeVote));

        // 用户尚未投票
        when(exitVoteRecordMapper.selectCount(any())).thenReturn(0L);

        // 无待处理邀请
        when(teamRequestMapper.selectCount(any())).thenReturn(0L);

        Result<UserBadgesVO> result = meController.getBadges(TOKEN);

        assertThat(result.getData().getPendingVotes()).isEqualTo(1);
        assertThat(result.getData().getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("已投票的不计入待处理投票")
    void getBadges_shouldNotCountVoted() {
        TeamMember membership = new TeamMember();
        membership.setProjectId(100L);
        membership.setUserId(USER_ID);
        when(teamMemberMapper.selectList(any())).thenReturn(Collections.singletonList(membership));
        when(projectMapper.selectList(any())).thenReturn(Collections.emptyList());

        ExitVote activeVote = new ExitVote();
        activeVote.setId(10L);
        activeVote.setProjectId(100L);
        activeVote.setStatus("voting");
        when(exitVoteMapper.selectList(any())).thenReturn(Collections.singletonList(activeVote));

        // 用户已经投过票了
        when(exitVoteRecordMapper.selectCount(any())).thenReturn(1L);

        when(teamRequestMapper.selectCount(any())).thenReturn(0L);

        Result<UserBadgesVO> result = meController.getBadges(TOKEN);

        assertThat(result.getData().getPendingVotes()).isEqualTo(0);
    }

    @Test
    @DisplayName("有已结束项目且有未评成员时应正确计数待互评数")
    void getBadges_shouldCountPendingEvaluations_whenProjectEnded() {
        // 1. 用户在一个活跃项目中
        TeamMember membership = new TeamMember();
        membership.setProjectId(100L);
        membership.setUserId(USER_ID);
        List<TeamMember> memberships = Collections.singletonList(membership);

        // 2. 项目列表中包含该项目且状态为 ended，尚未过期
        Project project = new Project();
        project.setId(100L);
        project.setStatus("ended");
        project.setEvalDeadline(java.time.LocalDateTime.now().plusDays(1));

        // 3. 有 2 个其他活跃成员 (200L 和 300L)
        TeamMember m1 = new TeamMember();
        m1.setProjectId(100L);
        m1.setUserId(200L);
        m1.setStatus("active");
        TeamMember m2 = new TeamMember();
        m2.setProjectId(100L);
        m2.setUserId(300L);
        m2.setStatus("active");

        // mock 成员查询的连续返回值 (第一次查自己的项目，第二次查项目的所有活跃成员)
        when(teamMemberMapper.selectList(any()))
                .thenReturn(memberships)
                .thenReturn(Arrays.asList(m1, m2));

        when(projectMapper.selectList(any())).thenReturn(Collections.singletonList(project));

        // 4. 用户已提交的评价数（只评了 200L，300L 未评）
        Evaluation eval = new Evaluation();
        eval.setProjectId(100L);
        eval.setEvaluatorId(USER_ID);
        eval.setTargetId(200L);
        when(evaluationMapper.selectList(any())).thenReturn(Collections.singletonList(eval));

        when(teamRequestMapper.selectCount(any())).thenReturn(0L);

        Result<UserBadgesVO> result = meController.getBadges(TOKEN);

        assertThat(result.getData().getPendingEvaluations()).isEqualTo(1); // 200L已评，300L未评，应为1
        assertThat(result.getData().getTotal()).isEqualTo(1);
    }
}
