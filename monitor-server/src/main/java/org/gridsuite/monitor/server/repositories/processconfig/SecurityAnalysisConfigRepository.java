/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.monitor.server.repositories.processconfig;

import org.gridsuite.monitor.server.entities.processconfig.SecurityAnalysisConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
public interface SecurityAnalysisConfigRepository extends JpaRepository<SecurityAnalysisConfigEntity, UUID> {
}
