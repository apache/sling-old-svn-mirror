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
package org.apache.sling.ide.eclipse.ui.propertyPages;

import java.util.List;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.dialogs.PropertyPageContributorManager;
import org.eclipse.ui.internal.dialogs.PropertyPageManager;

public class SlingProjectPropertyPage extends PropertyPage {

    private static final String PAGE_ID = "org.apache.sling.ide.projectPropertyPage";
    private static final String PAGE_ID_OVERRIDE = PAGE_ID + ".override";

    public static void openPropertyDialog(Shell shell, IProject project) {

        // find out if the override page is contributed, and show that instead of the default one
        // TODO - stop relying on internals

        PropertyPageManager pageManager = new PropertyPageManager();
        PropertyPageContributorManager.getManager().contribute(pageManager, project);

        List<?> nodes = pageManager.getElements(PreferenceManager.PRE_ORDER);
        boolean overridePresent = false;
        for (Object node : nodes) {
            if (((IPreferenceNode) node).getId().equals(PAGE_ID_OVERRIDE)) {
                overridePresent = true;
                break;
            }
        }

        String pageId = overridePresent ? PAGE_ID_OVERRIDE : PAGE_ID;

        PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(shell, project,
                pageId, new String[] { pageId }, null);
        dialog.open();
    }

    private Text folderText;

    @Override
    protected Control createContents(Composite parent) {

        Composite c = new Composite(parent, SWT.NONE);

        c.setLayout(new GridLayout(3, false));

        new Label(c, SWT.NONE).setText("Content sync root directory");
        folderText = new Text(c, SWT.BORDER);
        folderText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        folderText.setText(ProjectUtil.getSyncDirectoryValue(getProject()).toString());

        folderText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateApplyButton();
            }
        });

        Button browseButton = new Button(c, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final IProject project = getProject();
                ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), project, false, null);
                dialog.showClosedProjects(false);
                dialog.setValidator(new ISelectionValidator() {

                    @Override
                    public String isValid(Object selection) {

                        if (!(selection instanceof IPath)) {
                            return null;
                        }

                        IPath path = (IPath) selection;
                        if (project.getFullPath().isPrefixOf(path)) {
                            return null;
                        }

                        return "The folder must be contained in the " + project.getName() + " project";
                    }
                });

                dialog.open();

                Object[] results = dialog.getResult();
                if (results == null) {
                    return;
                }

                IPath selectedPath = (IPath) results[0];
                folderText.setText(selectedPath.removeFirstSegments(1).toString());
            }
        });

        Dialog.applyDialogFont(c);

        return c;
    }

    @Override
    public boolean isValid() {

        String path = folderText.getText();
        IResource member = getProject().findMember(path);

        if (member == null) {
            setErrorMessage("Resource " + path + " is not a part of project " + getProject().getName());
            return false;
        } else if (member.getType() != IResource.FOLDER) {
            setErrorMessage("Resource " + path + " is not a folder");
            return false;
        }

        setErrorMessage(null);

        return true;
    }

    @Override
    public boolean performOk() {

        try {
            ProjectUtil.setSyncDirectoryPath(getProject(), new Path(folderText.getText()));
            getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (Exception e) {
            setErrorMessage("Could not refresh project "+getProject()+", "+e);
            return false;
        } catch(Error er) {
            Activator.getDefault().getPluginLogger().error("Error occurred: "+er, er);
            // rethrow though
            throw er;
        }

        return super.performOk();
    }

    private IProject getProject() {
        IProject project = (IProject) getElement().getAdapter(IProject.class);
        return project;
    }
}
