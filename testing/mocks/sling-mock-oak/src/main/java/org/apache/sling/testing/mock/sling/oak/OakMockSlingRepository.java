/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling.oak;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(SlingRepository.class)
public final class OakMockSlingRepository implements SlingRepository {

    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private Repository repository;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    
    private static final Logger log = LoggerFactory.getLogger(OakMockSlingRepository.class);
    
    @Activate
    protected void activate(BundleContext bundleContext) {
        executor = Executors.newSingleThreadExecutor();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        
        if (bundleContext.getServiceReference(Executor.class) == null) {
            bundleContext.registerService(Executor.class, executor, null);
        }
        
        Oak oak = new Oak()
                .with(executor)
                .with(scheduledExecutor);
        
        Jcr jcr = new Jcr(oak)
                .with(new ExtraSlingContent())
                .with(executor)
                .with(scheduledExecutor);
        
        this.repository = jcr.createRepository();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        // shutdown executors
        // force immediate shutdown for all executors without waiting for tasks for completion - we're only in unit tests! 
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        shutdownExecutorService(repository, "scheduledExecutor");

        // shutdown OAK JCR repository
        ((JackrabbitRepository)repository).shutdown();
    }

    private void shutdownExecutorService(Object instance, String fieldName) {
        try {
            Field executorField = instance.getClass().getDeclaredField(fieldName); 
            executorField.setAccessible(true);
            ExecutorService executor = (ExecutorService)executorField.get(instance);
            executor.shutdownNow();
        }
        catch (Throwable ex) {
            log.error("Potential Memory leak: Unable to shutdown executor service from field '" + fieldName + "' in " + instance, ex);
        }
    }
    
    public String getDescriptor(String key) {
        return repository.getDescriptor(key);
    }

    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    public String getDefaultWorkspace() {
        return "default";
    }

    public Session login() throws LoginException, RepositoryException {
        return repository.login();
    }

    public Session login(Credentials credentials, String workspaceName) 
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return repository.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    public Session login(Credentials credentials) 
            throws LoginException, RepositoryException {
        return repository.login(credentials);
    }

    public Session login(String workspaceName) 
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return repository.login((workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    public Session loginAdministrative(String workspaceName) 
            throws RepositoryException {
        final Credentials credentials = new SimpleCredentials(ADMIN_NAME, ADMIN_PASSWORD.toCharArray());
        return this.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    @Override
    public Session loginService(String subServiceName, String workspaceName) 
            throws LoginException, RepositoryException {
        return loginAdministrative(workspaceName);
    }
    
    public Value getDescriptorValue(String key) {
        return repository.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return repository.getDescriptorValues(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return repository.isSingleValueDescriptor(key);
    }

    public boolean isStandardDescriptor(String key) {
        return repository.isStandardDescriptor(key);
    }

    @Override
    public Session impersonateFromService(String subServiceName, Credentials credentials, String workspaceName)
            throws LoginException, RepositoryException {
        return repository.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

}
