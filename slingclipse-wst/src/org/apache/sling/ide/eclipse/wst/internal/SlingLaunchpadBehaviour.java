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
package org.apache.sling.ide.eclipse.wst.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.apache.sling.slingclipse.api.Command;
import org.apache.sling.slingclipse.api.FileInfo;
import org.apache.sling.slingclipse.api.Repository;
import org.apache.sling.slingclipse.api.RepositoryInfo;
import org.apache.sling.slingclipse.api.ResponseType;
import org.apache.sling.slingclipse.api.Result;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegate {

    @Override
    public void stop(boolean force) {

        setServerState(IServer.STATE_STOPPED);
    }

    public void start(IProgressMonitor monitor) throws CoreException {

        boolean success = false;

        Result<String> result = null;
        Command<String> command = getRepository(monitor).newListChildrenNodeCommand("/", ResponseType.XML);
        result = command.execute();
        success = result.isSuccess();

        if (success) {
            setServerState(IServer.STATE_STARTED);
        } else {
            setServerState(IServer.STATE_STOPPED);
            String message = "Unable to connect to Sling Lanchpad. Please make sure a Launchpad instance is running ";
            if (result != null) {
                message += " (" + result.toString() + ")";
            }
            throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.ide.eclipse.wst",
                    message));
        }
    }

    // TODO refine signature, visibility
    protected void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        // TODO check that ports are free

        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);
    }

    @Override
    protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
            throws CoreException {

        StringBuilder trace = new StringBuilder();
        trace.append("SlingLaunchpadBehaviour.publishModule(");

        switch (kind) {
            case IServer.PUBLISH_CLEAN:
                trace.append("PUBLISH_CLEAN, ");
                break;
            case IServer.PUBLISH_INCREMENTAL:
                trace.append("PUBLISH_INCREMENTAL, ");
                break;
            case IServer.PUBLISH_AUTO:
                trace.append("PUBLISH_AUTO, ");
                break;
            case IServer.PUBLISH_FULL:
                trace.append("PUBLISH_FULL, ");
                break;
            default:
                trace.append("UNKNOWN - ").append(kind).append(", ");
        }

        switch (deltaKind) {
            case ServerBehaviourDelegate.ADDED:
                trace.append("ADDED, ");
                break;
            case ServerBehaviourDelegate.CHANGED:
                trace.append("CHANGED, ");
                break;
            case ServerBehaviourDelegate.NO_CHANGE:
                trace.append("NO_CHANGE, ");
                break;
            case ServerBehaviourDelegate.REMOVED:
                trace.append("REMOVED, ");
                break;
            default:
                trace.append("UNKONWN - ").append(deltaKind).append(", ");
                break;
        }
        
        trace.append(Arrays.toString(module)).append(")");

        System.out.println(trace.toString());

        Repository repository = getRepository(monitor);

        IModuleResource[] moduleResources = getResources(module);

        switch (deltaKind) {
            case ServerBehaviourDelegate.CHANGED:
                IModuleResourceDelta[] publishedResourceDelta = getPublishedResourceDelta(module);
                for (IModuleResourceDelta resourceDelta : publishedResourceDelta) {

                    StringBuilder deltaTrace = new StringBuilder();
                    deltaTrace.append("- processing delta kind ");

                    switch (resourceDelta.getKind()) {
                        case IModuleResourceDelta.ADDED:
                            deltaTrace.append("ADDED ");
                            break;
                        case IModuleResourceDelta.CHANGED:
                            deltaTrace.append("CHANGED ");
                            break;
                        case IModuleResourceDelta.NO_CHANGE:
                            deltaTrace.append("NO_CHANGE ");
                            break;
                        case IModuleResourceDelta.REMOVED:
                            deltaTrace.append("REMOVED ");
                            break;
                        default:
                            deltaTrace.append("UNKNOWN - ").append(resourceDelta.getKind());
                    }

                    deltaTrace.append("for resource ").append(resourceDelta.getModuleResource());

                    System.out.println(deltaTrace);

                    switch (resourceDelta.getKind()) {
                        case IModuleResourceDelta.ADDED:
                        case IModuleResourceDelta.CHANGED:
                        case IModuleResourceDelta.NO_CHANGE: // TODO is this needed?
                            execute(addFileCommand(repository, resourceDelta.getModuleResource()));
                            break;
                        case IModuleResourceDelta.REMOVED:
                            execute(removeFileCommand(repository, resourceDelta.getModuleResource()));
                            break;
                    }
                }
                break;

            case ServerBehaviourDelegate.ADDED:
            case ServerBehaviourDelegate.NO_CHANGE: // TODO is this correct ?
                for (IModuleResource resource : moduleResources) {
                    execute(addFileCommand(repository, resource));
                }
                break;
            case ServerBehaviourDelegate.REMOVED:
                for (IModuleResource resource : moduleResources) {
                    execute(removeFileCommand(repository, resource));
                }
                break;
        }


        // set state to published
        super.publishModule(kind, deltaKind, module, monitor);
    }

    private Repository getRepository(IProgressMonitor monitor) {

        SlingLaunchpadServer launchpadServer = (SlingLaunchpadServer) getServer().loadAdapter(
                SlingLaunchpadServer.class, monitor);

        SlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();

        Repository repository = SlingclipsePlugin.getDefault().getRepository();
        try {
            // TODO configurable scheme?
            URI uri = new URI("http", null, getServer().getHost(), configuration.getPort(),
                    configuration.getContextPath(), null, null);
            RepositoryInfo repositoryInfo = new RepositoryInfo(configuration.getUsername(),
                    configuration.getPassword(), uri.toString());
            repository.setRepositoryInfo(repositoryInfo);
        } catch (URISyntaxException e) {
            // TODO handle error
        }
        return repository;
    }

    private void execute(Command<?> command) throws CoreException {
        if (command == null) {
            return;
        }
        Result<?> result = command.execute();

        System.out.println("COMMAND  : " + command + " -> " + result);

        if (!result.isSuccess()) // TODO proper logging
            throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
    }

    private Command<?> addFileCommand(Repository repository, IModuleResource resource) {

        FileInfo info = createFileInfo(resource);

        System.out.println("For " + resource + " build fileInfo " + info);
        if (info == null) {
            return null;
        }

        return repository.newAddNodeCommand(info);
    }

    private FileInfo createFileInfo(IModuleResource resource) {

        IResource file = (IFile) resource.getAdapter(IFile.class);
        if (file == null) {
            file = (IFolder) resource.getAdapter(IFolder.class);
        }

        if (file == null) {
            // Usually happens on server startup, it seems to be safe to ignore for now
            System.out.println("Got null '" + IFile.class.getSimpleName() + "' and '" + IFolder.class.getSimpleName()
                    + "'for " + resource);
            return null;
        }

        IPath rootPath = resource.getModuleRelativePath().removeLastSegments(1); // TODO correct name

        String relativePath = rootPath.toOSString();

        FileInfo info = new FileInfo(file.getLocation().toOSString(), relativePath, file.getName());

        System.out.println("For " + resource + " built fileInfo " + info);

        return info;
    }

    private Command<?> removeFileCommand(Repository repository, IModuleResource resource) {

        FileInfo info = createFileInfo(resource);

        if (info == null) {
            return null;
        }

        return repository.newDeleteNodeCommand(info);
    }
}
