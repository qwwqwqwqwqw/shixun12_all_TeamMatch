package com.teammatch.m4.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.teammatch.m4.dto.ProjectCreateDTO;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.ProjectSkill;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.mapper.M4ProjectMapper;
import com.teammatch.m4.service.impl.ProjectServiceImpl;
import com.teammatch.m6.service.BoardService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProjectServiceImpl 单元测试
 * 覆盖 T-113 createProject / T-117 startProject / T-118 endProject / T-119 checkAndCloseEvalWindow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService 单元测试")
class ProjectServiceImplTest {

    @Mock
    private M4ProjectMapper projectMapper;

    @Mock
    private ProjectSkillService projectSkillService;

    @Mock
    private TeamMemberService teamMemberService;

    @Mock
    private BoardService boardService;

    @Mock
    private TeamRequestService teamRequestService;

    @InjectMocks
    private ProjectServiceImpl service;

    private static final Long PROJECT_ID   = 1L;
    private static final Long CREATOR_ID   = 10L;
    private static final Long BOARD_ID     = 1L;

    /**
     * startProject 调用 new LambdaUpdateWrapper<TeamRequest>() 时需要 TeamRequest 的 Lambda 缓存。
     * 单元测试中 Spring 不启动，无法自动注册，需手动初始化 MyBatis-Plus 实体元数据。
     */
    @BeforeAll
    static void initMybatisPlusEntityCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("com.teammatch.m4.mapper.M4TeamRequestMapper");
        TableInfoHelper.initTableInfo(assistant, TeamRequest.class);
    }

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 在父类中，@InjectMocks 不会自动注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", projectMapper);
        // @Lazy @Autowired 字段同样需手动注入
        ReflectionTestUtils.setField(service, "teamRequestService", teamRequestService);
    }

    // ====================================================================== T-113

    @Test
    @DisplayName("createProject: boardId 合法 + 带技能 → 保存项目/技能/队长成员")
    void createProject_withSkills_savesAll() {
        ProjectCreateDTO dto = buildDto(List.of(101L, 102L));
        when(boardService.existsActiveBoard(BOARD_ID)).thenReturn(true);
        when(projectMapper.insert(any(Project.class))).thenReturn(1);
        when(projectSkillService.saveBatch(any())).thenReturn(true);
        when(teamMemberService.save(any(TeamMember.class))).thenReturn(true);

        Project result = service.createProject(dto);

        assertThat(result.getStatus()).isEqualTo("recruiting");
        verify(projectSkillService).saveBatch(any());
        verify(teamMemberService).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("createProject: 无技能 → 不调用 projectSkillService.saveBatch")
    void createProject_noSkills_skipsSkillSave() {
        ProjectCreateDTO dto = buildDto(null);
        when(boardService.existsActiveBoard(BOARD_ID)).thenReturn(true);
        when(projectMapper.insert(any(Project.class))).thenReturn(1);
        when(teamMemberService.save(any(TeamMember.class))).thenReturn(true);

        service.createProject(dto);

        verify(projectSkillService, never()).saveBatch(any());
    }

    @Test
    @DisplayName("createProject: boardId 不存在 → 抛异常")
    void createProject_invalidBoard_throws() {
        ProjectCreateDTO dto = buildDto(null);
        when(boardService.existsActiveBoard(BOARD_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createProject(dto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("板块不存在");
    }

    @Test
    @DisplayName("createProject: boardId 为 null → 抛异常")
    void createProject_nullBoardId_throws() {
        ProjectCreateDTO dto = buildDto(null);
        dto.setBoardId(null);

        assertThatThrownBy(() -> service.createProject(dto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("板块不存在");
    }

    @Test
    @DisplayName("createProject: 队长成员角色为 leader")
    void createProject_captainRole_isLeader() {
        ProjectCreateDTO dto = buildDto(null);
        when(boardService.existsActiveBoard(BOARD_ID)).thenReturn(true);
        when(projectMapper.insert(any(Project.class))).thenReturn(1);
        when(teamMemberService.save(any(TeamMember.class))).thenReturn(true);

        service.createProject(dto);

        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberService).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("leader");
        assertThat(captor.getValue().getUserId()).isEqualTo(CREATOR_ID);
    }

    // ====================================================================== T-117

    @Test
    @DisplayName("startProject: 正常流程 → 状态改为 in_progress，pending 请求过期")
    void startProject_success() {
        Project project = recruitingProject();
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);
        when(teamRequestService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.startProject(PROJECT_ID, CREATOR_ID));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("in_progress");
        verify(teamRequestService).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("startProject: 项目不存在 → 抛异常")
    void startProject_notFound_throws() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.startProject(PROJECT_ID, CREATOR_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("startProject: 非队长操作 → 抛异常")
    void startProject_notLeader_throws() {
        Project project = recruitingProject();
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        assertThatThrownBy(() -> service.startProject(PROJECT_ID, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("startProject: 项目非 recruiting 状态 → 抛异常")
    void startProject_wrongStatus_throws() {
        Project project = recruitingProject();
        project.setStatus("in_progress");
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        assertThatThrownBy(() -> service.startProject(PROJECT_ID, CREATOR_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("当前状态");
    }

    // ====================================================================== T-118

    @Test
    @DisplayName("endProject: 正常流程 → 状态改为 ended，设置 endedAt 和 evalDeadline")
    void endProject_success() {
        Project project = recruitingProject();
        project.setStatus("in_progress");
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        assertThatNoException().isThrownBy(() -> service.endProject(PROJECT_ID, CREATOR_ID));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("ended");
        assertThat(saved.getEndedAt()).isNotNull();
        assertThat(saved.getEvalDeadline()).isAfter(saved.getEndedAt());
    }

    @Test
    @DisplayName("endProject: evalDeadline = endedAt + 7天")
    void endProject_evalDeadline_isEndedAtPlusSeven() {
        Project project = recruitingProject();
        project.setStatus("in_progress");
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        service.endProject(PROJECT_ID, CREATOR_ID);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        Project saved = captor.getValue();
        // evalDeadline 应约等于 endedAt + 7 天（允许 1 秒误差）
        assertThat(saved.getEvalDeadline())
            .isAfterOrEqualTo(saved.getEndedAt().plusDays(7).minusSeconds(1))
            .isBeforeOrEqualTo(saved.getEndedAt().plusDays(7).plusSeconds(1));
    }

    @Test
    @DisplayName("endProject: 项目不存在 → 抛异常")
    void endProject_notFound_throws() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.endProject(PROJECT_ID, CREATOR_ID))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("endProject: 非队长操作 → 抛异常")
    void endProject_notLeader_throws() {
        Project project = recruitingProject();
        project.setStatus("in_progress");
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        assertThatThrownBy(() -> service.endProject(PROJECT_ID, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("队长");
    }

    @Test
    @DisplayName("endProject: 项目非 in_progress 状态 → 抛异常")
    void endProject_notInProgress_throws() {
        Project project = recruitingProject(); // status=recruiting
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        assertThatThrownBy(() -> service.endProject(PROJECT_ID, CREATOR_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("进行中");
    }

    // ====================================================================== T-119

    @Test
    @DisplayName("checkAndCloseEvalWindow: ended + deadline 已过期 → 状态切为 eval_closed")
    void checkAndCloseEvalWindow_deadlinePassed_closesWindow() {
        Project project = recruitingProject();
        project.setStatus("ended");
        project.setEvalDeadline(LocalDateTime.now().minusHours(1)); // 已过期
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        String status = service.checkAndCloseEvalWindow(PROJECT_ID);

        assertThat(status).isEqualTo("eval_closed");
        verify(projectMapper).updateById(any(Project.class));
    }

    @Test
    @DisplayName("checkAndCloseEvalWindow: ended + deadline 未过期 → 保持 ended，不写库")
    void checkAndCloseEvalWindow_deadlineNotPassed_keepsEnded() {
        Project project = recruitingProject();
        project.setStatus("ended");
        project.setEvalDeadline(LocalDateTime.now().plusDays(5)); // 未过期
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        String status = service.checkAndCloseEvalWindow(PROJECT_ID);

        assertThat(status).isEqualTo("ended");
        verify(projectMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("checkAndCloseEvalWindow: 非 ended 状态 → 直接返回当前状态")
    void checkAndCloseEvalWindow_notEnded_returnsCurrentStatus() {
        Project project = recruitingProject(); // status=recruiting
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

        String status = service.checkAndCloseEvalWindow(PROJECT_ID);

        assertThat(status).isEqualTo("recruiting");
        verify(projectMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("checkAndCloseEvalWindow: 项目不存在 → 抛异常")
    void checkAndCloseEvalWindow_notFound_throws() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.checkAndCloseEvalWindow(PROJECT_ID))
            .isInstanceOf(RuntimeException.class);
    }

    // ====================================================================== helpers

    private ProjectCreateDTO buildDto(List<Long> skillTagIds) {
        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setCreatorId(CREATOR_ID);
        dto.setBoardId(BOARD_ID);
        dto.setTitle("测试项目");
        dto.setDescription("描述");
        dto.setMaxMembers(5);
        dto.setSkillTagIds(skillTagIds);
        return dto;
    }

    private Project recruitingProject() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setCreatorId(CREATOR_ID);
        p.setBoardId(BOARD_ID);
        p.setTitle("测试项目");
        p.setStatus("recruiting");
        p.setMaxMembers(5);
        return p;
    }
}
