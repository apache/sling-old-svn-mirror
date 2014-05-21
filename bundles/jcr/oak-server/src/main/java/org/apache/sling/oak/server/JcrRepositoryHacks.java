/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.oak.server;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.observation.CommitRateLimiter;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;

class JcrRepositoryHacks extends RepositoryImpl {
    
    // TODO TCCL switching shouldn't be needed?
    // LoginModules are not found without this
    static abstract class LoginHelper {
        Session TCCLLogin() throws RepositoryException {
            final Thread thread = Thread.currentThread();
            final ClassLoader loader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(Oak.class.getClassLoader());
                return doLogin();
            } finally {
                thread.setContextClassLoader(loader);
            }
            
        }
        
        protected abstract Session doLogin() throws RepositoryException;
    };
    
	JcrRepositoryHacks(ContentRepository contentRepository, Whiteboard whiteboard, 
	        SecurityProvider securityProvider, int observationQueueLenght, CommitRateLimiter commitRateLimiter) {
		super(contentRepository, whiteboard, securityProvider, observationQueueLenght, commitRateLimiter);
	}
	
    @Override
    public Session login() throws RepositoryException {
        return new LoginHelper() {
            protected Session doLogin() throws RepositoryException {
                return JcrRepositoryHacks.super.login();
            }
        }.TCCLLogin();
    }

    @Override
    public Session login(final Credentials creds, final String workspace, final Map<String, Object> opt)
            throws RepositoryException {
        return new LoginHelper() {
            protected Session doLogin() throws RepositoryException {
                return JcrRepositoryHacks.super.login(creds, workspace, opt);
            }
        }.TCCLLogin();
    }

    @Override
    public Session login(final Credentials credentials) throws RepositoryException {
        return new LoginHelper() {
            protected Session doLogin() throws RepositoryException {
                return JcrRepositoryHacks.super.login(credentials);
            }
        }.TCCLLogin();
    }

    @Override
    public Session login(final String workspace) throws RepositoryException {
        return new LoginHelper() {
            protected Session doLogin() throws RepositoryException {
                return JcrRepositoryHacks.super.login(workspace);
            }
        }.TCCLLogin();
    }

    @Override
    public Session login(final Credentials credentials, final String workspace)
            throws RepositoryException {
        return new LoginHelper() {
            protected Session doLogin() throws RepositoryException {
                return JcrRepositoryHacks.super.login(credentials, workspace);
            }
        }.TCCLLogin();
    }
}