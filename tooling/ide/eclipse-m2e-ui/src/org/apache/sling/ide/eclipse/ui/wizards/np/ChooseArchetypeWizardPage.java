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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.sling.ide.eclipse.m2e.internal.Activator;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.index.IndexListener;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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

@SuppressWarnings("restriction")
public class ChooseArchetypeWizardPage extends WizardPage implements IndexListener {
	
    static final Comparator<String> ARTIFACT_KEY_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            
            String[] gav1 = o1.split(" : ");
            String[] gav2 = o2.split(" : ");
            
            // ensure we have sane values
            if ( gav1.length != 3 || gav2.length != 3 ) {
                return 0;
            }
            
            // natural order by groupId
            String groupId1 = gav1[0];
            String groupId2 = gav2[0];
            
            int res = groupId1.compareTo(groupId2);
            if ( res != 0 ) {
                return res;
            }
            
            // natural order by artifactId
            String artifactId1 = gav1[1];
            String artifactId2 = gav2[1];
            
            res = artifactId1.compareTo(artifactId2);
            if ( res != 0 ) {
                return res;
            }
            
            // reverse order by version ( newest first )
            ArtifactVersion version1 = new DefaultArtifactVersion(gav1[2]);
            ArtifactVersion version2 = new DefaultArtifactVersion(gav2[2]);
            
            return version2.compareTo(version1);
        }
    };
    
    private static final String LOADING_PLEASE_WAIT = "loading, please wait...";
    private Combo knownArchetypes;
	private Map<String, Archetype> archetypesMap = new TreeMap<>(ARTIFACT_KEY_COMPARATOR);
	private Button useDefaultWorkspaceLocationButton;
	private Label locationLabel;
	private Combo locationCombo;

	public ChooseArchetypeWizardPage(AbstractNewMavenBasedSlingApplicationWizard parent) {
		super("chooseArchetypePage");
		setTitle("Choose Project Location and Archetype");
		setDescription("This step defines the project location and archetype");
		setImageDescriptor(parent.getLogo());
	}

    @Override
    public AbstractNewMavenBasedSlingApplicationWizard getWizard() {
        return (AbstractNewMavenBasedSlingApplicationWizard) super.getWizard();
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

        knownArchetypes = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        knownArchetypes.setLayoutData(gd);
		knownArchetypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		knownArchetypes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				getContainer().showPage(getNextPage());
			}
		});
		
		setPageComplete(false);

        MavenPlugin.getIndexManager().addIndexListener(this);

		setControl(container);
	}

    @Override
    public void dispose() {

        MavenPlugin.getIndexManager().removeIndexListener(this);
        super.dispose();
    }

	public Archetype getSelectedArchetype() {

        int idx = knownArchetypes.getSelectionIndex();
        if (idx == -1) {
            return null;
        }

        String archetype = knownArchetypes.getItem(idx);

        return archetypesMap.get(archetype);
	}
	
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && knownArchetypes.getItemCount() == 0) {
            // initialize as late as possible to take advantage of the error reporting
            // and progress from the parent wizard
            initialize();
        }
    }

	private void initialize() {
		knownArchetypes.add(LOADING_PLEASE_WAIT);
        knownArchetypes.select(0);
		try {
            getContainer().run(true, false, new RefreshArchetypesRunnable());
		} catch (InvocationTargetException e) {
            getWizard().reportError(e.getTargetException());
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	}

	private String keyFor(Archetype archetype2) {
		return archetype2.getGroupId() + " : "+archetype2.getArtifactId() + " : "+archetype2.getVersion();
	}

	private void dialogChanged() {
		if (knownArchetypes.getItemCount()==1 &&
				knownArchetypes.getItem(0).equals(LOADING_PLEASE_WAIT)) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
        if (knownArchetypes.getSelectionIndex() == -1) {
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

    @Override
    public void indexAdded(IRepository repository) {

    }

    @Override
    public void indexChanged(IRepository repository) {

        Activator.getDefault().getPluginLogger()
                .trace("Reloading archetypes as index for repository {0} has changed", repository);

        try {
            new RefreshArchetypesRunnable().run(new NullProgressMonitor());
        } catch (final InvocationTargetException e) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (isCurrentPage()) {
                        setErrorMessage("Failed refreshing archetypes : " + e.getCause().getMessage());
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void indexRemoved(IRepository repository) {

    }

    @Override
    public void indexUpdating(IRepository repository) {

    }

    class RefreshArchetypesRunnable implements IRunnableWithProgress {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            Logger logger = Activator.getDefault().getPluginLogger();

            monitor.beginTask("Discovering archetypes...", 5);
            ArchetypeManager manager = MavenPluginActivator.getDefault().getArchetypeManager();
            monitor.worked(1);

            // optionally allow the parent to install any archetypes
            getWizard().installArchetypes();

            Collection<ArchetypeCatalogFactory> archetypeCatalogs = manager.getArchetypeCatalogs();
            monitor.worked(2);
            ArrayList<Archetype> candidates = new ArrayList<>();
            for (ArchetypeCatalogFactory catalogFactory : archetypeCatalogs) {
                try {
                    ArchetypeCatalog catalog = catalogFactory.getArchetypeCatalog();
                    @SuppressWarnings("unchecked")
                    java.util.List<Archetype> arcs = catalog.getArchetypes();

                    logger.trace("Catalog factory {0} provided {1} archetypes", catalogFactory,
                            arcs != null ? arcs.size() : 0);

                    if (arcs != null) {
                        candidates.addAll(arcs);
                    }
                } catch (CoreException ce) {
                    throw new InvocationTargetException(ce);
                }
            }
            monitor.worked(1);
            boolean changed = false;

            logger.trace("Considering {0} archetypes from {1} archetype catalogs", candidates.size(),
                    archetypeCatalogs.size());

            for (Archetype candidate : candidates) {
                if (getWizard().acceptsArchetype(candidate)) {
                    String key = keyFor(candidate);
                    Archetype old = archetypesMap.put(key, candidate);

                    logger.trace("Registered archetype {0}", candidate);

                    if (old == null || !old.equals(candidate)) {
                        changed = true;
                    }

                    logger.trace("Old archetype was {0}, changed = {1}", old, changed);
                }
            }

            monitor.worked(1);
            if (changed || archetypesMap.isEmpty()) {
                logger.trace("Triggering refresh since changed is true");
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        Set<String> keys = archetypesMap.keySet();
                        knownArchetypes.removeAll();
                        for (String aKey : keys) {
                            knownArchetypes.add(aKey);
                        }
                        knownArchetypes.pack();

                        if (knownArchetypes.getItemCount() == 0) {
                            setErrorMessage("No suitable archetypes found. Please make sure that the proper maven repositories are configured and indexes are up to date.");
                        } else {
                            knownArchetypes.select(0);
                            updateStatus(null);
                        }

                    }
                });
            }
            monitor.done();

        }
    }
}