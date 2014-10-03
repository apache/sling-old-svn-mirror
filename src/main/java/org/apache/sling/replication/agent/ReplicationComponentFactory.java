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
package org.apache.sling.replication.agent;

import java.util.Map;

/**
 * factory for {@link org.apache.sling.replication.agent.ReplicationComponent}s
 */
public interface ReplicationComponentFactory {

    /**
     * create a {@link org.apache.sling.replication.agent.ReplicationComponent}
     *
     * @param type              the <code>Class</code> of the component to be created
     * @param properties        the properties to be supplied for the initialization of the component
     * @param componentProvider the {@link org.apache.sling.replication.agent.ReplicationComponentProvider} used to eventually
     *                          wire additional required {@link org.apache.sling.replication.agent.ReplicationComponent}s
     * @param <ComponentType>   the actual type of the {@link org.apache.sling.replication.agent.ReplicationComponent}
     *                          to be created
     * @return
     */
    <ComponentType> ComponentType createComponent(java.lang.Class<ComponentType> type,
                                                  Map<String, Object> properties,
                                                  ReplicationComponentProvider componentProvider);
}
