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
package org.apache.sling.slingclipse.internal;

import java.util.Iterator;

import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class AddSlingNatureAction implements IObjectActionDelegate, IExecutableExtension {

    private ISelection selection;

    @Override
    public void run(IAction action) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }

        IStructuredSelection structuredSelection = (IStructuredSelection) selection;

        try {
            for (Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
                Object selected = it.next();
                if (selected instanceof IProject) {
                    IProject project = (IProject) selected;

                    IProjectDescription description = project.getDescription();

                    addSlingNature(description);
                    addSlingBuilder(description);
                    
                    project.setDescription(description, null);
                }
            }
        } catch (CoreException e) {
            ErrorDialog.openError(null, "Error occurred while adding the sling nature", e.getMessage(), e.getStatus());
            SlingclipsePlugin.getDefault().getLog().log(e.getStatus());
        }
    }

    private void addSlingNature(IProjectDescription description) {
        String[] currentNatureIds = description.getNatureIds();
        String[] newNatureIds = new String[currentNatureIds.length + 1];
        System.arraycopy(currentNatureIds, 0, newNatureIds, 0, currentNatureIds.length);
        newNatureIds[currentNatureIds.length] = SlingProjectNature.SLING_NATURE_ID;

        description.setNatureIds(newNatureIds);
    }

    private void addSlingBuilder(IProjectDescription description) {
        boolean addBuilder = true;
        ICommand[] currentBuilders = description.getBuildSpec();
        for ( ICommand command : currentBuilders ) {
            if ( command.getBuilderName().equals(SlingProjectBuilder.SLING_BUILDER_ID) ) {
                addBuilder = false;
                break;
            }
        }
        
        if (!addBuilder)
            return;

        ICommand newBuilder = description.newCommand();
        newBuilder.setBuilderName(SlingProjectBuilder.SLING_BUILDER_ID);

        ICommand[] newBuilders = new ICommand[currentBuilders.length + 1];
        newBuilders[currentBuilders.length] = newBuilder;
        description.setBuildSpec(newBuilders);
    }


    @Override
    public void selectionChanged(IAction action, ISelection selection) {

        this.selection = selection;
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        // nothing to do
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // nothing to do
    }
}
