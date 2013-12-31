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
package org.apache.sling.featureflags;

import java.util.Map;

import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ConsumerType;

/**
 * A feature provider activates one more features.
 */
@ConsumerType
public interface FeatureProvider {

    /**
     * Checks whether the feature is enabled for the current execution
     * context.
     */
    boolean isEnabled(String featureName, ProviderContext context);

    /**
     * Return the list of available features from this provider.
     */
    String [] getFeatureNames();

    /**
     * Returns the resource type mapping for a feature.
     * This mapping is only used if {@link #isEnabled(String, ExecutionContext)}
     * return true for the given feature/context. The caller of this
     * method must ensure to call {@link #isEnabled(String, ExecutionContext)}
     * before calling this method and only call this method if
     * {@link #isEnabled(String, ExecutionContext)} return <code>true</code>
     */
    Map<String, String> getResourceTypeMapping(String featureName);

    /**
     * Checks whether a resource should be hidden for a feature.
     * This check is only executed if {@link #isEnabled(String, ExecutionContext)}
     * return true for the given feature/context. The caller of this
     * method must ensure to call {@link #isEnabled(String, ExecutionContext)}
     * before calling this method and only call this method if
     * {@link #isEnabled(String, ExecutionContext)} return <code>true</code>
     */
    boolean hideResource(String featureName, Resource resource);
}
