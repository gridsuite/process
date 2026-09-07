/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.ProcessConfig;
import org.gridsuite.monitor.commons.types.processconfig.ProcessConfigFieldComparison;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.dto.processconfig.MetadataInfos;
import org.gridsuite.monitor.server.dto.processconfig.PersistedProcessConfig;
import org.gridsuite.monitor.server.dto.processconfig.ProcessConfigComparison;
import org.gridsuite.monitor.server.entities.processconfig.AbstractProcessConfigEntity;
import org.gridsuite.monitor.server.error.MonitorServerException;
import org.gridsuite.monitor.server.repositories.processconfig.ProcessConfigRepository;
import org.gridsuite.monitor.server.services.processconfig.handlers.ProcessConfigHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gridsuite.monitor.server.error.MonitorServerBusinessErrorCode.DIFFERENT_PROCESS_CONFIG_TYPE;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class ProcessConfigService {
    private final ProcessConfigRepository processConfigRepository;
    private final Map<ProcessType, ProcessConfigHandler<?, ?>> processConfigHandlers;

    public ProcessConfigService(ProcessConfigRepository processConfigRepository,
                                List<ProcessConfigHandler<?, ?>> listHandlers) {
        this.processConfigRepository = processConfigRepository;
        this.processConfigHandlers = listHandlers.stream().collect(Collectors.toMap(
            ProcessConfigHandler::getProcessType, Function.identity(), (h1, h2) -> {
                throw new IllegalStateException(String.format("Duplicate process config handlers for process type: %s", h1.getProcessType()));
            }
        ));
    }

    @SuppressWarnings("unchecked")
    private <C extends ProcessConfig, E extends AbstractProcessConfigEntity> ProcessConfigHandler<C, E> getHandler(ProcessType processType) {
        ProcessConfigHandler<?, ?> handler = processConfigHandlers.get(processType);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported process config type: " + processType);
        }
        return (ProcessConfigHandler<C, E>) handler;
    }

    @Transactional
    public UUID createProcessConfig(ProcessConfig processConfig) {
        AbstractProcessConfigEntity entity = getHandler(processConfig.processType()).toEntity(processConfig);
        return processConfigRepository.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public Optional<PersistedProcessConfig> getProcessConfig(UUID processConfigUuid) {
        return processConfigRepository.findById(processConfigUuid)
            .map(this::toPersistedProcessConfig);
    }

    @Transactional(readOnly = true)
    public List<PersistedProcessConfig> getProcessConfigs(ProcessType processType) {
        return getHandler(processType).findAll().stream()
            .map(this::toPersistedProcessConfig)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MetadataInfos> getProcessConfigsMetadata(List<UUID> processConfigUuids) {
        return processConfigRepository.findAllById(processConfigUuids).stream()
            .map(entity -> new MetadataInfos(entity.getId(), entity.getProcessType()))
            .toList();
    }

    @Transactional
    public Optional<UUID> updateProcessConfig(UUID processConfigUuid, ProcessConfig processConfig) {
        return processConfigRepository.findById(processConfigUuid)
            .map(entity -> {
                if (entity.getProcessType() != processConfig.processType()) {
                    throw new IllegalArgumentException("Process config type mismatch : " + entity.getProcessType());
                }
                getHandler(processConfig.processType()).update(processConfig, entity);
                return processConfigUuid;
            });
    }

    @Transactional
    public Optional<UUID> duplicateProcessConfig(UUID sourceProcessConfigUuid) {
        return processConfigRepository.findById(sourceProcessConfigUuid)
            .map(sourceEntity -> {
                AbstractProcessConfigEntity entity = getHandler(sourceEntity.getProcessType()).copyEntity(sourceEntity);
                return processConfigRepository.save(entity).getId();
            });
    }

    @Transactional
    public Optional<UUID> deleteProcessConfig(UUID processConfigUuid) {
        if (processConfigRepository.existsById(processConfigUuid)) {
            processConfigRepository.deleteById(processConfigUuid);
            return Optional.of(processConfigUuid);
        } else {
            return Optional.empty();
        }
    }

    private PersistedProcessConfig toPersistedProcessConfig(AbstractProcessConfigEntity entity) {
        return new PersistedProcessConfig(entity.getId(), toProcessConfig(entity));
    }

    private ProcessConfig toProcessConfig(AbstractProcessConfigEntity entity) {
        return getHandler(entity.getProcessType()).toDto(entity);
    }

    @Transactional(readOnly = true)
    public Optional<ProcessConfigComparison> compareProcessConfigs(UUID uuid1, UUID uuid2) {
        Optional<AbstractProcessConfigEntity> processConfigEntity1 = processConfigRepository.findById(uuid1);
        Optional<AbstractProcessConfigEntity> processConfigEntity2 = processConfigRepository.findById(uuid2);

        if (processConfigEntity1.isEmpty() || processConfigEntity2.isEmpty()) {
            return Optional.empty();
        }

        if (processConfigEntity1.get().getProcessType() != processConfigEntity2.get().getProcessType()) {
            throw new MonitorServerException(DIFFERENT_PROCESS_CONFIG_TYPE, "Cannot compare different process config types",
                Map.of("processConfigEntity1Type", processConfigEntity1.get().getProcessType(), "processConfigEntity2Type", processConfigEntity2.get().getProcessType()));
        }

        ProcessConfig processConfig1 = toProcessConfig(processConfigEntity1.get());
        ProcessConfig processConfig2 = toProcessConfig(processConfigEntity2.get());

        List<ProcessConfigFieldComparison> differences = processConfig1.compareWith(processConfig2);
        boolean identical = differences.stream().allMatch(ProcessConfigFieldComparison::identical);

        return Optional.of(new ProcessConfigComparison(uuid1, uuid2, identical, differences));
    }
}
