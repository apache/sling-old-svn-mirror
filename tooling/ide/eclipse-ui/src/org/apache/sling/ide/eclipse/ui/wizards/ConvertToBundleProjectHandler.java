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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConvertToBundleProjectHandler extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        
		if (selection instanceof IStructuredSelection) {
			List<IProject> applicableProjects = new LinkedList<>();
			IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject p : allProjects) {
				if (p.isOpen() && ProjectHelper.isPotentialBundleProject(p)) {
					applicableProjects.add(p);
				}
			}
			Object[] elems = ((IStructuredSelection) selection).toArray();
			List<IProject> initialSelection = new ArrayList<>(elems.length);

			for (Object elem : elems) {
				IProject project = null;

				if (elem instanceof IFile) {
					IFile file = (IFile) elem;
					project = file.getProject();
				} else if (elem instanceof IProject) {
					project = (IProject) elem;
				} else if (elem instanceof IJavaProject) {
					project = ((IJavaProject) elem).getProject();
				}
				if (project != null)
					initialSelection.add(project);
			}

			ConvertProjectsWizard wizard = new ConvertProjectsWizard(applicableProjects, initialSelection, 
                    "Convert to Bundle Project(s)", "Select project(s) to convert to Bundle project(s)\n"
                            + "List contains open Java projects that are not yet bundle or content projects.");

			final Display display = getDisplay();
			final WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
			BusyIndicator.showWhile(display, new Runnable() {
				public void run() {
					dialog.open();
				}
			});
			if (dialog.getReturnCode()!=WizardDialog.OK) {
				// user did not click OK
				return null;
			}
			final List<IProject> selectedProjects = wizard.getSelectedProjects();
			if (selectedProjects == null || selectedProjects.size()==0) {
				// no project was selected
			    return null;
			}
			IRunnableWithProgress r = new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						for (IProject project : selectedProjects) {
						    ConfigurationHelper.convertToBundleProject(project);
						}
					} catch (CoreException e) {
						e.printStackTrace();
						MessageDialog.openError(getDisplay().getActiveShell(), "Could not convert project",
								e.getMessage());
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(r);
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(getDisplay().getActiveShell(), "Could not convert project",
						e.getMessage());
			}
		}
		
		return null;
	}

	public Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

}
