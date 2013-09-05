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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegate {

    private SerializationManager serializationManager;

    @Override
    public void stop(boolean force) {

        setServerState(IServer.STATE_STOPPED);
    }

    public void start(IProgressMonitor monitor) throws CoreException {

        boolean success = false;

        Command<ResourceProxy> command = ServerUtil.getRepository(getServer(), monitor).newListChildrenNodeCommand("/");
        Result<ResourceProxy> result = command.execute();
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

    // TODO refine signature
    public void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
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

        Repository repository = ServerUtil.getRepository(getServer(), monitor);

        IModuleResource[] moduleResources = getResources(module);
        
        // TODO it would be more efficient to have a module -> filter mapping
        // it would be simpler to implement this in SlingContentModuleAdapter, but
        // the behaviour for resources being filtered out is deletion, and that
        // would be an incorrect ( or at least suprising ) behaviour at development time

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

    private void execute(Command<?> command) throws CoreException {
        if (command == null) {
            return;
        }
        Result<?> result = command.execute();

        if (!result.isSuccess()) // TODO proper logging
            throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
    }

    private Command<?> addFileCommand(Repository repository, IModuleResource resource) throws CoreException {

        FileInfo info = createFileInfo(resource, repository);

        System.out.println("For " + resource + " build fileInfo " + info);
        if (info == null) {
            return null;
        }

        if (serializationManager().isSerializationFile(info.getLocation())) {
            try {
                IFile file = (IFile) resource.getAdapter(IFile.class);
                InputStream contents = file.getContents();
                Map<String, Object> serializationData = serializationManager().readSerializationData(contents);
                return repository.newUpdateContentNodeCommand(info, serializationData);
            } catch (IOException e) {
                // TODO logging
                e.printStackTrace();
                return null;
            }
        } else {
            return repository.newAddNodeCommand(info);
        }
    }

    private FileInfo createFileInfo(IModuleResource resource, Repository repository) {

        IResource file = (IFile) resource.getAdapter(IFile.class);
        if (file == null) {
            file = (IFolder) resource.getAdapter(IFolder.class);
        }

        if (file == null) {
            // Usually happens on server startup, it seems to be safe to ignore for now
            System.out.println("Got null '" + IFile.class.getSimpleName() + "' and '" + IFolder.class.getSimpleName()
                    + "' for " + resource);
            return null;
        }

        IProject project = file.getProject();

        String syncDirectory = ProjectUtil.getSyncDirectoryValue(project);
        File syncDirectoryAsFile = ProjectUtil.getSyncDirectoryFullPath(project).toFile();

        Filter filter = null;
        try {
            filter = loadFilter(project, project.getFolder(syncDirectory));
        } catch (CoreException e) {
            // TODO error handling
            e.printStackTrace();
        }

        if (filter != null) {
            FilterResult filterResult = getFilterResult(resource, filter, syncDirectoryAsFile,
                    repository.getRepositoryInfo());
            if (filterResult == FilterResult.DENY) {
                return null;
            }
        }

        IPath relativePath = resource.getModuleRelativePath().removeLastSegments(1);

        FileInfo info = new FileInfo(file.getLocation().toOSString(), relativePath.toOSString(), file.getName());

        System.out.println("For " + resource + " built fileInfo " + info);

        return info;
    }

    private FilterResult getFilterResult(IModuleResource resource, Filter filter, File contentSyncRoot,
            RepositoryInfo repositoryInfo) {

        String filePath = resource.getModuleRelativePath().toOSString();
        if (serializationManager().isSerializationFile(filePath)) {
            filePath = serializationManager.getBaseResourcePath(filePath);
        }

        System.out.println("Filtering by " + filePath + " for " + resource);

        return filter.filter(contentSyncRoot, filePath, repositoryInfo);
    }

    private Command<?> removeFileCommand(Repository repository, IModuleResource resource) {

        FileInfo info = createFileInfo(resource, repository);

        if (info == null) {
            return null;
        }

        return repository.newDeleteNodeCommand(info);
    }

    private Filter loadFilter(IProject project, final IFolder syncFolder) throws CoreException {
        FilterLocator filterLocator = Activator.getDefault().getFilterLocator();
        File filterLocation = filterLocator.findFilterLocation(syncFolder.getLocation().toFile());
        if (filterLocation == null) {
            return null;
        }
        IPath filterPath = Path.fromOSString(filterLocation.getAbsolutePath());
        IFile filterFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filterPath);
        Filter filter = null;
        if (filterFile != null && filterFile.exists()) {
            InputStream contents = filterFile.getContents();
            try {
                filter = filterLocator.loadFilter(contents);
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Failed loading filter file for project " + project.getName() + " from location " + filterFile,
                        e));
            } finally {
                try {
                    contents.close();
                } catch (IOException e) {
                    // TODO exception handling
                }
            }
        }
        return filter;
    }

    private SerializationManager serializationManager() {
        if (serializationManager == null) {
            serializationManager = Activator.getDefault().getSerializationManager();
        }

        return serializationManager;
    }


}
