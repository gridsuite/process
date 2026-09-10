/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.entities.processexecution;

import jakarta.persistence.*;
import lombok.*;
import org.gridsuite.monitor.commons.types.processexecution.ProcessStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "process_execution")
public class ProcessExecutionEntity {

    @Id
    private UUID id;

    @Column
    private String type;

    @Column
    private UUID caseUuid;

    @Column
    private UUID processConfigId;

    @Column
    @Enumerated(EnumType.STRING)
    private ProcessStatus status;

    @Column
    private String executionEnvName;

    @Column
    private Instant scheduledAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column
    private UUID reportId;

    @Column
    private String userId;

    @Column
    private String debugFileLocation;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "execution_id", foreignKey = @ForeignKey(name = "processExecutionStep_processExecution_fk"))
    @OrderBy("stepOrder ASC")
    private List<ProcessExecutionStepEntity> steps;
}
