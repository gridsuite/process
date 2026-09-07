/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.mappers.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.LoadFlowConfig;
import org.gridsuite.monitor.server.config.MapStructConfig;
import org.gridsuite.monitor.server.entities.processconfig.LoadFlowConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Mapper(config = MapStructConfig.class)
public interface LoadFlowConfigMapper extends ProcessConfigMapper<LoadFlowConfig, LoadFlowConfigEntity> {
    @Mapping(target = "id", ignore = true)
    LoadFlowConfigEntity toEntity(LoadFlowConfig dto);

    LoadFlowConfig toDto(LoadFlowConfigEntity entity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(LoadFlowConfig dto, @MappingTarget LoadFlowConfigEntity entity);
}
