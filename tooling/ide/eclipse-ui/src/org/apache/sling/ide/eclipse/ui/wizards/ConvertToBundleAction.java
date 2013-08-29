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

import org.apache.maven.model.Model;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class ConvertToBundleAction  implements IObjectActionDelegate {

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

			boolean confirmed = MessageDialog.openConfirm(getDisplay().getActiveShell(), "Convert to Sling/OSGi Bundle Project", 
					"Confirm the conversion of this project to a Sling/OSGi Bundle Project");
			
			if (confirmed) {
				IRunnableWithProgress r = new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try {
							ConfigurationHelper.convertToBundleProject(project);
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
			if (iss.toList().size()!=1) {
				action.setEnabled(false);
			} else {
				Object firstElement = iss.getFirstElement();
				if (firstElement!=null && (firstElement instanceof IProject)) {
					final IProject project = (IProject) firstElement;
					if (ProjectHelper.isBundleProject(project)) {
						action.setEnabled(false);
					} else {
						Model mavenModel = MavenHelper.getMavenModel(project);
						if ("bundle".equals(mavenModel.getPackaging())) {
							action.setEnabled(true);
						} else {
							action.setEnabled(false);
						}
					}
				} else {
					action.setEnabled(false);
				}
			}
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
