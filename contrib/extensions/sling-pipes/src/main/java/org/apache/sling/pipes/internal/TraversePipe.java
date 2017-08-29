/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.pipes.internal;

import org.apache.commons.collections.IteratorUtils;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Traverse either nodes or properties, in breadth first or depth first, for properties, they can be white listed
 */
public class TraversePipe extends BasePipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraversePipe.class);
    public static final String RESOURCE_TYPE = RT_PREFIX + "traverse";

    /**
     * Pipe Constructor
     *
     * @param plumber  plumber
     * @param resource configuration resource
     * @throws Exception in case configuration is not working
     */
    public TraversePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }


    @Override
    public Iterator<Resource> getOutput() {
        return new TraversingIterator(getInput(), getResource().getValueMap());
    }

    /**
     * iterative DFS or BFS jcr node tree iterator, transforming each visited node in a configured set of resources
     */
    public class TraversingIterator implements Iterator<Resource>{
        protected final static String PN_PROPERTIES = "properties";
        protected final static String PN_NAMEGLOBS = "nameGlobs";
        protected final static String PN_BREADTH = "breadthFirst";
        protected final static String PN_DEPTH = "depth";
        boolean properties;
        int initialLevel;
        int maxLevel;
        String[] nameGlobs;
        boolean breadthFirst;
        Iterator<Resource> currentResources;
        List<Node> nodesToVisit = new ArrayList<>();

        /**
         * From a given node, refresh resources extracted out of it depending on configuration
         * @param node
         * @throws RepositoryException
         */
        void refreshResourceIterator(Node node) throws RepositoryException {
            if (properties){
                PropertyIterator it = nameGlobs != null ? node.getProperties(nameGlobs) : node.getProperties();
                currentResources = IteratorUtils.transformedIterator(it, o -> {
                    try {
                        return resolver.getResource(((Property) o).getPath());
                    } catch (RepositoryException e) {
                        LOGGER.error("unable to read property", e);
                    }
                    return null;
                });
            } else {
                currentResources = IteratorUtils.singletonIterator(resolver.getResource(node.getPath()));
            }
        }

        int getDepth(String path) {
            return path.split("/").length;
        }

        boolean isBeforeLastLevel(Node node) throws RepositoryException {
            return maxLevel < 0 || getDepth(node.getPath()) < maxLevel;
        }

        /**
         * Constructor with root node, & configuration
         * @param root
         * @param configuration
         */
        TraversingIterator(Resource root, ValueMap configuration){
            properties = configuration.get(PN_PROPERTIES, false);
            if (properties) {
                nameGlobs = configuration.get(PN_NAMEGLOBS, String[].class);
            }
            breadthFirst = configuration.get(PN_BREADTH, false);
            maxLevel = configuration.get(PN_DEPTH, -1);
            if (maxLevel > 0){
                initialLevel = getDepth(root.getPath());
                maxLevel = initialLevel + maxLevel;
            }
            nodesToVisit.add(root.adaptTo(Node.class));
        }

        /**
         * Navigate up to the next node that have resources out of it
         * @return
         */
        boolean goToNextElligibleNode() {
            try {
                while ((currentResources == null || !currentResources.hasNext()) && nodesToVisit.size() > 0) {
                    Node node = nodesToVisit.remove(0);
                    LOGGER.debug("visiting {}", node.getPath());
                    refreshResourceIterator(node);
                    int indexAdd = breadthFirst ? nodesToVisit.size() : 0;
                    if (isBeforeLastLevel(node)) {
                        nodesToVisit.addAll(indexAdd, IteratorUtils.toList(node.getNodes()));
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error("unable to read node children", e);
            }
            return currentResources != null && currentResources.hasNext();
        }

        @Override
        public boolean hasNext() {
            return (currentResources != null && currentResources.hasNext()) || goToNextElligibleNode();
        }

        @Override
        public Resource next() {
            return currentResources.next();
        }
    }
}
