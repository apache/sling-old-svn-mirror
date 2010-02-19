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
package org.apache.sling.event.impl;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.jcr.api.SlingRepository;

public class RepositoryTestUtil {

    /** We hold an admin session the whole time. */
    private static Session adminSession;

    private static Repository repository;

    private static void init() throws RepositoryException {
        if ( repository == null ) {
            final File f = new File("target/repository");
            repository = JcrUtils.getRepository(f.toURI().toString());

            final SimpleCredentials cred = new SimpleCredentials("admin", "admin".toCharArray());
            adminSession = repository.login(cred);
        }
    }

    public static void startRepository() throws RepositoryException {
        init();
    }

    public static void stopRepository() {
        if ( adminSession != null ) {
            adminSession.logout();
            adminSession = null;
        }
        repository = null;
    }

    public static Session getAdminSession() {
        return adminSession;
    }

    public static SlingRepository getSlingRepository() {
        return new RepositoryWrapper(repository);
    }

    public static boolean registerNodeType(Session session, InputStream resourceAsStream) {
        try {
            CndImporter.registerNodeTypes(new InputStreamReader(resourceAsStream, "UTF-8"), session);
            return true;
        } catch (Exception e) {
            // ignore
            return false;
        }
    }

    public static final class RepositoryWrapper implements SlingRepository {

        protected final Repository wrapped;

        public RepositoryWrapper(Repository r) {
            wrapped = r;
        }

        public String getDescriptor(String key) {
            return wrapped.getDescriptor(key);
        }

        public String[] getDescriptorKeys() {
            return wrapped.getDescriptorKeys();
        }

        public Session login() throws LoginException, RepositoryException {
            return wrapped.login();
        }

        public Session login(Credentials credentials, String workspaceName)
                throws LoginException, NoSuchWorkspaceException,
                RepositoryException {
            return wrapped.login(credentials, workspaceName);
        }

        public Session login(Credentials credentials) throws LoginException,
                RepositoryException {
            return wrapped.login(credentials);
        }

        public Session login(String workspaceName) throws LoginException,
                NoSuchWorkspaceException, RepositoryException {
            return wrapped.login(workspaceName);
        }

        public String getDefaultWorkspace() {
            return "default";
        }

        public Session loginAdministrative(String workspace)
                throws RepositoryException {
            final Credentials credentials = new SimpleCredentials("admin",
                    "admin".toCharArray());
            return this.login(credentials, workspace);
        }

        public Value getDescriptorValue(String key) {
            return wrapped.getDescriptorValue(key);
        }

        public Value[] getDescriptorValues(String key) {
            return wrapped.getDescriptorValues(key);
        }

        public boolean isSingleValueDescriptor(String key) {
            return wrapped.isSingleValueDescriptor(key);
        }

        public boolean isStandardDescriptor(String key) {
            return wrapped.isStandardDescriptor(key);
        }
    }
}