/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.worker.server.core.orchestrator;

import org.gridsuite.monitor.commons.types.processconfig.ProcessConfig;
import org.gridsuite.monitor.worker.server.core.context.ProcessStepExecutionContext;
import org.gridsuite.monitor.worker.server.core.process.ProcessStep;

/**
 * Child orchestrator interface responsible for executing a single {@link ProcessStep} within a process run.
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface StepExecutor {

    /**
     * Execute a step and publish step status updates around the execution.
     *
     * @param context step execution context
     * @param step step definition to execute
     * @param <C> concrete {@link ProcessConfig} type associated with the parent process
     * @throws RuntimeException any exception thrown by the step implementation is propagated after updating status
     */
    <C extends ProcessConfig> void executeStep(ProcessStepExecutionContext<C> context, ProcessStep<C> step);
}
