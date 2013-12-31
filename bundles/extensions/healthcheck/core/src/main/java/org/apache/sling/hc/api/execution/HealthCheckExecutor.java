/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.api.execution;

import java.util.Collection;

import org.apache.sling.hc.api.HealthCheck;
import org.osgi.framework.ServiceReference;

import aQute.bnd.annotation.ProviderType;

/**
 * Executes health checks registered as OSGi services and
 * implementing the interface {@link HealthCheck}.
 *
 */
@ProviderType
public interface HealthCheckExecutor {

    /**
     * Executes all health checks
     *
     * @return Collection of results. The collection might be empty.
     */
    Collection<HealthCheckExecutionResult> execute(ServiceReference... healthCheckReferences);
}