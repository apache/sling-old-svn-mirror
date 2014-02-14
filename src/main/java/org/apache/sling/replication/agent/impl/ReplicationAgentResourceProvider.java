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
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ResourceProvider} for {@link ReplicationAgent}s
 */
@Component(metatype = false)
@Service(value = ResourceProvider.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS,
                value = {
                        ReplicationAgentResource.BASE_PATH,
                        ReplicationAgentConfigurationResource.BASE_PATH,
                        ReplicationAgentResource.IMPORTER_BASE_PATH
                })

})
public class ReplicationAgentResourceProvider implements ResourceProvider {

    static final String SECURITY_OBJECT = "/system/replication/security";

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

        if (!isAuthorized(resourceResolver)) return null;

        Resource resource = null;

        if (path.equals(ReplicationAgentConfigurationResource.BASE_PATH)) {

            return new SyntheticResource(resourceResolver, path, ReplicationAgentConfigurationResource.RESOURCE_ROOT_TYPE);

        } else if (path.equals(ReplicationAgentResource.BASE_PATH)) {

            return new SyntheticResource(resourceResolver, path, ReplicationAgentResource.RESOURCE_ROOT_TYPE);

        } else if (path.equals(ReplicationAgentResource.IMPORTER_BASE_PATH)) {

            return new SyntheticResource(resourceResolver, path, ReplicationAgentResource.IMPORTER_RESOURCE_TYPE);

        } else if (path.startsWith(ReplicationAgentConfigurationResource.BASE_PATH + "/")) {

            String agentName = getAgentNameAtPath(path);

            ServiceReference configurationManagerServiceReference = context
                    .getServiceReference(ReplicationAgentConfigurationManager.class
                            .getName());
            if (configurationManagerServiceReference != null) {
                ReplicationAgentConfigurationManager agentConfigurationManager = (ReplicationAgentConfigurationManager) context
                        .getService(configurationManagerServiceReference);
                ReplicationAgentConfiguration configuration;
                try {
                    configuration = agentConfigurationManager.getConfiguration(agentName);
                    resource = new ReplicationAgentConfigurationResource(configuration, resourceResolver);
                } catch (AgentConfigurationException e) {
                    log.warn("could not find a configuration", e);
                }
            } else {
                log.warn("could not find a configuration manager service");
            }
        } else if (path.startsWith(ReplicationAgentResource.BASE_PATH + "/")) {

            if (path.endsWith(ReplicationAgentQueueResource.SUFFIX_PATH)) {
                String agentPath = path.substring(0, path.lastIndexOf('/'));
                log.info("resolving queue with path {}", agentPath);

                ReplicationAgent replicationAgent = getAgentAtPath(agentPath);
                try {
                    resource = replicationAgent != null ? new ReplicationAgentQueueResource(replicationAgent.getQueue(null),
                            resourceResolver) : null;
                } catch (ReplicationQueueException e) {
                    log.warn("could not find a queue for agent {}", agentPath);
                }
            } else if (path.endsWith(ReplicationAgentQueueResource.EVENT_SUFFIX_PATH)) {
                resource = new SyntheticResource(resourceResolver, path, ReplicationAgentQueueResource.EVENT_RESOURCE_TYPE);
            } else {
                ReplicationAgent replicationAgent = getAgentAtPath(path);
                log.info("resolving agent with path {}", path);

                resource = replicationAgent != null ? new ReplicationAgentResource(replicationAgent,
                        resourceResolver) : null;
            }
        }

        log.info("resource found: {}", resource != null ? resource.getPath() : "none");
        return resource;
    }


    private boolean isAuthorized(ResourceResolver resourceResolver) {
        boolean isAuthorized = false;
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            try {
                isAuthorized = session.nodeExists(SECURITY_OBJECT);
            } catch (Exception ex) {
            }
        }


        if (isAuthorized) {
            log.debug("granting access to agent resources as user can read /system/replication/security");
        } else {
            log.debug("denying access to agent resources as user can't read /system/replication/security");
        }

        return isAuthorized;
    }

    private String getAgentNameAtPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private ReplicationAgent getAgentAtPath(String path) {
        ReplicationAgent replicationAgent = null;
        String agentName = getAgentNameAtPath(path);

        log.debug("resolving agent {} at {}", agentName, path);

        ServiceReference[] replicationAgentReferences;
        try {
            replicationAgentReferences = context.getServiceReferences(
                    ReplicationAgent.class.getName(), "(name=" + agentName + ")");

            if (replicationAgentReferences != null && replicationAgentReferences.length == 1) {
                replicationAgent = (ReplicationAgent) context
                        .getService(replicationAgentReferences[0]);

                log.debug("replication agent found: {}", replicationAgent);
            } else {
                log.warn("could not find a replication agent with name {}", agentName);
            }
        } catch (InvalidSyntaxException e) {
            log.warn("there was a syntax problem while getting agent service {}", e);
        }
        return replicationAgent;
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

}