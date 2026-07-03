package com.teammatch.m6.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.constants.PenaltyType;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.ReportCreateDTO;
import com.teammatch.m6.dto.ReportHandleDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.entity.Report;
import com.teammatch.m6.mapper.ReportMapper;
import com.teammatch.m6.service.PenaltyService;
import com.teammatch.m6.service.impl.ReportServiceImpl;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ReportService 单元测试
 * 使用 Mockito Mock 数据库操作，专注于 Service 业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("举报服务单元测试")
class ReportServiceTest {

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private PenaltyService penaltyService;

    @Mock
    private com.teammatch.service.storage.OssService ossService;

    @InjectMocks
    private ReportServiceImpl service;

    private static final Long REPORT_ID = 1L;
    private static final Long REPORTER_ID = 100L;
    private static final Long HANDLER_ID = 999L;

    private Report pendingReport;
    private Report resolvedReport;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", reportMapper);
        lenient().when(ossService.normalizeStoredUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(ossService.resolveAccessibleUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));

        pendingReport = new Report();
        pendingReport.setId(REPORT_ID);
        pendingReport.setReporterId(REPORTER_ID);
        pendingReport.setTargetType("user");
        pendingReport.setTargetId(200L);
        pendingReport.setReason("违规内容");
        pendingReport.setStatus("pending");
        pendingReport.setCreatedAt(LocalDateTime.now().minusDays(1));
        pendingReport.setUpdatedAt(LocalDateTime.now().minusDays(1));

        resolvedReport = new Report();
        resolvedReport.setId(2L);
        resolvedReport.setReporterId(REPORTER_ID);
        resolvedReport.setTargetType("project");
        resolvedReport.setTargetId(300L);
        resolvedReport.setReason("虚假项目");
        resolvedReport.setStatus("resolved");
        resolvedReport.setHandlerId(HANDLER_ID);
        resolvedReport.setHandleResult("已处理");
        resolvedReport.setHandledAt(LocalDateTime.now());
        resolvedReport.setCreatedAt(LocalDateTime.now().minusDays(2));
        resolvedReport.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== createReport ====================

    @Test
    @DisplayName("createReport: 正常提交举报")
    void createReport_success() {
        when(reportMapper.insert(any(Report.class))).thenReturn(1);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        Report result = service.createReport(REPORTER_ID, dto);

        assertThat(result.getReporterId()).isEqualTo(REPORTER_ID);
        assertThat(result.getTargetType()).isEqualTo("user");
        assertThat(result.getTargetId()).isEqualTo(200L);
        assertThat(result.getReason()).isEqualTo("违规内容");
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    @DisplayName("createReport: 举报项目类型")
    void createReport_projectTarget() {
        when(reportMapper.insert(any(Report.class))).thenReturn(1);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("project");
        dto.setTargetId(300L);
        dto.setReason("虚假项目信息");

        Report result = service.createReport(REPORTER_ID, dto);

        assertThat(result.getTargetType()).isEqualTo("project");
        assertThat(result.getTargetId()).isEqualTo(300L);
    }

    // ==================== getMyReports ====================

    @Test
    @DisplayName("getMyReports: 返回用户的举报列表")
    void getMyReports_success() {
        when(reportMapper.selectList(any()))
                .thenReturn(Arrays.asList(pendingReport, resolvedReport));

        List<Report> result = service.getMyReports(REPORTER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReporterId()).isEqualTo(REPORTER_ID);
        verify(reportMapper).selectList(any());
    }

    @Test
    @DisplayName("getMyReports: 无举报记录返回空列表")
    void getMyReports_empty() {
        when(reportMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        List<Report> result = service.getMyReports(REPORTER_ID);

        assertThat(result).isEmpty();
    }

    // ==================== getReportList ====================

    @Test
    @DisplayName("getReportList: 查询全部举报")
    void getReportList_all() {
        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(pendingReport, resolvedReport));

        List<Report> result = service.getReportList(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getReportList: 按状态筛选pending")
    void getReportList_byStatusPending() {
        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(pendingReport));

        List<Report> result = service.getReportList("pending");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("getReportList: 按状态筛选resolved")
    void getReportList_byStatusResolved() {
        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(resolvedReport));

        List<Report> result = service.getReportList("resolved");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("resolved");
    }

    @Test
    @DisplayName("getReportList: 按状态筛选dismissed")
    void getReportList_byStatusDismissed() {
        Report dismissedReport = new Report();
        dismissedReport.setStatus("dismissed");
        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(dismissedReport));

        List<Report> result = service.getReportList("dismissed");

        assertThat(result.get(0).getStatus()).isEqualTo("dismissed");
    }

    @Test
    @DisplayName("getReportList: 空字符串状态查询全部")
    void getReportList_emptyStatus() {
        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(pendingReport));

        List<Report> result = service.getReportList("");

        assertThat(result).hasSize(1);
    }

