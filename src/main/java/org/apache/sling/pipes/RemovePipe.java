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
package org.apache.sling.pipes;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

/**
 * this pipe tries to remove the input resource, abstracting its type,
 * returning parent of the input
 */
public class RemovePipe extends BasePipe {
    private static Logger logger = LoggerFactory.getLogger(RemovePipe.class);
    public static final String RESOURCE_TYPE = "slingPipes/rm";

    /**
     * In case input resource is a node and configuration is set, only configured properties,
     * and subtrees will be removed
     */
    Resource filter;

    public RemovePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        filter = getConfiguration();
    }

    @Override
    public boolean modifiesContent() {
        return true;
    }

    @Override
    public Iterator<Resource> getOutput() {
        Resource resource = getInput();
        String parentPath = null;
        try {
            if (resource.adaptTo(Node.class) != null) {
                parentPath = removeTree(resource, filter);
            } else if (resource.adaptTo(Property.class) != null){
                Property property = resource.adaptTo(Property.class);
                parentPath = property.getParent().getPath();
                logger.info("removing property {}", property.getPath());
                if (!isDryRun()){
                    property.remove();
                }
            }
        } catch (RepositoryException e){
            logger.error("unable to remove current resource {}", resource.getPath(), e);
        }
        if (parentPath != null) {
            return Collections.singleton(resolver.getResource(parentPath)).iterator();
        }
        return Collections.emptyIterator();
    }

    /**
     * remove properties, returns the number of properties that were configured to be removed
     * @return
     */
    private int removeProperties(Resource resource, Resource configuration) throws RepositoryException {
        int count = 0;
        if (configuration != null) {
            Node node = resource.adaptTo(Node.class);
            ValueMap configuredProperties = configuration.adaptTo(ValueMap.class);
            for (String key : configuredProperties.keySet()){
                if (! IGNORED_PROPERTIES.contains(key)){
                    count++;
                    if (node.hasProperty(key)){
                        logger.info("removing {}", node.getProperty(key).getPath());
                        if (! isDryRun()){
                            node.getProperty(key).remove();
                        }
                    }
                }
            }
        }
        return count;
    }

    private String removeTree(Resource resource, Resource configuration) throws RepositoryException {
        String remainingPath = resource.getPath();
        int configuredProperties = removeProperties(resource, configuration);
        Node configuredNode = configuration != null ? configuration.adaptTo(Node.class) : null;
        NodeIterator childrenConfiguration = configuredNode != null ? configuredNode.getNodes() : null;
        if (childrenConfiguration == null || (! childrenConfiguration.hasNext() && configuredProperties == 0)){
            //explicit configuration to remove the node altogether
            logger.info("removing {}", resource.getPath());
            remainingPath = resource.getParent().getPath();
            if (! isDryRun()){
                resource.adaptTo(Node.class).remove();
            }
        } else {
            while (childrenConfiguration.hasNext()){
                Node childConf = childrenConfiguration.nextNode();
                Resource child = resource.getChild(childConf.getName());
                if (child != null){
                    removeTree(child, configuration.getChild(childConf.getName()));
                }
            }
        }
        return remainingPath;
    }
}
