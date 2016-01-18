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
package org.apache.sling.distribution.trigger.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger} that listens for certain events and persists them
 * under a specific path in the repo
 */
public class PersistedJcrEventDistributionTrigger extends AbstractJcrEventTrigger implements DistributionTrigger {

    public static final String DEFAULT_NUGGETS_PATH = "/var/sling/distribution/nuggets";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String nuggetsPath;

    public PersistedJcrEventDistributionTrigger(SlingRepository repository, Scheduler scheduler, ResourceResolverFactory resolverFactory, String path, String servicename, String nuggetsPath) {
        super(repository, scheduler, resolverFactory, path, servicename);
        this.nuggetsPath = nuggetsPath == null || nuggetsPath.length() == 0 ? DEFAULT_NUGGETS_PATH : nuggetsPath;
    }

    @Override
    protected DistributionRequest processEvent(Event event) throws RepositoryException {
        log.debug("processing event {}", event);

        DistributionRequest distributionRequest = null;

        Session session = getSession();

        if (!session.nodeExists(nuggetsPath)) {
            initializeNuggetsPath(session);
        }

        if (session.hasPermission(nuggetsPath, Session.ACTION_ADD_NODE)) {
            log.debug("persisting event under {}", nuggetsPath);
            Node nuggetsNode = session.getNode(nuggetsPath);
            if (nuggetsNode != null) {
                String nodeName = String.valueOf(System.nanoTime());
                Node createdNode = nuggetsNode.addNode(nodeName, "nt:unstructured");
                if (createdNode != null) {
                    String path = createdNode.getPath();
                    createdNode.setProperty("identifier", event.getIdentifier());
                    createdNode.setProperty("path", event.getPath());
                    createdNode.setProperty("date", event.getDate());
                    createdNode.setProperty("type", event.getType());
                    createdNode.setProperty("userData", event.getUserData());
                    createdNode.setProperty("userID", event.getUserID());

                    Set<Map.Entry> set = event.getInfo().entrySet();
                    Collection<String> values = new ArrayList<String>();
                    for (Map.Entry entry : set) {
                        values.add(String.valueOf(entry.getKey()) + ":" + String.valueOf(entry.getValue()));
                    }
                    createdNode.setProperty("info", values.toArray(new String[values.size()]));
                    session.save();
                    log.info("event {} persisted at {}", event, path);
                    distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, path);
                } else {
                    log.warn("could not create node {}", nuggetsPath + "/" + nodeName);
                }
            } else {
                log.warn("could not get node {} to persist event", nuggetsPath);
            }
        } else {
            log.warn("not enough privileges to persist the event {} under {}", event, nuggetsPath);
        }

        return distributionRequest;
    }

    private void initializeNuggetsPath(Session session) throws RepositoryException {
        log.info("initializing nuggets path");
        if (session != null) {
            Node parent = session.getRootNode();
            if (session.hasPermission(parent.getPath(), Session.ACTION_ADD_NODE)) {
                for (String nodeName : nuggetsPath.split("/")) {
                    if (nodeName.length() > 0) {
                        if (!parent.hasNode(nodeName)) {
                            log.info("creating {}", nodeName);
                            parent = parent.addNode(nodeName, "sling:Folder");
                        } else {
                            log.debug("{} exists", nodeName);
                            parent = parent.getNode(nodeName);
                        }
                    }
                }
                session.save();
            }
        }
    }

    @Override
    public void enable() {
        log.debug("enabling persisting jcr event listener");
        Session session;
        try {
            session = getSession();
            if (!session.nodeExists(nuggetsPath)) {
                initializeNuggetsPath(session);
            }
        } catch (RepositoryException e) {
            log.warn("could not create nuggets path {}", nuggetsPath, e);
        }
    }
}
