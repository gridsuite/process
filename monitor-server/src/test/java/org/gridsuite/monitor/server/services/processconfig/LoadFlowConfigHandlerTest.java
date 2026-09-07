/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.LoadFlowConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.entities.processconfig.LoadFlowConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.LoadFlowConfigMapper;
import org.gridsuite.monitor.server.repositories.processconfig.LoadFlowConfigRepository;
import org.gridsuite.monitor.server.services.processconfig.handlers.LoadFlowConfigHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@ExtendWith(MockitoExtension.class)
class LoadFlowConfigHandlerTest extends AbstractProcessConfigHandlerTest<
    LoadFlowConfig,
    LoadFlowConfigEntity,
    LoadFlowConfigMapper,
    LoadFlowConfigRepository,
    LoadFlowConfigHandler> {

    @Override
    ProcessType getExpectedProcessType() {
        return ProcessType.LOADFLOW;
    }

    @Override
    LoadFlowConfigMapper createMapper() {
        return mock(LoadFlowConfigMapper.class);
    }

    @Override
    LoadFlowConfigRepository createRepository() {
        return mock(LoadFlowConfigRepository.class);
    }

    @Override
    LoadFlowConfigHandler createHandler(LoadFlowConfigMapper mapper, LoadFlowConfigRepository repository) {
        return new LoadFlowConfigHandler(mapper, repository);
    }

    @Override
    LoadFlowConfig createProcessConfig() {
        return mock(LoadFlowConfig.class);
    }

    @Override
    LoadFlowConfigEntity createProcessConfigEntity() {
        return mock(LoadFlowConfigEntity.class);
    }
}
