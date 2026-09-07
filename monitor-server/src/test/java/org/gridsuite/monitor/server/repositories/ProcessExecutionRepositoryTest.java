/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.repositories;

import org.gridsuite.monitor.commons.types.processexecution.ProcessStatus;
import org.gridsuite.monitor.commons.types.processexecution.ProcessType;
import org.gridsuite.monitor.commons.types.processexecution.StepStatus;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionEntity;
import org.gridsuite.monitor.server.entities.processexecution.ProcessExecutionStepEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@DataJpaTest
class ProcessExecutionRepositoryTest {

    @Autowired
    private ProcessExecutionRepository executionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void stepsShouldBeOrderedByStepOrder() {
        ProcessExecutionEntity execution = ProcessExecutionEntity.builder()
                .id(UUID.randomUUID())
                .type("SECURITY_ANALYSIS")
                .caseUuid(UUID.randomUUID())
                .status(ProcessStatus.RUNNING)
                .scheduledAt(Instant.now())
                .steps(new ArrayList<>())
                .build();

        // Add steps in non-sequential order (1 before 0)
        ProcessExecutionStepEntity step1 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepType("SECOND_STEP")
                .stepOrder(1)
                .status(StepStatus.COMPLETED)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        execution.getSteps().add(step1);
        ProcessExecutionStepEntity step0 = ProcessExecutionStepEntity.builder()
                .id(UUID.randomUUID())
                .stepType("FIRST_STEP")
                .stepOrder(0)
                .status(StepStatus.COMPLETED)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        execution.getSteps().add(step0);

        executionRepository.save(execution);
        // Force DB write and reload
        entityManager.flush();
        entityManager.clear();

        ProcessExecutionEntity retrieved = executionRepository.findById(execution.getId()).orElseThrow();
        assertThat(retrieved.getSteps()).hasSize(2);
        assertThat(retrieved.getSteps().get(0).getStepType()).isEqualTo("FIRST_STEP");
        assertThat(retrieved.getSteps().get(0).getStepOrder()).isZero();
        assertThat(retrieved.getSteps().get(1).getStepType()).isEqualTo("SECOND_STEP");
        assertThat(retrieved.getSteps().get(1).getStepOrder()).isEqualTo(1);
    }

    @Test
    void securityAnalysisLaunchedProcesses() {
        UUID case1Uuid = UUID.randomUUID();
        Instant scheduledAt1 = Instant.now().minusSeconds(60);
        Instant startedAt1 = Instant.now().minusSeconds(30);
        Instant completedAt1 = Instant.now();
        UUID report1Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution1 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case1Uuid)
            .status(ProcessStatus.COMPLETED)
            .executionEnvName("env1")
            .scheduledAt(scheduledAt1)
            .startedAt(startedAt1)
            .completedAt(completedAt1)
            .reportId(report1Uuid)
            .userId("user1")
            .build();

