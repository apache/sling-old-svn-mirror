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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.transport.Batcher;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.osgi.framework.Version;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegateWithModulePublishSupport {

    private ResourceChangeCommandFactory commandFactory;
	private ILaunch launch;
	private JVMDebuggerConnection debuggerConnection;
	
    @Override
    public void stop(boolean force) {
    	if (debuggerConnection!=null) {
    		debuggerConnection.stop(force);
    	}
        setServerState(IServer.STATE_STOPPED);
        try {
            ServerUtil.stopRepository(getServer(), new NullProgressMonitor());
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().warn("Failed to stop repository", e);
        }
    }

    public void start(IProgressMonitor monitor) throws CoreException {

        boolean success = false;
        Result<ResourceProxy> result = null;
        monitor.beginTask("Starting server", 5);
        
        Repository repository;
        try {
            repository = ServerUtil.connectRepository(getServer(), monitor);
        } catch (CoreException e) {
            setServerState(IServer.STATE_STOPPED);
            throw e;
        }

        monitor.worked(2); // 2/5 done

        try {
            if (getServer().getMode().equals(ILaunchManager.DEBUG_MODE)) {
                debuggerConnection = new JVMDebuggerConnection();
                success = debuggerConnection.connectInDebugMode(launch, getServer(), monitor);

                monitor.worked(3); // 5/5 done

            } else {
                
                Command<ResourceProxy> command = repository.newListChildrenNodeCommand("/");
                result = command.execute();
                success = result.isSuccess();
                
                monitor.worked(1); // 3/5 done
                
                RepositoryInfo repositoryInfo;
                try {
                    repositoryInfo = ServerUtil.getRepositoryInfo(getServer(), monitor);
                    OsgiClient client = Activator.getDefault().getOsgiClientFactory().createOsgiClient(repositoryInfo);
                    EmbeddedArtifactLocator artifactLocator = Activator.getDefault().getArtifactLocator();
                    Version remoteVersion = client.getBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME);
                    
                    monitor.worked(1); // 4/5 done
                    
                    final EmbeddedArtifact supportBundle = artifactLocator.loadToolingSupportBundle();

                    final Version embeddedVersion = new Version(supportBundle.getVersion());
                    
                    ISlingLaunchpadServer launchpadServer = (ISlingLaunchpadServer) getServer().loadAdapter(SlingLaunchpadServer.class,
                            monitor);
                    if (remoteVersion == null || remoteVersion.compareTo(embeddedVersion) < 0) {
                        InputStream contents = null;
                        try {
                            contents = supportBundle.openInputStream();
                            client.installBundle(contents, supportBundle.getName());
                        } finally {
                            IOUtils.closeQuietly(contents);
                        }
                        remoteVersion = embeddedVersion;

                    }
                    launchpadServer.setBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME, remoteVersion,
                            monitor);
                    
                    monitor.worked(1); // 5/5 done
                    
                } catch ( IOException e) {
                    Activator.getDefault().getPluginLogger()
                        .warn("Failed reading the installation support bundle", e);
                } catch (URISyntaxException e) {
                    Activator.getDefault().getPluginLogger()
                            .warn("Failed retrieving information about the installation support bundle", e);
                } catch (OsgiClientException e) {
                    Activator.getDefault().getPluginLogger()
                            .warn("Failed retrieving information about the installation support bundle", e);
                }
            }

            if (success) {
                setServerState(IServer.STATE_STARTED);
            } else {
                setServerState(IServer.STATE_STOPPED);
                String message = "Unable to connect to the Server. Please make sure a server instance is running ";
                if (result != null) {
                    message += " (" + result.toString() + ")";
                }
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message));
            }
        } finally {
            monitor.done();
        }
    }

    // TODO refine signature
    public void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        // TODO check that ports are free

    	this.launch = launch;
        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);
    }

    @Override
    protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
            throws CoreException {

        Logger logger = Activator.getDefault().getPluginLogger();
        
        if (commandFactory == null) {
            commandFactory = new ResourceChangeCommandFactory(Activator.getDefault().getSerializationManager());
        }

        logger.trace(traceOperation(kind, deltaKind, module));

        if (getServer().getServerState() == IServer.STATE_STOPPED) {
            logger.trace("Ignoring request to publish module when the server is stopped");
            setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
            return;
        }

        if ((kind == IServer.PUBLISH_AUTO || kind == IServer.PUBLISH_INCREMENTAL)
                && deltaKind == ServerBehaviourDelegate.NO_CHANGE) {
            logger.trace("Ignoring request to publish the module when no resources have changed; most likely another module has changed");
            setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
            return;
        }
        
        if (kind == IServer.PUBLISH_FULL && deltaKind == ServerBehaviourDelegate.REMOVED) {
            logger.trace("Ignoring request to unpublish all of the module resources");
            setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
            return;
        }

        if (ProjectHelper.isBundleProject(module[0].getProject())) {
            String serverMode = getServer().getMode();
            if (!serverMode.equals(ILaunchManager.DEBUG_MODE) || kind==IServer.PUBLISH_CLEAN) {
                // in debug mode, we rely on the hotcode replacement feature of eclipse/jvm
                // otherwise, for run and profile modes we explicitly publish the bundle module
                
                // TODO: make this configurable as part of the server config
                
                // SLING-3655 : when doing PUBLISH_CLEAN, the bundle deployment mechanism should 
                // still be triggered
                publishBundleModule(module, monitor);
                BundleStateHelper.resetBundleState(getServer(), module[0].getProject());
            }
        } else if (ProjectHelper.isContentProject(module[0].getProject())) {

            try {
                publishContentModule(kind, deltaKind, module, monitor);
                BundleStateHelper.resetBundleState(getServer(), module[0].getProject());
            } catch (SerializationException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Serialization error for "
                        + traceOperation(kind, deltaKind, module).toString(), e));
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IO error for "
                        + traceOperation(kind, deltaKind, module).toString(), e));
            }
        }
    }

    private String traceOperation(int kind, int deltaKind, IModule[] module) {
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

        switch (getServer().getServerState()) {
            case IServer.STATE_STARTED:
                trace.append("STARTED, ");
                break;

            case IServer.STATE_STARTING:
                trace.append("STARTING, ");
                break;

            case IServer.STATE_STOPPED:
                trace.append("STOPPED, ");
                break;

            case IServer.STATE_STOPPING:
                trace.append("STOPPING, ");
                break;

            default:
                trace.append("UNKONWN - ").append(getServer().getServerState()).append(", ");
                break;
        }

        trace.append(Arrays.toString(module)).append(")");

        return trace.toString();
    }

	private void publishBundleModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
		final IProject project = module[0].getProject();
        boolean installLocally = getServer().getAttribute(ISlingLaunchpadServer.PROP_INSTALL_LOCALLY, true);
		monitor.beginTask("deploying via local install", 5);

        try {
            OsgiClient osgiClient = Activator.getDefault().getOsgiClientFactory()
                    .createOsgiClient(ServerUtil.getRepositoryInfo(getServer(), monitor));

            Version supportBundleVersion = osgiClient
                    .getBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME);
            monitor.worked(1);
            if (supportBundleVersion == null) {
                throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID,
                        "The support bundle was not found, please install it via the server properties page."));
            }

            IJavaProject javaProject = ProjectHelper.asJavaProject(project);

            IFolder outputFolder = (IFolder) project.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());
            IPath outputLocation = outputFolder.getLocation();
            //ensure the MANIFEST.MF exists - if it doesn't then let the publish fail with a warn (instead of an error)
            IResource manifest = outputFolder.findMember("META-INF/MANIFEST.MF");
            if (manifest==null) {
                Activator.getDefault().getPluginLogger().warn("Project "+project+" does not have a META-INF/MANIFEST.MF (yet) - not publishing this time");
                Activator.getDefault().issueConsoleLog("InstallBundle", outputFolder.getLocation().toOSString(), "Project "+project+" does not have a META-INF/MANIFEST.MF (yet) - not publishing this time");
                monitor.done();
                setModulePublishState(module, IServer.PUBLISH_STATE_FULL);
                return;
            }
            
            monitor.worked(1);

            //TODO SLING-3767:
            //osgiClient must have a timeout!!!
            if ( installLocally ) {
                osgiClient.installLocalBundle(outputLocation.toOSString());
                monitor.worked(3);
            } else {

                JarBuilder builder = new JarBuilder();
                InputStream bundle = builder.buildJar(outputFolder);
                monitor.worked(1);
                
                osgiClient.installLocalBundle(bundle, outputFolder.getLocation().toOSString());
                monitor.worked(2);
            }

            setModulePublishState(module, IServer.PUBLISH_STATE_NONE);

        } catch (URISyntaxException e1) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e1.getMessage(), e1));
        } catch (OsgiClientException e1) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed installing bundle : "
                    + e1.getMessage(), e1));
        } finally {
            monitor.done();
        }
	}

    private void publishContentModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
            throws CoreException, SerializationException, IOException {

        Logger logger = Activator.getDefault().getPluginLogger();

		Repository repository = ServerUtil.getConnectedRepository(getServer(), monitor);
        if (repository == null) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Unable to find a repository for server " + getServer()));
        }
        
        Batcher batcher = Activator.getDefault().getBatcherFactory().createBatcher();
        
        // TODO it would be more efficient to have a module -> filter mapping
        // it would be simpler to implement this in SlingContentModuleAdapter, but
        // the behaviour for resources being filtered out is deletion, and that
        // would be an incorrect ( or at least suprising ) behaviour at development time

        List<IModuleResource> addedOrUpdatedResources = new ArrayList<IModuleResource>();
        IModuleResource[] allResources = getResources(module);
        Set<IPath> handledPaths = new HashSet<IPath>();

        switch (deltaKind) {
            case ServerBehaviourDelegate.CHANGED:
                for (IModuleResourceDelta resourceDelta : getPublishedResourceDelta(module)) {

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

                    logger.trace(deltaTrace.toString());

                    switch (resourceDelta.getKind()) {
                        case IModuleResourceDelta.ADDED:
                        case IModuleResourceDelta.CHANGED:
                        case IModuleResourceDelta.NO_CHANGE: // TODO is this needed?
                            Command<?> command = addFileCommand(repository, resourceDelta.getModuleResource());

                            if (command != null) {
                                ensureParentIsPublished(resourceDelta.getModuleResource(), repository, allResources,
                                        handledPaths, batcher);
                                addedOrUpdatedResources.add(resourceDelta.getModuleResource());
                            }
                            enqueue(batcher, command);
                            break;
                        case IModuleResourceDelta.REMOVED:
                            enqueue(batcher, removeFileCommand(repository, resourceDelta.getModuleResource()));
                            break;
                    }
                }
                break;

            case ServerBehaviourDelegate.ADDED:
            case ServerBehaviourDelegate.NO_CHANGE: // TODO is this correct ?
                for (IModuleResource resource : getResources(module)) {
                    Command<?> command = addFileCommand(repository, resource);
                    enqueue(batcher, command);
                    if (command != null) {
                        addedOrUpdatedResources.add(resource);
                    }
                }
                break;
            case ServerBehaviourDelegate.REMOVED:
                for (IModuleResource resource : getResources(module)) {
                    enqueue(batcher, removeFileCommand(repository, resource));
                }
                break;
        }

        // reorder the child nodes at the end, when all create/update/deletes have been processed
        for (IModuleResource resource : addedOrUpdatedResources) {
            enqueue(batcher, reorderChildNodesCommand(repository, resource));
        }

        execute(batcher);

        // set state to published
        super.publishModule(kind, deltaKind, module, monitor);
        setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
