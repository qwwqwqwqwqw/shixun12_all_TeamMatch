package com.teammatch.m4.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m4.dto.ExitVoteCreateDTO;
import com.teammatch.m4.dto.ExitVoteSubmitDTO;
import com.teammatch.m4.dto.ExitVoteVO;
import com.teammatch.m4.service.ExitVoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExitVoteController.class)
@DisplayName("ExitVoteController 测试")
class ExitVoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

    @MockBean
    private ExitVoteService exitVoteService;

        @Test
        @DisplayName("selfExit: 成功时返回 success")
        void selfExit_success_returnsSuccess() throws Exception {
        doNothing().when(exitVoteService).selfExit(1L, 100L);

        mockMvc.perform(post("/m4/projects/1/exit/self")
                .param("userId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.message").value(ReasonCode.SUCCESS.getMessage()));

        verify(exitVoteService).selfExit(1L, 100L);
        }

        @Test
        @DisplayName("selfExit: 队长主动退出时返回 M4_LEADER_CANNOT_EXIT")
        void selfExit_leaderCannotExit_returnsSpecificCode() throws Exception {
        doThrow(new RuntimeException("队长不能主动退出，请先转让队长身份"))
            .when(exitVoteService).selfExit(1L, 100L);

        mockMvc.perform(post("/m4/projects/1/exit/self")
                .param("userId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_LEADER_CANNOT_EXIT.getCode()));

        verify(exitVoteService).selfExit(1L, 100L);
        }

        @Test
        @DisplayName("initiateVote: 成功时返回投票详情")
        void initiateVote_success_returnsVote() throws Exception {
        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(1L);
        dto.setTargetUserId(2L);
        dto.setReason("长期失联");
        dto.setPenaltyLevel("negotiated");

        ExitVoteVO vo = new ExitVoteVO();
        vo.setId(10L);
        vo.setProjectId(1L);
        vo.setStatus("voting");

        when(exitVoteService.initiateVote(eq(1L), any(ExitVoteCreateDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/m4/projects/1/exit/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.status").value("voting"));

        verify(exitVoteService).initiateVote(eq(1L), any(ExitVoteCreateDTO.class));
        }

        @Test
        @DisplayName("initiateVote: 非队长发起时返回 M4_NOT_LEADER")
        void initiateVote_notLeader_returnsSpecificCode() throws Exception {
        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(1L);
        dto.setTargetUserId(2L);

        when(exitVoteService.initiateVote(eq(1L), any(ExitVoteCreateDTO.class)))
            .thenThrow(new RuntimeException("只有队长可以发起退出投票"));

        mockMvc.perform(post("/m4/projects/1/exit/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_NOT_LEADER.getCode()));
        }

        @Test
        @DisplayName("initiateVote: 项目状态非法时返回 M4_PROJECT_STATUS_INVALID")
        void initiateVote_invalidProjectStatus_returnsSpecificCode() throws Exception {
        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(1L);
        dto.setTargetUserId(2L);

        when(exitVoteService.initiateVote(eq(1L), any(ExitVoteCreateDTO.class)))
            .thenThrow(new RuntimeException("只有进行中的项目才能发起退出投票"));

        mockMvc.perform(post("/m4/projects/1/exit/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_STATUS_INVALID.getCode()));
        }

        @Test
        @DisplayName("initiateVote: 重复发起时返回 M4_DUPLICATE_OPERATION")
        void initiateVote_duplicate_returnsSpecificCode() throws Exception {
        ExitVoteCreateDTO dto = new ExitVoteCreateDTO();
        dto.setInitiatorId(1L);
        dto.setTargetUserId(2L);

        when(exitVoteService.initiateVote(eq(1L), any(ExitVoteCreateDTO.class)))
            .thenThrow(new RuntimeException("该成员已有进行中的退出投票"));

        mockMvc.perform(post("/m4/projects/1/exit/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_DUPLICATE_OPERATION.getCode()));
        }

    @Test
    @DisplayName("getVoteDetail: 查询成功时返回统一 Result JSON")
    void getVoteDetail_success_returnsResultJson() throws Exception {
        ExitVoteVO vo = new ExitVoteVO();
        vo.setId(10L);
        vo.setProjectId(1L);
        vo.setStatus("voting");

        when(exitVoteService.getVoteDetail(10L)).thenReturn(vo);

        mockMvc.perform(get("/m4/projects/1/exit/votes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("voting"));

        verify(exitVoteService).getVoteDetail(10L);
    }

    @Test
    @DisplayName("getVoteDetail: 投票不存在时返回 M4_RESOURCE_NOT_FOUND JSON")
    void getVoteDetail_notFound_returnsResultJson() throws Exception {
        when(exitVoteService.getVoteDetail(999L)).thenThrow(new RuntimeException("投票不存在"));

        mockMvc.perform(get("/m4/projects/1/exit/votes/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.M4_RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.M4_RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(exitVoteService).getVoteDetail(999L);
    }

    @Test
    @DisplayName("submitVote: 成功时返回 success")
    void submitVote_success_returnsSuccess() throws Exception {
        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(3L);
        dto.setChoice("agree");

        doNothing().when(exitVoteService).submitVote(eq(10L), any(ExitVoteSubmitDTO.class));

        mockMvc.perform(post("/m4/projects/1/exit/votes/10/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(exitVoteService).submitVote(eq(10L), any(ExitVoteSubmitDTO.class));
    }

    @Test
    @DisplayName("submitVote: 重复投票时返回 M4_DUPLICATE_OPERATION")
    void submitVote_duplicate_returnsSpecificCode() throws Exception {
        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(3L);
        dto.setChoice("agree");

        doThrow(new RuntimeException("DUPLICATE_VOTE"))
                .when(exitVoteService).submitVote(eq(10L), any(ExitVoteSubmitDTO.class));

        mockMvc.perform(post("/m4/projects/1/exit/votes/10/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.M4_DUPLICATE_OPERATION.getCode()));
    }

    @Test
    @DisplayName("submitVote: 投票已关闭时返回 M4_VOTE_ALREADY_CLOSED")
    void submitVote_closed_returnsSpecificCode() throws Exception {
        ExitVoteSubmitDTO dto = new ExitVoteSubmitDTO();
        dto.setVoterId(3L);
        dto.setChoice("agree");

        doThrow(new RuntimeException("投票已关闭"))
                .when(exitVoteService).submitVote(eq(10L), any(ExitVoteSubmitDTO.class));

        mockMvc.perform(post("/m4/projects/1/exit/votes/10/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.M4_VOTE_ALREADY_CLOSED.getCode()));
    }

    @Test
    @DisplayName("closeVote: 成功时返回关闭后的投票")
    void closeVote_success_returnsVote() throws Exception {
        ExitVoteVO vo = new ExitVoteVO();
        vo.setId(10L);
        vo.setStatus("closed");
        vo.setResult("pass");

        when(exitVoteService.closeVote(10L, 1L)).thenReturn(vo);

        mockMvc.perform(post("/m4/projects/1/exit/votes/10/close")
                        .param("operatorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("closed"))
                .andExpect(jsonPath("$.data.result").value("pass"));

        verify(exitVoteService).closeVote(10L, 1L);
    }

    @Test
    @DisplayName("closeVote: 非发起人关闭时返回 M4_NOT_LEADER")
    void closeVote_notInitiator_returnsSpecificCode() throws Exception {
        when(exitVoteService.closeVote(10L, 2L)).thenThrow(new RuntimeException("只有发起人可以关闭投票"));

        mockMvc.perform(post("/m4/projects/1/exit/votes/10/close")
                        .param("operatorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.M4_NOT_LEADER.getCode()));

        verify(exitVoteService).closeVote(10L, 2L);
    }

    @Test
    @DisplayName("closeVote: 投票不存在时返回 M4_RESOURCE_NOT_FOUND")
    void closeVote_notFound_returnsSpecificCode() throws Exception {
        when(exitVoteService.closeVote(999L, 1L)).thenThrow(new RuntimeException("投票不存在"));

        mockMvc.perform(post("/m4/projects/1/exit/votes/999/close")
                        .param("operatorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.M4_RESOURCE_NOT_FOUND.getCode()));

        verify(exitVoteService).closeVote(999L, 1L);
    }
}