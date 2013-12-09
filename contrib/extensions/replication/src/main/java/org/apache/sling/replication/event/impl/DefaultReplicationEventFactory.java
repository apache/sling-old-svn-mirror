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
package org.apache.sling.replication.event.impl;

import java.util.Dictionary;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.event.ReplicationEvent;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.osgi.service.event.EventAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ReplicationEventFactory} OSGi service
 */
@Component(immediate = true)
@Service(value = ReplicationEventFactory.class)
public class DefaultReplicationEventFactory implements ReplicationEventFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private EventAdmin eventAdmin;

    public void generateEvent(ReplicationEventType replicationEventType, Dictionary<?, ?> properties) {
        ReplicationEvent replicationEvent = new ReplicationEvent(replicationEventType, properties);
        eventAdmin.postEvent(replicationEvent);
        if (log.isDebugEnabled()) {
            log.debug("replication event posted {}", replicationEvent);
        }
    }

}
