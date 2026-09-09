/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.monitor.commons.types.messaging.ProcessExecutionStep;
import org.gridsuite.monitor.server.dto.processexecution.ProcessExecution;
import org.gridsuite.monitor.server.dto.report.ReportPage;
import org.gridsuite.monitor.server.services.processexecution.ProcessExecutionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + MonitorApi.API_VERSION + "/")
@Tag(name = "Monitor server")
public class MonitorController {

    private final ProcessExecutionService processExecutionService;

    public static final String HEADER_USER_ID = "userId";

    public MonitorController(ProcessExecutionService processExecutionService) {
        this.processExecutionService = processExecutionService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute a process")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The process execution has been started"),
                           @ApiResponse(responseCode = "404", description = "Process config was not found")})
    public ResponseEntity<UUID> executeProcess(
            @Parameter(description = "Case uuid") @RequestParam(name = "caseUuid") UUID caseUuid,
            @Parameter(description = "Process config uuid") @RequestParam(name = "processConfigUuid") UUID processConfigUuid,
            @RequestParam(required = false, defaultValue = "false") boolean isDebug,
            @RequestHeader(HEADER_USER_ID) String userId) {
        Optional<UUID> executionId = processExecutionService.executeProcess(caseUuid, userId, processConfigUuid, isDebug);
        return executionId.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{executionId}/reports")
    @Operation(summary = "Get reports for an execution")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The execution reports"),
                           @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<ReportPage> getExecutionReports(@Parameter(description = "Execution UUID") @PathVariable UUID executionId) {
        Optional<ReportPage> reports = processExecutionService.getReports(executionId);
        return reports.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{executionId}/results")
    @Operation(summary = "Get results for an execution")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The execution results"),
                           @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<List<String>> getExecutionResults(@Parameter(description = "Execution UUID") @PathVariable UUID executionId) {
        Optional<List<String>> results = processExecutionService.getResults(executionId);
        return results.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/executions")
    @Operation(summary = "Get launched processes")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The launched processes")})
    public ResponseEntity<List<ProcessExecution>> getLaunchedProcesses() {
        return ResponseEntity.ok(processExecutionService.getLaunchedProcesses());
    }

    @GetMapping("/executions/{executionId}")
    @Operation(summary = "Get an execution")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The process execution"),
                           @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<ProcessExecution> getExecution(@Parameter(description = "Execution UUID") @PathVariable UUID executionId) {
        Optional<ProcessExecution> execution = processExecutionService.getExecution(executionId);
        return execution.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{executionId}/step-infos")
    @Operation(summary = "Get execution steps statuses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The execution steps statuses"),
        @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<List<ProcessExecutionStep>> getStepsInfos(@Parameter(description = "Execution UUID") @PathVariable UUID executionId) {
        return processExecutionService.getStepsInfos(executionId).map(list -> ResponseEntity.ok().body(list))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{executionId}/debug-infos")
    @Operation(summary = "Get execution debug file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Debug file downloaded"),
        @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<byte[]> getDebugInfos(@Parameter(description = "Execution UUID") @PathVariable UUID executionId) {
        return processExecutionService.getDebugInfos(executionId)
            .map(bytes -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"archive.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(bytes))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/executions/{executionId}")
    @Operation(summary = "Delete an execution")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Execution was deleted"),
                           @ApiResponse(responseCode = "404", description = "execution id was not found")})
    public ResponseEntity<Void> deleteExecution(@PathVariable UUID executionId) {
        Optional<UUID> deletedExecutionId = processExecutionService.deleteExecution(executionId);
        return deletedExecutionId.isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
