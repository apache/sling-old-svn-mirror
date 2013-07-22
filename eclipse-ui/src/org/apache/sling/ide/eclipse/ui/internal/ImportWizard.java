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
package org.apache.sling.ide.eclipse.ui.internal;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResponseType;
import org.apache.sling.ide.transport.Result;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServer;
import org.json.JSONException;
import org.json.JSONML;
import org.json.JSONObject;

/**
 * Renders the import wizard container page for the Slingclipse repository
 * import.
 */
public class ImportWizard extends Wizard implements IImportWizard {
	private ImportWizardPage mainPage;
    private SerializationManager serializationManager;

	/**
	 * Construct a new Import Wizard container instance.
	 */
	public ImportWizard() {
		super();
        Activator activator = Activator.getDefault();
        serializationManager = activator.getSerializationManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {

        if (!mainPage.isPageComplete()) {
            return false;
        }
		

            final IServer server = mainPage.getServer();
	 
			IPath destinationPath = mainPage.getResourcePath();
			
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(destinationPath.segments()[0]);
			final IPath projectRelativePath = destinationPath.removeFirstSegments(1);
			final String repositoryPath = mainPage.getRepositoryPath();
			
        try {
            getContainer().run(false, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    Repository repository = ServerUtil.getRepository(server, monitor);

                    monitor.setTaskName("Loading configuration...");
                    monitor.worked(5);
                    ISlingLaunchpadServer launchpad = (ISlingLaunchpadServer) server.loadAdapter(
                            ISlingLaunchpadServer.class, monitor);

                    int oldPublishState = launchpad.getPublishState();
                    // TODO disabling publish does not work; since the publish is done async
                    // Not sure if there is a simple workaround. Anyway, the only side effect is that we
                    // make too many calls after the import, functionality is not affected
                    if (server.canPublish().isOK() && oldPublishState != ISlingLaunchpadServer.PUBLISH_STATE_NEVER) {
                        launchpad.setPublishState(ISlingLaunchpadServer.PUBLISH_STATE_NEVER);
                    }

                    try {

                        // TODO: We should try to make this give 'nice' progress feedback (aka here's what I'm
                        // processing)
                        monitor.setTaskName("Importing...");
                        monitor.worked(10);

                        // we create the root node and assume this is a folder
                        createRoot(project, projectRelativePath, repositoryPath);

                        crawlChildrenAndImport(repository, repositoryPath, project, projectRelativePath);

                        monitor.setTaskName("Import Complete");
                        monitor.worked(100);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        if (oldPublishState != ISlingLaunchpadServer.PUBLISH_STATE_NEVER) {
                            launchpad.setPublishState(oldPublishState);
                        }
                        monitor.done();
                        }

                }

            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            mainPage.setErrorMessage("Import error : " + cause.getMessage()
                    + " . Please see the error log for details.");
            Activator.getDefault().getLog()
                    .log(new Status(Status.ERROR, Constants.PLUGIN_ID, "Repository import failed", cause));
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Repositoy Import"); // NON-NLS-1
		setNeedsProgressMonitor(true);
		mainPage = new ImportWizardPage("Import from Repository", selection); // NON-NLS-1
        setDefaultPageImageDescriptor(SharedImages.SLING_LOG);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		super.addPages();
		addPage(mainPage);
	}
	
    private void createRoot(final IProject project, final IPath projectRelativePath, final String repositoryPath)
            throws CoreException {

        IPath rootImportPath = projectRelativePath.append(repositoryPath);

        for (int i = rootImportPath.segmentCount() - 1; i > 0; i--)
            createFolder(project, rootImportPath.removeLastSegments(i));
    }

	/**
	 * Crawls the repository and recursively imports founds resources
	 * 
	 * @param repository the sling repository to import from
	 * @param path the current path to import from
	 * @param project the project to create resources in
	 * @param projectRelativePath the path, relative to the project root, where the resources should be created
	 * @param tracer 
	 * @throws JSONException
	 * @throws RepositoryException
	 * @throws CoreException
	 */
	// TODO: This probably should be pushed into the service layer	
    private void crawlChildrenAndImport(Repository repository, String path, IProject project, IPath projectRelativePath)
            throws JSONException, RepositoryException, CoreException {

        System.out.println("crawlChildrenAndImport(" + repository + ", " + path + ", " + project + ", "
                + projectRelativePath + ")");

        String children = executeCommand(repository.newListChildrenNodeCommand(path, ResponseType.JSON));
		JSONObject json = new JSONObject(children);
		String primaryType= json.optString(Repository.JCR_PRIMARY_TYPE);
 
		if (Repository.NT_FILE.equals(primaryType)){
            importFile(repository, path, project, projectRelativePath);
		}else if (Repository.NT_FOLDER.equals(primaryType)){
			createFolder(project, projectRelativePath.append(path));
		}else if(Repository.NT_RESOURCE.equals(primaryType)){
			//DO NOTHING
        } else {
			createFolder(project, projectRelativePath.append(path));
            String content = executeCommand(repository.newGetNodeContentCommand(path, ResponseType.JSON));
			JSONObject jsonContent = new JSONObject(content);
			String contentXml = JSONML.toString(jsonContent);		
            createFile(project, projectRelativePath.append(serializationManager.getSerializationFilePath(path)),
                    contentXml.getBytes(Charset.forName("UTF-8") /* TODO is this enough? */));
		}
 		
        for (Iterator<?> keys = json.keys(); keys.hasNext();) {
            String key = (String) keys.next();
			JSONObject innerjson=json.optJSONObject(key);
			if (innerjson!=null){
                crawlChildrenAndImport(repository, path + "/" + key, project, projectRelativePath);
			}
		}
	}

    private <T> T executeCommand(Command<T> command) throws RepositoryException {
		
		Result<T> result = command.execute();
		return result.get();
	}	
	
    private void importFile(Repository repository, String path, IProject project, IPath destinationPath)
            throws JSONException, RepositoryException, CoreException {

        System.out.println("importFile: " + path + " -> " + destinationPath);

        byte[] node = executeCommand(repository.newGetNodeCommand(path));
			createFile(project, destinationPath.append(path), node );
	}
	
	private void createFolder(IProject project, IPath destinationPath) throws CoreException{

		IFolder destinationFolder = project.getFolder(destinationPath);
		if ( destinationFolder.exists() )
			return;

		destinationFolder.create(true, true, null /* TODO progress monitor */);
	}
	
	private void createFile(IProject project, IPath path, byte[] node) throws CoreException {		
		
		IFile destinationFile = project.getFile(path);
		if ( destinationFile.exists() )
			return;
		
		destinationFile.create(new ByteArrayInputStream(node), true, null /* TODO progress monitor */);
	}
}
