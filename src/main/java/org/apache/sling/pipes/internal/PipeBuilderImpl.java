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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
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
     * @param resolver
     * @param plumber
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

    @Override
    public PipeBuilder mv(String expr) {
        return pipe(MovePipe.RESOURCE_TYPE);
    }

    @Override
    public PipeBuilder write(Object... conf) throws IllegalAccessException {
        return pipe(WritePipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder filter(Object... conf) throws IllegalAccessException {
        return pipe(FilterPipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder auth(Object... conf) throws IllegalAccessException {
        return pipe(AuthorizablePipe.RESOURCE_TYPE).conf(conf);
    }

    @Override
    public PipeBuilder xpath(String expr) throws IllegalAccessException {
        return pipe(XPathPipe.RESOURCE_TYPE).expr(expr);
    }

    @Override
    public PipeBuilder $(String expr) throws IllegalAccessException {
        return pipe(SlingQueryPipe.RESOURCE_TYPE).expr(expr);
    }

    @Override
    public PipeBuilder rm() {
        return pipe(RemovePipe.RESOURCE_TYPE);
    }

    @Override
    public PipeBuilder json(String expr) throws IllegalAccessException {
        return pipe(JsonPipe.RESOURCE_TYPE).expr(expr);
    }

    @Override
    public PipeBuilder mkdir(String expr) throws IllegalAccessException {
        return pipe(PathPipe.RESOURCE_TYPE).expr(expr);
    }

    @Override
    public PipeBuilder echo(String path) throws IllegalAccessException {
        return pipe(BasePipe.RESOURCE_TYPE).path(path);
    }

    @Override
    public PipeBuilder parent() {
        return pipe(ParentPipe.RESOURCE_TYPE);
    }

    /**
     * check of presence of a current step, fails loudly if it's not the case
     * @throws IllegalAccessException
     */
    protected void checkCurrentStep() throws IllegalAccessException {
        if (currentStep == null){
            throw new IllegalAccessException("A pipe should have been configured first");
        }
    }

    @Override
    public PipeBuilder with(String param, String value) throws IllegalAccessException {
        checkCurrentStep();
        currentStep.properties.put(param, value);
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

    @Override
    public PipeBuilder conf(Object... properties) throws IllegalAccessException {
        checkCurrentStep();
        if (properties.length % 2 > 0){
            throw new IllegalArgumentException("there should be an even number of arguments");
        }
        for (int i = 0; i < properties.length; i += 2){
            currentStep.conf.put(properties[i], properties[i + 1]);
        }
        return this;
    }

    /**
     * build a time + random based path under /var/pipes
     * @return
     */
    protected String buildPipePath() {
        final Calendar now = Calendar.getInstance();
        return PIPES_REPOSITORY_PATH + '/' + now.get(Calendar.YEAR) + '/' + now.get(Calendar.MONTH) + '/' + now.get(Calendar.DAY_OF_MONTH) + "/"
                + UUID.randomUUID().toString();
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
            ResourceUtil.getOrCreateResource(resolver, subPipePath, step.properties, NT_SLING_ORDERED_FOLDER, false);
            logger.debug("built subpipe {}", subPipePath);
            if (!step.conf.isEmpty()){
                ResourceUtil.getOrCreateResource(resolver, subPipePath + "/" + Pipe.NN_CONF, step.conf, NT_SLING_FOLDER, false);
                logger.debug("built subpipe {}'s conf node", subPipePath);
            }
        }
        resolver.commit();
        logger.debug("built pipe under {}", rootPath);
        return plumber.getPipe(pipeResource);
    }

    /**
     * builds & run configured pipe
     * @return
     * @throws Exception
     */
    public Set<String> run() throws Exception {
        Pipe pipe = this.build();
        return plumber.execute(resolver, pipe, null, new NopWriter(), true);
    }

    /**
     * holds a subpipe set of informations
     */
    public class Step {
        String name;
        Map properties;
        Map conf;
        Step(String type){
            properties = new HashMap();
            conf = new HashMap();
            properties.put(SLING_RESOURCE_TYPE_PROPERTY, type);
        }
    }
}
