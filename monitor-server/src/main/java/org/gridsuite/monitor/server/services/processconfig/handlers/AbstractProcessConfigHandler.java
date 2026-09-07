/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig.handlers;

import org.gridsuite.monitor.commons.types.processconfig.ProcessConfig;
import org.gridsuite.monitor.server.entities.processconfig.AbstractProcessConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.ProcessConfigMapper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
public abstract class AbstractProcessConfigHandler<
        C extends ProcessConfig,
        E extends AbstractProcessConfigEntity
    > implements ProcessConfigHandler<C, E> {

    private final ProcessConfigMapper<C, E> mapper;
    private final JpaRepository<E, UUID> repository;

    protected AbstractProcessConfigHandler(
            ProcessConfigMapper<C, E> mapper,
            JpaRepository<E, UUID> repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public void update(C processConfig, E entity) {
        mapper.updateEntityFromDto(processConfig, entity);
    }

    @Override
    public E copyEntity(E sourceEntity) {
        return toEntity(toDto(sourceEntity));
    }

    @Override
    public E toEntity(C processConfig) {
        return mapper.toEntity(processConfig);
    }

    @Override
    public C toDto(E entity) {
        return mapper.toDto(entity);
    }

    @Override
    public List<E> findAll() {
        return repository.findAll();
    }
}
