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
package org.apache.sling.pipes.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.pipes.AuthorizablePipe;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.ContainerPipe;
import org.apache.sling.pipes.FilterPipe;
import org.apache.sling.pipes.PathPipe;
import org.apache.sling.pipes.JsonPipe;
import org.apache.sling.pipes.MovePipe;
import org.apache.sling.pipes.MultiPropertyPipe;
import org.apache.sling.pipes.ParentPipe;
import org.apache.sling.pipes.Pipe;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.pipes.ReferencePipe;
import org.apache.sling.pipes.RemovePipe;
import org.apache.sling.pipes.SlingQueryPipe;
import org.apache.sling.pipes.WritePipe;
import org.apache.sling.pipes.XPathPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Filter;

/**
 * implements plumber interface, and registers default pipes
 */
@Component
@Service
public class PlumberImpl implements Plumber {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    Map<String, Class<? extends BasePipe>> registry;

    @Reference (policy= ReferencePolicy.DYNAMIC, cardinality= ReferenceCardinality.OPTIONAL_UNARY)
    protected volatile Distributor distributor = null;

    @Activate
    public void activate(){
        registry = new HashMap<>();
        registerPipe(BasePipe.RESOURCE_TYPE, BasePipe.class);
        registerPipe(ContainerPipe.RESOURCE_TYPE, ContainerPipe.class);
        registerPipe(SlingQueryPipe.RESOURCE_TYPE, SlingQueryPipe.class);
        registerPipe(WritePipe.RESOURCE_TYPE, WritePipe.class);
        registerPipe(JsonPipe.RESOURCE_TYPE, JsonPipe.class);
        registerPipe(MultiPropertyPipe.RESOURCE_TYPE, MultiPropertyPipe.class);
        registerPipe(AuthorizablePipe.RESOURCE_TYPE, AuthorizablePipe.class);
        registerPipe(XPathPipe.RESOURCE_TYPE, XPathPipe.class);
        registerPipe(ReferencePipe.RESOURCE_TYPE, ReferencePipe.class);
        registerPipe(RemovePipe.RESOURCE_TYPE, RemovePipe.class);
        registerPipe(ParentPipe.RESOURCE_TYPE, ParentPipe.class);
        registerPipe(MovePipe.RESOURCE_TYPE, MovePipe.class);
        registerPipe(PathPipe.RESOURCE_TYPE, PathPipe.class);
        registerPipe(FilterPipe.RESOURCE_TYPE, FilterPipe.class);
    }

    @Override
    public Pipe getPipe(Resource resource) {
        if ((resource == null) || !registry.containsKey(resource.getResourceType())) {
            log.error("Pipe configuration resource is either null, or its type is not registered");
        } else {
            try {
                Class<? extends Pipe> pipeClass = registry.get(resource.getResourceType());
                return pipeClass.getDeclaredConstructor(Plumber.class, Resource.class).newInstance(this, resource);
            } catch (Exception e) {
                log.error("Unable to properly instantiate the pipe configured in {}", resource.getPath(), e);
            }
        }
        return null;
    }

    @Override
    public Set<String> execute(ResourceResolver resolver, String path, Map additionalBindings, boolean save) throws Exception {
        Resource pipeResource = resolver.getResource(path);
        Pipe pipe = getPipe(pipeResource);
        if (pipe == null) {
            throw new Exception("unable to build pipe based on configuration at " + path);
        }
        return execute(resolver, pipe, additionalBindings, save);
    }

    @Override
    public Set<String> execute(ResourceResolver resolver, Pipe pipe, Map additionalBindings, boolean save) throws Exception {
        if (additionalBindings != null && pipe instanceof ContainerPipe){
            pipe.getBindings().addBindings(additionalBindings);
        }

        log.info("[{}] execution starts, save ({})", pipe, save);
        Set<String> set = new HashSet<>();
        for (Iterator<Resource> it = pipe.getOutput(); it.hasNext();){
            Resource resource = it.next();
            if (resource != null) {
                log.debug("[{}] retrieved {}", pipe.getName(), resource.getPath());
                set.add(resource.getPath());
            }
        }
        if  (pipe.modifiesContent() && save && resolver.hasChanges() && !pipe.isDryRun()){
            log.info("[{}] saving changes...", pipe.getName());
            resolver.commit();
            if (distributor != null && StringUtils.isNotBlank(pipe.getDistributionAgent())) {
                log.info("a distribution agent is configured, will try to distribute the changes");
                DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, true, set.toArray(new String[set.size()]));
                DistributionResponse response = distributor.distribute(pipe.getDistributionAgent(), resolver, request);
                log.info("distribution response : {}", response);
            }
        }
        log.info("[{}] done executing.", pipe.getName());
        return set;
    }

    @Override
    public void registerPipe(String type, Class<? extends BasePipe> pipeClass) {
        registry.put(type, pipeClass);
    }
}
