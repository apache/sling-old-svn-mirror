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

import static org.apache.sling.api.resource.ResourceResolverFactory.SUBSERVICE;
import static org.apache.sling.pipes.BasePipe.PN_STATUS;
import static org.apache.sling.pipes.BasePipe.PN_STATUS_MODIFIED;
import static org.apache.sling.pipes.BasePipe.STATUS_FINISHED;
import static org.apache.sling.pipes.BasePipe.STATUS_STARTED;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.pipes.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implements plumber interface, registers default pipes, and provides execution facilities
 */
@Component(service = {Plumber.class, JobConsumer.class}, property = {
        JobConsumer.PROPERTY_TOPICS +"="+PlumberImpl.SLING_EVENT_TOPIC
})
@Designate(ocd = PlumberImpl.Configuration.class)
public class PlumberImpl implements Plumber, JobConsumer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    public static final int DEFAULT_BUFFER_SIZE = 1000;

    @ObjectClassDefinition(name="Apache Sling Pipes : Plumber configuration")
    public @interface Configuration {
        @AttributeDefinition(description="Number of iterations after which plumber should saves a pipe execution")
        int bufferSize() default PlumberImpl.DEFAULT_BUFFER_SIZE;

        @AttributeDefinition(description="Name of service user, with appropriate rights, that will be used for async execution")
        String serviceUser();

        @AttributeDefinition(description="Users allowed to register async pipes")
        String[] authorizedUsers() default  {"admin"};
    }

    Map<String, Class<? extends BasePipe>> registry;

    public static final String SLING_EVENT_TOPIC = "org/apache/sling/pipes/topic";

    private int bufferSize;

    private Map serviceUser;

    private List<String> allowedUsers;

    @Activate
    public void activate(Configuration configuration){
        bufferSize = configuration.bufferSize();
        serviceUser = Collections.singletonMap(SUBSERVICE, configuration.serviceUser());
        allowedUsers = Arrays.asList(configuration.authorizedUsers());
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
        registerPipe(TraversePipe.RESOURCE_TYPE, TraversePipe.class);
    }

    @Reference(policy= ReferencePolicy.DYNAMIC, cardinality= ReferenceCardinality.OPTIONAL)
    protected volatile Distributor distributor = null;

    @Reference
    JobManager jobManager;

    @Reference
    ResourceResolverFactory factory;

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
    public Job executeAsync(ResourceResolver resolver, String path, Map bindings) {
        if (allowedUsers.contains(resolver.getUserID())) {
            if (StringUtils.isBlank((String)serviceUser.get(SUBSERVICE))) {
                log.error("please configure plumber service user");
            }
            final Map props = new HashMap();
            props.put(SlingConstants.PROPERTY_PATH, path);
            props.put(PipeBindings.NN_ADDITIONALBINDINGS, bindings);
            return jobManager.addJob(SLING_EVENT_TOPIC, props);
        }
        return null;
    }

    @Override
    public Set<String> execute(ResourceResolver resolver, String path, Map additionalBindings, OutputWriter writer, boolean save) throws Exception {
        Resource pipeResource = resolver.getResource(path);
        Pipe pipe = getPipe(pipeResource);
        if (pipe == null) {
            throw new Exception("unable to build pipe based on configuration at " + path);
        }
        if (additionalBindings != null && (Boolean)additionalBindings.getOrDefault(BasePipe.READ_ONLY, true) && pipe.modifiesContent()) {
            throw new Exception("This pipe modifies content, you should use a POST request");
        }
        return execute(resolver, pipe, additionalBindings, writer, save);
    }

    @Override
    public Set<String> execute(ResourceResolver resolver, Pipe pipe, Map additionalBindings, OutputWriter writer, boolean save) throws Exception {
        try {
            if (additionalBindings != null && pipe instanceof ContainerPipe){
                pipe.getBindings().addBindings(additionalBindings);
            }
            log.info("[{}] execution starts, save ({})", pipe, save);
            writer.setPipe(pipe);
            if (isRunning(pipe.getResource())){
                throw new RuntimeException("Pipe is already running");
            }
            writeStatus(pipe, STATUS_STARTED);
            resolver.commit();

            Set<String> set = new HashSet<>();
            for (Iterator<Resource> it = pipe.getOutput(); it.hasNext();){
                Resource resource = it.next();
                if (resource != null) {
                    log.debug("[{}] retrieved {}", pipe.getName(), resource.getPath());
                    writer.write(resource);
                    set.add(resource.getPath());
                    persist(resolver, pipe, set, resource);
                }
            }
            if (save && pipe.modifiesContent()) {
                persist(resolver, pipe, set, null);
            }
            log.info("[{}] done executing.", pipe.getName());
            writer.ends();
            return set;
        } finally {
            writeStatus(pipe, STATUS_FINISHED);
            resolver.commit();
        }
    }

    /**
     * Persists pipe change if big enough, or ended, and eventually distribute changes
     * @param resolver resolver to use
     * @param pipe pipe at the origin of the changes,
     * @param paths paths that have been changed,
     * @param currentResource if running, null if ended
     * @throws PersistenceException in case save fails
     */
    protected void persist(ResourceResolver resolver, Pipe pipe, Set<String> paths, Resource currentResource) throws Exception {
        if  (pipe.modifiesContent() && resolver.hasChanges() && !pipe.isDryRun()){
            if (currentResource == null || paths.size() % bufferSize == 0){
                log.info("[{}] saving changes...", pipe.getName());
                writeStatus(pipe, currentResource == null ? STATUS_FINISHED : currentResource.getPath());
                resolver.commit();
            }
            if (currentResource == null && distributor != null && StringUtils.isNotBlank(pipe.getDistributionAgent())) {
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

    @Override
    public boolean isTypeRegistered(String type) {
        return registry.containsKey(type);
    }

    /**
     * writes the status of the pipe, also update <code>PN_STATUS_MODIFIED</code> date
     * @param pipe target pipe
     * @param status status to write
     * @throws RepositoryException in case write goes wrong
     */
    protected void writeStatus(Pipe pipe, String status) throws RepositoryException {
        if (StringUtils.isNotBlank(status)){
            ModifiableValueMap vm = pipe.getResource().adaptTo(ModifiableValueMap.class);
            vm.put(PN_STATUS, status);
            Calendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            vm.put(PN_STATUS_MODIFIED, cal);
        }
    }

    @Override
    public String getStatus(Resource pipeResource) {
        Resource statusResource = pipeResource.getChild(PN_STATUS);
        if (statusResource != null){
            String status = statusResource.adaptTo(String.class);
            if (StringUtils.isNotBlank(status)){
                return status;
            }
        }
        return STATUS_FINISHED;
    }

    @Override
    public PipeBuilder getBuilder(ResourceResolver resolver) {
        PipeBuilder builder = new PipeBuilderImpl(resolver, this);
        return builder;
    }

    @Override
    public boolean isRunning(Resource pipeResource) {
        return !getStatus(pipeResource).equals(STATUS_FINISHED);
    }

    @Override
    public JobResult process(Job job) {
        try(ResourceResolver resolver = factory.getServiceResourceResolver(serviceUser)){
            String path = (String)job.getProperty(SlingConstants.PROPERTY_PATH);
            Map bindings = (Map)job.getProperty(PipeBindings.NN_ADDITIONALBINDINGS);
            execute(resolver, path, bindings, new NopWriter(), true);
            return JobResult.OK;
        } catch (LoginException e) {
            log.error("unable to retrieve resolver for executing scheduled pipe", e);
        } catch (Exception e) {
            log.error("failed to execute the pipe", e);
        }
        return JobResult.FAILED;
    }
}
