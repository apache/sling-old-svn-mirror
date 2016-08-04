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
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.SlingLaunchpadServer;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

public abstract class ServerUtil {
    
    //TODO: SLING-3587 - following constants are from wst.Server - an internal class
    // we should replace this with proper API - but afaik this information is not
    // accessible via any API ..
    private static final int AUTO_PUBLISH_DISABLE = 1;
    private static final int AUTO_PUBLISH_RESOURCE = 2;
    private static final int AUTO_PUBLISH_BUILD = 3;
    private static final String PROP_AUTO_PUBLISH_SETTING = "auto-publish-setting";
    private static final String PROP_AUTO_PUBLISH_TIME = "auto-publish-time";

    public static Repository getDefaultRepository(IProject project) {
        IServer server = getDefaultServer(project);
        if (server == null) {
            return null;
        }
        try {
            RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
            try {
                RepositoryInfo repositoryInfo = getRepositoryInfo(server, new NullProgressMonitor());
                return repository.getRepository(repositoryInfo, true);
            } catch (URISyntaxException | RuntimeException | RepositoryException e) {
                throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
            }
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting a repository for " + project, e);
            return null;
        }
    }

    private static IServer getDefaultServer(IProject project) {
        IModule module = org.eclipse.wst.server.core.ServerUtil.getModule(project);
        if (module==null) {
            // if there's no module for a project then there's no IServer for sure - which 
            // is what we need to create a RepositoryInfo
            return null;
        }
        IServer server = ServerCore.getDefaultServer(module);
        if (server!=null) {
            return server;
        }
        // then we cannot create a repository
        IServer[] allServers = ServerCore.getServers();
        out: for (int i = 0; i < allServers.length; i++) {
            IServer aServer = allServers[i];
            IModule[] allModules = aServer.getModules();
            for (int j = 0; j < allModules.length; j++) {
                IModule aMoudle = allModules[j];
                if (aMoudle.equals(module)) {
                    server = aServer;
                    break out;
                }
            }
        }
        return server;
    }

    private static Set<IServer> getAllServers(IProject project) {
        IModule module = org.eclipse.wst.server.core.ServerUtil.getModule(project);
        if (module==null) {
            // if there's no module for a project then there's no IServer for sure - which 
            // is what we need to create a RepositoryInfo
            return null;
        }
        Set<IServer> result = new HashSet<>();
        IServer defaultServer = ServerCore.getDefaultServer(module);
        if (defaultServer!=null) {
            result.add(defaultServer);
        }
        
        IServer[] allServers = ServerCore.getServers();
        for (int i = 0; i < allServers.length; i++) {
            IServer aServer = allServers[i];
            IModule[] allModules = aServer.getModules();
            for (int j = 0; j < allModules.length; j++) {
                IModule aMoudle = allModules[j];
                if (aMoudle.equals(module)) {
                    result.add(aServer);
                    break;
                }
            }
        }
        return result;
    }

    public static Repository getConnectedRepository(IServer server, IProgressMonitor monitor) throws CoreException {
        if (server==null) {
            throw new CoreException(new Status(Status.WARNING, Activator.PLUGIN_ID, "No server available/selected."));
        }
        if (server.getServerState()!=IServer.STATE_STARTED) {
            throw new CoreException(new Status(Status.WARNING, Activator.PLUGIN_ID, "Server not started, please start server first."));
        }
        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            return repository.getRepository(repositoryInfo, false);
        } catch (RuntimeException | URISyntaxException | RepositoryException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public static Repository connectRepository(IServer server, IProgressMonitor monitor) throws CoreException {
        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            return repository.connectRepository(repositoryInfo);
        } catch (RuntimeException | URISyntaxException | RepositoryException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public static void stopRepository(IServer server, IProgressMonitor monitor) throws CoreException {
        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            repository.disconnectRepository(repositoryInfo);
        } catch (RuntimeException | URISyntaxException e) {
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

    public static void triggerIncrementalBuild(IResource anyResourceInThatProject, IProgressMonitor monitorOrNull) {
        if (anyResourceInThatProject==null) {
            throw new IllegalArgumentException("anyResourceInThatProject must not be null");
        }
        IProject proj = anyResourceInThatProject.getProject();
        if (proj==null) {
            throw new IllegalStateException("no project found for "+anyResourceInThatProject);
        }
        Set<IServer> servers = getAllServers(proj);
        
        if (servers!=null) {
            if (monitorOrNull==null) {
                monitorOrNull = new NullProgressMonitor();
            }
            for (IServer server : servers) {
                if (server!=null) {
                    int autoPublishSetting = server.getAttribute(PROP_AUTO_PUBLISH_SETTING, AUTO_PUBLISH_RESOURCE);
                    int autoPublishTime = server.getAttribute(PROP_AUTO_PUBLISH_TIME, 15);
                    if (autoPublishSetting==AUTO_PUBLISH_RESOURCE) {
                        //TODO: ignoring the autoPublishTime - SLING-3587
                        server.publish(IServer.PUBLISH_INCREMENTAL, monitorOrNull);
                    }
                }
            }
        }
    }
    
    public static boolean runsOnLocalhost(IServerWorkingCopy server) {
        
        return "localhost".equals(server.getHost());
    }
}