        UUID case2Uuid = UUID.randomUUID();
        Instant scheduledAt2 = Instant.now().minusSeconds(90);
        Instant startedAt2 = Instant.now().minusSeconds(20);
        UUID report2Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution2 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case2Uuid)
            .status(ProcessStatus.RUNNING)
            .executionEnvName("env2")
            .scheduledAt(scheduledAt2)
            .startedAt(startedAt2)
            .reportId(report2Uuid)
            .userId("user2")
            .build();

        UUID case3Uuid = UUID.randomUUID();
        Instant scheduledAt3 = Instant.now().minusSeconds(90);
        UUID report3Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution3 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case3Uuid)
            .status(ProcessStatus.SCHEDULED)
            .executionEnvName("env3")
            .scheduledAt(scheduledAt3)
            .reportId(report3Uuid)
            .userId("user3")
            .build();

        executionRepository.saveAll(List.of(execution1, execution2, execution3));

        // Force DB write and reload
        entityManager.flush();
        entityManager.clear();

        List<ProcessExecutionEntity> retrieved = executionRepository.findByTypeAndStartedAtIsNotNullOrderByStartedAtDesc(ProcessType.SECURITY_ANALYSIS.name());
        assertThat(retrieved).hasSize(2);

        assertThat(retrieved.get(0).getType()).isEqualTo(ProcessType.SECURITY_ANALYSIS.name());
        assertThat(retrieved.get(0).getCaseUuid()).isEqualTo(case2Uuid);
        assertThat(retrieved.get(0).getStatus()).isEqualTo(ProcessStatus.RUNNING);
        assertThat(retrieved.get(0).getExecutionEnvName()).isEqualTo("env2");
        assertThat(retrieved.get(0).getScheduledAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(scheduledAt2.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(0).getStartedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(startedAt2.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(0).getCompletedAt()).isNull();
        assertThat(retrieved.get(0).getReportId()).isEqualTo(report2Uuid);
        assertThat(retrieved.get(0).getUserId()).isEqualTo("user2");

        assertThat(retrieved.get(1).getType()).isEqualTo(ProcessType.SECURITY_ANALYSIS.name());
        assertThat(retrieved.get(1).getCaseUuid()).isEqualTo(case1Uuid);
        assertThat(retrieved.get(1).getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(retrieved.get(1).getExecutionEnvName()).isEqualTo("env1");
        assertThat(retrieved.get(1).getScheduledAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(scheduledAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(1).getStartedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(startedAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(1).getCompletedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(completedAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(1).getReportId()).isEqualTo(report1Uuid);
        assertThat(retrieved.get(1).getUserId()).isEqualTo("user1");
    }

    @Test
    void allLaunchedProcesses() {
        UUID case1Uuid = UUID.randomUUID();
        Instant scheduledAt1 = Instant.now().minusSeconds(20);
        Instant startedAt1 = Instant.now().minusSeconds(30);
        Instant completedAt1 = Instant.now();
        UUID report1Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution1 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.SECURITY_ANALYSIS.name())
            .caseUuid(case1Uuid)
            .status(ProcessStatus.COMPLETED)
            .executionEnvName("env1")
            .scheduledAt(scheduledAt1)
            .startedAt(startedAt1)
            .completedAt(completedAt1)
            .reportId(report1Uuid)
            .userId("user1")
            .build();

        UUID case2Uuid = UUID.randomUUID();
        Instant scheduledAt2 = Instant.now().minusSeconds(40);
        Instant startedAt2 = Instant.now().minusSeconds(20);
        UUID report2Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution2 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.LOADFLOW.name())
            .caseUuid(case2Uuid)
            .status(ProcessStatus.RUNNING)
            .executionEnvName("env2")
            .scheduledAt(scheduledAt2)
            .startedAt(startedAt2)
            .reportId(report2Uuid)
            .userId("user2")
            .build();

        UUID case3Uuid = UUID.randomUUID();
        Instant scheduledAt3 = Instant.now().minusSeconds(50);
        UUID report3Uuid = UUID.randomUUID();
        ProcessExecutionEntity execution3 = ProcessExecutionEntity.builder()
            .id(UUID.randomUUID())
            .type(ProcessType.SHORT_CIRCUIT.name())
            .caseUuid(case3Uuid)
            .status(ProcessStatus.SCHEDULED)
            .executionEnvName("env3")
            .scheduledAt(scheduledAt3)
            .reportId(report3Uuid)
            .userId("user3")
            .build();

        executionRepository.saveAll(List.of(execution1, execution2, execution3));

        // Force DB write and reload
        entityManager.flush();
        entityManager.clear();

        List<ProcessExecutionEntity> retrieved = executionRepository.findAllByScheduledAtIsNotNullOrderByScheduledAtDesc();
        assertThat(retrieved).hasSize(3);

        assertThat(retrieved.get(0).getType()).isEqualTo(ProcessType.SECURITY_ANALYSIS.name());
        assertThat(retrieved.get(0).getCaseUuid()).isEqualTo(case1Uuid);
        assertThat(retrieved.get(0).getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(retrieved.get(0).getExecutionEnvName()).isEqualTo("env1");
        assertThat(retrieved.get(0).getScheduledAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(scheduledAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(0).getStartedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(startedAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(0).getCompletedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(completedAt1.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(0).getReportId()).isEqualTo(report1Uuid);
        assertThat(retrieved.get(0).getUserId()).isEqualTo("user1");

        assertThat(retrieved.get(1).getType()).isEqualTo(ProcessType.LOADFLOW.name());
        assertThat(retrieved.get(1).getCaseUuid()).isEqualTo(case2Uuid);
        assertThat(retrieved.get(1).getStatus()).isEqualTo(ProcessStatus.RUNNING);
        assertThat(retrieved.get(1).getExecutionEnvName()).isEqualTo("env2");
        assertThat(retrieved.get(1).getScheduledAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(scheduledAt2.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(1).getStartedAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(startedAt2.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(1).getCompletedAt()).isNull();
        assertThat(retrieved.get(1).getReportId()).isEqualTo(report2Uuid);
        assertThat(retrieved.get(1).getUserId()).isEqualTo("user2");

        assertThat(retrieved.get(2).getType()).isEqualTo(ProcessType.SHORT_CIRCUIT.name());
        assertThat(retrieved.get(2).getCaseUuid()).isEqualTo(case3Uuid);
        assertThat(retrieved.get(2).getStatus()).isEqualTo(ProcessStatus.SCHEDULED);
        assertThat(retrieved.get(2).getExecutionEnvName()).isEqualTo("env3");
        assertThat(retrieved.get(2).getScheduledAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(scheduledAt3.truncatedTo(ChronoUnit.MILLIS));
        assertThat(retrieved.get(2).getStartedAt()).isNull();
        assertThat(retrieved.get(2).getCompletedAt()).isNull();
        assertThat(retrieved.get(2).getReportId()).isEqualTo(report3Uuid);
        assertThat(retrieved.get(2).getUserId()).isEqualTo("user3");
    }
}
