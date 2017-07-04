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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * pipe that writes to configured resource
 */
public class WritePipe extends BasePipe {
    private static final Logger logger = LoggerFactory.getLogger(WritePipe.class);
    public static final String RESOURCE_TYPE = "slingPipes/write";
    Node confTree;
    private List<Resource> propertiesToRemove;
    Pattern addPatch = Pattern.compile("\\+\\[(.*)\\]");
    Pattern multi = Pattern.compile("\\[(.*)\\]");

    /**
     * public constructor
     * @param plumber plumber instance
     * @param resource configuration resource
     * @throws Exception bad configuration handling
     */
    public WritePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        if (getConfiguration() == null){
            throw new Exception("write pipe is misconfigured: it should have a configuration node");
        }
        confTree = getConfiguration().adaptTo(Node.class);
    }


    /**
     * convert the configured string value (can be an expression) in a value that can be written in a resource.
     * also handles patch for multivalue properties like <code>+[value]</code> in which case <code>value</code>
     * is added to the MV property
     * @param resource resource to which value will be written
     * @param key property to which value will be written
     * @param expression configured value to write
     * @return actual value to write to the resource
     */
    protected Object computeValue(Resource resource, String key, String expression){
        Object value = bindings.instantiateObject((String) expression);
        if (value != null && value instanceof String) {
            //in that case we treat special case like MV or patches
            String sValue = (String)value;
            Matcher patch = addPatch.matcher(sValue);
            if (patch.matches()) {
                String newValue = patch.group(1);
                String[] actualValues = resource.adaptTo(ValueMap.class).get(key, String[].class);
                List<String> newValues = actualValues != null ? new LinkedList<>(Arrays.asList(actualValues)) : new ArrayList<String>();
                if (!newValues.contains(newValue)) {
                    newValues.add(newValue);
                }
                return newValues.toArray(new String[newValues.size()]);
            }
            Matcher multiMatcher = multi.matcher(sValue);
            if (multiMatcher.matches()) {
                return multiMatcher.group(1).split(",");
            }
        }
        return value;
    }


    protected Object computeValue(Resource resource, String key, Object expression) {
        if (expression instanceof String) {
            return computeValue(resource, key, (String)expression);
        } else if (expression instanceof String[]){
            List<String> values = new ArrayList<>();
            for (String expr : (String[])expression){
                values.add((String)computeValue(resource, key, expr));
            }
            return values.toArray(new String[values.size()]);
        }
        return expression;
    }

    @Override
    public boolean modifiesContent() {
        return true;
    }

    /**
     * Write properties from the configuration to the target resource,
     * instantiating both property names & values
     *
     * @param conf configured resource that holds all properties to write (and children)
     * @param target target resource on which configured values will be written
     * @throws RepositoryException issues occuring when traversing nodes
     */
    private void copyProperties(Resource conf, Resource target) throws RepositoryException {
        ValueMap writeMap = conf.adaptTo(ValueMap.class);
        ModifiableValueMap properties = target.adaptTo(ModifiableValueMap.class);

        //writing current node
        if (properties != null && writeMap != null) {
            for (Map.Entry<String, Object> entry : writeMap.entrySet()) {
                if (!IGNORED_PROPERTIES.contains(entry.getKey())) {
                    String key = parent != null ? bindings.instantiateExpression(entry.getKey()) : entry.getKey();
                    Object value = computeValue(target, key, entry.getValue());
                    if (value == null) {
                        //null value are not handled by modifiable value maps,
                        //removing the property if it exists
                        addPropertyToRemove(target.getChild(key));
                    } else {
                        logger.info("writing {}={}",target.getPath() + "@" + key, value);
                        if (!isDryRun()){
                            properties.put(key, value);
                        }
                    }
                }
            }
        }
    }

    /**
     * we store all property to remove for very last moment (in order to potentially reuse their value)
     * @param property property resource that should be removed
     */
    private void addPropertyToRemove(Resource property){
        if (property != null) {
            if (propertiesToRemove == null) {
                propertiesToRemove = new ArrayList<>();
            }
            propertiesToRemove.add(property);
        }
    }

    /**
     * write the configured tree at the target resource, creating each node if needed, copying values.
     * @param conf configuration JCR tree to write to target resource
     * @param target target resource to write
     */
    private void writeTree(Node conf, Resource target) throws RepositoryException {
        copyProperties(resolver.getResource(conf.getPath()), target);
        NodeIterator childrenConf = conf.getNodes();
        if (childrenConf.hasNext()){
            Node targetNode = target.adaptTo(Node.class);
            while (childrenConf.hasNext()){
                Node childConf = childrenConf.nextNode();
                String name = childConf.getName();
                logger.info("dubbing {} at {}", conf.getPath(), target.getPath());
                if (!isDryRun()){
                    Node childTarget = targetNode.hasNode(name) ? targetNode.getNode(name) : targetNode.addNode(name, childConf.getPrimaryNodeType().getName());
                    writeTree(childConf, resolver.getResource(childTarget.getPath()));
                }
            }
        }
    }


    @Override
    public Iterator<Resource> getOutput() {
        try {
            Resource resource = getInput();
            if (resource != null) {
                writeTree(confTree, resource);
                if (propertiesToRemove != null && !propertiesToRemove.isEmpty()){
                    for (Resource propertyResource : propertiesToRemove) {
                        logger.info("removing {}", propertyResource.getPath());
                        if (!isDryRun()){
                            Property property = propertyResource.adaptTo(Property.class);
                            if (property != null) {
                                property.remove();
                            }
                        }
                    }
                }
                return super.getOutput();
            }
        } catch (Exception e) {
            logger.error("unable to write values, cutting pipe", e);
        } finally {
            if (propertiesToRemove != null){
                propertiesToRemove.clear();
            }
        }
        return EMPTY_ITERATOR;
    }
}
