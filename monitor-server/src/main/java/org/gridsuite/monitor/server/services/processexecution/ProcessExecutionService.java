/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.services.processexecution;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStatusUpdate;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.commons.types.result.ResultInfos;
import org.gridsuite.monitor.server.clients.ReportRestClient;
import org.gridsuite.monitor.server.clients.S3RestClient;
import org.gridsuite.monitor.server.dto.processexecution.ProcessExecution;
import org.gridsuite.monitor.server.dto.report.ReportPage;
import org.gridsuite.monitor.server.messaging.NotificationService;
import org.gridsuite.monitor.server.services.result.ResultService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@Service
public class ProcessExecutionService {

    private final ProcessExecutionTxService processExecutionTxService;
    private final NotificationService notificationService;
    private final ReportRestClient reportRestClient;
    private final ResultService resultService;
    private final S3RestClient s3RestClient;

    public ProcessExecutionService(ProcessExecutionTxService processExecutionTxService,
                                   NotificationService notificationService,
                                   ReportRestClient reportRestClient,
                                   ResultService resultService,
                                   S3RestClient s3RestClient) {
        this.processExecutionTxService = processExecutionTxService;
        this.notificationService = notificationService;
        this.reportRestClient = reportRestClient;
        this.resultService = resultService;
        this.s3RestClient = s3RestClient;
    }

    public Optional<UUID> executeProcess(UUID caseUuid, String userId, UUID processConfigId, boolean isDebug) {
        UUID executionId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        Optional<ProcessCreationResult> result = processExecutionTxService.createExecution(
            caseUuid, userId, processConfigId, executionId, reportId, isDebug
        );

        if (result.isEmpty()) {
            return Optional.empty();
        }

        notificationService.sendProcessRunMessage(
            caseUuid,
            result.get().processConfig(),
            executionId,
            reportId,
            result.get().debugLocationFile()
        );

        notificationService.sendProcessUpdatedMessage(executionId, result.get().processConfig().processType());

        return Optional.of(executionId);
    }

    public void updateExecutionStatus(UUID executionId, ProcessExecutionStatusUpdate payload) {
        processExecutionTxService.updateExecutionStatus(executionId, payload);
        notificationService.sendProcessUpdatedMessage(executionId, payload.getProcessType());
    }

    public void updateStepStatus(UUID executionId, ProcessExecutionStep processExecutionStep) {
        processExecutionTxService.updateStepStatus(executionId, processExecutionStep);
    }

    public void updateStepsStatuses(UUID executionId, List<ProcessExecutionStep> processExecutionSteps) {
        processExecutionTxService.updateStepsStatuses(executionId, processExecutionSteps);
    }

    public Optional<ReportPage> getReports(UUID executionId) {
        return processExecutionTxService.getReportId(executionId)
            .map(reportRestClient::getReport);
    }

    public Optional<List<String>> getResults(UUID executionId) {
        Optional<List<ResultInfos>> resultInfos = processExecutionTxService.getResultInfos(executionId);
        return resultInfos.map(results -> results.stream()
            .map(resultService::getResult)
            .toList());
    }

    public Optional<byte[]> getDebugInfos(UUID executionId) {
        return processExecutionTxService.getDebugFileLocation(executionId)
            .filter(Objects::nonNull)
            .map(debugFileLocation -> {
                try {
                    return s3RestClient.downloadDirectoryAsZip(debugFileLocation);
                } catch (IOException e) {
                    throw new PowsyblException("An error occurred while downloading debug files", e);
                }
            });
    }

    public List<ProcessExecution> getLaunchedProcesses() {
        return processExecutionTxService.getLaunchedProcesses();
    }

    public Optional<ProcessExecution> getExecution(UUID executionId) {
        return processExecutionTxService.getExecution(executionId);
    }

    public Optional<List<ProcessExecutionStep>> getStepsInfos(UUID executionId) {
        return processExecutionTxService.getStepsInfos(executionId);
    }

    public Optional<UUID> deleteExecution(UUID executionId) {
        Optional<ProcessDeletionInfos> executionDeletionData = processExecutionTxService.deleteExecution(executionId);
        if (executionDeletionData.isPresent()) {
            executionDeletionData.get().resultInfos().forEach(resultService::deleteResult);
            if (executionDeletionData.get().reportId() != null) {
                reportRestClient.deleteReport(executionDeletionData.get().reportId());
            }
            return Optional.of(executionId);
        }
        return Optional.empty();
    }
}
