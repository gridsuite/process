/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.mappers.processexecution;

import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.server.config.MapStructConfig;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionStepEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Radouane Khouadri <radouane.khouadri at rte-france.com>
 */
@Mapper(config = MapStructConfig.class)
public interface ProcessExecutionStepMapper {

    ProcessExecutionStep toDto(ProcessExecutionStepEntity entity);

    ProcessExecutionStepEntity toEntity(ProcessExecutionStep dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromEntity(ProcessExecutionStepEntity source, @MappingTarget ProcessExecutionStepEntity target);

}
