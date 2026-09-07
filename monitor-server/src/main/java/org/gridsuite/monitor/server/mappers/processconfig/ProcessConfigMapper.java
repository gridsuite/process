/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.mappers.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.ProcessConfig;
import org.gridsuite.monitor.server.entities.processconfig.AbstractProcessConfigEntity;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
public interface ProcessConfigMapper<C extends ProcessConfig, E extends AbstractProcessConfigEntity> {
    E toEntity(C dto);

    C toDto(E entity);

    void updateEntityFromDto(C dto, E entity);
}
