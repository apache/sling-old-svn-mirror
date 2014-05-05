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

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.SlingLaunchpadServer;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;

public abstract class ServerUtil {

    public static Repository getRepository(IServer server, IProgressMonitor monitor) throws CoreException {


        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            return repository.newRepository(repositoryInfo);
        } catch (URISyntaxException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        } catch (RuntimeException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        } catch (RepositoryException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public static RepositoryInfo getRepositoryInfo(IServer server, IProgressMonitor monitor) throws URISyntaxException {

        ISlingLaunchpadServer launchpadServer = (ISlingLaunchpadServer) server.loadAdapter(SlingLaunchpadServer.class,
                monitor);

        ISlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();

        // TODO configurable scheme?
        URI uri = new URI("http", null, server.getHost(), configuration.getPort(), configuration.getContextPath(),
                null, null);
        return new RepositoryInfo(configuration.getUsername(),
                configuration.getPassword(), uri.toString());
    }

    private ServerUtil() {

    }
}
