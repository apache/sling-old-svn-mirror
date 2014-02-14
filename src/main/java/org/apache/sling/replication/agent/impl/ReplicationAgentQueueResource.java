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
import org.apache.sling.replication.queue.ReplicationQueue;

/**
 * A {@link org.apache.sling.api.resource.Resource} for {@link ReplicationQueue}s
 */
public class ReplicationAgentQueueResource extends AbstractResource {

    public static final String RESOURCE_TYPE = "sling/replication/agent/queue";

    public static final String SUFFIX_PATH = "/queue";

    public static final String EVENT_RESOURCE_TYPE = "sling/replication/agent/queue/event";

    public static final String EVENT_SUFFIX_PATH = "/queue/event";

    private final ReplicationQueue queue;

    private final ResourceResolver resourceResolver;

    public ReplicationAgentQueueResource(
            ReplicationQueue queue,
            ResourceResolver resourceResolver) {
        this.queue = queue;
        this.resourceResolver = resourceResolver;
    }

    public String getPath() {
        return ReplicationAgentResource.BASE_PATH + '/' + queue.getName() + "/queue";
    }

    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public String getResourceSuperType() {
        return null;
    }

    public ResourceMetadata getResourceMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setResolutionPath(ReplicationAgentResource.BASE_PATH + '/' + queue.getName());
        return metadata;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (ReplicationQueue.class == type) {
            return (AdapterType) queue;
        } else {
            return super.adaptTo(type);
        }
    }
}