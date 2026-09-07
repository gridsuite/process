/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.ProcessConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.entities.processconfig.AbstractProcessConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.ProcessConfigMapper;
import org.gridsuite.monitor.server.services.processconfig.handlers.ProcessConfigHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
public abstract class AbstractProcessConfigHandlerTest<
    C extends ProcessConfig,
    E extends AbstractProcessConfigEntity,
    M extends ProcessConfigMapper<C, E>,
    R extends JpaRepository<E, UUID>,
    H extends ProcessConfigHandler<C, E>> {

    protected M mapper;
    protected R repository;
    protected H handler;

    abstract ProcessType getExpectedProcessType();

    abstract M createMapper();

    abstract R createRepository();

    abstract H createHandler(M mapper, R repository);

    abstract C createProcessConfig();

    abstract E createProcessConfigEntity();

    @BeforeEach
    protected void setUp() {
        mapper = createMapper();
        repository = createRepository();
        handler = createHandler(mapper, repository);
    }

    @Test
    void getProcessTypeTest() {
        assertThat(handler.getProcessType()).isEqualTo(getExpectedProcessType());
    }

    @Test
    void updateTest() {
        E processConfigEntity = createProcessConfigEntity();
        C processConfig = createProcessConfig();

        handler.update(processConfig, processConfigEntity);

        verify(mapper).updateEntityFromDto(processConfig, processConfigEntity);
    }

    @Test
    void copyEntityTest() {
        E processConfigEntity1 = createProcessConfigEntity();
        E expectedProcessConfigEntity = createProcessConfigEntity();
        C processConfig = createProcessConfig();

        when(mapper.toDto(processConfigEntity1)).thenReturn(processConfig);
        when(mapper.toEntity(processConfig)).thenReturn(expectedProcessConfigEntity);

        AbstractProcessConfigEntity result = handler.copyEntity(processConfigEntity1);

        assertThat(result).isEqualTo(expectedProcessConfigEntity);
        verify(mapper).toDto(processConfigEntity1);
        verify(mapper).toEntity(any());
    }

    @Test
    void toEntityTest() {
        E expectedProcessConfigEntity = createProcessConfigEntity();
        C processConfig = createProcessConfig();

        when(mapper.toEntity(processConfig)).thenReturn(expectedProcessConfigEntity);

        AbstractProcessConfigEntity result = handler.toEntity(processConfig);

        assertThat(result).isEqualTo(expectedProcessConfigEntity);
        verify(mapper).toEntity(processConfig);
    }

    @Test
    void toDtoTest() {
        E processConfigEntity = createProcessConfigEntity();
        C expectedProcessConfig = createProcessConfig();

        when(mapper.toDto(processConfigEntity)).thenReturn(expectedProcessConfig);

        ProcessConfig result = handler.toDto(processConfigEntity);

        assertThat(result).isEqualTo(expectedProcessConfig);
        verify(mapper).toDto(processConfigEntity);
    }

    @Test
    void findAllTest() {
        E processConfigEntity1 = createProcessConfigEntity();
        E processConfigEntity2 = createProcessConfigEntity();

        when(repository.findAll()).thenReturn(List.of(processConfigEntity1, processConfigEntity2));

        List<E> result = handler.findAll();

        assertThat(result).isEqualTo(List.of(processConfigEntity1, processConfigEntity2));
        verify(repository).findAll();
    }
}
