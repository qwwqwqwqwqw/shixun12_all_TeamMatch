package com.teammatch.m4.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m4.dto.ProjectCreateDTO;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.service.ProjectService;
import com.teammatch.m4.service.ProjectSkillService;
import com.teammatch.m4.service.TeamMemberService;
import com.teammatch.mapper.SkillTagMapper;
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

@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController 测试")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private TeamMemberService teamMemberService;

    @MockBean
    private ProjectSkillService projectSkillService;

    @MockBean
    private SkillTagMapper skillTagMapper;

    // ------------------------------------------------------------------ T-113

    @Test
    @DisplayName("createProject: 成功返回项目信息")
    void createProject_success_returnsProject() throws Exception {
        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setCreatorId(10L);
        dto.setBoardId(1L);
        dto.setTitle("测试项目");
        dto.setDescription("描述");
        dto.setMaxMembers(5);

        Project project = new Project();
        project.setId(1L);
        project.setTitle("测试项目");
        project.setStatus("recruiting");

        when(projectService.createProject(any(ProjectCreateDTO.class))).thenReturn(project);

        mockMvc.perform(post("/m4/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("recruiting"));

        verify(projectService).createProject(any(ProjectCreateDTO.class));
    }

    @Test
    @DisplayName("createProject: boardId 不存在时返回 PARAM_ERROR")
    void createProject_invalidBoard_returnsParamError() throws Exception {
        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setCreatorId(10L);
        dto.setBoardId(999L);
        dto.setTitle("测试项目");
        dto.setDescription("描述");
        dto.setMaxMembers(5);

        when(projectService.createProject(any(ProjectCreateDTO.class)))
            .thenThrow(new RuntimeException("板块不存在或已禁用"));

        mockMvc.perform(post("/m4/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()));
    }

    // ------------------------------------------------------------------ T-114

    @Test
    @DisplayName("getProjectList: 无过滤返回分页列表")
    void getProjectList_noFilter_returnsPage() throws Exception {
        Page<Project> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(projectService.page(any(), any())).thenReturn(page);

        mockMvc.perform(get("/m4/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("getProjectList: 带 status 过滤时正常返回")
    void getProjectList_withStatusFilter_returnsPage() throws Exception {
        Page<Project> page = new Page<>(1, 10);
        page.setRecords(List.of());

        when(projectService.page(any(), any())).thenReturn(page);

        mockMvc.perform(get("/m4/projects").param("status", "recruiting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));
    }

    // ------------------------------------------------------------------ T-115

    @Test
    @DisplayName("getProjectInfo: 项目存在时返回详情")
    void getProjectInfo_exists_returnsProject() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setTitle("演示项目");
        project.setStatus("in_progress");

        when(projectService.getById(1L)).thenReturn(project);

        mockMvc.perform(get("/m4/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data.title").value("演示项目"));
    }

    @Test
    @DisplayName("getProjectInfo: 项目不存在时返回 M4_PROJECT_NOT_FOUND")
    void getProjectInfo_notFound_returnsNotFound() throws Exception {
        when(projectService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/m4/projects/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_NOT_FOUND.getCode()));
    }

    // ------------------------------------------------------------------ T-116

    @Test
    @DisplayName("updateProjectInfo: 更新成功时返回 success")
    void updateProjectInfo_success_returnsSuccess() throws Exception {
        Project update = new Project();
        update.setTitle("新标题");

        when(projectService.updateById(any(Project.class))).thenReturn(true);

        mockMvc.perform(put("/m4/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));
    }

    @Test
    @DisplayName("updateProjectInfo: 项目不存在时返回 M4_PROJECT_NOT_FOUND")
    void updateProjectInfo_notFound_returnsNotFound() throws Exception {
        Project update = new Project();
        update.setTitle("新标题");

        when(projectService.updateById(any(Project.class))).thenReturn(false);

        mockMvc.perform(put("/m4/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_NOT_FOUND.getCode()));
    }

    // ------------------------------------------------------------------ T-117

    @Test
    @DisplayName("startProject: 成功时返回 success")
    void startProject_success_returnsSuccess() throws Exception {
        doNothing().when(projectService).startProject(1L, 10L);

        mockMvc.perform(post("/m4/projects/1/start").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(projectService).startProject(1L, 10L);
    }

    @Test
    @DisplayName("startProject: 状态不符时返回 M4_PROJECT_STATUS_INVALID")
    void startProject_invalidStatus_returnsStatusInvalid() throws Exception {
        doThrow(new RuntimeException("当前状态不可进入组队中"))
            .when(projectService).startProject(1L, 10L);

        mockMvc.perform(post("/m4/projects/1/start").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_STATUS_INVALID.getCode()));
    }

    // ------------------------------------------------------------------ T-118

    @Test
    @DisplayName("endProject: 成功时返回 success")
    void endProject_success_returnsSuccess() throws Exception {
        doNothing().when(projectService).endProject(1L, 10L);

        mockMvc.perform(post("/m4/projects/1/end").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(projectService).endProject(1L, 10L);
    }

    @Test
    @DisplayName("endProject: 项目非进行中时返回 M4_PROJECT_STATUS_INVALID")
    void endProject_notInProgress_returnsStatusInvalid() throws Exception {
        doThrow(new RuntimeException("只有进行中的项目才能结束"))
            .when(projectService).endProject(1L, 10L);

        mockMvc.perform(post("/m4/projects/1/end").param("operatorId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_STATUS_INVALID.getCode()));
    }

    // ------------------------------------------------------------------ T-119

    @Test
    @DisplayName("checkEvalStatus: 返回当前互评状态")
    void checkEvalStatus_success_returnsStatus() throws Exception {
        when(projectService.checkAndCloseEvalWindow(1L)).thenReturn("ended");

        mockMvc.perform(get("/m4/projects/1/eval-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data").value("ended"));
    }

    @Test
    @DisplayName("checkEvalStatus: 项目不存在时返回 M4_PROJECT_NOT_FOUND")
    void checkEvalStatus_notFound_returnsNotFound() throws Exception {
        when(projectService.checkAndCloseEvalWindow(99L))
            .thenThrow(new RuntimeException("该项目不存在"));

        mockMvc.perform(get("/m4/projects/99/eval-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.M4_PROJECT_NOT_FOUND.getCode()));
    }

    // ------------------------------------------------------------------ T-132

    @Test
    @DisplayName("getMembers: 返回项目成员列表")
    void getMembers_success_returnsList() throws Exception {
        TeamMember member = new TeamMember();
        member.setUserId(10L);
        member.setRole("leader");
        member.setStatus("active");

        when(teamMemberService.getProjectMembers(1L)).thenReturn(List.of(member));

        mockMvc.perform(get("/m4/projects/1/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
            .andExpect(jsonPath("$.data[0].role").value("leader"));
    }
}
