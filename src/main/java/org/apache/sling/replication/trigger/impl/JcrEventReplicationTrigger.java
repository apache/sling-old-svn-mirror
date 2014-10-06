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
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR observation based {@link org.apache.sling.replication.trigger.ReplicationTrigger}.
 * It filters events having {@link javax.jcr.observation.ObservationManager#setUserData(String)} set to
 * {@link org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder#USER_DATA}
 */
public class JcrEventReplicationTrigger implements ReplicationTrigger {

    public static final String TYPE = "jcrEvent";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SlingRepository repository;
    private final String path;

    private Session adminSession;

    public JcrEventReplicationTrigger(SlingRepository repository, String path) {
        this.repository = repository;
        this.path = path;
    }

    public JcrEventReplicationTrigger(Map<String, Object> properties, SlingRepository repository) {
        this(repository, PropertiesUtil.toString(properties.get("path"), null));
    }

    public void register(String handlerId, ReplicationTriggerRequestHandler requestHandler) {
        log.info("activating ExampleObservation");
        try {
            adminSession = repository.loginService("replicationService", null);
            adminSession.getWorkspace().getObservationManager().addEventListener(
                    new JcrEventReplicationTriggerHandler(requestHandler), Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED |
                            Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED, path, true, null, null, false);
        } catch (RepositoryException e) {
            log.error("unable to register session", e);
        }
    }

    public void unregister(String handlerId) {
        if (adminSession != null) {
            adminSession.logout();
        }
    }

    private class JcrEventReplicationTriggerHandler implements EventListener {
        private final ReplicationTriggerRequestHandler requestHandler;

        public JcrEventReplicationTriggerHandler(ReplicationTriggerRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void onEvent(EventIterator eventIterator) {
            try {
                while (eventIterator.hasNext()) {
                    Event event = eventIterator.nextEvent();
                    // TODO : check for JackrabbitEvent#isExternal
                    String userData = event.getUserData();
                    log.info("event userData is {}", userData);
                    if (!FileVaultReplicationPackageBuilder.USER_DATA.equals(userData)) {
                        log.info("triggering replication from jcr event {}", event);

                        Object pathProperty = event.getPath();
                        if (pathProperty != null) {
                            String replicatingPath = String.valueOf(pathProperty);
                            requestHandler.handle(new ReplicationRequest(System.currentTimeMillis(), Event.NODE_MOVED ==
                                    event.getType() ? ReplicationActionType.DELETE : ReplicationActionType.ADD, replicatingPath));
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error while treating events", e);
            }
        }

    }
}
