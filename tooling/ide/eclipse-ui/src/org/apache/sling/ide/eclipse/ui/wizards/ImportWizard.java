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
package org.apache.sling.ide.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.ImportRepositoryContentAction;
import org.apache.sling.ide.eclipse.ui.internal.ImportWizardPage;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServer;

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

        IResource resource = mainPage.getResource();
        final IProject project = resource.getProject();
        final IPath projectRelativePath = resource.getProjectRelativePath();

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    // wrap the import action in a IWorkspaceRunnable to make sure that changes are only
                    // published once at the end. This is especially important since the import action
                    // sets persistent properties on the modified resources to avoid them being published
                    // following this change ( see org.apache.sling.ide.core.ResourceUtil )
                    try {
                        ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

                            @Override
                            public void run(IProgressMonitor monitor) throws CoreException {
                                try {
                                    new ImportRepositoryContentAction(server, projectRelativePath, project,
                                            serializationManager).run(monitor);
                                } catch (SerializationException e) {
                                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                            "Import failed", e));
                                } catch (InvocationTargetException e) {
                                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                            "Import failed", e.getCause()));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    serializationManager.destroy();
                                }
                            }
                        }, project, IWorkspace.AVOID_UPDATE, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            mainPage.setErrorMessage("Import error : " + cause.getMessage()
                    + " . Please see the error log for details.");
            Activator.getDefault().getPluginLogger().error("Repository import failed", cause);
            return false;
        } catch (OperationCanceledException | InterruptedException e) {
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
        setDefaultPageImageDescriptor(WhitelabelSupport.getWizardBanner());

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
}
