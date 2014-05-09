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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.sling.ide.transport.RepositoryInfo;

public abstract class RepositoryUtils {

    private static final RepositoryProvider REPOSITORY_PROVIDER = new RepositoryProvider();
    private static final String[] WEBDAV_URL_LOCATIONS = new String[] { "server/-/jcr:root", "crx/-/jcr:root" };

    public static Repository getRepository(RepositoryInfo repositoryInfo) throws RepositoryException {

        return REPOSITORY_PROVIDER.getRepository(getRepositoryAddress(repositoryInfo));
    }

    public static RepositoryAddress getRepositoryAddress(RepositoryInfo repositoryInfo) {
        StringBuilder errors = new StringBuilder();
        for (String webDavUrlLocation : WEBDAV_URL_LOCATIONS) {
            Session session = null;
            try {
                // TODO proper error handling
                String url = repositoryInfo.getUrl() + webDavUrlLocation;
                RepositoryAddress address = new RepositoryAddress(url);
                Repository repository = REPOSITORY_PROVIDER.getRepository(address);
                // TODO - this can be costly performance-wise ; we should cache this information
                session = repository.login(new SimpleCredentials(repositoryInfo.getUsername(), repositoryInfo
                        .getPassword().toCharArray()));
                return address;
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (RepositoryException e) {
                errors.append(webDavUrlLocation).append(" : ").append(e.getMessage()).append('\n');
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
