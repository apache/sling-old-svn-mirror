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
import java.util.Arrays;
import java.util.Iterator;

import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConvertToContentPackageAction implements IObjectActionDelegate {

    private ISelection fSelection;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.
     * action.IAction, org.eclipse.ui.IWorkbenchPart)
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
            final IProject project = (IProject) ((IStructuredSelection) fSelection)
                    .getFirstElement();

            ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                    getDisplay().getActiveShell(),
                    new WorkbenchLabelProvider(),
                    new BaseWorkbenchContentProvider());
            dialog.setMessage("Select content sync root location (containing the jcr root)");
            dialog.setTitle("Content Sync Root");
            IContainer initialContainer = ProjectHelper
                    .getInferredContentProjectContentRoot(project);
            if (initialContainer != null) {
                dialog.setInitialElementSelections(Arrays
                        .asList(initialContainer));
            }
            dialog.addFilter(new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement,
                        Object element) {
                    if (element instanceof IProject) {
                        return ((IProject) element).equals(project);
                    }
                    // display folders only
                    return element instanceof IContainer;
                }
            });
            dialog.setInput(new IWorkbenchAdapter() {

                @Override
                public Object getParent(Object o) {
                    return null;
                }

                @Override
                public String getLabel(Object o) {
                    return null;
                }

                @Override
                public ImageDescriptor getImageDescriptor(Object object) {
                    return null;
                }

                @Override
                public Object[] getChildren(Object o) {
                    return new Object[] { project };
                }
            }); // this is the root element
            dialog.setAllowMultiple(false);
            dialog.setValidator(new ISelectionStatusValidator() {

                @Override
                public IStatus validate(Object[] selection) {

                    if (selection.length > 0) {
                        final Object item = selection[0];
                        if (item instanceof IContainer) {
                            IContainer selectedContainer = (IContainer) item;
                            String errorMsg = ProjectHelper
                                    .validateContentPackageStructure(selectedContainer);
                            if (errorMsg != null) {
                                return new Status(IStatus.ERROR,
                                        Activator.PLUGIN_ID, errorMsg);
                            } else {
                                return new Status(IStatus.OK,
                                        Activator.PLUGIN_ID, "");
                            }
                        }
                    }
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "");
                }

            });
            if (dialog.open() == ContainerSelectionDialog.OK) {
                Object[] result = dialog.getResult();
                if (result != null && result.length > 0) {
                    final IContainer container = (IContainer) result[0];
                    IRunnableWithProgress r = new IRunnableWithProgress() {

                        @Override
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                IResource jcrRoot = container
                                        .findMember("jcr_root");
                                if (jcrRoot == null
                                        || !(jcrRoot instanceof IFolder)) {
                                    MessageDialog.openError(getDisplay()
                                            .getActiveShell(),
                                            "Could not convert project",
                                            "jcr_root not found under "
                                                    + container
                                                    + " (or not a Folder)");
                                }
                                ConfigurationHelper
                                        .convertToContentPackageProject(
                                                project,
                                                monitor,
                                                jcrRoot.getProjectRelativePath());
                            } catch (CoreException e) {
                                Activator.getDefault().getPluginLogger()
                                        .warn("Could not convert project", e);
                                MessageDialog.openError(getDisplay()
                                        .getActiveShell(),
                                        "Could not convert project", e
                                                .getMessage());
                            }
                        }
                    };
                    try {
                        PlatformUI.getWorkbench().getProgressService()
                                .busyCursorWhile(r);
                    } catch (Exception e) {
                        Activator.getDefault().getPluginLogger()
                                .warn("Could not convert project", e);
                        MessageDialog.openError(getDisplay().getActiveShell(),
                                "Could not convert project", e.getMessage());
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
     * .IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        fSelection = selection;
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection iss = (IStructuredSelection) selection;
            Iterator<?> it = iss.iterator();
            if (!it.hasNext()) {
                action.setEnabled(false);
                return;
            }
            while (it.hasNext()) {
                Object elem = it.next();
                if (elem != null && (elem instanceof IProject)) {
                    final IProject project = (IProject) elem;
                    if (ProjectHelper.isContentProject(project) || ProjectHelper.isBundleProject(project)) {
                        action.setEnabled(false);
                        return;
                    } else {
                        // SLING-3651 : always show action -
                        //              allows to provide more filter detail infos
                        continue;
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