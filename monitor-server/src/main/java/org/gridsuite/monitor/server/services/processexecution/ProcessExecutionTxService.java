/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processexecution;

import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStatusUpdate;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.commons.types.processexecution.ProcessStatus;
import org.gridsuite.monitor.commons.types.result.ResultInfos;
import org.gridsuite.monitor.server.dto.processconfig.PersistedProcessConfig;
import org.gridsuite.monitor.server.dto.processexecution.ProcessExecution;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionEntity;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionStepEntity;
import org.gridsuite.monitor.server.mappers.processexecution.ProcessExecutionMapper;
import org.gridsuite.monitor.server.mappers.processexecution.ProcessExecutionStepMapper;
import org.gridsuite.monitor.server.repositories.ProcessExecutionRepository;
import org.gridsuite.monitor.server.services.processconfig.ProcessConfigService;
import org.gridsuite.monitor.server.utils.S3PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProcessExecutionTxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutionTxService.class);

    private final ProcessExecutionRepository processExecutionRepository;
    private final ProcessExecutionStepMapper processExecutionStepMapper;
    private final ProcessExecutionMapper processExecutionMapper;
    private final ProcessConfigService processConfigService;
    private final S3PathResolver s3PathResolver;

    public ProcessExecutionTxService(ProcessExecutionRepository processExecutionRepository,
                                     ProcessExecutionStepMapper processExecutionStepMapper,
                                     ProcessExecutionMapper processExecutionMapper,
                                     ProcessConfigService processConfigService,
                                     S3PathResolver s3PathResolver) {
        this.processExecutionRepository = processExecutionRepository;
        this.processExecutionStepMapper = processExecutionStepMapper;
        this.processExecutionMapper = processExecutionMapper;
        this.processConfigService = processConfigService;
        this.s3PathResolver = s3PathResolver;
    }

    @Transactional
    public Optional<ProcessCreationResult> createExecution(UUID caseUuid, String userId, UUID processConfigId, UUID executionId, UUID reportId, boolean isDebug) {
        Optional<PersistedProcessConfig> persistedProcessConfigOpt = processConfigService.getProcessConfig(processConfigId);
        if (persistedProcessConfigOpt.isPresent()) {
            PersistedProcessConfig persistedProcessConfig = persistedProcessConfigOpt.get();

            String debugFileLocation = isDebug
                ? s3PathResolver.toDebugLocation(persistedProcessConfig.processConfig().processType().name(), executionId)
                : null;

            processExecutionRepository.save(ProcessExecutionEntity.builder()
                .id(executionId)
                .type(persistedProcessConfig.processConfig().processType().name())
                .caseUuid(caseUuid)
                .processConfigId(persistedProcessConfig.id())
                .status(ProcessStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .reportId(reportId)
                .userId(userId)
                .debugFileLocation(debugFileLocation)
                .build());

            return Optional.of(new ProcessCreationResult(
                debugFileLocation,
                persistedProcessConfig.processConfig()
            ));
        }
        return Optional.empty();
    }

    @Transactional
    public void updateExecutionStatus(UUID executionId, ProcessExecutionStatusUpdate payload) {
        processExecutionRepository.findById(executionId).ifPresentOrElse(execution -> {
            execution.setStatus(payload.getStatus());
            if (payload.getExecutionEnvName() != null) {
                execution.setExecutionEnvName(payload.getExecutionEnvName());
            }
            if (payload.getStartedAt() != null) {
                execution.setStartedAt(payload.getStartedAt());
            }
            if (payload.getCompletedAt() != null) {
                execution.setCompletedAt(payload.getCompletedAt());
            }
            processExecutionRepository.save(execution);
        }, () -> LOGGER.warn("Execution {} not found in DB, ignoring status update", executionId));
    }

    @Transactional
    public void updateStepStatus(UUID executionId, ProcessExecutionStep processExecutionStep) {
        processExecutionRepository.findById(executionId).ifPresentOrElse(execution -> {
            ProcessExecutionStepEntity stepEntity = processExecutionStepMapper.toEntity(processExecutionStep);
            updateStep(execution, stepEntity);
            processExecutionRepository.save(execution);
        }, () -> LOGGER.warn("Execution {} not found in DB, ignoring step update", executionId));
    }

    @Transactional
    public void updateStepsStatuses(UUID executionId, List<ProcessExecutionStep> processExecutionSteps) {
        processExecutionRepository.findById(executionId).ifPresentOrElse(execution -> {
            processExecutionSteps.forEach(processExecutionStep -> {
                ProcessExecutionStepEntity stepEntity = processExecutionStepMapper.toEntity(processExecutionStep);
                updateStep(execution, stepEntity);
            });
            processExecutionRepository.save(execution);
        }, () -> LOGGER.warn("Execution {} not found in DB, ignoring steps update", executionId));
    }

    public Optional<UUID> getReportId(UUID executionId) {
        return processExecutionRepository.findById(executionId)
            .map(ProcessExecutionEntity::getReportId);
    }

    public Optional<List<ResultInfos>> getResultInfos(UUID executionId) {
        return processExecutionRepository.findById(executionId)
            .map(execution -> Optional.ofNullable(execution.getSteps()).orElse(List.of()).stream()
                .filter(step -> step.getResultId() != null)
                .map(step -> new ResultInfos(step.getResultId(), step.getResultType()))
                .toList());
    }

    public Optional<String> getDebugFileLocation(UUID executionId) {
        return processExecutionRepository.findById(executionId)
            .map(ProcessExecutionEntity::getDebugFileLocation);
    }

    public List<ProcessExecution> getLaunchedProcesses() {
        return processExecutionRepository.findAllByScheduledAtIsNotNullOrderByScheduledAtDesc().stream()
            .map(processExecutionMapper::toDto)
            .toList();
    }

    public Optional<ProcessExecution> getExecution(UUID executionId) {
        return processExecutionRepository.findById(executionId)
            .map(processExecutionMapper::toDto);
    }

    public Optional<List<ProcessExecutionStep>> getStepsInfos(UUID executionId) {
        return processExecutionRepository.findById(executionId)
            .map(execution -> Optional.ofNullable(execution.getSteps()).orElse(List.of()).stream()
                .map(processExecutionStepMapper::toDto)
                .toList());
    }

    @Transactional
    public Optional<ProcessDeletionInfos> deleteExecution(UUID executionId) {
        Optional<ProcessExecutionEntity> opt =
            processExecutionRepository.findById(executionId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        ProcessExecutionEntity entity = opt.get();
        ProcessDeletionInfos infos = ProcessDeletionInfos.fromProcessExecutionEntity(entity);

        processExecutionRepository.delete(entity);

        return Optional.of(infos);
    }

    private void updateStep(ProcessExecutionEntity execution, ProcessExecutionStepEntity stepEntity) {
        List<ProcessExecutionStepEntity> steps = Optional.ofNullable(execution.getSteps()).orElseGet(() -> {
            List<ProcessExecutionStepEntity> newSteps = new ArrayList<>();
            execution.setSteps(newSteps);
            return newSteps;
        });
        steps.stream()
            .filter(s -> s.getId().equals(stepEntity.getId()))
            .findFirst()
            .ifPresentOrElse(
                existingStep -> processExecutionStepMapper.updateEntityFromEntity(stepEntity, existingStep),
                () -> steps.add(stepEntity));
    }
}
