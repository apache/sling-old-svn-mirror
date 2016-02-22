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
package org.apache.sling.testing.mock.sling;

/**
 * The resource resolver mock implementation supports different underlying
 * repository implementations.
 */
public enum ResourceResolverType {

    /**
     * Uses Sling "resourceresolver-mock" implementation, no underlying JCR
     * repository.
     * <ul>
     * <li>Simulates an In-Memory resource tree, does not provide adaptions to
     * JCR.</li>
     * <li>You can use it to make sure the code you want to test does not
     * contain references to JCR API.</li>
     * <li>Behaves slightly different from JCR resource mapping e.g. handling
     * binary and date values.</li>
     * <li>This resource resolver type is very fast.</li>
     * </ul>
     */
    RESOURCERESOLVER_MOCK(RRMockMockResourceResolverAdapter.class.getName(), null, NodeTypeMode.NOT_SUPPORTED),

    /**
     * Uses a simple JCR "in-memory" mock as underlying repository.
     * <ul>
     * <li>Uses the real Sling Resource Resolver and JCR Resource mapping
     * implementation.</li>
     * <li>The mock JCR implementation from Apache Sling is used.</li>
     * <li>It supports the most important, but not all JCR features. Extended
     * features like Versioning, Eventing, Search, Transaction handling etc. are
     * not supported.</li>
     * <li>This resource resolver type is quite fast.</li>
     * </ul>
     */
    JCR_MOCK(MockJcrResourceResolverAdapter.class.getName(), null, NodeTypeMode.NAMESPACES_ONLY),

    /**
     * Uses a real JCR Jackrabbit repository.
     * <ul>
     * <li>Uses the real Sling Resource Resolver and JCR Resource mapping
     * implementation.</li>
     * <li>The JCR repository is started on first access, this may take some
     * seconds.</li>
     * <li>Beware: The repository is not cleared for each unit test, so make
     * sure us use a unique node path for each unit test.</li>
     * </ul>
     */
    JCR_JACKRABBIT("org.apache.sling.testing.mock.sling.jackrabbit.JackrabbitMockResourceResolverAdapter",
            "org.apache.sling:org.apache.sling.testing.sling-mock-jackrabbit", NodeTypeMode.NODETYPES_REQUIRED),

    /**
     * Uses a real JCR Jackrabbit Oak repository.
     * <ul>
     * <li>Uses the real Sling Resource Resolver and JCR Resource mapping
     * implementation.</li>
     * <li>The JCR repository is started on first access, this may take some
     * seconds.</li>
     * <li>The <tt>MemoryNodeStore</tt> implementation is used, with no 
     * customizations.</li>
     * </ul>
     */
    JCR_OAK("org.apache.sling.testing.mock.sling.oak.OakMockResourceResolverAdapter",
            "org.apache.sling:org.apache.sling.testing.sling-mock-jackrabbit-oak", NodeTypeMode.NODETYPES_REQUIRED),
            
    /**
     * Provides resource resolver environment without any ResourceProvider.
     * You have to register one yourself to do anything useful with it.
     * <ul>
     * <li>Uses the real Sling Resource Resolver  implementation.</li>
     * <li>The performance of this resource resolver type depends on the resource provider registered.</li>
     * </ul>
     */
    NONE(MockNoneResourceResolverAdapter.class.getName(), null, NodeTypeMode.NOT_SUPPORTED);

            

    private final String resourceResolverTypeAdapterClass;
    private final String artifactCoordinates;
    private final NodeTypeMode nodeTypeMode;
    

    private ResourceResolverType(final String resourceResolverTypeAdapterClass, final String artifactCoordinates,
            final NodeTypeMode nodeTypeMode) {
        this.resourceResolverTypeAdapterClass = resourceResolverTypeAdapterClass;
        this.artifactCoordinates = artifactCoordinates;
        this.nodeTypeMode = nodeTypeMode;
    }

    String getResourceResolverTypeAdapterClass() {
        return this.resourceResolverTypeAdapterClass;
    }

    String getArtifactCoordinates() {
        return this.artifactCoordinates;
    }

    /**
     * @return How JCR namespaces and node types have to be handled.
     */
    public NodeTypeMode getNodeTypeMode() {
        return nodeTypeMode;
    }

}
