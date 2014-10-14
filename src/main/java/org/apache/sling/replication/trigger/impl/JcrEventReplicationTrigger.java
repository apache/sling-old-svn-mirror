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
package org.apache.sling.replication.trigger.impl;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR observation based {@link org.apache.sling.replication.trigger.ReplicationTrigger}.
 */
public class JcrEventReplicationTrigger extends AbstractJcrEventTrigger implements ReplicationTrigger {

    public static final String TYPE = "jcrEvent";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public JcrEventReplicationTrigger(SlingRepository repository, String path, String serviceName) {
        super(repository, path, serviceName);
    }

    public JcrEventReplicationTrigger(Map<String, Object> properties, SlingRepository repository) {
        this(repository, PropertiesUtil.toString(properties.get(PATH), null), PropertiesUtil.toString(properties.get(SERVICENAME), null));
    }

    @Override
    protected ReplicationRequest processEvent(Event event) throws RepositoryException {
        log.info("triggering replication from jcr event {}", event);
        ReplicationRequest replicationRequest = null;
        Object pathProperty = event.getPath();
        if (pathProperty != null) {
            String replicatingPath = String.valueOf(pathProperty);
            replicationRequest = new ReplicationRequest(System.currentTimeMillis(), Event.NODE_REMOVED ==
                    event.getType() ? ReplicationActionType.DELETE : ReplicationActionType.ADD, replicatingPath);
        }
        return replicationRequest;
    }
}