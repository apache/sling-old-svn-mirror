/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.contextaware.config.resource.spi;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Allows application to define a strategy to find context paths for content paths.
 * A context paths is the root path of a "configuration context", which is a subtree in the resource hierarchy.
 * Each context may have it's own context-aware configuration attached to.
 * If multiple context path strategy implementations are defined the fist one with matching 
 * path (context.roots) is used ordered by service ranking.
 */
@ConsumerType
public interface ContextPathStrategy {

    /**
     * The name of the service registration property containing the root paths
     * this context strategy should apply to. If none is set it is applied to all paths.
     */
    String ROOTS = "context.roots";

    /**
     * Finds context paths for the given resource.
     * @param resource Context resource
     * @return Root resource for each context found (in order of closest matching first).
     *      If none are found an empty list is returned.
     */
    @Nonnull Collection<Resource> findContextPaths(@Nonnull Resource resource);

}
