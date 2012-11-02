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
package org.apache.sling.slingclipse.ui.wizards;

import java.util.List;

import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.apache.sling.slingclipse.helper.SlingclipseHelper;
import org.apache.sling.slingclipse.preferences.PreferencesMessages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

/**
 * Wizard page for importing content from Sling Repositories.
 */
public class ImportWizardPage extends WizardResourceImportPage {

	private IStructuredSelection selection;
	private Text password;
	private Text user;
	private Text repo;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.WizardDataTransferPage#createOptionsGroup(org.
	 * eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createOptionsGroup(Composite parent) {
		Group optionsGroup = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		optionsGroup.setLayout(layout);
		optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		optionsGroup.setText("Options");
		optionsGroup.setFont(parent.getFont());

		Composite container = new Composite(optionsGroup, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.minimumWidth = 450;
		container.setLayoutData(gridData);
		
		IPreferenceStore store = SlingclipsePlugin.getDefault()
				.getPreferenceStore();

		Label repoLabel = new Label(container, SWT.NONE);
		repoLabel.setText("Repository URL:");
		repo = new Text(container, SWT.BORDER);
		repo.setText(store.getString(PreferencesMessages.REPOSITORY_URL
				.getKey()));
		repo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		repo.addModifyListener(modifyListener);

		Label userLabel = new Label(container, SWT.NONE);
		userLabel.setText("Username:");
		user = new Text(container, SWT.BORDER);
		user.setText(store.getString(PreferencesMessages.USERNAME.getKey()));
		user.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		user.addModifyListener(modifyListener);

		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("Password:");
		password = new Text(container, SWT.BORDER);
		password.setText(store.getString(PreferencesMessages.PASSWORD.getKey()));
		password.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		password.addModifyListener(modifyListener);
 		
		createOptionsGroupButtons(optionsGroup);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.WizardResourceImportPage#createSourceGroup(org
	 * .eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createSourceGroup(Composite parent) {

		// TODO: Currently only supports first selection
		IResource resource = ((IResource) selection.getFirstElement());
		String pathStr = resource.getFullPath().toPortableString();

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.minimumWidth = 450;
		container.setLayoutData(gridData);
		
		Label pathLabel = new Label(container, SWT.NONE);
		pathLabel.setText("Repository Path:");
		path = new Text(container, SWT.BORDER);
		path.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		path.addModifyListener(modifyListener);
		

		if (SlingclipseHelper.isValidSlingProjectPath(pathStr)) {
			path.setText(SlingclipseHelper.getSlingProjectPath(pathStr));
		}
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
	 * Get the password with which to connect to the Sling Repository.
	 * 
	 * @return the username
	 */
	public String getPassword() {
		return password != null ? password.getText() : null;
	}

	/**
	 * Returns the Sling Repository URL to connect to.
	 * 
	 * @return the Sling Repository URL
	 */
	public String getRepositoryUrl() {
		return repo != null ? repo.getText() : null;
	}

	/**
	 * Returns the path from which to import from the Sling Repository.
	 * 
	 * @return the repository path
	 */
	public String getRepositoryPath() {
		return path != null ? path.getText() : null;
	}

	/**
	 * Get the username with which to connect to the Sling Repository.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return user != null ? user.getText() : null;
	}
	
	
	public String getIntoFolderPath(){ 
		IPath containerNameField= super.getResourcePath();  
		String workspacePath=((IResource)selection.getFirstElement()).getWorkspace().getRoot().getLocation().toOSString();
 		return workspacePath+"/"+containerNameField.toOSString();
	}
	

	public void handleEvent(Event event) {
		super.handleEvent(event);
		determinePageCompletion();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#validateOptionsGroup()
	 */
	@Override
	protected boolean validateOptionsGroup() {
		if (this.getRepositoryUrl() == null
				|| this.getRepositoryUrl().trim().length() == 0
				|| this.getUsername() == null
				|| this.getUsername().trim().length() == 0
				|| this.getPassword() == null
				|| this.getPassword().trim().length() == 0) {
			setErrorMessage("Please enter valid server information");
			return false;
		}
		IPath containerNameField= super.getResourcePath();  
		if (!containerNameField.toOSString().endsWith(SlingclipseHelper.JCR_ROOT)){
			setErrorMessage("Please enter a valid Sling project folder (e.g. jcr_content)");
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
