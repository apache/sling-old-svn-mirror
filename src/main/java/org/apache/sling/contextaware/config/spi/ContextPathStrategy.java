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
package org.apache.sling.contextaware.config.spi;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Allows application to define a strategy to find context paths for content paths.
 * Each context path may have it's own context-specific configuration.
 */
@ConsumerType
public interface ContextPathStrategy {

    /**
     * Finds context paths for the given context resource.
     * @param resource Context resource
     * @return Context paths that where detected in the given path
     *      (in order of closest matching first).
     *      If none are found an empty list is returned.
     */
    @Nonnull Collection<String> findContextPaths(@Nonnull Resource resource);

}
