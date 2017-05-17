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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * provides generic utilities for a pipe, is also a dummy pipe (outputs its input, without changing anything)
 */
public class BasePipe implements Pipe {

    private final Logger logger = LoggerFactory.getLogger(BasePipe.class);

    public static final String RESOURCE_TYPE = "slingPipes/base";
    public static final String DRYRUN_KEY = "dryRun";
    public static final String READ_ONLY = "readOnly";
    public static final String PN_STATUS = "status";
    public static final String PN_STATUS_MODIFIED = "statusModified";
    public static final String STATUS_STARTED = "started";
    public static final String STATUS_FINISHED = "finished";
    protected static final String DRYRUN_EXPR = "${" + DRYRUN_KEY + "}";

    protected ResourceResolver resolver;
    protected ValueMap properties;
    protected Resource resource;
    protected ContainerPipe parent;
    protected String distributionAgent;
    protected PipeBindings bindings;
    protected ReferencePipe referrer;

    // used by pipes using complex JCR configurations
    public static final List<String> IGNORED_PROPERTIES = Arrays.asList(new String[]{"jcr:lastModified", "jcr:primaryType", "jcr:created", "jcr:createdBy"});


    protected Boolean dryRunObject;

    @Override
    public ContainerPipe getParent() {
        return parent;
    }

    @Override
    public void setParent(ContainerPipe parent) {
        this.parent = parent;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    protected Plumber plumber;

    private String name;

    /**
     * Pipe Constructor
     * @param plumber plumber
     * @param resource configuration resource
     * @throws Exception in case configuration is not working
     */
    public BasePipe(Plumber plumber, Resource resource) throws Exception {
        this.resource = resource;
        properties = resource.adaptTo(ValueMap.class);
        resolver = resource.getResourceResolver();
        this.plumber = plumber;
        name = properties.get(PN_NAME, resource.getName());
        distributionAgent = properties.get(PN_DISTRIBUTION_AGENT, String.class);
        bindings = new PipeBindings(resource);
    }

    @Override
    public boolean isDryRun() {
        if (dryRunObject == null) {
            Object run =  bindings.isBindingDefined(DRYRUN_KEY) ? bindings.instantiateObject(DRYRUN_EXPR) : false;
            dryRunObject =  run != null && run instanceof Boolean ? (Boolean)run : false;
        }
        boolean dryRun = dryRunObject != null ? dryRunObject : false;
        return dryRun;
    }

    @Override
    public String toString() {
        return name + " " + "(path: " + resource.getPath() + ", dryRun: " + isDryRun() + ", modifiesContent: " + modifiesContent() + ")";
    }

    @Override
    public boolean modifiesContent() {
        return false;
    }

    @Override
    public String getName(){
        return name;
    }

    /**
     * Get pipe's expression, instanciated or not
     * @return configured expression
     */
    public String getExpr(){
        String rawExpression = properties.get(PN_EXPR, "");
        return bindings.instantiateExpression(rawExpression);
    }

    /**
     * Get pipe's path, instanciated or not
     * @return configured path (can be empty)
     */
    public String getPath() {
        String rawPath = properties.get(PN_PATH, "");
        return bindings.instantiateExpression(rawPath);
    }

    @Override
    public Resource getConfiguredInput() {
        Resource configuredInput = null;
        String path = getPath();
        if (StringUtils.isNotBlank(path)){
            configuredInput = resolver.getResource(path);
            if (configuredInput == null) {
                logger.warn("configured path {} is not found, expect some troubles...", path);
            }
        }
        return configuredInput;
    }

    /**
     * Retrieves previous pipe if contained by a parent, or referrer's
     * @return pipe before this one or the referrer's can be null in case there is no parent
     */
    protected Pipe getPreviousPipe(){
        return referrer == null ? (parent != null ? parent.getPreviousPipe(this) : null) : referrer.getPreviousPipe();
    }

    @Override
    public Resource getInput() {
        Resource resource = getConfiguredInput();
        if (resource == null && parent != null){
            Pipe previousPipe = getPreviousPipe();
            if (previousPipe != null) {
                return bindings.getExecutedResource(previousPipe.getName());
            }
        }
        return resource;
    }


    @Override
    public Object getOutputBinding() {
        if (parent != null){
            Resource resource = bindings.getExecutedResource(getName());
            if (resource != null) {
                return resource.adaptTo(ValueMap.class);
            }
        }
        return null;
    }

    @Override
    public PipeBindings getBindings() {
        return bindings;
    }

    @Override
    public void setBindings(PipeBindings bindings) {
        this.bindings = bindings;
    }

    /**
     * default execution, just returns current resource
     * @return output of this pipe, which is here the input resource
     */
    public Iterator<Resource> getOutput(){
        Resource resource = getInput();
        if (resource != null){
            return Collections.singleton(resource).iterator();
        }
        return EMPTY_ITERATOR;
    }

    /**
     * Get configuration node
     * @return configuration node if any
     */
    public Resource getConfiguration() {
        return resource.getChild(NN_CONF);
    }

    @Override
    public String getDistributionAgent() {
        return distributionAgent;
    }

    @Override
    public void setReferrer(ReferencePipe pipe) {
        referrer = pipe;
    }

    /**
     * Empty resource iterator
     */
    public static final Iterator<Resource> EMPTY_ITERATOR = Collections.emptyIterator();
}
