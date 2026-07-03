package com.teammatch.m4.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m4.dto.TeamRequestDTO;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.service.TeamRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamRequestController.class)
@DisplayName("TeamRequestController 测试")
class TeamRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamRequestService teamRequestService;

    // ------------------------------------------------------------------ T-122

    @Test
    @DisplayName("sendInvite: 队长邀请成功返回 success")
    void sendInvite_success_returnsSuccess() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(10L);
        dto.setToUserId(20L);
        dto.setMessage("欢迎加入");

        doNothing().when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("invite"));

        mockMvc.perform(post("/m4/team-requests/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("invite"));
    }

    @Test
    @DisplayName("sendInvite: 项目非招募状态时返回 M4003")
    void sendInvite_projectNotRecruiting_returnsConflict() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(10L);
        dto.setToUserId(20L);

        doThrow(new RuntimeException("PROJECT_NOT_RECRUITING"))
            .when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("invite"));

        mockMvc.perform(post("/m4/team-requests/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_STATUS_INVALID.getCode()));
    }

    @Test
    @DisplayName("sendInvite: 重复 pending 请求时返回 M4_DUPLICATE_PENDING_REQUEST")
    void sendInvite_duplicatePending_returnsDuplicatePendingRequest() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(10L);
        dto.setToUserId(20L);

        doThrow(new RuntimeException("DUPLICATE_PENDING_REQUEST"))
            .when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("invite"));

        mockMvc.perform(post("/m4/team-requests/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_DUPLICATE_PENDING_REQUEST.getCode()));
    }

    @Test
    @DisplayName("sendInvite: 用户已在项目中时返回 M4_USER_ALREADY_IN_PROJECT")
    void sendInvite_userAlreadyInProject_returnsUserAlreadyInProject() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(10L);
        dto.setToUserId(20L);

        doThrow(new RuntimeException("USER_ALREADY_IN_PROJECT"))
            .when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("invite"));

        mockMvc.perform(post("/m4/team-requests/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_USER_ALREADY_IN_PROJECT.getCode()));
    }

    // ------------------------------------------------------------------ T-123

    @Test
    @DisplayName("sendApply: 申请成功返回 success")
    void sendApply_success_returnsSuccess() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(30L);
        dto.setToUserId(10L);
        dto.setMessage("希望加入");

        doNothing().when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("apply"));

        mockMvc.perform(post("/m4/team-requests/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("apply"));
    }

    @Test
    @DisplayName("sendApply: 项目已满员时返回 M4007")
    void sendApply_projectFull_returnsConflict() throws Exception {
        TeamRequestDTO dto = new TeamRequestDTO();
        dto.setProjectId(1L);
        dto.setFromUserId(30L);
        dto.setToUserId(10L);

        doThrow(new RuntimeException("PROJECT_FULL"))
            .when(teamRequestService).sendRequest(any(TeamRequestDTO.class), eq("apply"));

        mockMvc.perform(post("/m4/team-requests/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_TEAM_FULL.getCode()));
    }

    // ------------------------------------------------------------------ T-124

    @Test
    @DisplayName("acceptRequest: 接受请求成功返回 success")
    void acceptRequest_success_returnsSuccess() throws Exception {
        doNothing().when(teamRequestService).acceptRequest(1L, 10L);

        mockMvc.perform(post("/m4/team-requests/1/accept").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(teamRequestService).acceptRequest(1L, 10L);
    }

    @Test
    @DisplayName("acceptRequest: 请求状态不合法时返回 M4014")
    void acceptRequest_invalidStatus_returnsConflict() throws Exception {
        doThrow(new RuntimeException("该请求已处理"))
            .when(teamRequestService).acceptRequest(1L, 10L);

        mockMvc.perform(post("/m4/team-requests/1/accept").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_REQUEST_ALREADY_HANDLED.getCode()));
    }

    // ------------------------------------------------------------------ T-129

    @Test
    @DisplayName("rejectRequest: 拒绝请求成功返回 success")
    void rejectRequest_success_returnsSuccess() throws Exception {
        doNothing().when(teamRequestService).rejectRequest(1L, 10L);

        mockMvc.perform(post("/m4/team-requests/1/reject").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(teamRequestService).rejectRequest(1L, 10L);
    }

    @Test
    @DisplayName("rejectRequest: 非接收方操作时返回 M4015")
    void rejectRequest_notReceiver_returnsConflict() throws Exception {
        doThrow(new RuntimeException("无权操作此请求"))
            .when(teamRequestService).rejectRequest(1L, 99L);

        mockMvc.perform(post("/m4/team-requests/1/reject").param("operatorId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_UNAUTHORIZED_REQUEST.getCode()));
    }

    // ------------------------------------------------------------------ T-130

    @Test
    @DisplayName("cancelRequest: 取消请求成功返回 success")
    void cancelRequest_success_returnsSuccess() throws Exception {
        doNothing().when(teamRequestService).cancelRequest(1L, 10L);

        mockMvc.perform(post("/m4/team-requests/1/cancel").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(teamRequestService).cancelRequest(1L, 10L);
    }

    @Test
    @DisplayName("cancelRequest: 请求不存在时返回 M4_RESOURCE_NOT_FOUND")
    void cancelRequest_notFound_returnsResourceNotFound() throws Exception {
        doThrow(new RuntimeException("请求不存在"))
            .when(teamRequestService).cancelRequest(99L, 10L);

        mockMvc.perform(post("/m4/team-requests/99/cancel").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_RESOURCE_NOT_FOUND.getCode()));
    }

    // ------------------------------------------------------------------ T-131

    @Test
    @DisplayName("getRequestList: 返回收到的请求列表")
    void getRequestList_received_returnsList() throws Exception {
        TeamRequest req = new TeamRequest();
        req.setId(1L);
        req.setRequestType("invite");
        req.setStatus("pending");

        when(teamRequestService.getRequestList(10L, "received")).thenReturn(List.of(req));

        mockMvc.perform(get("/m4/team-requests")
                .param("userId", "10")
                .param("direction", "received"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data[0].requestType").value("invite"));
    }

    @Test
    @DisplayName("getRequestList: 返回发出的请求列表")
    void getRequestList_sent_returnsList() throws Exception {
        when(teamRequestService.getRequestList(10L, "sent")).thenReturn(List.of());

        mockMvc.perform(get("/m4/team-requests")
                .param("userId", "10")
                .param("direction", "sent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));
    }
}
