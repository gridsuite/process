/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processexecution;

import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStatusUpdate;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.commons.types.processconfig.ModificationInfo;
import org.gridsuite.monitor.commons.types.processconfig.SecurityAnalysisConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessStatus;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.commons.types.processexecution.StepStatus;
import org.gridsuite.monitor.commons.types.result.ResultInfos;
import org.gridsuite.monitor.commons.types.result.ResultType;
import org.gridsuite.monitor.server.dto.processconfig.PersistedProcessConfig;
import org.gridsuite.monitor.server.dto.processexecution.ProcessExecution;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionEntity;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionStepEntity;
import org.gridsuite.monitor.server.mappers.processexecution.ProcessExecutionMapper;
import org.gridsuite.monitor.server.mappers.processexecution.ProcessExecutionStepMapper;
import org.gridsuite.monitor.server.repositories.ProcessExecutionRepository;
import org.gridsuite.monitor.server.services.processconfig.ProcessConfigService;
import org.gridsuite.monitor.server.utils.S3PathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@ExtendWith({MockitoExtension.class})
class ProcessExecutionTxServiceTest {
    @Mock
    private ProcessExecutionRepository executionRepository;

    @Mock
    private ProcessConfigService processConfigService;

    @Mock
    private S3PathResolver s3PathResolver;

    @Spy
    private ProcessExecutionStepMapper processExecutionStepMapper = Mappers.getMapper(ProcessExecutionStepMapper.class);

    @Spy
    private ProcessExecutionMapper processExecutionMapper = Mappers.getMapper(ProcessExecutionMapper.class);

