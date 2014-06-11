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
import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.RepositoryFactory;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.sling.ide.transport.RepositoryInfo;

public abstract class RepositoryUtils {

    private static final String REPOSITORY_PROVIDER_NOT_YET_READY_MSG = "Repository provider not yet ready, please retry in a moment";
    private static final RepositoryProvider REPOSITORY_PROVIDER = new RepositoryProvider();
    private static final Object SYNC = new Object();
    private static final String[] WEBDAV_URL_LOCATIONS = new String[] { "server/-/jcr:root", "crx/-/jcr:root" };

    /**
     * Tries to figure out, if the repository provider is ready.
     * <p>
     * Also see SLING-3647.
     * <p>
     * This is heuristic at the moment: it assumes readiness as soon as *any*
     * RepositoryFactory is registered. Whether or not a repository with 
     * a particular address can be created, is then a second step.
     * <p>
     * @return
     */
    public static boolean isRepositoryProviderReady() {
        Iterator<RepositoryFactory> providerIt = ServiceRegistry.lookupProviders(RepositoryFactory.class);
        return providerIt.hasNext();
    }
    
    public static Repository getRepository(RepositoryInfo repositoryInfo) throws RepositoryException {
        final RepositoryAddress repositoryAddress = getRepositoryAddress(repositoryInfo);
        synchronized (SYNC) {
            try{
                return REPOSITORY_PROVIDER.getRepository(repositoryAddress);
            } catch(RepositoryException re) {
                if (isRepositoryProviderReady()) {
                    throw re;
                } else {
                    throw new RepositoryException(REPOSITORY_PROVIDER_NOT_YET_READY_MSG, re);
                }
            }
        }
    }

    public static RepositoryAddress getRepositoryAddress(RepositoryInfo repositoryInfo) {
        StringBuilder errors = new StringBuilder();
        for (String webDavUrlLocation : WEBDAV_URL_LOCATIONS) {

                Session session = null;
                String url = repositoryInfo.getUrl() + webDavUrlLocation;
                try {
                    // TODO proper error handling
                    RepositoryAddress address = new RepositoryAddress(url);
                    Repository repository;
                    synchronized (SYNC) {
                        repository = REPOSITORY_PROVIDER.getRepository(address);
                    }

                    // TODO - this can be costly performance-wise ; we should cache this information
                    session = repository.login(new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo
                            .getPassword().toCharArray()));
                    return address;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (RepositoryException e) {
                    e.printStackTrace();
                    errors.append(url).append(" : ").append(e.getMessage()).append('\n');
                    continue;
                } finally {
                    if (session != null) {
                        session.logout();
                    }
                }
        }

        errors.deleteCharAt(errors.length() - 1);

        IllegalArgumentException iae = new IllegalArgumentException("No repository found at " + repositoryInfo.getUrl() + "\n"
                + errors.toString());
        if (isRepositoryProviderReady()) {
            throw iae;
        } else {
            throw new IllegalArgumentException(REPOSITORY_PROVIDER_NOT_YET_READY_MSG, iae);
        }
    }

    public static Credentials getCredentials(RepositoryInfo repositoryInfo) {

        return new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword().toCharArray());
    }

    private RepositoryUtils() {

    }
}
