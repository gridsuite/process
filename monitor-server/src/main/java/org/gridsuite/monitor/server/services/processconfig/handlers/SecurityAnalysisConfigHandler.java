/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processconfig.handlers;

import org.gridsuite.monitor.commons.types.processconfig.SecurityAnalysisConfig;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.server.entities.processconfig.SecurityAnalysisConfigEntity;
import org.gridsuite.monitor.server.mappers.processconfig.SecurityAnalysisConfigMapper;
import org.gridsuite.monitor.server.repositories.processconfig.SecurityAnalysisConfigRepository;
import org.springframework.stereotype.Service;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class SecurityAnalysisConfigHandler extends AbstractProcessConfigHandler<
    SecurityAnalysisConfig,
    SecurityAnalysisConfigEntity> {

    public SecurityAnalysisConfigHandler(SecurityAnalysisConfigMapper mapper, SecurityAnalysisConfigRepository repository) {
        super(mapper, repository);
    }

    @Override
    public ProcessType getProcessType() {
        return ProcessType.SECURITY_ANALYSIS;
    }
}
