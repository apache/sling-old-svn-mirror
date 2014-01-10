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

import aQute.bnd.annotation.ConsumerType;

/**
 * A {@link Feature} which is mapping resource types can be adapted to
 * this service interface.
 */
@ConsumerType
public interface ResourceTypeMapping {

    /**
     * Returns the resource type mapping for a feature.
     * This mapping is only executed if {@link Feature#isEnabled(ExecutionContext)}
     * return true for the given feature/context. The caller of this
     * method must ensure to call {@link Feature#isEnabled(ExecutionContext)}
     * before calling this method and only call this method if
     * {@link Feature#isEnabled(ExecutionContext)} returned <code>true</code>
     */
    Map<String, String> getResourceTypeMapping();
}
