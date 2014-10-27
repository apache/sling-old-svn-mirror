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
package org.apache.sling.replication.component;

/**
 * provider for already existing {@link ReplicationComponent}s
 */
public interface ReplicationComponentProvider {

    /**
     * Retrieves an already existing component by name.
     * If null is passed as componentName then a default component is returned.
     *
     * @param type            the {@link java.lang.Class} of the component to be retrieved
     * @param componentName   the component name as a <code>String</code>
     * @param <ComponentType> the actual type of the {@link ReplicationComponent}
     *                        to be retrieved
     * @return the {@link ReplicationComponent} of the specified type,
     * with the specified name, or <code>null</code> if such a {@link ReplicationComponent}
     * doesn't exist
     */
    <ComponentType extends ReplicationComponent> ComponentType getComponent(java.lang.Class<ComponentType> type,
                                                                            String componentName);
}
