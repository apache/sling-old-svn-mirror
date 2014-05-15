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

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.eclipse.wst.server.core.IServer;

public class ExportWizardPage extends WizardDataTransferPage {

    private SlingLaunchpadCombo repositoryCombo;
    private IResource syncStartPoint;

    public ExportWizardPage(IResource syncStartPoint) {
        super("Repository selection");
        setTitle("Repository selection");
        setDescription("Select a repository to export content to");
        this.syncStartPoint = syncStartPoint;
    }

    @Override
    public void handleEvent(Event event) {
        determinePageCompletion();
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Composite container = new Composite(composite, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.minimumWidth = 450;
        container.setLayoutData(gridData);

        new Label(container, SWT.NONE).setText("Repository: ");

        repositoryCombo = new SlingLaunchpadCombo(container, syncStartPoint.getProject());
        repositoryCombo.getWidget().addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                determinePageCompletion();
                updateWidgetEnablements();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                determinePageCompletion();
                updateWidgetEnablements();
            }
        });
        repositoryCombo.refreshRepositoryList(new NullProgressMonitor());

        createOptionsGroup(composite);

        setControl(composite);

        updateWidgetEnablements();
        determinePageCompletion();
    }

    public IServer getServer() {
        return repositoryCombo.getServer();
    }

    @Override
    protected boolean allowNewContainerName() {
        return false;
    }

    @Override
    protected void createOptionsGroup(Composite parent) {

        // not really options but to placement is good enough
        Label filterLabel = new Label(parent, SWT.NONE);
        GridDataFactory.fillDefaults().applyTo(filterLabel);

        IFile filterFile = ResourcesPlugin.getWorkspace().getRoot()
                .getFileForLocation(ProjectUtil.findFilterPath(syncStartPoint.getProject()));

        if (filterFile != null && filterFile.exists()) {
            filterLabel.setText("Will apply export filter from /" + filterFile.getProjectRelativePath() + ".");
        } else {
            filterLabel.setText("No filter definition found, will export all resources.");
        }
    }

    @Override
    protected boolean validateDestinationGroup() {
        String repositoryError = repositoryCombo.getErrorMessage();
        if (repositoryError != null) {
            setErrorMessage(repositoryError);
            return false;
        }

        return true;
    }
}
