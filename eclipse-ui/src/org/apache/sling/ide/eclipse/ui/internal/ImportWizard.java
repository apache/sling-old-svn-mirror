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

import java.lang.reflect.InvocationTargetException;

import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                try {
                    new ImportRepositoryContentAction(server, projectRelativePath, project, serializationManager).run(monitor);
                } catch (SerializationException e) {
                    throw new InvocationTargetException(e);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    serializationManager.destroy();
                }
            }
        };

        try {
            getContainer().run(false, true, runnable);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            mainPage.setErrorMessage("Import error : " + cause.getMessage()
                    + " . Please see the error log for details.");
            Activator.getDefault().getPluginLogger().error("Repository import failed", cause);
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
}
