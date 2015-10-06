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
package org.apache.sling.ide.jcr;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.vault.davex.DAVExRepositoryFactory;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.RepositoryFactory;
import org.apache.sling.ide.impl.vlt.Activator;
import org.apache.sling.ide.transport.RepositoryInfo;

public abstract class RepositoryUtils {

    private static final Object SYNC = new Object();
    private static final String[] WEBDAV_URL_LOCATIONS = new String[] { "server/-/jcr:root", "crx/-/jcr:root" };
    private static final RepositoryFactory FACTORY = new DAVExRepositoryFactory();
    private static final Map<RepositoryAddress, Repository> REGISTERED_REPOSITORIES = new HashMap<>();
    
    public static Repository getRepository(RepositoryInfo repositoryInfo) throws RepositoryException {
        final RepositoryAddress repositoryAddress = getRepositoryAddress(repositoryInfo);
        synchronized (SYNC) {
            // will be populated implicitly by call to getRepositoryAddress
            return REGISTERED_REPOSITORIES.get(repositoryAddress);
        }
    }

    public static RepositoryAddress getRepositoryAddress(RepositoryInfo repositoryInfo) {
        StringBuilder errors = new StringBuilder();
        for (String webDavUrlLocation : WEBDAV_URL_LOCATIONS) {

            Session session = null;
            String url = repositoryInfo.appendPath(webDavUrlLocation);
            try {
                RepositoryAddress address = new RepositoryAddress(url);
                Repository repository;
                synchronized (SYNC) {
                    repository = REGISTERED_REPOSITORIES.get(address);

                    if (repository == null) {
                        Set<String> supportedSchemes = FACTORY.getSupportedSchemes();
                        if (!supportedSchemes.contains(address.getURI().getScheme())) {
                            throw new IllegalArgumentException("Unable to create a a repository for "
                                    + address.getURI()
                                    + ", since the scheme is unsupported. Only schemes '" + supportedSchemes
                                    + "' are supported");
                        }

                        // SLING-3739: ensure that a well-known ClassLoader is used
                        ClassLoader old = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(Repository.class.getClassLoader());
                        try {
                            repository = FACTORY.createRepository(address);
                        } finally {
                            Thread.currentThread().setContextClassLoader(old);
                        }
                        REGISTERED_REPOSITORIES.put(address, repository);
                    }
                }

                session = repository.login(new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo
                        .getPassword().toCharArray()));
                return address;
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (RepositoryException e) {
                Activator.getDefault().getPluginLogger().trace("Failed connecting to repository at " + url, e);
                errors.append(url).append(" : ").append(e.getMessage()).append('\n');
                continue;
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        errors.deleteCharAt(errors.length() - 1);

        throw new IllegalArgumentException("No repository found at " + repositoryInfo.getUrl() + "\n"
                + errors.toString());

    }

    public static Credentials getCredentials(RepositoryInfo repositoryInfo) {

        return new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword().toCharArray());
    }

    private RepositoryUtils() {

    }
}