    @InjectMocks
    private ProcessExecutionTxService processExecutionTxService;

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
                List.of(new ModificationInfo(UUID.randomUUID(), "descr", true)),
                UUID.randomUUID()
        );
    }

    @Test
    void executeProcessCreateExecution() {
        String debugFileLocation = "debug/file/location";
        when(s3PathResolver.toDebugLocation(eq(ProcessType.SECURITY_ANALYSIS.name()), any(UUID.class))).thenReturn(debugFileLocation);
        when(processConfigService.getProcessConfig(any(UUID.class))).thenReturn(Optional.of(new PersistedProcessConfig(UUID.randomUUID(), securityAnalysisConfig)));

        Optional<ProcessCreationResult> result = processExecutionTxService.createExecution(caseUuid, userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);

        assertThat(result).isNotEmpty();
        verify(processConfigService).getProcessConfig(any(UUID.class));
        verify(executionRepository).save(argThat(execution ->
                        execution.getId() != null &&
                        ProcessType.SECURITY_ANALYSIS.name().equals(execution.getType()) &&
                        caseUuid.equals(execution.getCaseUuid()) &&
                        userId.equals(execution.getUserId()) &&
                        ProcessStatus.SCHEDULED.equals(execution.getStatus()) &&
                        execution.getScheduledAt() != null &&
                        execution.getStartedAt() == null
        ));
        verify(s3PathResolver).toDebugLocation(eq(ProcessType.SECURITY_ANALYSIS.name()), any(UUID.class));
    }

    @Test
    void updateExecutionStatusShouldUpdateStatusOnly() {
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .reportId(reportId)
                .userId(userId)
                .status(ProcessStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        processExecutionTxService.updateExecutionStatus(executionId, new ProcessExecutionStatusUpdate(ProcessType.SECURITY_ANALYSIS, ProcessStatus.RUNNING, null, null, null));

        verify(executionRepository).findById(executionId);
        assertThat(execution.getStatus()).isEqualTo(ProcessStatus.RUNNING);
        assertThat(execution.getExecutionEnvName()).isNull();
        assertThat(execution.getStartedAt()).isNull();
        assertThat(execution.getCompletedAt()).isNull();
        assertThat(execution.getReportId()).isEqualTo(reportId);
        verify(executionRepository).save(execution);
    }

    @Test
    void updateExecutionStatusShouldUpdateAllFields() {
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .reportId(reportId)
                .userId(userId)
                .status(ProcessStatus.RUNNING)
                .scheduledAt(Instant.now())
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        String envName = "production-env";
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();

        processExecutionTxService.updateExecutionStatus(executionId, new ProcessExecutionStatusUpdate(ProcessType.SECURITY_ANALYSIS, ProcessStatus.COMPLETED, envName, startedAt, completedAt));

        verify(executionRepository).findById(executionId);
        assertThat(execution.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(execution.getExecutionEnvName()).isEqualTo(envName);
        assertThat(execution.getStartedAt()).isEqualTo(startedAt);
        assertThat(execution.getCompletedAt()).isEqualTo(completedAt);
        assertThat(execution.getReportId()).isEqualTo(reportId);
        verify(executionRepository).save(execution);
    }

    @Test
    void updateExecutionStatusShouldHandleExecutionNotFound() {
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        processExecutionTxService.updateExecutionStatus(executionId, new ProcessExecutionStatusUpdate(ProcessType.SECURITY_ANALYSIS, ProcessStatus.COMPLETED, "env", Instant.now(), Instant.now()));

        verify(executionRepository).findById(executionId);
        verify(executionRepository, never()).save(any());
    }

    @Test
    void updateStepStatusShouldAddNewStep() {
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .userId(userId)
                .status(ProcessStatus.RUNNING)
                .steps(new ArrayList<>())
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        UUID stepId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        ProcessExecutionStep processExecutionStep = ProcessExecutionStep.builder()
                .id(stepId)
                .stepType("LOAD_FLOW")
                .status(StepStatus.RUNNING)
                .resultId(resultId)
                .resultType(ResultType.SECURITY_ANALYSIS)
                .startedAt(startedAt)
                .build();

        processExecutionTxService.updateStepStatus(executionId, processExecutionStep);

        verify(executionRepository).findById(executionId);
        assertThat(execution.getSteps()).hasSize(1);
        ProcessExecutionStepEntity addedStep = execution.getSteps().getFirst();
        assertThat(addedStep.getId()).isEqualTo(stepId);
        assertThat(addedStep.getStepType()).isEqualTo("LOAD_FLOW");
        assertThat(addedStep.getStatus()).isEqualTo(StepStatus.RUNNING);
        assertThat(addedStep.getResultId()).isEqualTo(resultId);
        assertThat(addedStep.getResultType()).isEqualTo(ResultType.SECURITY_ANALYSIS);
        assertThat(addedStep.getStartedAt()).isEqualTo(startedAt);
        verify(executionRepository).save(execution);
    }

    @Test
    void updateStepStatusShouldUpdateExistingStep() {
        UUID stepId = UUID.randomUUID();
        UUID originalResultId = UUID.randomUUID();
        UUID newResultId = UUID.randomUUID();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();
        ProcessExecutionStepEntity existingStep = ProcessExecutionStepEntity.builder()
                .id(stepId)
                .stepType("LOAD_FLOW")
                .status(StepStatus.RUNNING)
                .resultId(originalResultId)
                .startedAt(startedAt)
                .build();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .type(ProcessType.SECURITY_ANALYSIS.name())
                .caseUuid(caseUuid)
                .userId(userId)
                .status(ProcessStatus.RUNNING)
                .steps(new ArrayList<>(List.of(existingStep)))
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        ProcessExecutionStep updateDto = ProcessExecutionStep.builder()
                .id(stepId)
                .stepType("LOAD_FLOW_UPDATED")
                .status(StepStatus.COMPLETED)
                .resultId(newResultId)
                .resultType(ResultType.SECURITY_ANALYSIS)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();

        processExecutionTxService.updateStepStatus(executionId, updateDto);

        verify(executionRepository).findById(executionId);
        assertThat(execution.getSteps()).hasSize(1);
        ProcessExecutionStepEntity updatedStep = execution.getSteps().getFirst();
        assertThat(updatedStep.getId()).isEqualTo(stepId);
        assertThat(updatedStep.getStepType()).isEqualTo("LOAD_FLOW_UPDATED");
        assertThat(updatedStep.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(updatedStep.getResultId()).isEqualTo(newResultId);
        assertThat(updatedStep.getResultType()).isEqualTo(ResultType.SECURITY_ANALYSIS);
        assertThat(updatedStep.getCompletedAt()).isEqualTo(completedAt);
        verify(executionRepository).save(execution);
    }

    @Test
    void updateStepsStatusesShouldUpdateExistingSteps() {
        UUID stepId1 = UUID.randomUUID();
        UUID stepId2 = UUID.randomUUID();
        UUID originalResultId1 = UUID.randomUUID();
        UUID originalResultId2 = UUID.randomUUID();
        UUID newResultId1 = UUID.randomUUID();
        UUID newResultId2 = UUID.randomUUID();
        Instant startedAt1 = Instant.now().minusSeconds(60);
        Instant startedAt2 = Instant.now().minusSeconds(40);
        Instant completedAt1 = Instant.now();
        Instant completedAt2 = Instant.now();

        ProcessExecutionStepEntity existingStep1 = ProcessExecutionStepEntity.builder()
            .id(stepId1)
            .stepType("LOAD_NETWORK")
            .status(StepStatus.RUNNING)
            .resultId(originalResultId1)
            .startedAt(startedAt1)
            .build();
        ProcessExecutionStepEntity existingStep2 = ProcessExecutionStepEntity.builder()
            .id(stepId2)
            .stepType("LOAD_FLOW")
            .status(StepStatus.RUNNING)
            .resultId(originalResultId2)
            .startedAt(startedAt2)
            .build();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
            .id(executionId)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(caseUuid)
            .userId(userId)
            .status(ProcessStatus.RUNNING)
            .steps(new ArrayList<>(List.of(existingStep1, existingStep2)))
            .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        ProcessExecutionStep updateDto1 = ProcessExecutionStep.builder()
            .id(stepId1)
            .stepType("LOAD_NETWORK_UPDATED")
            .status(StepStatus.COMPLETED)
            .resultId(newResultId1)
            .resultType(ResultType.SECURITY_ANALYSIS)
            .startedAt(startedAt1)
            .completedAt(completedAt1)
            .build();
        ProcessExecutionStep updateDto2 = ProcessExecutionStep.builder()
            .id(stepId2)
            .stepType("LOAD_FLOW_UPDATED")
            .status(StepStatus.COMPLETED)
            .resultId(newResultId2)
            .resultType(ResultType.SECURITY_ANALYSIS)
            .startedAt(startedAt2)
            .completedAt(completedAt2)
            .build();
        processExecutionTxService.updateStepsStatuses(executionId, List.of(updateDto1, updateDto2));

        verify(executionRepository).findById(executionId);
        assertThat(execution.getSteps()).hasSize(2);
        ProcessExecutionStepEntity updatedStep1 = execution.getSteps().get(0);
        assertThat(updatedStep1.getId()).isEqualTo(stepId1);
        assertThat(updatedStep1.getStepType()).isEqualTo("LOAD_NETWORK_UPDATED");
        assertThat(updatedStep1.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(updatedStep1.getResultId()).isEqualTo(newResultId1);
        assertThat(updatedStep1.getResultType()).isEqualTo(ResultType.SECURITY_ANALYSIS);
        assertThat(updatedStep1.getCompletedAt()).isEqualTo(completedAt1);

        ProcessExecutionStepEntity updatedStep2 = execution.getSteps().get(1);
        assertThat(updatedStep2.getId()).isEqualTo(stepId2);
        assertThat(updatedStep2.getStepType()).isEqualTo("LOAD_FLOW_UPDATED");
        assertThat(updatedStep2.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(updatedStep2.getResultId()).isEqualTo(newResultId2);
        assertThat(updatedStep2.getResultType()).isEqualTo(ResultType.SECURITY_ANALYSIS);
        assertThat(updatedStep2.getCompletedAt()).isEqualTo(completedAt2);

        verify(executionRepository).save(execution);
    }

    @Test
    void getReportIdShouldReturnExecutionReportId() {
        ProcessExecutionStepEntity step0 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepOrder(0)
                .build();
        ProcessExecutionStepEntity step1 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepOrder(1)
                .build();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .reportId(reportId)
                .userId(userId)
                .steps(List.of(step0, step1))
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        Optional<UUID> result = processExecutionTxService.getReportId(executionId);
        assertThat(result).contains(reportId);

        verify(executionRepository).findById(executionId);
    }

    @Test
    void getReportIdShouldReturnEmptyWhenExecutionNotFound() {
        UUID executionUuid = UUID.randomUUID();
        when(executionRepository.findById(executionUuid)).thenReturn(Optional.empty());

        Optional<UUID> reports = processExecutionTxService.getReportId(executionUuid);

        assertThat(reports).isNotPresent();
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getResultInfosShouldReturnResultInfos() {
        UUID resultId1 = UUID.randomUUID();
        UUID resultId2 = UUID.randomUUID();
        ProcessExecutionStepEntity step0 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepOrder(0)
                .resultId(resultId1)
                .resultType(ResultType.SECURITY_ANALYSIS)
                .build();
        ProcessExecutionStepEntity step1 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepOrder(1)
                .resultId(resultId2)
                .resultType(ResultType.SECURITY_ANALYSIS)
                .build();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(executionId)
                .steps(List.of(step0, step1))
                .userId(userId)
                .build();
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        Optional<List<ResultInfos>> results = processExecutionTxService.getResultInfos(executionId);

        assertThat(results).isPresent();
        assertThat(results.get()).usingRecursiveComparison().isEqualTo(List.of(
                new ResultInfos(resultId1, ResultType.SECURITY_ANALYSIS),
                new ResultInfos(resultId2, ResultType.SECURITY_ANALYSIS)
        ));
        verify(executionRepository).findById(executionId);
    }

    @Test
    void getResultsShouldReturnEmptyWhenExecutionNotFound() {
        UUID executionUuid = UUID.randomUUID();
        when(executionRepository.findById(executionUuid)).thenReturn(Optional.empty());

        Optional<List<ResultInfos>> results = processExecutionTxService.getResultInfos(executionUuid);

        assertThat(results).isEmpty();
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getLaunchedProcessesByType() {
        UUID execution1Uuid = UUID.randomUUID();
        UUID case1Uuid = UUID.randomUUID();
        UUID config1Uuid = UUID.randomUUID();
        UUID report1Uuid = UUID.randomUUID();
        Instant scheduledAt1 = Instant.now().minusSeconds(60);
        Instant startedAt1 = Instant.now().minusSeconds(30);
        Instant completedAt1 = Instant.now();
        ProcessExecutionEntity execution1 = ProcessExecutionEntity.builder()
            .id(execution1Uuid)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case1Uuid)
            .processConfigId(config1Uuid)
            .status(ProcessStatus.COMPLETED)
            .executionEnvName("env1")
            .scheduledAt(scheduledAt1)
            .startedAt(startedAt1)
            .completedAt(completedAt1)
            .reportId(report1Uuid)
            .userId("user1")
            .build();

        UUID execution2Uuid = UUID.randomUUID();
        UUID case2Uuid = UUID.randomUUID();
        UUID config2Uuid = UUID.randomUUID();
        UUID report2Uuid = UUID.randomUUID();
        Instant scheduledAt2 = Instant.now().minusSeconds(90);
        Instant startedAt2 = Instant.now().minusSeconds(80);
        ProcessExecutionEntity execution2 = ProcessExecutionEntity.builder()
            .id(execution2Uuid)
            .type(ProcessType.LOADFLOW.name())
            .caseUuid(case2Uuid)
            .processConfigId(config2Uuid)
            .status(ProcessStatus.RUNNING)
            .executionEnvName("env2")
            .scheduledAt(scheduledAt2)
            .startedAt(startedAt2)
            .reportId(report2Uuid)
            .userId("user2")
            .build();

        when(executionRepository.findByTypeAndStartedAtIsNotNullOrderByStartedAtDesc(ProcessType.SECURITY_ANALYSIS.name())).thenReturn(List.of(execution1));

        List<ProcessExecution> result = processExecutionTxService.getLaunchedProcesses(ProcessType.SECURITY_ANALYSIS);

        ProcessExecution processExecution1 = new ProcessExecution(execution1Uuid, ProcessType.SECURITY_ANALYSIS.name(), case1Uuid, config1Uuid, ProcessStatus.COMPLETED, "env1", scheduledAt1,
                startedAt1, completedAt1, report1Uuid, "user1");

        assertThat(result).hasSize(1).containsExactly(processExecution1);
        verify(executionRepository).findByTypeAndStartedAtIsNotNullOrderByStartedAtDesc(ProcessType.SECURITY_ANALYSIS.name());
    }

    @Test
    void getAllLaunchedProcesses() {
        UUID execution1Uuid = UUID.randomUUID();
        UUID case1Uuid = UUID.randomUUID();
        UUID config1Uuid = UUID.randomUUID();
        UUID report1Uuid = UUID.randomUUID();
        Instant scheduledAt1 = Instant.now().minusSeconds(60);
        Instant startedAt1 = Instant.now().minusSeconds(30);
        Instant completedAt1 = Instant.now();
        ProcessExecutionEntity execution1 = ProcessExecutionEntity.builder()
            .id(execution1Uuid)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case1Uuid)
            .processConfigId(config1Uuid)
            .status(ProcessStatus.COMPLETED)
            .executionEnvName("env1")
            .scheduledAt(scheduledAt1)
            .startedAt(startedAt1)
            .completedAt(completedAt1)
            .reportId(report1Uuid)
            .userId("user1")
            .build();

        UUID execution2Uuid = UUID.randomUUID();
        UUID case2Uuid = UUID.randomUUID();
        UUID config2Uuid = UUID.randomUUID();
        UUID report2Uuid = UUID.randomUUID();
        Instant scheduledAt2 = Instant.now().minusSeconds(90);
        Instant startedAt2 = Instant.now().minusSeconds(80);
        ProcessExecutionEntity execution2 = ProcessExecutionEntity.builder()
            .id(execution2Uuid)
            .type(ProcessType.LOADFLOW.name())
            .caseUuid(case2Uuid)
            .processConfigId(config2Uuid)
            .status(ProcessStatus.RUNNING)
            .executionEnvName("env2")
            .scheduledAt(scheduledAt2)
            .startedAt(startedAt2)
            .reportId(report2Uuid)
            .userId("user2")
            .build();

        when(executionRepository.findAllByScheduledAtIsNotNullOrderByScheduledAtDesc()).thenReturn(List.of(execution2, execution1));

        List<ProcessExecution> result = processExecutionTxService.getLaunchedProcesses(null);

        ProcessExecution processExecution1 = new ProcessExecution(execution1Uuid, ProcessType.SECURITY_ANALYSIS.name(), case1Uuid, config1Uuid, ProcessStatus.COMPLETED, "env1", scheduledAt1,
            startedAt1, completedAt1, report1Uuid, "user1");
        ProcessExecution processExecution2 = new ProcessExecution(execution2Uuid, ProcessType.LOADFLOW.name(), case2Uuid, config2Uuid, ProcessStatus.RUNNING, "env2", scheduledAt2,
            startedAt2, null, report2Uuid, "user2");

        assertThat(result).hasSize(2).containsExactly(processExecution2, processExecution1);
        verify(executionRepository).findAllByScheduledAtIsNotNullOrderByScheduledAtDesc();
    }

    @Test
    void getExecutionReturnsExecution() {
        UUID executionUuid = UUID.randomUUID();
        UUID configUuid = UUID.randomUUID();
        UUID reportUuid = UUID.randomUUID();
        Instant scheduledAt = Instant.now().minusSeconds(60);
        Instant startedAt = Instant.now().minusSeconds(30);
        Instant completedAt = Instant.now();
        ProcessExecutionEntity processExecutionEntity = ProcessExecutionEntity.builder()
            .id(executionUuid)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(caseUuid)
            .processConfigId(configUuid)
            .status(ProcessStatus.COMPLETED)
            .executionEnvName("env1")
            .scheduledAt(scheduledAt)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .reportId(reportUuid)
            .userId("user1")
            .build();
        ProcessExecution processExecution = new ProcessExecution(executionUuid, ProcessType.SECURITY_ANALYSIS.name(), caseUuid, configUuid, ProcessStatus.COMPLETED, "env1", scheduledAt,
            startedAt, completedAt, reportUuid, "user1");

        when(executionRepository.findById(executionUuid)).thenReturn(Optional.of(processExecutionEntity));

        Optional<ProcessExecution> result = processExecutionTxService.getExecution(executionUuid);

        assertThat(result).contains(processExecution);
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getExecutionNotFoundReturnsEmpty() {
        UUID executionUuid = UUID.randomUUID();

        when(executionRepository.findById(executionUuid)).thenReturn(Optional.empty());

        Optional<ProcessExecution> result = processExecutionTxService.getExecution(executionUuid);

        assertThat(result).isEmpty();
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getStepsInfos() {
        UUID executionUuid = UUID.randomUUID();
        UUID stepId1 = UUID.randomUUID();
        UUID stepId2 = UUID.randomUUID();
        Instant startedAt1 = Instant.now();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
            .id(executionUuid)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .steps(List.of(ProcessExecutionStepEntity.builder().id(stepId1).stepType("loadNetwork").stepOrder(0).status(StepStatus.RUNNING).startedAt(startedAt1).build(),
                ProcessExecutionStepEntity.builder().id(stepId2).stepType("applyModifs").stepOrder(1).status(StepStatus.SCHEDULED).build()))
            .build();

        when(executionRepository.findById(executionUuid)).thenReturn(Optional.of(execution));

        Optional<List<ProcessExecutionStep>> result = processExecutionTxService.getStepsInfos(executionUuid);

        ProcessExecutionStep processExecutionStep1 = new ProcessExecutionStep(stepId1, "loadNetwork", 0, StepStatus.RUNNING, null, null, startedAt1, null);
        ProcessExecutionStep processExecutionStep2 = new ProcessExecutionStep(stepId2, "applyModifs", 1, StepStatus.SCHEDULED, null, null, null, null);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2).containsExactly(processExecutionStep1, processExecutionStep2);
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getStepsInfosShouldReturnEmptyWhenExecutionNotFound() {
        UUID executionUuid = UUID.randomUUID();
        when(executionRepository.findById(executionUuid)).thenReturn(Optional.empty());

        Optional<List<ProcessExecutionStep>> result = processExecutionTxService.getStepsInfos(executionUuid);

        assertThat(result).isEmpty();
        verify(executionRepository).findById(executionUuid);
    }

    @Test
    void getStepsInfosShouldReturnEmptyListWhenNoSteps() {
        UUID executionUuid = UUID.randomUUID();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
            .id(executionUuid)
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .steps(null)
            .build();
        when(executionRepository.findById(executionUuid)).thenReturn(Optional.of(execution));

        Optional<List<ProcessExecutionStep>> result = processExecutionTxService.getStepsInfos(executionUuid);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @Test
    void deleteExecutionShouldDeleteExecutionAndReturnResultsAndReports() {
        UUID resultId1 = UUID.randomUUID();
        UUID resultId2 = UUID.randomUUID();
        ProcessExecutionStepEntity step0 = ProcessExecutionStepEntity.builder()
            .id(UUID.randomUUID())
            .stepOrder(0)
            .resultId(resultId1)
            .build();
        ProcessExecutionStepEntity step1 = ProcessExecutionStepEntity.builder()
            .id(UUID.randomUUID())
            .stepOrder(1)
            .resultId(resultId2)
            .resultType(ResultType.SECURITY_ANALYSIS)
            .build();
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
            .id(executionId)
            .reportId(reportId)
            .steps(List.of(step0, step1))
            .build();

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        doNothing().when(executionRepository).delete(execution);

        Optional<ProcessDeletionInfos> processDeletionInfos = processExecutionTxService.deleteExecution(executionId);
        assertThat(processDeletionInfos.get()).usingRecursiveComparison()
            .isEqualTo(new ProcessDeletionInfos(reportId, List.of(new ResultInfos(resultId2, ResultType.SECURITY_ANALYSIS))));

        verify(executionRepository).findById(executionId);
        verify(executionRepository).delete(execution);
    }

    @Test
    void deleteExecutionShouldReturnFalseWhenExecutionNotFound() {
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        Optional<ProcessDeletionInfos> deletedExecution = processExecutionTxService.deleteExecution(executionId);
        assertThat(deletedExecution).isNotPresent();

        verify(executionRepository).findById(executionId);
        verifyNoMoreInteractions(executionRepository);
    }

    @Test
    void getExistingDebugInfo() {
        String debugFileLocation = "debug/file/location";
        ProcessExecutionEntity execution = new ProcessExecutionEntity();
        execution.setDebugFileLocation(debugFileLocation);

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        Optional<String> result = processExecutionTxService.getDebugFileLocation(executionId);

        assertThat(result).isPresent();
        assertThat(result.get()).contains(debugFileLocation);

        verify(executionRepository).findById(executionId);
    }

    @Test
    void getNotExistingExecutionDebugInfo() {
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        Optional<String> result = processExecutionTxService.getDebugFileLocation(executionId);

        assertThat(result).isEmpty();

        verify(executionRepository).findById(executionId);
    }

    @Test
    void getExecutionWithoutDebugInfo() {
        ProcessExecutionEntity execution = new ProcessExecutionEntity();

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        Optional<String> result = processExecutionTxService.getDebugFileLocation(executionId);

        assertThat(result).isEmpty();

        verify(executionRepository).findById(executionId);
    }
}
