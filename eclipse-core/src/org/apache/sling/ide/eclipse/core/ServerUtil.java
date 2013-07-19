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
package org.apache.sling.ide.eclipse.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.sling.ide.eclipse.wst.internal.SlingLaunchpadServer;
import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.apache.sling.slingclipse.api.Repository;
import org.apache.sling.slingclipse.api.RepositoryInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;

public abstract class ServerUtil {

    public static Repository getRepository(IServer server, IProgressMonitor monitor) {

        ISlingLaunchpadServer launchpadServer = (ISlingLaunchpadServer) server.loadAdapter(SlingLaunchpadServer.class,
                monitor);

        ISlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();

        Repository repository = SlingclipsePlugin.getDefault().getRepository();
        try {
            // TODO configurable scheme?
            URI uri = new URI("http", null, server.getHost(), configuration.getPort(), configuration.getContextPath(),
                    null, null);
            RepositoryInfo repositoryInfo = new RepositoryInfo(configuration.getUsername(),
                    configuration.getPassword(), uri.toString());
            repository.setRepositoryInfo(repositoryInfo);
        } catch (URISyntaxException e) {
            // TODO handle error
        }
        return repository;
    }

    private ServerUtil() {

    }
}
