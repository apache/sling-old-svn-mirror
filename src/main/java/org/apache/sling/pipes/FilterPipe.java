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
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * intends to output the input only if configured conditions are fulfilled
 */
public class FilterPipe extends BasePipe {
    private static Logger logger = LoggerFactory.getLogger(FilterPipe.class);
    public static final String RESOURCE_TYPE = "slingPipes/filter";
    public static final String PREFIX_FILTER = "slingPipesFilter_";
    public static final String PN_NOCHILDREN = PREFIX_FILTER + "noChildren";
    public static final String PN_TEST = PREFIX_FILTER + "test";

    public FilterPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    boolean propertiesPass(ValueMap current, ValueMap filter){
        if (filter.containsKey(PN_TEST)){
            if (!(Boolean) bindings.instantiateObject(filter.get(PN_TEST, "${false}"))){
                return false;
            }
        }
        for (String key : filter.keySet()){
            if (! IGNORED_PROPERTIES.contains(key) && !key.startsWith(PREFIX_FILTER)){
                Pattern pattern = Pattern.compile(filter.get(key, String.class));
                if (!pattern.matcher(current.get(key, String.class)).matches()){
                    return false;
                }
            }
        }
        return true;
    }

    boolean filterPasses(Resource currentResource, Resource filterResource){
        try {
            ValueMap current = currentResource.adaptTo(ValueMap.class);
            ValueMap filter = filterResource.adaptTo(ValueMap.class);
            if (propertiesPass(current, filter)) {
                Node currentNode = currentResource.adaptTo(Node.class);
                boolean noChildren = (Boolean) bindings.instantiateObject(filter.get(PN_NOCHILDREN, "${false}"));
                if (noChildren) {
                    return !currentNode.hasNodes();
                } else {
                    Node filterNode = filterResource.adaptTo(Node.class);
                    boolean returnValue = true;
                    for (NodeIterator children = filterNode.getNodes(); returnValue && children.hasNext();){
                        String childName = children.nextNode().getName();
                        if (!currentNode.hasNode(childName)){
                            return false;
                        } else {
                            returnValue &= filterPasses(currentResource.getChild(childName), filterResource.getChild(childName));
                        }
                    }
                    return returnValue;
                }
            }
        } catch (Exception e){
            logger.error("error when executing filter", e);
        }
        return false;
    }

    @Override
    public Iterator<Resource> getOutput() {
        Resource resource = getInput();
        if (resource != null){
            if (filterPasses(resource, getConfiguration())){
                logger.debug("filter passes for {}", resource.getPath());
                return super.getOutput();
            } else {
                logger.info("{} got filtered out", resource.getPath());
            }
        }
        return Collections.emptyIterator();
    }
}
