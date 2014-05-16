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
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.eclipse.wst.server.core.IServer;

/**
 * Wizard page for importing content from Sling Repositories.
 */
public class ImportWizardPage extends WizardDataTransferPage {

    private SlingLaunchpadCombo repositoryCombo;
    private Label importLabel;
	private Button containerBrowseButton;
    private IProject project;
	private Text containerNameField;
	private Label adjustJcrRootText;
    private IFolder importRoot;
    private Composite adjustComposite;

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
		super(pageName);
		setTitle(pageName); // NON-NLS-1
		setDescription("Import content from a Sling Repository into the workspace"); // NON-NLS-1

        Object selectedResource = selection.getFirstElement();
        if (selectedResource instanceof IProject) {
            importRoot = ProjectUtil.getSyncDirectory((IProject) selectedResource);
        } else if (selectedResource instanceof IFile) {
            // the selection dialog does not support files, so we force this to be the parent folder
            // also, since the content sync root must be a folder, the parent must be a folder, can't
            // be a project
            importRoot = (IFolder) ((IFile) selectedResource).getParent();
        } else if (selectedResource instanceof IFolder) {
            importRoot = (IFolder) selectedResource;
        }

        if (importRoot != null) {
            project = importRoot.getProject();
        }
	}
	
    public IResource getResource() {
        return importRoot;
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

        // not really options but to placement is good enough
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        container.setLayoutData(gridData);

        importLabel = new Label(container, SWT.NONE);

	}
	
	@Override
	public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Composite container = new Composite(composite, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.minimumWidth = 450;
		container.setLayoutData(gridData);

        new Label(container, SWT.NONE).setText("Repository: ");

        repositoryCombo = new SlingLaunchpadCombo(container, project);
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

        Composite containerGroup = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        containerGroup.setLayout(layout);
        containerGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        containerGroup.setFont(composite.getFont());

        // container label
        Label resourcesLabel = new Label(containerGroup, SWT.NONE);
        resourcesLabel.setText("Import into:");
        resourcesLabel.setFont(composite.getFont());

        containerNameField = new Text(containerGroup, SWT.SINGLE | SWT.BORDER);
        containerNameField.addListener(SWT.Modify, this);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        containerNameField.setLayoutData(data);
        containerNameField.setFont(composite.getFont());

        containerBrowseButton = new Button(containerGroup, SWT.PUSH);
        containerBrowseButton.setText("Select location...");
        containerBrowseButton.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL));
        containerBrowseButton.addListener(SWT.Selection, this);
        containerBrowseButton.setFont(composite.getFont());
        setButtonLayoutData(containerBrowseButton);
        
        if (importRoot != null) {
            containerNameField.setText(importRoot.getFullPath().toPortableString());
        } else {
            setErrorMessage("Select an import location");
        }
        
        adjustComposite = new Composite(composite, SWT.NONE);
        adjustComposite.setLayout(new RowLayout());

        adjustJcrRootText = new Label(adjustComposite, SWT.NONE);
        adjustJcrRootText.setFont(containerGroup.getFont());
        adjustJcrRootText();
        
        Link openPropertiesLink = new Link(adjustComposite, SWT.NONE);
        openPropertiesLink.setText("(<a>change</a>)");
        openPropertiesLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(getShell(), project, 
						"org.apache.sling.ide.projectPropertyPage", 
						new String[] {"org.apache.sling.ide.projectPropertyPage"}, null);
				dialog.open();
				updateWidgetEnablements();
			}
		});
        
        createOptionsGroup(composite);
        
        setControl(composite);

        updateWidgetEnablements();
	}

	public void handleEvent(Event event) {
		if (event.widget == containerBrowseButton) {
			handleContainerBrowseButtonPressed();
		}
		
        updateWidgetEnablements();
		determinePageCompletion();
	}

    protected IPath queryForLocation(IProject initialSelection, String msg,
            String title) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(
                getControl().getShell(), initialSelection,
                allowNewContainerName(), msg);
        if (title != null) {
			dialog.setTitle(title);
		}
        dialog.showClosedProjects(false);
        dialog.setValidator(new ISelectionValidator() {
			
			@Override
			public String isValid(Object selection) {
				if (!(selection instanceof IPath)) {
                    return "Please select a valid import location";
				} 
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IContainer container = (IContainer) root.findMember((IPath) selection);
				if (container instanceof IProject) {
					return "Please select a folder inside the project";
				}
				
                if (!ProjectHelper.isContentProject(container.getProject())) {
                    return "Project " + container.getProject().getName() + " is not a a Sling content project";
                }

				if ( ! ProjectUtil.isInsideContentSyncRoot(container) ) {
                    return "Please select a folder inside the content sync root folder "
                            + ProjectUtil.getSyncDirectory(container.getProject()).getProjectRelativePath();
				}
				
                return null;
			}
		});
        dialog.open();
        Object[] result = dialog.getResult();
        if (result != null && result.length == 1) {
            return (IPath) result[0];
        }
        return null;
    }

    private void handleContainerBrowseButtonPressed() {
        IPath result = queryForLocation(project, "Select a location to import data to", "Select location");
    	if (result!=null) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            importRoot = (IFolder) root.findMember(result);
            project = importRoot.getProject();
	        
            containerNameField.setText(importRoot.getFullPath().toPortableString());
            repositoryCombo.setProject(project);
            repositoryCombo.refreshRepositoryList(new NullProgressMonitor());
    	}
	}

	public IServer getServer() {

        return repositoryCombo.getServer();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#validateOptionsGroup()
	 */
	@Override
	protected boolean validateOptionsGroup() {
		if (getControl() == null) {
			// still under construction
			return true;
		}

        if (this.repositoryCombo == null || this.repositoryCombo.getServer() == null) {
            setErrorMessage("Please select a Sling launchpad instance");
			return false;
		}

		return true;
	}

    @Override
    protected void updateWidgetEnablements() {
        super.updateWidgetEnablements();
        
        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
			setMessage(null);
		}

        // called too early
        if (importLabel == null) {
            return;
        }

        adjustComposite.setVisible(project != null);
        adjustComposite.getParent().layout();

        if (importRoot != null) {
            IFile filterFile = getFilter();

            if (filterFile != null && filterFile.exists()) {
                importLabel.setText("Will apply import filter from /" + filterFile.getProjectRelativePath() + ".");
            } else {
                importLabel.setText("No filter definition found, will import all resources.");
            }
            importLabel.setVisible(true);
        }
        importLabel.setVisible(importRoot != null);
        importLabel.getParent().layout();
    }

    private IFile getFilter() {

        IPath filterPath = ProjectUtil.findFilterPath(project);
        if (filterPath == null) {
            return null;
        }
        return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filterPath);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#validateSourceGroup()
	 */
	@Override
	protected boolean validateSourceGroup() {
		if (getControl() == null) {
			// still under construction
			return true;
		}
        if (adjustJcrRootText != null) {
            adjustJcrRootText();
            adjustJcrRootText.getParent().pack();
        }

        if (project == null || importRoot == null) {
            setErrorMessage("Please select a location to import to");
            return false;
        }

        String repositoryError = repositoryCombo.getErrorMessage();
        if (repositoryError != null) {
            setErrorMessage(repositoryError);
            return false;
        }

        return true;
	}

	private void adjustJcrRootText() {
        if (project != null) {
            adjustJcrRootText.setText("Content sync root is: " + project.getName() + "/"
                    + ProjectUtil.getSyncDirectoryValue(project));
        }
	}
}
