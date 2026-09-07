/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processexecution;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStatusUpdate;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.commons.types.processconfig.ModificationInfo;
import org.gridsuite.monitor.commons.types.processconfig.SecurityAnalysisConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessStatus;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.commons.types.processexecution.StepStatus;
import org.gridsuite.monitor.commons.types.result.ResultInfos;
import org.gridsuite.monitor.commons.types.result.ResultType;
import org.gridsuite.monitor.server.clients.ReportRestClient;
import org.gridsuite.monitor.server.clients.S3RestClient;
import org.gridsuite.monitor.server.dto.processexecution.ProcessExecution;
import org.gridsuite.monitor.server.dto.report.ReportLog;
import org.gridsuite.monitor.server.dto.report.ReportPage;
import org.gridsuite.monitor.server.dto.report.Severity;
import org.gridsuite.monitor.server.messaging.NotificationService;
import org.gridsuite.monitor.server.services.result.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Kevin Le Saulnier <kevin.le-saulnier at rte-france.com>
 */
@ExtendWith({MockitoExtension.class})
class ProcessExecutionServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReportRestClient reportRestClient;

    @Mock
    private ResultService resultService;

    @Mock
    private S3RestClient s3RestClient;

    @Mock
    private ProcessExecutionTxService processExecutionTxService;

    @InjectMocks
    private ProcessExecutionService processExecutionService;

    private SecurityAnalysisConfig securityAnalysisConfig;
    private UUID caseUuid;
    private UUID executionId;
    private UUID reportId;
    private String userId;

    @BeforeEach
    void setUp() {
        caseUuid = UUID.randomUUID();
        executionId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        userId = "user1";
        securityAnalysisConfig = new SecurityAnalysisConfig(
            UUID.randomUUID(),
            List.of(new ModificationInfo(UUID.randomUUID(), "descr1", true)),
            UUID.randomUUID()
        );
    }

    @Test
    void executeProcessCreateExecutionAndSendNotifications() {
        String debugFileLocation = "debug/file/location";

        doReturn(Optional.of(new ProcessCreationResult(debugFileLocation, securityAnalysisConfig)))
            .when(processExecutionTxService).createExecution(eq(caseUuid), eq(userId), any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean());

        Optional<UUID> result = processExecutionService.executeProcess(caseUuid, userId, UUID.randomUUID(), true);

        assertThat(result).isNotEmpty();
        verify(notificationService).sendProcessRunMessage(
            eq(caseUuid),
            eq(securityAnalysisConfig),
            eq(result.get()),
            any(UUID.class),
            eq(debugFileLocation)
        );
        verify(notificationService).sendProcessUpdatedMessage(any(UUID.class), any(ProcessType.class));
    }

    @Test
    void updateExecutionStatusShouldDelegateToTxService() {
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();
        String executionEnvName = "test-env";

        ProcessExecutionStatusUpdate update = new ProcessExecutionStatusUpdate(ProcessType.SECURITY_ANALYSIS, ProcessStatus.COMPLETED, executionEnvName, startedAt, completedAt);
        processExecutionService.updateExecutionStatus(executionId, update);

        verify(processExecutionTxService).updateExecutionStatus(executionId, update);
        verify(notificationService).sendProcessUpdatedMessage(executionId, ProcessType.SECURITY_ANALYSIS);
    }

    @Test
    void updateStepStatusShouldDelegateToTxService() {
        ProcessExecutionStep step = ProcessExecutionStep.builder()
            .id(UUID.randomUUID())
            .stepType("LOAD_FLOW")
            .stepOrder(1)
            .status(StepStatus.RUNNING)
            .resultId(UUID.randomUUID())
            .resultType(ResultType.SECURITY_ANALYSIS)
            .startedAt(Instant.now())
            .build();

        processExecutionService.updateStepStatus(executionId, step);

        verify(processExecutionTxService).updateStepStatus(executionId, step);
    }

    @Test
    void updateStepsStatusesShouldDelegateToTxService() {
        List<ProcessExecutionStep> steps = List.of(
            ProcessExecutionStep.builder()
                .id(UUID.randomUUID())
                .stepType("LOAD_FLOW")
                .stepOrder(1)
                .status(StepStatus.RUNNING)
                .build(),
            ProcessExecutionStep.builder()
                .id(UUID.randomUUID())
                .stepType("SECURITY_ANALYSIS")
                .stepOrder(2)
                .status(StepStatus.SCHEDULED)
                .build()
        );

        processExecutionService.updateStepsStatuses(executionId, steps);

        verify(processExecutionTxService).updateStepsStatuses(executionId, steps);
    }

    @Test
    void getReportsShouldReturnReports() {
        when(processExecutionTxService.getReportId(executionId)).thenReturn(Optional.of(reportId));

        ReportLog reportLog1 = new ReportLog("message1", Severity.INFO, 1, UUID.randomUUID());
        ReportLog reportLog2 = new ReportLog("message2", Severity.WARN, 2, UUID.randomUUID());
        ReportLog reportLog3 = new ReportLog("message3", Severity.ERROR, 1, UUID.randomUUID());
        ReportPage reportPage = new ReportPage(1, List.of(reportLog1, reportLog2, reportLog3), 100, 10);

        when(reportRestClient.getReport(reportId)).thenReturn(reportPage);

        Optional<ReportPage> result = processExecutionService.getReports(executionId);
        assertThat(result).contains(reportPage);

        verify(processExecutionTxService).getReportId(executionId);
        verify(reportRestClient).getReport(reportId);
    }

    @Test
    void getResultsShouldReturnResults() {
        UUID resultId1 = UUID.randomUUID();
        UUID resultId2 = UUID.randomUUID();
        String result1 = "{\"result\": \"data1\"}";
        String result2 = "{\"result\": \"data2\"}";
        List<ResultInfos> resultInfos = List.of(new ResultInfos(resultId1, ResultType.SECURITY_ANALYSIS), new ResultInfos(resultId2, ResultType.SECURITY_ANALYSIS));
        when(resultService.getResult(resultInfos.get(0)))
            .thenReturn(result1);
        when(resultService.getResult(resultInfos.get(1)))
            .thenReturn(result2);
        when(processExecutionTxService.getResultInfos(executionId)).thenReturn(Optional.of(resultInfos));

        Optional<List<String>> results = processExecutionService.getResults(executionId);

        assertThat(results).isPresent();
        assertThat(results.get()).hasSize(2).containsExactly(result1, result2);
        verify(processExecutionTxService).getResultInfos(executionId);
        verify(resultService, times(2)).getResult(any(ResultInfos.class));
    }

    @Test
    void deleteExecutionShouldDeleteResultsAndReports() {
        UUID resultId1 = UUID.randomUUID();
        UUID resultId2 = UUID.randomUUID();
        when(processExecutionTxService.deleteExecution(executionId))
            .thenReturn(Optional.of(new ProcessDeletionInfos(reportId, List.of(new ResultInfos(resultId1, ResultType.SECURITY_ANALYSIS), new ResultInfos(resultId2, ResultType.SECURITY_ANALYSIS)))));

        doNothing().when(reportRestClient).deleteReport(reportId);
        doNothing().when(resultService).deleteResult(any(ResultInfos.class));

        Optional<UUID> deletedExecutionId = processExecutionService.deleteExecution(executionId);
        assertThat(deletedExecutionId).contains(executionId);

        verify(reportRestClient).deleteReport(reportId);
        verify(resultService, times(2)).deleteResult(any(ResultInfos.class));
    }

    @Test
    void deleteExecutionShouldReturnFalseWhenExecutionNotFound() {
        when(processExecutionTxService.deleteExecution(executionId)).thenReturn(Optional.empty());

        Optional<UUID> deletedExecution = processExecutionService.deleteExecution(executionId);
        assertThat(deletedExecution).isNotPresent();

        verify(processExecutionTxService).deleteExecution(executionId);
        verifyNoInteractions(reportRestClient);
        verifyNoInteractions(resultService);
    }

    @Test
    void getExistingDebugInfo() throws Exception {
        String debugFileLocation = "debug/file/location";
        byte[] expectedBytes = "zip-content".getBytes();

        when(processExecutionTxService.getDebugFileLocation(executionId)).thenReturn(Optional.of(debugFileLocation));
        when(s3RestClient.downloadDirectoryAsZip(debugFileLocation)).thenReturn(expectedBytes);

        Optional<byte[]> result = processExecutionService.getDebugInfos(executionId);

        assertThat(result).isPresent();
        assertThat(expectedBytes).isEqualTo(result.get());

        verify(processExecutionTxService).getDebugFileLocation(executionId);
        verify(s3RestClient).downloadDirectoryAsZip("debug/file/location");
    }

    @Test
    void getNotExistingExecutionDebugInfo() {
        when(processExecutionTxService.getDebugFileLocation(executionId)).thenReturn(Optional.empty());

        Optional<byte[]> result = processExecutionService.getDebugInfos(executionId);

        assertThat(result).isEmpty();

        verify(processExecutionTxService).getDebugFileLocation(executionId);
        verifyNoInteractions(s3RestClient);
    }

    @Test
    void getExistingDebugInfoError() throws Exception {
        String debugFileLocation = "debug/file/location";

        when(processExecutionTxService.getDebugFileLocation(executionId)).thenReturn(Optional.of(debugFileLocation));

        when(s3RestClient.downloadDirectoryAsZip(debugFileLocation)).thenThrow(new IOException("S3 error"));

        PowsyblException exception = assertThrows(
            PowsyblException.class,
            () -> processExecutionService.getDebugInfos(executionId)
        );

        assertThat(exception.getMessage()).contains("An error occurred while downloading debug files");

        verify(processExecutionTxService).getDebugFileLocation(executionId);
        verify(s3RestClient).downloadDirectoryAsZip(debugFileLocation);
    }

    @Test
    void getLaunchedProcessesShouldDelegateToTxService() {
        List<ProcessExecution> executions = List.of(
            ProcessExecution.builder()
                .id(UUID.randomUUID())
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .processConfigId(UUID.randomUUID())
                .status(ProcessStatus.RUNNING)
                .scheduledAt(Instant.now())
                .userId(userId)
                .build()
        );

        when(processExecutionTxService.getLaunchedProcesses(ProcessType.SECURITY_ANALYSIS)).thenReturn(executions);

        List<ProcessExecution> result = processExecutionService.getLaunchedProcesses(ProcessType.SECURITY_ANALYSIS);

        assertThat(result).isEqualTo(executions);
        verify(processExecutionTxService).getLaunchedProcesses(ProcessType.SECURITY_ANALYSIS);
    }

    @Test
    void getAllLaunchedProcessesShouldDelegateToTxService() {
        List<ProcessExecution> executions = List.of(
            ProcessExecution.builder()
                .id(UUID.randomUUID())
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .processConfigId(UUID.randomUUID())
                .status(ProcessStatus.RUNNING)
                .scheduledAt(Instant.now())
                .userId(userId)
                .build(),
            ProcessExecution.builder()
                .id(UUID.randomUUID())
                .type(ProcessType.LOADFLOW.name())
                .caseUuid(caseUuid)
                .processConfigId(UUID.randomUUID())
                .status(ProcessStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .userId(userId)
                .build()
        );

        when(processExecutionTxService.getLaunchedProcesses(null)).thenReturn(executions);

        List<ProcessExecution> result = processExecutionService.getLaunchedProcesses(null);

        assertThat(result).isEqualTo(executions);
        verify(processExecutionTxService).getLaunchedProcesses(null);
    }

    @Test
    void getExecutionReturnsExecution() {
        ProcessExecution processExecution = mock(ProcessExecution.class);

        when(processExecutionTxService.getExecution(executionId)).thenReturn(Optional.of(processExecution));

        Optional<ProcessExecution> result = processExecutionService.getExecution(executionId);

        assertThat(result).contains(processExecution);
        verify(processExecutionTxService).getExecution(executionId);
    }

    @Test
    void getExecutionNotFoundReturnsEmpty() {
        when(processExecutionTxService.getExecution(executionId)).thenReturn(Optional.empty());

        Optional<ProcessExecution> result = processExecutionService.getExecution(executionId);

        assertThat(result).isEmpty();
        verify(processExecutionTxService).getExecution(executionId);
    }

    @Test
    void getStepsInfosShouldDelegateToTxService() {
        List<ProcessExecutionStep> steps = List.of(
            ProcessExecutionStep.builder()
                .id(UUID.randomUUID())
                .stepType("LOAD_FLOW")
                .stepOrder(1)
                .status(StepStatus.COMPLETED)
                .startedAt(Instant.now().minusSeconds(30))
                .completedAt(Instant.now())
                .build()
        );

        when(processExecutionTxService.getStepsInfos(executionId)).thenReturn(Optional.of(steps));

        Optional<List<ProcessExecutionStep>> result = processExecutionService.getStepsInfos(executionId);

        assertThat(result).contains(steps);
        verify(processExecutionTxService).getStepsInfos(executionId);
    }

    @Test
    void getExecutionWithoutDebugInfo() {
        when(processExecutionTxService.getDebugFileLocation(executionId)).thenReturn(Optional.empty());

        Optional<byte[]> result = processExecutionService.getDebugInfos(executionId);

        assertThat(result).isEmpty();

        verifyNoInteractions(s3RestClient);
    }
}
