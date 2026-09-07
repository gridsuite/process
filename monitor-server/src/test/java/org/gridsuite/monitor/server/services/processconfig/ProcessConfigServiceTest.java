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
import org.gridsuite.monitor.server.entities.processconfig.LoadFlowConfigEntity;
import org.gridsuite.monitor.server.entities.processconfig.SecurityAnalysisConfigEntity;
import org.gridsuite.monitor.server.error.MonitorServerException;
import org.gridsuite.monitor.server.repositories.processconfig.ProcessConfigRepository;
import org.gridsuite.monitor.server.services.processconfig.handlers.ProcessConfigHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.gridsuite.monitor.server.error.MonitorServerBusinessErrorCode.DIFFERENT_PROCESS_CONFIG_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ExtendWith(MockitoExtension.class)
class ProcessConfigServiceTest {
    @Mock
    private ProcessConfigRepository processConfigRepository;

    @Mock
    private ProcessConfigHandler<ProcessConfig, AbstractProcessConfigEntity> handler;

    private ProcessConfigService processConfigService;

    private static final ProcessType PROCESS_TYPE = ProcessType.SECURITY_ANALYSIS;
    @Mock
    private ProcessConfig processConfig;
    @Mock
    private AbstractProcessConfigEntity processConfigEntity;

    @BeforeEach
    void setUp() {
        when(handler.getProcessType()).thenReturn(PROCESS_TYPE);

        processConfigService = new ProcessConfigService(
            processConfigRepository,
            List.of(handler)
        );
    }

    @Test
    void createProcessConfig() {
        UUID expectedProcessConfigUuid = UUID.randomUUID();

        AbstractProcessConfigEntity savedEntity = mock(AbstractProcessConfigEntity.class);

        when(processConfig.processType()).thenReturn(PROCESS_TYPE);
        when(handler.toEntity(processConfig)).thenReturn(processConfigEntity);
        when(processConfigRepository.save(processConfigEntity)).thenReturn(savedEntity);
        when(savedEntity.getId()).thenReturn(expectedProcessConfigUuid);

        UUID result = processConfigService.createProcessConfig(processConfig);

        assertThat(result).isEqualTo(expectedProcessConfigUuid);
        verify(handler).toEntity(processConfig);
        verify(processConfigRepository).save(processConfigEntity);
    }

    @Test
    void getProcessConfig() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(PROCESS_TYPE);
        when(handler.toDto(processConfigEntity)).thenReturn(processConfig);
        when(processConfigEntity.getId()).thenReturn(processConfigUuid);

        Optional<PersistedProcessConfig> result = processConfigService.getProcessConfig(processConfigUuid);

