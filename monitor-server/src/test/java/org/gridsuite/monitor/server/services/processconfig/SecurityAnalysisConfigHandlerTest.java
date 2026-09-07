/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.SecurityAnalysisConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.entities.processconfig.SecurityAnalysisConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.SecurityAnalysisConfigMapper;
import org.gridsuite.monitor.server.repositories.processconfig.SecurityAnalysisConfigRepository;
import org.gridsuite.monitor.server.services.processconfig.handlers.SecurityAnalysisConfigHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@ExtendWith(MockitoExtension.class)
class SecurityAnalysisConfigHandlerTest extends AbstractProcessConfigHandlerTest<
    SecurityAnalysisConfig,
    SecurityAnalysisConfigEntity,
    SecurityAnalysisConfigMapper,
    SecurityAnalysisConfigRepository,
    SecurityAnalysisConfigHandler> {

    @Override
    ProcessType getExpectedProcessType() {
        return ProcessType.SECURITY_ANALYSIS;
    }

    @Override
    SecurityAnalysisConfigMapper createMapper() {
        return mock(SecurityAnalysisConfigMapper.class);
    }

    @Override
    SecurityAnalysisConfigRepository createRepository() {
        return mock(SecurityAnalysisConfigRepository.class);
    }

    @Override
    SecurityAnalysisConfigHandler createHandler(SecurityAnalysisConfigMapper mapper, SecurityAnalysisConfigRepository repository) {
        return new SecurityAnalysisConfigHandler(mapper, repository);
    }

    @Override
    SecurityAnalysisConfig createProcessConfig() {
        return mock(SecurityAnalysisConfig.class);
    }

    @Override
    SecurityAnalysisConfigEntity createProcessConfigEntity() {
        return mock(SecurityAnalysisConfigEntity.class);
    }
}
