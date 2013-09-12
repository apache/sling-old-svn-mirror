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
import java.util.Iterator;

import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class ConvertToContentPackageAction implements IObjectActionDelegate {

	private ISelection fSelection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			final IProject project = (IProject) ((IStructuredSelection) fSelection).getFirstElement();

			String jcrRootLocation = "src/main/content/jcr_root";
			final InputDialog id = new InputDialog(getDisplay().getActiveShell(), "Convert to Sling Content-Package Project", 
					"Confirm jcr_root location of "+project.getName()+":", jcrRootLocation, new IInputValidator() {
						
						@Override
						public String isValid(String newText) {
							if (newText!=null && newText.trim().length()>0) {
								final IResource l = project.findMember(newText);
								if (l!=null && l.exists()) {
									return null;
								} else {
									return "Directory not found: "+newText;
								}
							} else {
								return "Please specify location of jcr_root";
							}
						}
					});
			if (id.open() == IStatus.OK) {
				IRunnableWithProgress r = new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try {
							ConfigurationHelper.convertToContentPackageProject(project, monitor, id.getValue());
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
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection iss = (IStructuredSelection) selection;
			Iterator<Object> it = iss.iterator();
			if (!it.hasNext()) {
				action.setEnabled(false);
				return;
			}
			while(it.hasNext()) {
				Object elem = it.next();
				if (elem!=null && (elem instanceof IProject)) {
					final IProject project = (IProject) elem;
					if (ProjectHelper.isContentProject(project)) {
						action.setEnabled(false);
						return;
					} else if (ProjectHelper.isPotentialContentProject(project)) {
						continue;
					} else {
						action.setEnabled(false);
						return;
					}
				} else {
					action.setEnabled(false);
					return;
				}
			}
			action.setEnabled(true);
		} else {
			action.setEnabled(false);
		}
	}

	public Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

}
