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
package org.apache.sling.ide.eclipse.wst.ui.internal;


import java.util.List;

import org.apache.sling.slingclipse.helper.SlingclipseHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardResourceImportPage;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

/**
 * Wizard page for importing content from Sling Repositories.
 */
public class ImportWizardPage extends WizardResourceImportPage {

	private IStructuredSelection selection;
	private Text path;
	private ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent event) {
			try{
				determinePageCompletion();
				updateWidgetEnablements();
			}catch(Exception e){
				//TODO: Log or just ignore?
			}
		}
	};
    private Combo repositoryCombo;

	/**
	 * Creates an import wizard page for importing from a Sling Repository. If
	 * the initial resource selection contains exactly one container resource
	 * then it will be used as the default import destination. Multiple
	 * selections are not supported, but are not disallowed.
	 * 
	 * @param pageName
	 *            the name of the page
	 * @param selection
	 *            the current resource selection
	 */
	public ImportWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		this.selection = selection;
		setTitle(pageName); // NON-NLS-1
		setDescription("Import content from a Sling Repository into the workspace"); // NON-NLS-1

        setContainerFieldValue(getProject(selection).getFullPath()
                .append(SlingclipseHelper.JCR_ROOT).toOSString());
	}

    private IProject getProject(IStructuredSelection selection) {
        return (IProject) selection.getFirstElement();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.WizardResourceImportPage#allowNewContainerName()
	 */
	@Override
	protected boolean allowNewContainerName() {
		return false;
	}

	@Override
	protected void createOptionsGroup(Composite parent) {

	}

    @Override
	protected void createSourceGroup(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.minimumWidth = 450;
		container.setLayoutData(gridData);

        new Label(container, SWT.NONE).setText("Repository: ");

        repositoryCombo = new Combo(container, SWT.DROP_DOWN);
        repositoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        List<IServer> servers = SelectionUtils.getServersLinkedToProject(selection, new NullProgressMonitor());
        if (servers.size() > 1) {
            repositoryCombo.add(""); // force selection only if there is more than one server
        }
        for (IServer server : servers) {
            repositoryCombo.add(server.getId());
        }

        if (servers.size() == 1) {
            repositoryCombo.select(0);
        }

        Label pathLabel = new Label(container, SWT.NONE);
        pathLabel.setText("Repository Path:");
        path = new Text(container, SWT.BORDER);
        path.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        path.addModifyListener(modifyListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardResourceImportPage#getFileProvider()
	 */
	@Override
	protected ITreeContentProvider getFileProvider() {
		// TODO Not sure if I need to return anything here...
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardResourceImportPage#getFolderProvider()
	 */
	@Override
	protected ITreeContentProvider getFolderProvider() {
		// TODO Not sure if I need to return anything here...
		return null;
	}

	/**
	 * Returns the path from which to import from the Sling Repository.
	 * 
	 * @return the repository path
	 */
	public String getRepositoryPath() {
		return path != null ? path.getText() : null;
	}

	public void handleEvent(Event event) {
		super.handleEvent(event);
		determinePageCompletion();
	}

    @Override
    public IPath getResourcePath() {
        return super.getResourcePath();
    }

    public IServer getServer() {
        for (IServer server : ServerCore.getServers())
            if (server.getId().equals(repositoryCombo.getText()))
                return server;

        return null;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#validateOptionsGroup()
	 */
	@Override
	protected boolean validateOptionsGroup() {
        if (this.repositoryCombo == null || this.repositoryCombo.getSelectionIndex() == -1) {
            setErrorMessage("Please select a valid index");
			return false;
		}
		
		if ( !getRepositoryPath().startsWith("/") ) {
			setErrorMessage("The repository path needs to be absolute");
			return false;
		}
		
        if (!getResourcePath().toOSString().endsWith(SlingclipseHelper.JCR_ROOT)) {
			setErrorMessage("Please enter a valid Sling project folder (e.g. jcr_root)");
			return false;
		}
		
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#validateSourceGroup()
	 */
	@Override
	protected boolean validateSourceGroup() {
		if (getRepositoryPath() == null
				|| getRepositoryPath().trim().length() == 0) {
			setErrorMessage("Please enter a valid Sling Repository path");
			return false;
		}
		return true;
	}
}