    // ==================== handleReport ====================

    @Test
    @DisplayName("handleReport: 正常处理举报为resolved")
    void handleReport_resolveSuccess() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setHandleResult("已核实并处理");

        Report result = service.handleReport(REPORT_ID, HANDLER_ID, dto);

        assertThat(result.getStatus()).isEqualTo("resolved");
        assertThat(result.getHandlerId()).isEqualTo(HANDLER_ID);
        assertThat(result.getHandleResult()).isEqualTo("已核实并处理");
        assertThat(result.getHandledAt()).isNotNull();
        verify(reportMapper).updateById(any(Report.class));
    }

    @Test
    @DisplayName("handleReport: 正常处理举报为dismissed")
    void handleReport_dismissSuccess() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("dismissed");
        dto.setHandleResult("举报不实，予以驳回");

        Report result = service.handleReport(REPORT_ID, HANDLER_ID, dto);

        assertThat(result.getStatus()).isEqualTo("dismissed");
        assertThat(result.getHandlerId()).isEqualTo(HANDLER_ID);
    }

    @Test
    @DisplayName("handleReport: 处理不存在的举报抛出异常")
    void handleReport_notFound_throws() {
        when(reportMapper.selectById(999L)).thenReturn(null);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");

        assertThatThrownBy(() -> service.handleReport(999L, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("举报不存在");
    }

    @Test
    @DisplayName("handleReport: 重复处理已处理的举报抛出异常")
    void handleReport_alreadyHandled_throws() {
        when(reportMapper.selectById(2L)).thenReturn(resolvedReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("dismissed");

        assertThatThrownBy(() -> service.handleReport(2L, HANDLER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("举报已处理");
    }

    @Test
    @DisplayName("handleReport: resolved且createPenalty联动创建处罚")
    void handleReport_resolveWithPenalty_success() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);
        Penalty penalty = new Penalty();
        penalty.setId(10L);
        when(penaltyService.createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class))).thenReturn(penalty);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(5);

        service.handleReport(REPORT_ID, HANDLER_ID, dto);

        verify(penaltyService).createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class));
    }

    @Test
    @DisplayName("handleReport: project举报不可联动处罚")
    void handleReport_projectWithPenalty_throws() {
        Report projectReport = new Report();
        projectReport.setId(REPORT_ID);
        projectReport.setTargetType("project");
        projectReport.setTargetId(300L);
        projectReport.setStatus("pending");
        when(reportMapper.selectById(REPORT_ID)).thenReturn(projectReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(5);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅 user 类型举报");
        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("handleReport: createPenalty缺少penaltyType抛出异常")
    void handleReport_createPenaltyMissingType_throws() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("penaltyType");
        verify(reportMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("handleReport: 联动处罚时不能处罚自己")
    void handleReport_selfPenalty_throws() {
        pendingReport.setTargetId(HANDLER_ID);
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);
        when(penaltyService.createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class)))
                .thenThrow(new BusinessException(ReasonCode.PENALTY_SELF_NOT_ALLOWED));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.FUNCTION_LIMIT);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.PENALTY_SELF_NOT_ALLOWED));
    }

    @Test
    @DisplayName("handleReport: resolved且createPenalty联动创建function_limit处罚")
    void handleReport_resolveWithFunctionLimitPenalty_success() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);
        Penalty penalty = new Penalty();
        penalty.setId(11L);
        when(penaltyService.createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class))).thenReturn(penalty);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.FUNCTION_LIMIT);

        service.handleReport(REPORT_ID, HANDLER_ID, dto);

        ArgumentCaptor<PenaltyCreateDTO> captor = ArgumentCaptor.forClass(PenaltyCreateDTO.class);
        verify(penaltyService).createPenalty(eq(HANDLER_ID), captor.capture());
        PenaltyCreateDTO captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(pendingReport.getTargetId());
        assertThat(captured.getType()).isEqualTo(PenaltyType.FUNCTION_LIMIT);
        assertThat(captured.getRelatedReportId()).isEqualTo(REPORT_ID);
        assertThat(captured.getReason()).isEqualTo(pendingReport.getReason());
    }

    @Test
    @DisplayName("handleReport: 联动处罚使用自定义penaltyReason")
    void handleReport_resolveWithPenalty_usesCustomPenaltyReason() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);
        when(penaltyService.createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class))).thenReturn(new Penalty());

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(8);
        dto.setPenaltyReason("多次恶意刷屏");

        service.handleReport(REPORT_ID, HANDLER_ID, dto);

        ArgumentCaptor<PenaltyCreateDTO> captor = ArgumentCaptor.forClass(PenaltyCreateDTO.class);
        verify(penaltyService).createPenalty(eq(HANDLER_ID), captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("多次恶意刷屏");
        assertThat(captor.getValue().getCreditDeductValue()).isEqualTo(8);
    }

    @Test
    @DisplayName("handleReport: resolved但未开启createPenalty不联动处罚")
    void handleReport_resolvedWithoutCreatePenalty_noPenaltyCreated() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");

        service.handleReport(REPORT_ID, HANDLER_ID, dto);

        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("handleReport: dismissed且createPenalty=true不联动处罚")
    void handleReport_dismissedWithCreatePenaltyFlag_noPenaltyCreated() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("dismissed");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(5);

        service.handleReport(REPORT_ID, HANDLER_ID, dto);

        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("handleReport: 非法penaltyType抛出异常")
    void handleReport_invalidPenaltyType_throws() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType("ban_forever");
        dto.setCreditDeductValue(5);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("penaltyType 必须是");
        verify(reportMapper, never()).updateById(any());
        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("handleReport: credit_deduct缺少creditDeductValue抛出异常")
    void handleReport_creditDeductMissingValue_throws() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creditDeductValue");
        verify(reportMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("handleReport: credit_deduct扣分值非正数抛出异常")
    void handleReport_creditDeductNonPositiveValue_throws() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(0);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creditDeductValue");
        verify(reportMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("handleReport: 联动创建处罚失败包装为RuntimeException")
    void handleReport_createPenaltyFails_wrappedRuntimeException() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);
        when(penaltyService.createPenalty(eq(HANDLER_ID), any(PenaltyCreateDTO.class)))
                .thenThrow(new IllegalStateException("写入 credit_change 流水失败"));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);
        dto.setPenaltyType(PenaltyType.CREDIT_DEDUCT);
        dto.setCreditDeductValue(5);

        assertThatThrownBy(() -> service.handleReport(REPORT_ID, HANDLER_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("创建处罚失败")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("handleReport: 无处理结果说明也可正常处理")
    void handleReport_noResultText() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);
        when(reportMapper.updateById(any(Report.class))).thenReturn(1);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        // handleResult为null

        Report result = service.handleReport(REPORT_ID, HANDLER_ID, dto);

        assertThat(result.getStatus()).isEqualTo("resolved");
        assertThat(result.getHandleResult()).isNull();
    }

    // ==================== getReportById ====================

    @Test
    @DisplayName("getReportById: 查询存在的举报")
    void getReportById_exists() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pendingReport);

        Report result = service.getReportById(REPORT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("getReportById: 查询不存在的举报返回null")
    void getReportById_notExists() {
        when(reportMapper.selectById(999L)).thenReturn(null);

        Report result = service.getReportById(999L);

        assertThat(result).isNull();
    }
}
