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
package org.apache.sling.caconfig.spi;

import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how (and if) resources in a resource hierarchy should inherit form each other.
 * Primary use case is property inheritance over the inheritance chain.
 */
@ConsumerType
public interface ConfigurationInheritanceStrategy {

    /**
     * Pick or merge resources for inheritance.
     * @param configResources Iterator of configuration resources that form the inheritance hierarchy.
     *     First resource is the "closest" match, the other resources may be used to inherit from.
     * @return Inherited resource or null if this strategy does not support the given resources
     */
    @CheckForNull Resource getResource(@Nonnull Iterator<Resource> configResources);

}
