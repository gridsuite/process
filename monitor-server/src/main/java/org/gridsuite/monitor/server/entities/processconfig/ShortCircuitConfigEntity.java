/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.entities.processconfig;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;

import java.util.UUID;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@Entity
@Table(name = "short_circuit_config")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "shortCircuitConfig_id_fk_constraint"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShortCircuitConfigEntity extends AbstractProcessConfigEntity {
    @Column(name = "short_circuit_parameters_uuid")
    private UUID shortCircuitParametersUuid;

    @Override
    public ProcessType getProcessType() {
        return ProcessType.SHORT_CIRCUIT;
    }
}
