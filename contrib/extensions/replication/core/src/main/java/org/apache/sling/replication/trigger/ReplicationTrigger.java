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
package org.apache.sling.replication.trigger;

import javax.annotation.Nonnull;

import org.apache.sling.replication.component.ReplicationComponent;

/**
 * A {@link org.apache.sling.replication.trigger.ReplicationTrigger} is responsible to trigger
 * {@link org.apache.sling.replication.communication.ReplicationRequest}s upon certain 'events' (e.g. Sling / Jcr events,
 * periodic polling, etc.).
 * A {@link org.apache.sling.replication.trigger.ReplicationTrigger} is meant to be stateless so that more than one
 * {@link org.apache.sling.replication.trigger.ReplicationRequestHandler} can be registered into the same trigger.
 */
public interface ReplicationTrigger extends ReplicationComponent {

    /**
     * register a request handler to be triggered and returns a corresponding registration id
     *
     * @param requestHandler handler
     * @throws org.apache.sling.replication.trigger.ReplicationTriggerException if registration fails
     */
    void register(@Nonnull ReplicationRequestHandler requestHandler) throws ReplicationTriggerException;

    /**
     * unregister the given handler, if existing
     *
     * @param requestHandler handler to unregister
     * @throws org.apache.sling.replication.trigger.ReplicationTriggerException if unregistration fails
     */
    void unregister(@Nonnull ReplicationRequestHandler requestHandler) throws ReplicationTriggerException;
}
