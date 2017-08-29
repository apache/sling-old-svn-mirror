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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.pipes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.sling.jcr.resource.JcrResourceConstants.NT_SLING_FOLDER;
import static org.apache.sling.jcr.resource.JcrResourceConstants.NT_SLING_ORDERED_FOLDER;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

/**
 * Implementation of the PipeBuilder interface
 */
public class PipeBuilderImpl implements PipeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PipeBuilderImpl.class);

    public static final String PIPES_REPOSITORY_PATH = "/var/pipes";

    public static final String[] DEFAULT_NAMES = new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

    List<Step> steps;

    Step currentStep;

    Plumber plumber;

    ResourceResolver resolver;

    /**
     * protected constructor (to only allow internal classes to build it out)
     * @param resolver resolver with which the pipe will be built and executed
     * @param plumber instance of the plumber
     */
    protected PipeBuilderImpl(ResourceResolver resolver, Plumber plumber){
        this.plumber = plumber;
        this.resolver = resolver;
    }

    @Override
    public PipeBuilder pipe(String type){
        if (!plumber.isTypeRegistered(type)){
            throw new IllegalArgumentException(type + " is not a registered pipe type");
        }
        if (steps == null){
            steps = new ArrayList<>();
        }
        currentStep = new Step(type);
        steps.add(currentStep);
        return this;
    }

    /**
     * internal utility to glob pipe configuration &amp; expression configuration
     * @param type pipe type
     * @param expr expression
     * @return updated instance of PipeBuilder
     */
    protected PipeBuilder pipeWithExpr(String type, String expr){
        try {
            pipe(type).expr(expr);
        } catch (IllegalAccessException e){
            logger.error("exception while configuring {}", type, e);
        }
        return this;
    }

    @Override
    public PipeBuilder mv(String expr) {
        return pipeWithExpr(MovePipe.RESOURCE_TYPE, expr);
    }

    @Override
    public PipeBuilder write(Object... conf) throws IllegalAccessException {
        return pipe(WritePipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder grep(Object... conf) throws IllegalAccessException {
        return pipe(FilterPipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder auth(Object... conf) throws IllegalAccessException {
        return pipe(AuthorizablePipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder xpath(String expr) {
        return pipeWithExpr(XPathPipe.RESOURCE_TYPE, expr);
    }

    @Override
    public PipeBuilder $(String expr) {
        return pipeWithExpr(SlingQueryPipe.RESOURCE_TYPE, expr);
    }

    @Override
    public PipeBuilder rm() {
        return pipe(RemovePipe.RESOURCE_TYPE);
    }

    @Override
    public PipeBuilder traverse() {
        return pipe(TraversePipe.RESOURCE_TYPE);
    }

    @Override
    public PipeBuilder json(String expr) {
        return pipeWithExpr(JsonPipe.RESOURCE_TYPE, expr);
    }

    @Override
    public PipeBuilder mkdir(String expr) {
        return pipeWithExpr(PathPipe.RESOURCE_TYPE, expr);
    }

    @Override
    public PipeBuilder echo(String path) {
        try {
            pipe(BasePipe.RESOURCE_TYPE).path(path);
        } catch(IllegalAccessException e){
            logger.error("error when calling echo {}", path, e);
        }
        return this;
    }

    @Override
    public PipeBuilder parent() {
        return pipe(ParentPipe.RESOURCE_TYPE);
    }

    /**
     * check of presence of a current step, fails loudly if it's not the case
     * @throws IllegalAccessException exception thrown if current step is not present
     */
    protected void checkCurrentStep() throws IllegalAccessException {
        if (currentStep == null){
            throw new IllegalAccessException("A pipe should have been configured first");
        }
    }

    @Override
    public PipeBuilder with(Object... params) throws IllegalAccessException {
        return writeToCurrentStep(null, params);
    }

    @Override
    public PipeBuilder conf(Object... properties) throws IllegalAccessException {
        return writeToCurrentStep(Pipe.NN_CONF, properties);
    }

    /**
     * Add some configurations to current's Step node defined by name (if null, will be step's properties)
     * @param name name of the configuration node, can be null in which case it's the subpipe itself
     * @param params key/value pair list of configuration
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case configuration is wrong
     */
    protected PipeBuilder writeToCurrentStep(String name, Object... params) throws IllegalAccessException {
        checkCurrentStep();
        if (params.length % 2 > 0){
            throw new IllegalArgumentException("there should be an even number of arguments");
        }
        Map props = name != null ? currentStep.confs.get(name) : currentStep.properties;
        if (props == null){
            props = new HashMap();
            if (name != null){
                currentStep.confs.put(name, props);
            }
        }
        for (int i = 0; i < params.length; i += 2){
            props.put(params[i], params[i + 1]);
        }
        return this;
    }

    @Override
    public PipeBuilder expr(String value) throws IllegalAccessException {
        return this.with(Pipe.PN_EXPR, value);
    }

    @Override
    public PipeBuilder path(String value) throws IllegalAccessException {
        return this.with(Pipe.PN_PATH, value);
    }

    @Override
    public PipeBuilder name(String name) throws IllegalAccessException {
        checkCurrentStep();
        currentStep.name = name;
        return this;
    }

    /**
     * build a time + random based path under /var/pipes
     * @return full path of future Pipe
     */
    protected String buildPipePath() {
        final Calendar now = Calendar.getInstance();
        return PIPES_REPOSITORY_PATH + '/' + now.get(Calendar.YEAR) + '/' + now.get(Calendar.MONTH) + '/' + now.get(Calendar.DAY_OF_MONTH) + "/"
                + UUID.randomUUID().toString();
    }

    /**
     * Create a configuration resource
     * @param resolver current resolver
     * @param path path of the resource
     * @param type type of the node to be created
     * @param data map of properties to add
     * @throws PersistenceException in case configuration resource couldn't be persisted
     */
    protected void createResource(ResourceResolver resolver, String path, String type, Map data) throws PersistenceException {
        ResourceUtil.getOrCreateResource(resolver, path, data, type, false);
    }

    @Override
    public Pipe build() throws PersistenceException {
        String rootPath = buildPipePath();
        Resource pipeResource = ResourceUtil.getOrCreateResource(resolver, rootPath, ContainerPipe.RESOURCE_TYPE, NT_SLING_FOLDER, true);
        int index = 0;
        for (Step step : steps){
            String name = StringUtils.isNotBlank(step.name) ? step.name : DEFAULT_NAMES.length > index ? DEFAULT_NAMES[index] : Integer.toString(index);
            index++;
            String subPipePath = rootPath + "/" + Pipe.NN_CONF + "/" + name;
            createResource(resolver, subPipePath, NT_SLING_ORDERED_FOLDER, step.properties);
            logger.debug("built subpipe {}", subPipePath);
            for (Map.Entry<String, Map> entry : step.confs.entrySet()){
                createResource(resolver, subPipePath + "/" + entry.getKey(), NT_SLING_FOLDER, entry.getValue());
                logger.debug("built subpipe {}'s {} node", subPipePath, entry.getKey());
            }
        }
        resolver.commit();
        logger.debug("built pipe under {}", rootPath);
        return plumber.getPipe(pipeResource);
    }

    @Override
    public Set<String> run() throws Exception {
        return run(null);
    }

    @Override
    public Set<String> run(Map bindings) throws Exception {
        Pipe pipe = this.build();
        return plumber.execute(resolver, pipe, bindings,  new NopWriter() , true);
    }

    @Override
    public Job runAsync(Map bindings) throws PersistenceException {
        Pipe pipe = this.build();
        return plumber.executeAsync(resolver, pipe.getResource().getPath(), bindings);
    }

    /**
     * holds a subpipe set of informations
     */
    public class Step {
        String name;
        Map properties;
        Map<String, Map> confs = new HashMap<>();
        Step(String type){
            properties = new HashMap();
            properties.put(SLING_RESOURCE_TYPE_PROPERTY, type);
        }
    }
}
