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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.MavenLaunchHelper;
import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.Repository;
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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegate {

    private SerializationManager serializationManager;
	private ILaunch launch;
	private JVMDebuggerConnection debuggerConnection;
	
    @Override
    public void stop(boolean force) {
    	if (debuggerConnection!=null) {
    		debuggerConnection.stop(force);
    	}
        setServerState(IServer.STATE_STOPPED);
    }

    public void start(IProgressMonitor monitor) throws CoreException {

        boolean success = false;
        Result<ResourceProxy> result = null;

        if (getServer().getMode().equals(ILaunchManager.DEBUG_MODE)) {
        	debuggerConnection = new JVMDebuggerConnection();
        	success = debuggerConnection.connectInDebugMode(launch, getServer(), monitor);
			
        } else {
	        
        	Command<ResourceProxy> command = ServerUtil.getRepository(getServer(), monitor).newListChildrenNodeCommand("/");
	        result = command.execute();
	        success = result.isSuccess();
        }

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

    	this.launch = launch;
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

        try {
            if (ProjectHelper.isBundleProject(module[0].getProject())) {
                String serverMode = getServer().getMode();
                if (!serverMode.equals(ILaunchManager.DEBUG_MODE)) {
                    // in debug mode, we rely on the hotcode replacement feature of eclipse/jvm
                    // otherwise, for run and profile modes we explicitly publish the bundle module
                    // TODO: make this configurable as part of the server config
            		publishBundleModule(module, monitor);
            	}
            } else if (ProjectHelper.isContentProject(module[0].getProject())) {
                try {
                    publishContentModule(kind, deltaKind, module, monitor);
                } catch (SerializationException e) {
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Serialization error for "
                            + trace.toString(), e));
                }
            }
        } finally {
            if (serializationManager != null) {
                serializationManager.destroy();
            }
        }
    }

	private void publishBundleModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
		final IProject project = module[0].getProject();
        boolean installLocally = getServer().getAttribute(ISlingLaunchpadServer.PROP_INSTALL_LOCALLY, true);
		if (!installLocally) {
			try{
				final String launchMemento = MavenLaunchHelper.createMavenLaunchConfigMemento(project.getLocation().toString(),
						"sling:install", "bundle", false, null);
				IFolder dotLaunches = project.getFolder(".settings").getFolder(".launches");
				if (!dotLaunches.exists()) {
					dotLaunches.create(true, true, monitor);
				}
				IFile launchFile = dotLaunches.getFile("sling_install.launch");
				InputStream in = new ByteArrayInputStream(launchMemento.getBytes());
				if (!launchFile.exists()) {
					launchFile.create(in, true, monitor);
				}

				ILaunchConfiguration launchConfig = 
						DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(launchFile);
				launchConfig.launch(ILaunchManager.RUN_MODE, monitor);
			} catch(Exception e) {
				// TODO proper logging
				e.printStackTrace();
			}
		} else {
			monitor.beginTask("deploying via local install", 5);
	        HttpClient httpClient = new HttpClient();
	        String hostname = getServer().getHost();
	        int launchpadPort = getServer().getAttribute(ISlingLaunchpadServer.PROP_PORT, 8080);
	        PostMethod method = new PostMethod("http://"+hostname+":"+launchpadPort+"/system/sling/tooling/install");
	        String username = getServer().getAttribute(ISlingLaunchpadServer.PROP_USERNAME, "admin");
	        String password = getServer().getAttribute(ISlingLaunchpadServer.PROP_PASSWORD, "admin");
	        String userInfo = username+":"+password;
	        if (userInfo != null) {
	        	Credentials c = new UsernamePasswordCredentials(userInfo);
	        	try {
					httpClient.getState().setCredentials(
							new AuthScope(method.getURI().getHost(), method
									.getURI().getPort()), c);
				} catch (URIException e) {
					// TODO proper logging
					e.printStackTrace();
				}
	        }
	        IJavaProject javaProject = ProjectHelper.asJavaProject(project);
	        IPath outputLocation = javaProject.getOutputLocation();
			outputLocation = outputLocation.makeRelativeTo(project.getFullPath());
	        IPath location = project.getRawLocation();
	        if (location==null) {
	        	location = project.getLocation();
	        }
			method.addParameter("dir", location.toString() + "/" + outputLocation.toString());
	        monitor.worked(1);
            try {
				httpClient.executeMethod(method);
		        monitor.worked(4);
		        setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
			} catch (HttpException e) {
				// TODO proper logging
				e.printStackTrace();
			} catch (IOException e) {
				// TODO proper logging
				e.printStackTrace();
			}
		}
	}

	private void publishContentModule(int kind, int deltaKind,
			IModule[] module, IProgressMonitor monitor) throws CoreException, SerializationException {

		if (runLaunchesIfExist(kind, deltaKind, module, monitor)) {
			return;
		}
		// otherwise fallback to old behaviour
		
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
        setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
//        setServerPublishState(IServer.PUBLISH_STATE_NONE);
	}

	private boolean runLaunchesIfExist(int kind, int deltaKind, IModule[] module,
			IProgressMonitor monitor) throws CoreException {
		final IProject project = module[0].getProject();
		final IFolder dotLaunches = project.getFolder(".settings").getFolder(".launches");
		final List<IFile> launches = new LinkedList<IFile>();
		if (dotLaunches.exists()) {
			final IResource[] members = dotLaunches.members();
			if (members!=null) {
				for (int i = 0; i < members.length; i++) {
					final IResource aMember = members[i];
					if (aMember instanceof IFile) {
						launches.add((IFile)aMember);
					}
				}
			}
		}
		if (launches.size()>0) {
			if (kind == IServer.PUBLISH_AUTO && deltaKind == ServerBehaviourDelegate.NO_CHANGE) {
				// then nothing is to be done, there are no changes
				return true;
			}
	        for (Iterator<IFile> it = launches.iterator(); it.hasNext();) {
				IFile aLaunchFile = it.next();
				try{
//					@SuppressWarnings("restriction")
//					IWorkbench workbench = DebugUIPlugin.getDefault().getWorkbench();
//					if (workbench==null) {
//						// we're not in the context of a workbench?
//						System.err.println("We're not in the context of a workbench?");
//					}
//					IWorkbenchWindow aw = workbench.getActiveWorkbenchWindow();
//					if (aw==null) {
//						// we're not in the context of a workbench window?
//					}
					ILaunchConfiguration launchConfig = 
							DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(aLaunchFile);
					if (launchConfig!=null) {
						DebugUITools.launch( launchConfig, ILaunchManager.RUN_MODE);
					}
				} catch(Exception e) {
					// TODO logging
					
					e.printStackTrace();
				}

			}
	        super.publishModule(kind, deltaKind, module, monitor);
	        setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
	        return true;
		}
		return false;
	}

    private void execute(Command<?> command) throws CoreException {
        if (command == null) {
            return;
        }
        Result<?> result = command.execute();

        if (!result.isSuccess()) // TODO proper logging
            throw new CoreException(new Status(Status.ERROR, "some.plugin", result.toString()));
    }

    private Command<?> addFileCommand(Repository repository, IModuleResource resource) throws CoreException,
            SerializationException {

        FileInfo info = createFileInfo(resource, repository);

        IResource res = getResource(resource);
        if (res == null) {
            return null;
        }

        System.out.println("For " + resource + " build fileInfo " + info);
        if (info == null) {
            return null;
        }

        File syncDirectoryAsFile = ProjectUtil.getSyncDirectoryFullPath(res.getProject()).toFile();

        if (serializationManager(repository, syncDirectoryAsFile).isSerializationFile(info.getLocation())) {

            // TODO - we don't support files with different names, see the docview file ( ui.xml ) pathological case
            if (!info.getName().equals(".content.xml")) {
                return null;
            }
            try {
                IFile file = (IFile) resource.getAdapter(IFile.class);
                InputStream contents = file.getContents();
                Map<String, Object> serializationData = serializationManager(repository, syncDirectoryAsFile)
                        .readSerializationData(contents);
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

    private FileInfo createFileInfo(IModuleResource resource, Repository repository) throws SerializationException {

        IResource file = getResource(resource);
        if (file == null) {
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
                    repository);
            if (filterResult == FilterResult.DENY) {
                return null;
            }
        }

        IPath relativePath = resource.getModuleRelativePath().removeLastSegments(1);

        FileInfo info = new FileInfo(file.getLocation().toOSString(), relativePath.toOSString(), file.getName());

        System.out.println("For " + resource + " built fileInfo " + info);

        return info;
    }

    private IResource getResource(IModuleResource resource) {

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

        return file;
    }

    private FilterResult getFilterResult(IModuleResource resource, Filter filter, File contentSyncRoot,
            Repository repository) throws SerializationException {

        String filePath = resource.getModuleRelativePath().toOSString();
        if (serializationManager(repository, contentSyncRoot).isSerializationFile(filePath)) {
            filePath = serializationManager.getBaseResourcePath(filePath);
        }

        System.out.println("Filtering by " + filePath + " for " + resource);

        return filter.filter(contentSyncRoot, filePath, repository.getRepositoryInfo());
    }

    private Command<?> removeFileCommand(Repository repository, IModuleResource resource) throws SerializationException {

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

    private SerializationManager serializationManager(Repository repository, File contentSyncRoot)
            throws SerializationException {
        if (serializationManager == null) {
            serializationManager = Activator.getDefault().getSerializationManager();
            serializationManager.init(repository, contentSyncRoot);
        }

        return serializationManager;
    }


}