        assertThat(result).isPresent();
        assertThat(result.get().processConfig()).isEqualTo(processConfig);
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler).toDto(processConfigEntity);
    }

    @Test
    void getProcessConfigNotFound() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.empty());

        Optional<PersistedProcessConfig> result = processConfigService.getProcessConfig(processConfigUuid);

        assertThat(result).isEmpty();
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler, never()).toDto(any());
    }

    @Test
    void getProcessConfigs() {
        UUID processConfigUuid = UUID.randomUUID();

        when(handler.findAll()).thenReturn(List.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(PROCESS_TYPE);
        when(handler.toDto(processConfigEntity)).thenReturn(processConfig);
        when(processConfigEntity.getId()).thenReturn(processConfigUuid);

        List<PersistedProcessConfig> result = processConfigService.getProcessConfigs(PROCESS_TYPE);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().processConfig()).isEqualTo(processConfig);
        verify(handler).findAll();
        verify(handler).toDto(processConfigEntity);
    }

    @Test
    void getProcessConfigsMetadata() {
        UUID saUuid = UUID.randomUUID();
        UUID lfUuid = UUID.randomUUID();
        List<UUID> processConfigUuids = List.of(saUuid, lfUuid);

        SecurityAnalysisConfigEntity saEntity = new SecurityAnalysisConfigEntity();
        saEntity.setId(saUuid);
        LoadFlowConfigEntity lfEntity = new LoadFlowConfigEntity();
        lfEntity.setId(lfUuid);

        when(processConfigRepository.findAllById(processConfigUuids)).thenReturn(List.of(saEntity, lfEntity));

        List<MetadataInfos> result = processConfigService.getProcessConfigsMetadata(processConfigUuids);

        assertThat(result).isEqualTo(List.of(
            new MetadataInfos(saUuid, ProcessType.SECURITY_ANALYSIS),
            new MetadataInfos(lfUuid, ProcessType.LOADFLOW)
        ));
        verify(processConfigRepository).findAllById(processConfigUuids);
    }

    @Test
    void updateProcessConfig() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(PROCESS_TYPE);
        when(processConfig.processType()).thenReturn(PROCESS_TYPE);
        doNothing().when(handler).update(processConfig, processConfigEntity);

        Optional<UUID> result = processConfigService.updateProcessConfig(processConfigUuid, processConfig);

        assertThat(result)
            .isPresent()
            .contains(processConfigUuid);
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler).update(processConfig, processConfigEntity);
    }

    @Test
    void updateProcessConfigNotFound() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.empty());

        Optional<UUID> result = processConfigService.updateProcessConfig(processConfigUuid, processConfig);

        assertThat(result).isEmpty();
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler, never()).update(processConfig, processConfigEntity);
    }

    @Test
    void updateProcessConfigWithProcessTypeMismatch() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(ProcessType.SECURITY_ANALYSIS);
        when(processConfig.processType()).thenReturn(ProcessType.LOADFLOW);

        assertThatThrownBy(() -> processConfigService.updateProcessConfig(processConfigUuid, processConfig))
            .isInstanceOf(IllegalArgumentException.class);
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler, never()).update(processConfig, processConfigEntity);
    }

    @Test
    void duplicateSecurityAnalysisConfig() {
        UUID processConfigUuid = UUID.randomUUID();
        UUID copiedProcessConfigUuid = UUID.randomUUID();
        AbstractProcessConfigEntity copiedProcessConfigEntity = mock(AbstractProcessConfigEntity.class);

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(PROCESS_TYPE);
        when(handler.copyEntity(processConfigEntity)).thenReturn(copiedProcessConfigEntity);
        when(processConfigRepository.save(copiedProcessConfigEntity)).thenReturn(copiedProcessConfigEntity);
        when(copiedProcessConfigEntity.getId()).thenReturn(copiedProcessConfigUuid);

        Optional<UUID> result = processConfigService.duplicateProcessConfig(processConfigUuid);

        assertThat(result)
            .isPresent()
            .contains(copiedProcessConfigUuid);
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler).copyEntity(processConfigEntity);
        verify(processConfigRepository).save(copiedProcessConfigEntity);
    }

    @Test
    void duplicateProcessConfigNotFound() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.findById(processConfigUuid)).thenReturn(Optional.empty());

        Optional<UUID> result = processConfigService.duplicateProcessConfig(processConfigUuid);

        assertThat(result).isEmpty();
        verify(processConfigRepository).findById(processConfigUuid);
        verify(handler, never()).copyEntity(any());
    }

    @Test
    void deleteProcessConfig() {
        UUID processConfigUuid = UUID.randomUUID();
        when(processConfigRepository.existsById(processConfigUuid)).thenReturn(Boolean.TRUE);
        doNothing().when(processConfigRepository).deleteById(processConfigUuid);

        Optional<UUID> result = processConfigService.deleteProcessConfig(processConfigUuid);

        assertThat(result).contains(processConfigUuid);
        verify(processConfigRepository).existsById(processConfigUuid);
        verify(processConfigRepository).deleteById(processConfigUuid);
    }

    @Test
    void deleteProcessConfigNotFound() {
        UUID processConfigUuid = UUID.randomUUID();

        when(processConfigRepository.existsById(processConfigUuid)).thenReturn(Boolean.FALSE);

        Optional<UUID> result = processConfigService.deleteProcessConfig(processConfigUuid);

        assertThat(result).isEmpty();
        verify(processConfigRepository).existsById(processConfigUuid);
        verify(processConfigRepository, never()).deleteById(any());
    }

    @Test
    void compareProcessConfigsWithEqualProcessConfigs() {
        UUID processConfigUuid1 = UUID.randomUUID();
        UUID processConfigUuid2 = UUID.randomUUID();
        ProcessConfigFieldComparison fieldComparison = new ProcessConfigFieldComparison("field", true, "value1", "value2");

        when(processConfigRepository.findById(processConfigUuid1)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigRepository.findById(processConfigUuid2)).thenReturn(Optional.of(processConfigEntity));
        when(processConfigEntity.getProcessType()).thenReturn(PROCESS_TYPE);
        when(handler.toDto(processConfigEntity)).thenReturn(processConfig);
        when(processConfig.compareWith(processConfig)).thenReturn(List.of(fieldComparison));

        Optional<ProcessConfigComparison> result = processConfigService.compareProcessConfigs(processConfigUuid1, processConfigUuid2);

        assertThat(result)
            .isPresent()
            .contains(new ProcessConfigComparison(processConfigUuid1, processConfigUuid2, true, List.of(fieldComparison)));
        verify(processConfigRepository).findById(processConfigUuid1);
        verify(processConfigRepository).findById(processConfigUuid2);
        verify(handler, times(2)).toDto(processConfigEntity);
        verify(processConfig).compareWith(processConfig);
    }

    @Test
    void compareProcessConfigsWithDifferentProcessConfigs() {
        UUID processConfigUuid1 = UUID.randomUUID();
        UUID processConfigUuid2 = UUID.randomUUID();
        AbstractProcessConfigEntity processConfigEntity1 = mock(AbstractProcessConfigEntity.class);
        AbstractProcessConfigEntity processConfigEntity2 = mock(AbstractProcessConfigEntity.class);
        ProcessConfig processConfig1 = mock(ProcessConfig.class);
        ProcessConfig processConfig2 = mock(ProcessConfig.class);
        ProcessConfigFieldComparison fieldComparison = new ProcessConfigFieldComparison("field", false, "value1", "value2");

        when(processConfigRepository.findById(processConfigUuid1)).thenReturn(Optional.of(processConfigEntity1));
        when(processConfigRepository.findById(processConfigUuid2)).thenReturn(Optional.of(processConfigEntity2));
        when(processConfigEntity1.getProcessType()).thenReturn(PROCESS_TYPE);
        when(processConfigEntity2.getProcessType()).thenReturn(PROCESS_TYPE);
        when(handler.toDto(processConfigEntity1)).thenReturn(processConfig1);
        when(handler.toDto(processConfigEntity2)).thenReturn(processConfig2);
        when(processConfig1.compareWith(processConfig2)).thenReturn(List.of(fieldComparison));

        Optional<ProcessConfigComparison> result = processConfigService.compareProcessConfigs(processConfigUuid1, processConfigUuid2);

        assertThat(result)
            .isPresent()
            .contains(new ProcessConfigComparison(processConfigUuid1, processConfigUuid2, false, List.of(fieldComparison)));
        verify(processConfigRepository).findById(processConfigUuid1);
        verify(processConfigRepository).findById(processConfigUuid2);
        verify(handler).toDto(processConfigEntity1);
        verify(handler).toDto(processConfigEntity2);
        verify(processConfig1).compareWith(processConfig2);
    }

    @Test
    void compareProcessConfigWithFirstProcessConfigNotFound() {
        UUID processConfigUuid1 = UUID.randomUUID();
        UUID processConfigUuid2 = UUID.randomUUID();

        // test when the first process config is not found
        when(processConfigRepository.findById(processConfigUuid1)).thenReturn(Optional.empty());

        Optional<ProcessConfigComparison> result = processConfigService.compareProcessConfigs(processConfigUuid1, processConfigUuid2);

        assertThat(result).isEmpty();
        verify(processConfigRepository).findById(processConfigUuid1);
        verify(handler, never()).toDto(any());
    }

    @Test
    void compareProcessConfigWithSecondProcessConfigNotFound() {
        UUID processConfigUuid1 = UUID.randomUUID();
        UUID processConfigUuid2 = UUID.randomUUID();

        // test when the second process config is not found
        AbstractProcessConfigEntity processConfigEntity1 = mock(AbstractProcessConfigEntity.class);

        when(processConfigRepository.findById(processConfigUuid1)).thenReturn(Optional.of(processConfigEntity1));
        when(processConfigRepository.findById(processConfigUuid2)).thenReturn(Optional.empty());

        Optional<ProcessConfigComparison> result = processConfigService.compareProcessConfigs(processConfigUuid1, processConfigUuid2);

        assertThat(result).isEmpty();
        verify(processConfigRepository).findById(processConfigUuid1);
        verify(processConfigRepository).findById(processConfigUuid2);
        verify(handler, never()).toDto(any());
    }

    @Test
    void compareProcessConfigsShouldThrowWhenProcessTypesAreDifferent() {
        UUID processConfigUuid1 = UUID.randomUUID();
        UUID processConfigUuid2 = UUID.randomUUID();
        AbstractProcessConfigEntity processConfigEntity1 = mock(SecurityAnalysisConfigEntity.class);
        AbstractProcessConfigEntity processConfigEntity2 = mock(LoadFlowConfigEntity.class);

        when(processConfigRepository.findById(processConfigUuid1)).thenReturn(Optional.of(processConfigEntity1));
        when(processConfigRepository.findById(processConfigUuid2)).thenReturn(Optional.of(processConfigEntity2));
        when(processConfigEntity1.getProcessType()).thenReturn(ProcessType.SECURITY_ANALYSIS);
        when(processConfigEntity2.getProcessType()).thenReturn(ProcessType.LOADFLOW);

        assertThatThrownBy(() -> processConfigService.compareProcessConfigs(processConfigUuid1, processConfigUuid2))
            .isInstanceOf(MonitorServerException.class)
            .satisfies(ex -> {
                MonitorServerException e = (MonitorServerException) ex;
                assertThat(e.getErrorCode()).isEqualTo(DIFFERENT_PROCESS_CONFIG_TYPE);
            });
        verify(processConfigRepository).findById(processConfigUuid1);
        verify(processConfigRepository).findById(processConfigUuid2);
        verify(handler, never()).toDto(any());
    }
}
