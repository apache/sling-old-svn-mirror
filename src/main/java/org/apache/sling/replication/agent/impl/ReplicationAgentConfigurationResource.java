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

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

import org.apache.sling.replication.agent.ReplicationAgentConfiguration;

/**
 * a {@link org.apache.sling.api.resource.Resource} for a {@link ReplicationAgentConfiguration}
 */
public class ReplicationAgentConfigurationResource extends AbstractResource {

    public static final String RESOURCE_TYPE = "sling/replication/config/agent";

    public static final String RESOURCE_ROOT_TYPE = "sling/replication/config/agent/root";


    public static final String BASE_PATH = "/system/replication/config/agent";

    private final ReplicationAgentConfiguration replicationAgentConfiguration;

    private final ResourceResolver resourceResolver;

    public ReplicationAgentConfigurationResource(
                    ReplicationAgentConfiguration replicationAgentConfiguration,
                    ResourceResolver resourceResolver) {
        if (replicationAgentConfiguration == null) {
            throw new RuntimeException("cannot create a configuration resource with a null configuration");
        }
        this.replicationAgentConfiguration = replicationAgentConfiguration;
        this.resourceResolver = resourceResolver;
    }

    public String getPath() {
        return BASE_PATH + '/' + replicationAgentConfiguration.getName();
    }

    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public String getResourceSuperType() {
        return null;
    }

    public ResourceMetadata getResourceMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setResolutionPath(getPath());
        return metadata;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (ReplicationAgentConfiguration.class == type) {
            return (AdapterType) replicationAgentConfiguration;
        } else {
            return super.adaptTo(type);
        }
    }
}
