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

import org.apache.sling.replication.agent.ReplicationAgent;

/**
 * a {@link ReplicationAgent} {@link org.apache.sling.api.resource.Resource}
 */
public class ReplicationAgentResource extends AbstractResource {

    public static final String BASE_PATH = "/system/replication/agent";

    public static final String RESOURCE_TYPE = "sling/replication/agent";

    public static final String RESOURCE_ROOT_TYPE = "sling/replication/agent/root";

    public static final String IMPORTER_RESOURCE_TYPE = "sling/replication/agent/importer";

    public static final String IMPORTER_BASE_PATH =  "/system/replication/receive";


    private ReplicationAgent replicationAgent;

    private ResourceResolver resourceResolver;

    public ReplicationAgentResource(ReplicationAgent replicationAgent,
                    ResourceResolver resourceResolver) {
        if (replicationAgent == null) {
            throw new RuntimeException("cannot create an agent resource with a null agent");
        }
        this.replicationAgent = replicationAgent;
        this.resourceResolver = resourceResolver;
    }

    public String getPath() {
        return BASE_PATH + '/' + replicationAgent.getName();
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
        if (type == ReplicationAgent.class) {
            return (AdapterType) replicationAgent;
        }
        else {
            return super.adaptTo(type);
        }
    }

}
