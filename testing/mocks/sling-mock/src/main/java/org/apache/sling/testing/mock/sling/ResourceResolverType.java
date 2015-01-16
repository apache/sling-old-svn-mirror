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
    RESOURCERESOLVER_MOCK(RRMockMockResourceResolverAdapter.class.getName(), null),

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
    JCR_MOCK(JcrMockResourceResolverAdapter.class.getName(), null),

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
            "org.apache.sling:org.apache.sling.testing.sling-mock-jackrabbit");

    private final String resourceResolverTypeAdapterClass;
    private final String artifactCoordinates;

    private ResourceResolverType(final String resourceResolverTypeAdapterClass, final String artifactCoordinates) {
        this.resourceResolverTypeAdapterClass = resourceResolverTypeAdapterClass;
        this.artifactCoordinates = artifactCoordinates;
    }

    String getResourceResolverTypeAdapterClass() {
        return this.resourceResolverTypeAdapterClass;
    }

    String getArtifactCoordinates() {
        return this.artifactCoordinates;
    }

}
