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
package org.apache.sling.replication.agent.impl;

import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;

/**
 * {@link ResourceProvider} for {@link ReplicationAgent}s
 */
@Component(metatype = false)
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = ReplicationAgentResource.BASE_PATH)
public class ReplicationAgentResourceProvider implements ResourceProvider {

    private static final String CONFIGURATION_PATH = "/configuration";

    private static final String QUEUE_PATH = "/queue";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BundleContext context;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    protected void deactivate() {
        this.context = null;
    }

    @Deprecated
    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request,
                                String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {

        Resource resource = null;
        if (path.endsWith(CONFIGURATION_PATH)) {
            String agentPath = path.substring(0, path.lastIndexOf(CONFIGURATION_PATH));
            if (log.isInfoEnabled()) {
                log.info("resolving configuration for agent {}", agentPath);
            }
            ReplicationAgent replicationAgent = getAgentAtPath(agentPath);
            if (replicationAgent != null) {
                ServiceReference configurationManagerServiceReference = context
                        .getServiceReference(ReplicationAgentConfigurationManager.class
                                .getName());
                if (configurationManagerServiceReference != null) {
                    ReplicationAgentConfigurationManager agentConfigurationManager = (ReplicationAgentConfigurationManager) context
                            .getService(configurationManagerServiceReference);
                    ReplicationAgentConfiguration configuration;
                    try {
                        configuration = agentConfigurationManager
                                .getConfiguration(replicationAgent);
                        resource = new ReplicationAgentConfigurationResource(configuration,
                                resourceResolver);
                    } catch (AgentConfigurationException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("could not find a configuration", e);
                        }
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("could not find a configuration manager service");
                    }
                }
            }
        } else if (path.endsWith(QUEUE_PATH)) {
            String agentPath = path.substring(0, path.lastIndexOf(QUEUE_PATH));
            if (log.isInfoEnabled()) {
                log.info("resolving queue for agent {}", agentPath);
            }
            ReplicationAgent replicationAgent = getAgentAtPath(agentPath);
            if (replicationAgent != null) {
                try {
                    resource = new ReplicationAgentQueueResource(replicationAgent.getQueue(null), resourceResolver);
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("could not find a queue for agent {}", replicationAgent.getName());
                    }
                }
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("resolving agent with path {}", path);
            }
            ReplicationAgent replicationAgent = getAgentAtPath(path);
            resource = replicationAgent != null ? new ReplicationAgentResource(replicationAgent,
                    resourceResolver) : null;
        }
        if (log.isInfoEnabled()) {
            log.info("resource found: {}", resource != null ? resource.getPath() : "none");
        }
        return resource;
    }

    private ReplicationAgent getAgentAtPath(String path) {
        ReplicationAgent replicationAgent = null;
        String agentName = path.substring(path.lastIndexOf('/') + 1);
        if (log.isDebugEnabled()) {
            log.debug("resolving agent {} at {}", agentName, path);
        }
        ServiceReference[] replicationAgentReferences;
        try {
            replicationAgentReferences = context.getServiceReferences(
                    ReplicationAgent.class.getName(),
                    "(name=" + agentName + ")");
            if (replicationAgentReferences != null && replicationAgentReferences.length == 1) {
                replicationAgent = (ReplicationAgent) context
                        .getService(replicationAgentReferences[0]);
                if (log.isDebugEnabled()) {
                    log.debug("replication agent found: {}", replicationAgent);
                }
            } else {
                log.warn("could not find a replication agent with name {}", agentName);
            }
        } catch (InvalidSyntaxException e) {
            if (log.isWarnEnabled()) {
                log.warn("there was a syntax problem while getting agent service {}", e);
            }
        }
        return replicationAgent;
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

}