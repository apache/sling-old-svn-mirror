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

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellNavigationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings("restriction")
public class ArchetypeParametersWizardPage extends WizardPage {
	
	private static final String KEY_PROPERTY = "key";

	private static final String VALUE_PROPERTY = "value";

	private Text groupId;
	
	private Text artifactId;
	
	private Text javaPackage;
	
	private boolean javaPackageModified;

	private final AbstractNewMavenBasedSlingApplicationWizard parent;

	private TableViewer propertiesViewer;

	private Table propertiesTable;

	private List<RequiredProperty> properties;

	private Text version;
	
	public ArchetypeParametersWizardPage(AbstractNewMavenBasedSlingApplicationWizard parent) {
		super("archetypeParametersPage");
		this.parent = parent;
		setTitle("Configure Archetype Properties");
        setDescription("This step configures the archetype properties");
		setImageDescriptor(parent.getLogo());
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		
		Label label = new Label(container, SWT.NULL);
		label.setText("&Group Id:");
		groupId = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		groupId.setLayoutData(gd);
		groupId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				if (!javaPackageModified) {
					if (artifactId.getText().length()==0) {
						javaPackage.setText(getDefaultJavaPackage(groupId.getText(), ""));
					} else {
						javaPackage.setText(getDefaultJavaPackage(groupId.getText(), artifactId.getText()));
					}
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Artifact Id:");

		artifactId = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		artifactId.setLayoutData(gd);
		artifactId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				if (javaPackageModified) {
					return;
				}
				if (groupId.getText().length()==0) {
					javaPackage.setText(getDefaultJavaPackage("", artifactId.getText()));
				} else {
					javaPackage.setText(getDefaultJavaPackage(groupId.getText(), artifactId.getText()));
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Version:");

		version = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		version.setLayoutData(gd);
		version.setText("0.0.1-SNAPSHOT");
		version.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Package:");

		javaPackage = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		javaPackage.setLayoutData(gd);
		javaPackageModified = false;
		javaPackage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				javaPackageModified = true;
			}
		});
		javaPackage.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		label = new Label(container, SWT.NULL);
		gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
		label.setLayoutData(gd);
		label.setText("&Parameters:");


	    propertiesViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
	    propertiesTable = propertiesViewer.getTable();
	    propertiesTable.setLinesVisible(true);
	    propertiesTable.setHeaderVisible(true);
	    propertiesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
	    
	    CellNavigationStrategy strategy = new CellNavigationStrategy();
		TableViewerFocusCellManager focusCellMgr = new TableViewerFocusCellManager(propertiesViewer,
	    		new FocusCellOwnerDrawHighlighter(propertiesViewer),
	    		strategy);
	    
	    TableColumn propertiesTableNameColumn = new TableColumn(propertiesTable, SWT.NONE);
	    propertiesTableNameColumn.setWidth(130);
	    propertiesTableNameColumn.setText("Name");

	    TableColumn propertiesTableValueColumn = new TableColumn(propertiesTable, SWT.NONE);
	    propertiesTableValueColumn.setWidth(230);
	    propertiesTableValueColumn.setText("Value");

	    propertiesViewer.setColumnProperties(new String[] {KEY_PROPERTY, VALUE_PROPERTY});

	    propertiesViewer.setCellEditors(new CellEditor[] {new TextCellEditor(propertiesTable, SWT.NONE),
	        new TextCellEditor(propertiesTable, SWT.NONE)});
	    propertiesViewer.setCellModifier(new ICellModifier() {
	      public boolean canModify(Object element, String property) {
	        return true;
	      }

	      public void modify(Object element, String property, Object value) {
	        if(element instanceof TableItem) {
	          ((TableItem) element).setText(getTextIndex(property), String.valueOf(value));
	          dialogChanged();
	        }
	      }

	      public Object getValue(Object element, String property) {
	        if(element instanceof TableItem) {
	          return ((TableItem) element).getText(getTextIndex(property));
	        }
	        return null;
	      }
	    });		
		
		initialize();
		setPageComplete(false);
		setControl(container);
	}
	
	protected int getTextIndex(String property) {
		if (KEY_PROPERTY.equals(property)) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
		}
	}

	@SuppressWarnings("unchecked")
	private void initialize() {
		if (propertiesTable==null) {
			return;
		}
		
		Archetype archetype = parent.getChooseArchetypePage().getSelectedArchetype();
		if (archetype==null) {
			return;
		}
		
        try {
        	ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
        	ArtifactRepository remoteArchetypeRepository = archetypeManager.getArchetypeRepository(archetype);
			properties = (List<RequiredProperty>) archetypeManager.getRequiredProperties(archetype, remoteArchetypeRepository, null);
			
			Table table = propertiesViewer.getTable();
			table.setItemCount(properties.size());
			int i = 0;
			for (Iterator<RequiredProperty> it = properties.iterator(); it.hasNext();) {
				RequiredProperty rp = it.next();
				TableItem item = table.getItem(i++);
				if (!rp.getKey().equals(item.getText())) {
					// then create it - otherwise, reuse it
					item.setText(0, rp.getKey());
					item.setText(1, "");
					item.setData(item);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not process archetype: "+e.getMessage(), e);
		}

	}


	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		if (groupId.getText().length()==0) {
			updateStatus("group Id must be specified");
			return;
		}
		if (artifactId.getText().length()==0) {
			updateStatus("artifact Id must be specified");
			return;
		}
		if (version.getText().length()==0) {
			updateStatus("version must be specified");
			return;
		}
		if (javaPackage.getText().length()==0) {
			updateStatus("package must be specified");
			return;
		}
		int cnt = propertiesTable.getItemCount();
		for(int i=0; i<cnt; i++) {
			TableItem item = propertiesTable.getItem(i);
			if (item.getText(1).length()==0) {
				updateStatus(item.getText(0)+" must be specified");
				return;
			}
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getGroupId() {
		return groupId.getText();
	}

	public String getArtifactId() {
		return artifactId.getText();
	}

	public String getVersion() {
		return version.getText();
	}

	public String getJavaPackage() {
		return javaPackage.getText();
	}

	public Properties getProperties() {
		int cnt = propertiesTable.getItemCount();
		Properties p = new Properties();
		for(int i=0; i<cnt; i++) {
			TableItem item = propertiesTable.getItem(i);
			p.put(item.getText(0), item.getText(1));
		}
		return p;
	}
	
	public static String getDefaultJavaPackage(String groupId, String artifactId) {
		String name = (artifactId.isEmpty()) ? groupId : groupId+"."+artifactId;
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(name.replaceAll("-", "_"), ".");
		while(st.hasMoreTokens()) {
			String part = st.nextToken();
			while(part.length()>0 && !Character.isJavaIdentifierStart(part.charAt(0))) {
				part = part.substring(1);
			}
			if (part.length()==0) {
				continue;
			}
			if (sb.length()!=0) {
				sb.append(".");
			}
			sb.append(part);
		}
		return sb.toString();
	}

}