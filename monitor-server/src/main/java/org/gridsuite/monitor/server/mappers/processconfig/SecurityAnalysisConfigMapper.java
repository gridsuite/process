/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.mappers.processconfig;

import org.gridsuite.monitor.commons.types.processconfig.SecurityAnalysisConfig;
import org.gridsuite.monitor.server.config.MapStructConfig;
import org.gridsuite.monitor.server.entities.processconfig.SecurityAnalysisConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Radouane Khouadri <radouane.khouadri at rte-france.com>
 */
@Mapper(config = MapStructConfig.class)
public interface SecurityAnalysisConfigMapper extends ProcessConfigMapper<SecurityAnalysisConfig, SecurityAnalysisConfigEntity> {
    @Mapping(target = "id", ignore = true)
    SecurityAnalysisConfigEntity toEntity(SecurityAnalysisConfig dto);

    SecurityAnalysisConfig toDto(SecurityAnalysisConfigEntity entity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(SecurityAnalysisConfig dto, @MappingTarget SecurityAnalysisConfigEntity entity);
}
