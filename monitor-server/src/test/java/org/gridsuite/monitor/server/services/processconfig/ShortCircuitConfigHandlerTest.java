/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.ShortCircuitConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.entities.processconfig.ShortCircuitConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.ShortCircuitConfigMapper;
import org.gridsuite.monitor.server.repositories.processconfig.ShortCircuitConfigRepository;
import org.gridsuite.monitor.server.services.processconfig.handlers.ShortCircuitConfigHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@ExtendWith(MockitoExtension.class)
class ShortCircuitConfigHandlerTest extends AbstractProcessConfigHandlerTest<
    ShortCircuitConfig,
    ShortCircuitConfigEntity,
    ShortCircuitConfigMapper,
    ShortCircuitConfigRepository,
    ShortCircuitConfigHandler> {

    @Override
    ProcessType getExpectedProcessType() {
        return ProcessType.SHORT_CIRCUIT;
    }

    @Override
    ShortCircuitConfigMapper createMapper() {
        return mock(ShortCircuitConfigMapper.class);
    }

    @Override
    ShortCircuitConfigRepository createRepository() {
        return mock(ShortCircuitConfigRepository.class);
    }

    @Override
    ShortCircuitConfigHandler createHandler(ShortCircuitConfigMapper mapper, ShortCircuitConfigRepository repository) {
        return new ShortCircuitConfigHandler(mapper, repository);
    }

    @Override
    ShortCircuitConfig createProcessConfig() {
        return mock(ShortCircuitConfig.class);
    }

    @Override
    ShortCircuitConfigEntity createProcessConfigEntity() {
        return mock(ShortCircuitConfigEntity.class);
    }
}
