package com.teammatch.m4.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.m4.dto.TeamRequestDTO;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.mapper.M4TeamRequestMapper;
import com.teammatch.m4.service.impl.TeamRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TeamRequestServiceImpl 单元测试
 * 覆盖 T-122 sendRequest(invite) / T-123 sendRequest(apply) /
 *      T-124 acceptRequest / T-129 rejectRequest /
 *      T-130 cancelRequest / T-131 getRequestList
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRequestService 单元测试")
class TeamRequestServiceImplTest {

    @Mock
    private M4TeamRequestMapper teamRequestMapper;

    @Mock
    private ProjectService projectService;

    @Mock
    private TeamMemberService teamMemberService;

    @InjectMocks
    private TeamRequestServiceImpl service;

    private static final Long PROJECT_ID  = 1L;
    private static final Long LEADER_ID   = 10L;
    private static final Long INVITEE_ID  = 20L;
    private static final Long APPLICANT_ID = 30L;
    private static final Long REQUEST_ID  = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", teamRequestMapper);
    }

    // ====================================================================== T-122 invite

    @Test
    @DisplayName("sendRequest invite: 正常流程 → 保存 pending 请求")
    void sendRequest_invite_success() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        // 第一次：active 成员数 2；第二次：leader 数 1；第三次：目标成员数 0
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 1L, 0L);
        when(teamRequestMapper.selectCount(any())).thenReturn(0L); // 无重复
        when(teamRequestMapper.insert(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.sendRequest(inviteDto(), "invite"));

        verify(teamRequestMapper).insert(any(TeamRequest.class));
    }

    @Test
    @DisplayName("sendRequest invite: 项目不存在 → 抛异常")
    void sendRequest_projectNotFound_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.sendRequest(inviteDto(), "invite"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("sendRequest invite: 项目非 recruiting → 抛 PROJECT_NOT_RECRUITING")
    void sendRequest_projectNotRecruiting_throws() {
        Project p = recruitingProject(5);
        p.setStatus("in_progress");
        when(projectService.getById(PROJECT_ID)).thenReturn(p);

        assertThatThrownBy(() -> service.sendRequest(inviteDto(), "invite"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PROJECT_NOT_RECRUITING");
    }

    @Test
    @DisplayName("sendRequest invite: 项目已满员 → 抛 PROJECT_FULL")
    void sendRequest_projectFull_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(5L); // 满员

        assertThatThrownBy(() -> service.sendRequest(inviteDto(), "invite"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PROJECT_FULL");
    }

    @Test
    @DisplayName("sendRequest invite: 发起者不是队长 → 抛异常")
    void sendRequest_invite_notLeader_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        // 第一次：active 成员数 2；第二次：leader 数 0（不是队长）
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 0L);

        assertThatThrownBy(() -> service.sendRequest(inviteDto(), "invite"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("sendRequest invite: 已有 pending 重复请求 → 抛 DUPLICATE_PENDING_REQUEST")
    void sendRequest_invite_duplicatePending_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 1L, 0L);
        when(teamRequestMapper.selectCount(any())).thenReturn(1L); // 有重复

        assertThatThrownBy(() -> service.sendRequest(inviteDto(), "invite"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DUPLICATE_PENDING_REQUEST");
    }

    // ====================================================================== T-123 apply

    @Test
    @DisplayName("sendRequest apply: 正常流程 → 保存 pending 申请")
    void sendRequest_apply_success() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 0L); // active 成员数
        when(teamRequestMapper.selectCount(any())).thenReturn(0L); // 无重复
        when(teamRequestMapper.insert(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.sendRequest(applyDto(), "apply"));

        verify(teamRequestMapper).insert(any(TeamRequest.class));
    }

    @Test
    @DisplayName("sendRequest apply: 已有 pending 重复申请 → 抛 DUPLICATE_PENDING_REQUEST")
    void sendRequest_apply_duplicatePending_throws() {
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 0L);
        when(teamRequestMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.sendRequest(applyDto(), "apply"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DUPLICATE_PENDING_REQUEST");
    }

    // ====================================================================== T-124 acceptRequest

    @Test
    @DisplayName("acceptRequest invite: 被邀请人接受 → 写入被邀请人 team_member，状态改 accepted")
    void acceptRequest_invite_success() {
        TeamRequest req = inviteRequest(); // type=invite, fromUserId=LEADER_ID, toUserId=INVITEE_ID
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 0L); // active < max
        when(teamMemberService.save(any(TeamMember.class))).thenReturn(true);
        when(teamRequestMapper.updateById(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.acceptRequest(REQUEST_ID, INVITEE_ID));

        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(INVITEE_ID); // toUserId 加入
    }

    @Test
    @DisplayName("acceptRequest apply: 队长接受申请 → 写入申请人 team_member，状态改 accepted")
    void acceptRequest_apply_success() {
        TeamRequest req = applyRequest(); // type=apply, fromUserId=APPLICANT_ID, toUserId=LEADER_ID
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        // 第一次：leader count=1；第二次：active count=2；第三次：目标成员数 0
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(1L, 2L, 0L);
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.save(any(TeamMember.class))).thenReturn(true);
        when(teamRequestMapper.updateById(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.acceptRequest(REQUEST_ID, LEADER_ID));

        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(APPLICANT_ID); // fromUserId 加入
    }

    @Test
    @DisplayName("acceptRequest: 请求不存在 → 抛异常")
    void acceptRequest_notFound_throws() {
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("acceptRequest: 请求已处理（非 pending）→ 抛异常")
    void acceptRequest_notPending_throws() {
        TeamRequest req = inviteRequest();
        req.setStatus("accepted");
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已处理");
    }

    @Test
    @DisplayName("acceptRequest invite: 非被邀请人操作 → 抛异常")
    void acceptRequest_invite_wrongOperator_throws() {
        TeamRequest req = inviteRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, 99L)) // 不是 toUserId
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("acceptRequest apply: 非队长操作 → 抛异常")
    void acceptRequest_apply_notLeader_throws() {
        TeamRequest req = applyRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 不是队长

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("acceptRequest: 项目已不在 recruiting 状态 → 抛 PROJECT_NOT_RECRUITING")
    void acceptRequest_projectNotRecruiting_throws() {
        TeamRequest req = inviteRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        Project p = recruitingProject(5);
        p.setStatus("in_progress");
        when(projectService.getById(PROJECT_ID)).thenReturn(p);

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PROJECT_NOT_RECRUITING");
    }

    @Test
    @DisplayName("acceptRequest: 项目已满员（二次校验）→ 抛 PROJECT_FULL")
    void acceptRequest_projectFull_throws() {
        TeamRequest req = inviteRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(projectService.getById(PROJECT_ID)).thenReturn(recruitingProject(5));
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(5L); // 满员

        assertThatThrownBy(() -> service.acceptRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PROJECT_FULL");
    }

    // ====================================================================== T-129 rejectRequest

    @Test
    @DisplayName("rejectRequest invite: 被邀请人拒绝 → 状态改 rejected")
    void rejectRequest_invite_success() {
        TeamRequest req = inviteRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(teamRequestMapper.updateById(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.rejectRequest(REQUEST_ID, INVITEE_ID));

        ArgumentCaptor<TeamRequest> captor = ArgumentCaptor.forClass(TeamRequest.class);
        verify(teamRequestMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("rejected");
    }

    @Test
    @DisplayName("rejectRequest apply: 队长拒绝申请 → 状态改 rejected")
    void rejectRequest_apply_success() {
        TeamRequest req = applyRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // is leader
        when(teamRequestMapper.updateById(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.rejectRequest(REQUEST_ID, LEADER_ID));

        ArgumentCaptor<TeamRequest> captor = ArgumentCaptor.forClass(TeamRequest.class);
        verify(teamRequestMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("rejected");
    }

    @Test
    @DisplayName("rejectRequest: 请求不存在 → 抛异常")
    void rejectRequest_notFound_throws() {
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.rejectRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("rejectRequest: 请求已处理 → 抛异常")
    void rejectRequest_notPending_throws() {
        TeamRequest req = inviteRequest();
        req.setStatus("rejected");
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.rejectRequest(REQUEST_ID, INVITEE_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已处理");
    }

    @Test
    @DisplayName("rejectRequest invite: 非被邀请人操作 → 抛异常")
    void rejectRequest_invite_wrongOperator_throws() {
        TeamRequest req = inviteRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.rejectRequest(REQUEST_ID, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("rejectRequest apply: 非队长操作 → 抛异常")
    void rejectRequest_apply_notLeader_throws() {
        TeamRequest req = applyRequest();
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(teamMemberService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.rejectRequest(REQUEST_ID, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("队长");
    }

    // ====================================================================== T-130 cancelRequest

    @Test
    @DisplayName("cancelRequest: 发送方取消 → 状态改 cancelled")
    void cancelRequest_success() {
        TeamRequest req = inviteRequest(); // fromUserId=LEADER_ID
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
        when(teamRequestMapper.updateById(any(TeamRequest.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.cancelRequest(REQUEST_ID, LEADER_ID));

        ArgumentCaptor<TeamRequest> captor = ArgumentCaptor.forClass(TeamRequest.class);
        verify(teamRequestMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("cancelRequest: 请求不存在 → 抛异常")
    void cancelRequest_notFound_throws() {
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelRequest(REQUEST_ID, LEADER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("cancelRequest: 请求已处理 → 抛异常")
    void cancelRequest_notPending_throws() {
        TeamRequest req = inviteRequest();
        req.setStatus("accepted");
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.cancelRequest(REQUEST_ID, LEADER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("无法取消");
    }

    @Test
    @DisplayName("cancelRequest: 非发送方操作 → 抛异常")
    void cancelRequest_notSender_throws() {
        TeamRequest req = inviteRequest(); // fromUserId=LEADER_ID
        when(teamRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

        assertThatThrownBy(() -> service.cancelRequest(REQUEST_ID, INVITEE_ID)) // 不是 fromUserId
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("发送方");
    }

    // ====================================================================== T-131 getRequestList

    @Test
    @DisplayName("getRequestList received: 按 toUserId 查询")
    void getRequestList_received() {
        List<TeamRequest> expected = List.of(inviteRequest());
        when(teamRequestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expected);

        List<TeamRequest> result = service.getRequestList(INVITEE_ID, "received");

        assertThat(result).hasSize(1);
        verify(teamRequestMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getRequestList sent: 按 fromUserId 查询")
    void getRequestList_sent() {
        List<TeamRequest> expected = List.of(inviteRequest());
        when(teamRequestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expected);

        List<TeamRequest> result = service.getRequestList(LEADER_ID, "sent");

        assertThat(result).hasSize(1);
        verify(teamRequestMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ====================================================================== helpers

    private Project recruitingProject(int maxMembers) {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setCreatorId(LEADER_ID);
        p.setStatus("recruiting");
        p.setMaxMembers(maxMembers);
        return p;
    }

    private TeamRequestDTO inviteDto() {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(PROJECT_ID);
        dto.setFromUserId(LEADER_ID);
        dto.setToUserId(INVITEE_ID);
        dto.setMessage("邀请你加入");
        return dto;
    }

    private TeamRequestDTO applyDto() {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(PROJECT_ID);
        dto.setFromUserId(APPLICANT_ID);
        dto.setToUserId(LEADER_ID);
        dto.setMessage("申请加入");
        return dto;
    }

    private TeamRequest inviteRequest() {
        TeamRequest r = new TeamRequest();
        r.setId(REQUEST_ID);
        r.setProjectId(PROJECT_ID);
        r.setFromUserId(LEADER_ID);
        r.setToUserId(INVITEE_ID);
        r.setRequestType("invite");
        r.setStatus("pending");
        return r;
    }

    private TeamRequest applyRequest() {
        TeamRequest r = new TeamRequest();
        r.setId(REQUEST_ID);
        r.setProjectId(PROJECT_ID);
        r.setFromUserId(APPLICANT_ID);
        r.setToUserId(LEADER_ID);
        r.setRequestType("apply");
        r.setStatus("pending");
        return r;
    }
}
