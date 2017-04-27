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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.ContainerPipe;
import org.apache.sling.pipes.Pipe;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.pipes.ReferencePipe;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * implements plumber interface, and registers default pipes
 */
@Component(service = {Plumber.class})
public class PlumberImpl implements Plumber {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    Map<String, Class<? extends BasePipe>> registry;

    @Reference(policy= ReferencePolicy.DYNAMIC, cardinality= ReferenceCardinality.OPTIONAL)
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
        registerPipe(NotPipe.RESOURCE_TYPE, NotPipe.class);
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
        if (save) {
            persist(resolver, pipe, set);
        }
        log.info("[{}] done executing.", pipe.getName());
        return set;
    }

    @Override
    public void persist(ResourceResolver resolver, Pipe pipe, Set<String> paths) throws PersistenceException {
        if  (pipe.modifiesContent() && resolver.hasChanges() && !pipe.isDryRun()){
            log.info("[{}] saving changes...", pipe.getName());
            resolver.commit();
            if (distributor != null && StringUtils.isNotBlank(pipe.getDistributionAgent())) {
                log.info("a distribution agent is configured, will try to distribute the changes");
                DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, true, paths.toArray(new String[paths.size()]));
                DistributionResponse response = distributor.distribute(pipe.getDistributionAgent(), resolver, request);
                log.info("distribution response : {}", response);
            }
        }
    }

    @Override
    public void registerPipe(String type, Class<? extends BasePipe> pipeClass) {
        registry.put(type, pipeClass);
    }
}
