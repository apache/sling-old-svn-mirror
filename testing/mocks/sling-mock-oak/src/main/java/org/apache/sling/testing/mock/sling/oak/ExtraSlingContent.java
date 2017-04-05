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
package org.apache.sling.testing.mock.sling.oak;

import static java.util.Collections.singleton;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;

import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;

/**
 * Adds some default indexes useful for by sling resource-jcr mapping.
 * This is only a small subset of what is defined by default in the org.apache.sling.jcr.oak.server bundle.
 */
final class ExtraSlingContent implements RepositoryInitializer {

    @Override
    public void initialize(NodeBuilder root) {
        if (root.hasChildNode(INDEX_DEFINITIONS_NAME)) {
            NodeBuilder index = root.child(INDEX_DEFINITIONS_NAME);

            // jcr:
            property(index, "jcrLanguage", "jcr:language");
            property(index, "jcrLockOwner", "jcr:lockOwner");

            // sling:
            property(index, "slingAlias", "sling:alias");
            property(index, "slingResource", "sling:resource");
            property(index, "slingResourceType", "sling:resourceType");
            property(index, "slingVanityPath", "sling:vanityPath");
        }
    }

    /**
     * A convenience method to create a non-unique property index.
     */
    private static void property(NodeBuilder index, String indexName, String propertyName) {
        if (!index.hasChildNode(indexName)) {
            createIndexDefinition(index, indexName, true, false, singleton(propertyName), null);
        }
    }

}
