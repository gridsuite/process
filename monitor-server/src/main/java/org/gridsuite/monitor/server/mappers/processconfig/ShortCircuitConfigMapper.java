/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.mappers.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.ShortCircuitConfig;
import org.gridsuite.monitor.server.config.MapStructConfig;
import org.gridsuite.monitor.server.entities.processconfig.ShortCircuitConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@Mapper(config = MapStructConfig.class)
public interface ShortCircuitConfigMapper extends ProcessConfigMapper<ShortCircuitConfig, ShortCircuitConfigEntity> {
    @Mapping(target = "id", ignore = true)
    ShortCircuitConfigEntity toEntity(ShortCircuitConfig dto);

    ShortCircuitConfig toDto(ShortCircuitConfigEntity entity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ShortCircuitConfig dto, @MappingTarget ShortCircuitConfigEntity entity);
}
