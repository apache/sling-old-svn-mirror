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
package org.apache.sling.ide.eclipse.ui.wizards.np;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.archetype.catalog.Archetype;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.progress.IProgressService;

@SuppressWarnings("restriction")
public class ChooseArchetypeWizardPage extends WizardPage {
	
	private static final String LOADING_PLEASE_WAIT = "loading, please wait...";
	private List knownArchetypesList;
	private Map<String, Archetype> archetypesMap = new HashMap<String, Archetype>();
	private Button useDefaultWorkspaceLocationButton;
	private Label locationLabel;
	private Combo locationCombo;
	private final AbstractNewSlingApplicationWizard parent;

	public ChooseArchetypeWizardPage(AbstractNewSlingApplicationWizard parent) {
		super("chooseArchetypePage");
		this.parent = parent;
		setTitle("Choose Project Location and Archetype");
		setDescription("This step defines the project location and archetype");
		setImageDescriptor(parent.getLogo());
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

	    useDefaultWorkspaceLocationButton = new Button(container, SWT.CHECK);
	    GridData useDefaultWorkspaceLocationButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
	    useDefaultWorkspaceLocationButton.setLayoutData(useDefaultWorkspaceLocationButtonData);
	    useDefaultWorkspaceLocationButton
	        .setText("Use default Workspace location");
	    useDefaultWorkspaceLocationButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent e) {
	        boolean inWorkspace = useDefaultWorkspaceLocationButton.getSelection();
	        locationLabel.setEnabled(!inWorkspace);
	        locationCombo.setEnabled(!inWorkspace);
	        dialogChanged();
	      }
	    });
	    useDefaultWorkspaceLocationButton.setSelection(true);

	    locationLabel = new Label(container, SWT.NONE);
	    GridData locationLabelData = new GridData();
	    locationLabelData.horizontalIndent = 10;
	    locationLabel.setLayoutData(locationLabelData);
	    locationLabel.setText("Location:");
	    locationLabel.setEnabled(false);

	    locationCombo = new Combo(container, SWT.NONE);
	    GridData locationComboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
	    locationCombo.setLayoutData(locationComboData);
	    locationCombo.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent e) {
	    	  dialogChanged();
	      }
	    });
	    locationCombo.setEnabled(false);

	    Button locationBrowseButton = new Button(container, SWT.NONE);
	    GridData locationBrowseButtonData = new GridData(SWT.FILL, SWT.CENTER, false, false);
	    locationBrowseButton.setLayoutData(locationBrowseButtonData);
	    locationBrowseButton.setText("Browse...");
	    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent e) {
	        DirectoryDialog dialog = new DirectoryDialog(getShell());
	        dialog.setText("Select Location");

	        String path = locationCombo.getText();
	        if(path.length() == 0) {
	          path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
	        }
	        dialog.setFilterPath(path);

	        String selectedDir = dialog.open();
	        if(selectedDir != null) {
	          locationCombo.setText(selectedDir);
	          useDefaultWorkspaceLocationButton.setSelection(false);
	          dialogChanged();
	        }
	      }
	    });
		
		
		Label label = new Label(container, SWT.NULL);
		label.setText("&Archetype:");

		knownArchetypesList = new List(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		knownArchetypesList.setLayoutData(gd);
		knownArchetypesList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		
		initialize();
		setPageComplete(false);
		setControl(container);
	}

	public Archetype getSelectedArchetype() {
		String[] sel = knownArchetypesList.getSelection();
		if (sel==null || sel.length!=1) {
			return null;
		}
		String s = sel[0];
		Archetype a = archetypesMap.get(s);
		return a;
	}
	
	private void initialize() {
		knownArchetypesList.add(LOADING_PLEASE_WAIT);
		IWorkbench workbench = DebugUIPlugin.getDefault().getWorkbench();
		IProgressService progressService = workbench.getProgressService();
		try {
			progressService.run(true, false, new IRunnableWithProgress() {
					
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("discoverying archetypes...", 5);
				    ArchetypeManager manager = MavenPluginActivator.getDefault().getArchetypeManager();
				    monitor.worked(1);
				    Collection<ArchetypeCatalogFactory> archetypeCatalogs = manager.getArchetypeCatalogs();
				    monitor.worked(2);
				    ArrayList<Archetype> list = new ArrayList<Archetype>();
				    for(ArchetypeCatalogFactory catalog : archetypeCatalogs) {
				        try {
						  @SuppressWarnings("unchecked")
						  java.util.List<Archetype> arcs = catalog.getArchetypeCatalog().getArchetypes();
				          if(arcs != null) {
				            list.addAll(arcs);
				          }
				        } catch(Exception ce) {
				        	ce.printStackTrace();
				        }
				      }
				    monitor.worked(1);
				    for (Iterator<Archetype> it = list
							.iterator(); it.hasNext();) {
						Archetype archetype2 = it.next();
						if (parent.acceptsArchetype(archetype2)) {
							String key = keyFor(archetype2);
							archetypesMap.put(key, archetype2);
						}
						
					}
				    monitor.worked(1);
			        Display.getDefault().asyncExec(new Runnable() {
			            public void run() {
			            	Set<String> keys = archetypesMap.keySet();
			            	knownArchetypesList.removeAll();
			            	for (Iterator<String> it = keys.iterator(); it
									.hasNext();) {
								String aKey = it.next();
								knownArchetypesList.add(aKey);
							}
			            	knownArchetypesList.pack();
			            }
			          });
			        monitor.done();

				}
			});
		} catch (InvocationTargetException e) {
			// TODO proper logging
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO proper logging
			e.printStackTrace();
		}
	}

	private String keyFor(Archetype archetype2) {
		return archetype2.getGroupId() + " : "+archetype2.getArtifactId() + " : "+archetype2.getVersion();
	}

	private void dialogChanged() {
		if (knownArchetypesList.getItemCount()==1 &&
				knownArchetypesList.getItem(0).equals(LOADING_PLEASE_WAIT)) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
		if (knownArchetypesList.getSelectionCount()!=1) {
			updateStatus("archetype must be selected");
			return;
		}
		if (!useDefaultWorkspaceLocationButton.getSelection() &&
				locationCombo.getText().length()==0) {
			updateStatus("location must be specified");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public IPath getLocation() {
		if (!useDefaultWorkspaceLocationButton.getSelection() && 
				locationCombo.getText().length()>0) {
			return new Path(locationCombo.getText());
		} else {
			return null;
		}
	}

}