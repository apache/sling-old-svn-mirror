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
package org.apache.sling.pipes.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.pipes.Pipe;
import org.apache.sling.pipes.PipeBindings;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Check for pipes presence under <code>pipes</code> node of the given resource, and make their output available as pipes
 * to the script. Note that current resource is passed as a binding to the executed pipes, as a map of properties, plus
 * the name and path, with the name <code>BINDING_CURRENTRESOURCE</code>
 */
@Model(adaptables = Resource.class)
public class PipeModel {
    Logger LOG = LoggerFactory.getLogger(PipeModel.class);

    /**
     * name of the child nodes under which we should look for pipes
     */
    protected static final String NN_PIPES = "pipes";

    /**
     * name of the binding in which we should register current resource bindings
     */
    protected static final String BINDING_CURRENTRESOURCE = "currentResource";

    /**
     * map of the found pipes outputs
     */
    protected Map<String, Iterator<Resource>> outputs;

    /**
     * Getter for outputs
     * @return outputs of the model: pipe name as key, outputs as value
     */
    public Map<String, Iterator<Resource>> getOutputs() {
        return outputs;
    }

    @OSGiService
    protected Plumber plumber;

    /**
     * pipe root of the current resource
     */
    protected Resource root;

    /**
     * current resource
     */
    protected Resource currentResource;

    /**
     * Constructor
     * @param resource resource upon which this model is constructed
     */
    public PipeModel(Resource resource) {
        currentResource = resource;
        LOG.debug("constructing Pipe Model with {}", currentResource.getPath());
        root = resource.getChild(NN_PIPES);
    }

    @PostConstruct
    protected void init(){
        LOG.debug("initialising Pipe Model");
        if (root != null){
            outputs = new HashMap<>();
            for (Iterator<Resource> pipeCandidates = root.listChildren(); pipeCandidates.hasNext();){
                Resource candidate = pipeCandidates.next();
                try {
                    Pipe pipe = plumber.getPipe(candidate);
                    pipe.getBindings().addBinding(BINDING_CURRENTRESOURCE, currentResource.adaptTo(ValueMap.class));
                    pipe.getBindings().updateStaticBindings(BINDING_CURRENTRESOURCE, currentResource);
                    outputs.put(pipe.getName(), pipe.getOutput());
                    LOG.debug("found and initialized {}", pipe.getName());
                } catch(Exception e){
                    LOG.error("unable to bind {} pipe", candidate.getPath(), e);
                }
            }
        } else {
            LOG.debug("no root node found");
        }
    }
}