//        setServerPublishState(IServer.PUBLISH_STATE_NONE);
	}

    private void execute(Batcher batcher) throws CoreException {
        for ( Command<?> command : batcher.get()) {
            Result<?> result = command.execute();
    
            if (!result.isSuccess()) {
                // TODO - proper error logging
                throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed publishing path="
                        + command.getPath() + ", result=" + result.toString()));
            }
        }
    }

    /**
     * Ensures that the parent of this resource has been published to the repository
     * 
     * <p>
     * Note that the parents explicitly do not have their child nodes reordered, this will happen when they are
     * published due to a resource change
     * </p>
     * 
     * @param moduleResource the current resource
     * @param repository the repository to publish to
     * @param allResources all of the module's resources
     * @param handledPaths the paths that have been handled already in this publish operation, but possibly not
     *            registered as published
     * @param batcher 
     * @throws IOException
     * @throws SerializationException
     * @throws CoreException
     */
    private void ensureParentIsPublished(IModuleResource moduleResource, Repository repository,
            IModuleResource[] allResources, Set<IPath> handledPaths, Batcher batcher)
            throws CoreException, SerializationException, IOException {

        Logger logger = Activator.getDefault().getPluginLogger();

        IPath currentPath = moduleResource.getModuleRelativePath();

        logger.trace("Ensuring that parent of path {0} is published", currentPath);

        // we assume the root is always published
        if (currentPath.segmentCount() == 0) {
            logger.trace("Path {0} can not have a parent, skipping", currentPath);
            return;
        }

        IPath parentPath = currentPath.removeLastSegments(1);

        // already published by us, a parent of another resource that was published in this execution
        if (handledPaths.contains(parentPath)) {
            logger.trace("Parent path {0} was already handled, skipping", parentPath);
            return;
        }

        for (IModuleResource maybeParent : allResources) {
            if (maybeParent.getModuleRelativePath().equals(parentPath)) {
                // handle the parent's parent first, if needed
                ensureParentIsPublished(maybeParent, repository, allResources, handledPaths, batcher);
                // create this resource
                enqueue(batcher, addFileCommand(repository, maybeParent));
                handledPaths.add(maybeParent.getModuleRelativePath());
                logger.trace("Ensured that resource at path {0} is published", parentPath);
                return;
            }
        }

        throw new IllegalArgumentException("Resource at " + moduleResource.getModuleRelativePath()
                + " has parent path " + parentPath + " but no resource with that path is in the module's resources.");

    }

    private void enqueue(Batcher batcher, Command<?> command)  {
        if (command == null) {
            return;
        }
        
        batcher.add(command);
    }

    private Command<?> addFileCommand(Repository repository, IModuleResource resource) throws CoreException,
            SerializationException, IOException {

        IResource res = getResource(resource);

        if (res == null) {
            return null;
        }

        return commandFactory.newCommandForAddedOrUpdated(repository, res);
    }

    private Command<?> reorderChildNodesCommand(Repository repository, IModuleResource resource) throws CoreException,
            SerializationException, IOException {

        IResource res = getResource(resource);

        if (res == null) {
            return null;
        }

        return commandFactory.newReorderChildNodesCommand(repository, res);
    }

    private IResource getResource(IModuleResource resource) {

        IResource file = (IFile) resource.getAdapter(IFile.class);
        if (file == null) {
            file = (IFolder) resource.getAdapter(IFolder.class);
        }

        if (file == null) {
            // Usually happens on server startup, it seems to be safe to ignore for now
            Activator.getDefault().getPluginLogger()
                    .trace("Got null {0} and {1} for {2}", IFile.class.getSimpleName(),
                            IFolder.class.getSimpleName(), resource);
            return null;
        }

        return file;
    }

    private Command<?> removeFileCommand(Repository repository, IModuleResource resource)
            throws SerializationException, IOException, CoreException {

        IResource deletedResource = getResource(resource);

        if (deletedResource == null) {
            return null;
        }

        return commandFactory.newCommandForRemovedResources(repository, deletedResource);
    }
}
