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
import org.apache.sling.slingclipse.api.Result;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegate {

    @Override
    public void stop(boolean force) {
        // TODO stub
        setServerState(IServer.STATE_STOPPED);
    }

    public void start() {
        // TODO stub
        setServerState(IServer.STATE_STARTED);
    }

    // TODO refine signature, visibility
    protected void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        // TODO check that ports are free

        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);
    }

    @Override
    public IStatus canPublish() {
        IStatus canPublish = super.canPublish();
        System.out.println("SlingLaunchpadBehaviour.canPublish() is " + canPublish);
        return canPublish;
    }

    @Override
    public boolean canPublishModule(IModule[] module) {
        boolean result = super.canPublishModule(module);
        System.out.println("SlingLaunchpadBehaviour.canPublishModule() is " + result);
        return result;
    }

    @Override
    protected void publishServer(int kind, IProgressMonitor monitor) throws CoreException {
        System.out.println("SlingLaunchpadBehaviour.publishServer()");
        super.publishServer(kind, monitor);
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

        SlingLaunchpadServer launchpadServer = (SlingLaunchpadServer) getServer().loadAdapter(
                SlingLaunchpadServer.class, monitor);
        SlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();

        IModuleResource[] moduleResources = getResources(module);

        Repository repository = SlingclipsePlugin.getDefault().getRepository();
        try {
            // TODO configurable scheme?
            URI uri = new URI("http", null, getServer().getHost(), configuration.getPort(),
                    configuration.getContextPath(), null, null);
            RepositoryInfo repositoryInfo = new RepositoryInfo(configuration.getUsername(),
                    configuration.getPassword(), uri.toString());
            repository.setRepositoryInfo(repositoryInfo);
            System.out.println("RepositoryInfo=" + repository);
        } catch (URISyntaxException e) {
            // TODO handle error
        }

        switch (deltaKind) {
            case ServerBehaviourDelegate.CHANGED:
                IModuleResourceDelta[] publishedResourceDelta = getPublishedResourceDelta(module);
                for (IModuleResourceDelta resourceDelta : publishedResourceDelta) {
                    if (resourceDelta.getModuleResource() instanceof IModuleFile) {
                        switch (resourceDelta.getKind()) {
                            case IModuleResourceDelta.ADDED:
                            case IModuleResourceDelta.CHANGED:
                            case IModuleResourceDelta.NO_CHANGE: // TODO is this needed?
                                Result<?> result = addFileCommand(repository,
                                        (IModuleFile) resourceDelta.getModuleResource()).execute();
                                if (!result.isSuccess()) // TODO proper logging
                                    throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
                                break;
                            case IModuleResourceDelta.REMOVED:
                                Result<?> deleteResult = removeFileCommand(repository,
                                        (IModuleFile) resourceDelta.getModuleResource()).execute();
                                if (!deleteResult.isSuccess()) // TODO proper logging
                                    throw new CoreException(new Status(Status.ERROR, "some.plugin",
                                            deleteResult.toString()));
                                break;
                        }
                    }
                }
                break;

            case ServerBehaviourDelegate.ADDED:
            case ServerBehaviourDelegate.NO_CHANGE: // TODO is this correct ?
                for (IModuleResource resource : moduleResources) {

                    if (resource instanceof IModuleFile) {
                        Result<?> result = addFileCommand(repository, (IModuleFile) resource).execute();
                        if (!result.isSuccess()) // TODO proper logging
                            throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
                    } else {
                        // TODO log/barf
                    }
                }
                break;
            case ServerBehaviourDelegate.REMOVED:
                for (IModuleResource resource : moduleResources) {

                    if (resource instanceof IModuleFile) {
                        Result<?> result = removeFileCommand(repository, (IModuleFile) resource).execute();
                        if (!result.isSuccess()) // TODO proper logging
                            throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
                    } else {
                        // TODO log/barf
                    }
                }
                break;
        }


        // set state to published
        super.publishModule(kind, deltaKind, module, monitor);
    }

    private Command<?> addFileCommand(Repository repository, IModuleFile resource) {
        IFile file = (IFile) resource.getAdapter(IFile.class);

        IPath projectPath = file.getProject().getFullPath();
        IPath filePath = file.getFullPath();
        IPath relativePath = filePath.makeRelativeTo(projectPath);
        IPath rootPath = relativePath.removeLastSegments(1); // TODO correct name

        FileInfo info = new FileInfo(file.getLocation().toOSString(), rootPath.toOSString(), file.getName());

        System.out.println("For " + resource + " build fileInfo " + info);

        return repository.newAddNodeCommand(info);
    }

    private Command<?> removeFileCommand(Repository repository, IModuleFile resource) {
        IFile file = (IFile) resource.getAdapter(IFile.class);

        IPath projectPath = file.getProject().getFullPath();
        IPath filePath = file.getFullPath();
        IPath relativePath = filePath.makeRelativeTo(projectPath);
        IPath rootPath = relativePath.removeLastSegments(1); // TODO correct name

        FileInfo info = new FileInfo(file.getLocation().toOSString(), rootPath.toOSString(), file.getName());

        System.out.println("For " + resource + " build fileInfo " + info);

        return repository.newDeleteNodeCommand(info);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.core.model.ServerBehaviourDelegate#setupLaunchConfiguration(org.eclipse.debug.core.
     * ILaunchConfigurationWorkingCopy, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
            throws CoreException {
        System.out.println("SlingLaunchpadBehaviour.setupLaunchConfiguration()");
        super.setupLaunchConfiguration(workingCopy, monitor);
    }

}
