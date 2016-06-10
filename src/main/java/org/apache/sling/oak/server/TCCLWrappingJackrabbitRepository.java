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
package org.apache.sling.oak.server;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.oak.Oak;

/**
 * Custom <tt>JackrabbitRepository</tt> that ensures that the correct <tt>TCCL</tt> is set in OSGi environments
 * 
 * <p>Oak still requires that for {@link JackrabbitRepository#login()} and
 * {@link JackrabbitSession#impersonate(Credentials)} calls a custom thread context class loader is set. This wrapper
 * simply ensures that the TCCL is set for all calls.</p>
 */
public class TCCLWrappingJackrabbitRepository implements JackrabbitRepository {
    
    private final JackrabbitRepository wrapped;
    
    public TCCLWrappingJackrabbitRepository(JackrabbitRepository wrapped) {
        this.wrapped = wrapped;
    }
    
    // calls setting the TCCL

    @Override
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(credentials, workspaceName, null);
    }

    @Override
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return login(credentials, null, null);
    }

    @Override
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName, null);
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }

    @Override
    public Session login(Credentials credentials, String workspaceName, Map<String, Object> attributes)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        
        Thread thread = Thread.currentThread();

        ClassLoader oldClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(Oak.class.getClassLoader());

        try {
            Session session = wrapped.login(credentials, workspaceName,attributes);
            return new TCCLWrappingJackrabbitSession((JackrabbitSession) session);
        } finally {
            thread.setContextClassLoader(oldClassLoader);
        }        
    }
    
    // only pure delegate methods below

    @Override
    public String[] getDescriptorKeys() {
        return wrapped.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return wrapped.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return wrapped.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(String key) {
        return wrapped.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return wrapped.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor(String key) {
        return wrapped.getDescriptor(key);
    }

    @Override
    public void shutdown() {
        wrapped.shutdown();
        
    }

}
