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
package org.apache.sling.replication.transport.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;

/**
 * A no-operation {@link org.apache.sling.replication.transport.TransportHandler}
 */
@Component(metatype = false, label = "Nop Transport Handler")
@Service(value = TransportHandler.class)
@Property(name = "name", value = NopTransportHandler.NAME)
public class NopTransportHandler implements TransportHandler {
    public static final String NAME = "nop";

    public void transport(String agentName, ReplicationPackage replicationPackage) throws ReplicationTransportException {
    }

    public void enableProcessing(String agentName, ReplicationQueueProcessor responseProcessor) {
    }

    public void disableProcessing(String agentName) {
    }
}
