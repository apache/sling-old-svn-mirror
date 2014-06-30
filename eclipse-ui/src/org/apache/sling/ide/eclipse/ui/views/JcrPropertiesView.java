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
package org.apache.sling.ide.eclipse.ui.views;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDirManager;
import org.apache.sling.ide.eclipse.ui.nav.model.UpdateHandler;
import org.apache.sling.ide.eclipse.ui.views.JcrEditingSupport.ColumnId;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CellNavigationStrategy;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

public class JcrPropertiesView extends ViewPart {

    private static final String TITLE_FONT = "org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyTitle"; //$NON-NLS-1$

    private TableViewer viewer;
	private Action insertAction;
	private Action deleteAction;
	private Action doubleClickAction;

    private Label titleLabel;

    private ISelectionListener listener;

    private Action showInEditorAction;

    private Action pinAction;

    private JcrNode lastInput;

    private Action synchedAction;

    private String lastEditedOldPropertyName;

    private String lastEditedNewPropertyName;

    private ColumnId lastEditedColumnId;

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
		    if (parent instanceof JcrNode) {
		        JcrNode node = (JcrNode)parent;
		        final IPropertyDescriptor[] pds = node.getProperties().getPropertyDescriptors();
                return pds;
		    } else {
		        return new String[] {};
		    }
		}
	}
	/**
	 * The constructor.
	 */
	public JcrPropertiesView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
	    SyncDirManager.registerUpdateListener(new UpdateHandler() {
            
            @Override
            public void syncDirUpdated(SyncDir syncDir) {
                refreshContent();
            }
        });
	    
	    Composite c = new Composite(parent, SWT.NONE);
	    final GridLayout gridLayout = new GridLayout(1, true);
        c.setLayout(gridLayout);
	    
        titleLabel = new Label(c, SWT.WRAP);
        titleLabel.setText("");
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        titleLabel.setLayoutData(data);
        
        Label horizontalLine = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData(GridData.FILL_HORIZONTAL);
        horizontalLine.setLayoutData(data);
        Font font;
        if (! JFaceResources.getFontRegistry().hasValueFor(TITLE_FONT)) {
            FontData[] fontData = JFaceResources.getFontRegistry().getBold(
                    JFaceResources.DEFAULT_FONT).getFontData();
            /* title font is 2pt larger than that used in the tabs. */  
            fontData[0].setHeight(fontData[0].getHeight() + 2);
            JFaceResources.getFontRegistry().put(TITLE_FONT, fontData);
        }
        font = JFaceResources.getFont(TITLE_FONT);
        titleLabel.setFont(font);

        Composite tableParent = new Composite(c, SWT.NONE);
        tableParent.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableParent.setLayout(tableLayout);
        
        viewer = new TableViewer(tableParent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.HIDE_SELECTION | SWT.FULL_SELECTION);
        TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
                viewer, new FocusCellOwnerDrawHighlighter(viewer), new CellNavigationStrategy());
        ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
                viewer){

            @Override
            protected boolean isEditorActivationEvent(
                    ColumnViewerEditorActivationEvent event) {
                resetLastValueEdited();
                return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
                        || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
                        || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
                        || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
            }
        };
        int features = ColumnViewerEditor.TABBING_HORIZONTAL
                | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                | ColumnViewerEditor.TABBING_VERTICAL
                | ColumnViewerEditor.KEYBOARD_ACTIVATION
                | ColumnViewerEditor.KEEP_EDITOR_ON_DOUBLE_CLICK;
        TableViewerEditor.create(viewer, focusCellManager, actSupport, features);
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.setContentProvider(new ViewContentProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                final ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection iss = (IStructuredSelection)selection;
                    if (iss.isEmpty()) {
                        deleteAction.setEnabled(false);
                    } else {
                        deleteAction.setEnabled(true);
                    }
                } else {
                    deleteAction.setEnabled(false);
                }
            }
        });
		
		CellLabelProvider clp = new JcrCellLabelProvider(viewer);

		TableViewerColumn column0 = new TableViewerColumn(viewer, SWT.NONE);
		column0.getColumn().setText("Name");
		column0.getColumn().setResizable(true);
		column0.getColumn().setWidth(200);
        tableLayout.setColumnData(column0.getColumn(), new ColumnWeightData(30, 150));

        final TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.NONE);
        column1.getColumn().setText("Type");
        column1.getColumn().setResizable(true);
        column1.getColumn().setWidth(300);
        column1.setLabelProvider(clp);
        tableLayout.setColumnData(column1.getColumn(), new ColumnWeightData(10, 80));
        
        final TableViewerColumn column2 = new TableViewerColumn(viewer, SWT.NONE);
        column2.getColumn().setText("Value");
        column2.getColumn().setResizable(true);
        column2.getColumn().setWidth(300);
        tableLayout.setColumnData(column2.getColumn(), new ColumnWeightData(70, 250));
        
        final TableViewerColumn column3 = new TableViewerColumn(viewer, SWT.NONE);
        column3.getColumn().setText("Protected");
        column3.getColumn().setResizable(true);
        column3.getColumn().setWidth(300);
        column3.setLabelProvider(clp);
        tableLayout.setColumnData(column3.getColumn(), new ColumnWeightData(5, 57));

        final TableViewerColumn column4 = new TableViewerColumn(viewer, SWT.NONE);
        column4.getColumn().setText("Mandatory");
        column4.getColumn().setResizable(true);
        column4.getColumn().setWidth(300);
        column4.setLabelProvider(clp);
        tableLayout.setColumnData(column4.getColumn(), new ColumnWeightData(5, 62));

        final TableViewerColumn column5 = new TableViewerColumn(viewer, SWT.NONE);
        column5.getColumn().setText("Multiple");
        column5.getColumn().setResizable(true);
        column5.getColumn().setWidth(300);
        column5.setLabelProvider(clp);
        tableLayout.setColumnData(column5.getColumn(), new ColumnWeightData(5, 82));

        final TableViewerColumn column6 = new TableViewerColumn(viewer, SWT.NONE);
        column6.getColumn().setText("Auto Created");
        column6.getColumn().setResizable(true);
        column6.getColumn().setWidth(300);
        column6.setLabelProvider(clp);
        tableLayout.setColumnData(column6.getColumn(), new ColumnWeightData(5, 77));

        column0.setLabelProvider(clp);
        column0.setEditingSupport(new JcrEditingSupport(this, viewer, ColumnId.NAME));

        column1.setLabelProvider(clp);
        column1.setEditingSupport(new JcrEditingSupport(this, viewer, ColumnId.TYPE));

        column2.setLabelProvider(clp);
		column2.setEditingSupport(new JcrEditingSupport(this, viewer, ColumnId.VALUE));
	
        column5.setEditingSupport(new JcrEditingSupport(this, viewer, ColumnId.MULTIPLE));
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "org.apache.sling.ide.eclipse-ui.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		listener = new ISelectionListener() {
            
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection) {
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection iss = (IStructuredSelection)selection;
                    Object firstElem = iss.getFirstElement();
                    if (firstElem instanceof JcrNode) {
                        JcrNode jcrNode = (JcrNode)firstElem;
                        setInput(jcrNode);
                        return;
                    }
                }
            }
        };
        getViewSite().getPage().addSelectionListener(listener);
        final ISelection selection = getViewSite().getPage().getSelection();
        Display.getCurrent().asyncExec(new Runnable() {

            @Override
            public void run() {
                listener.selectionChanged(null, selection);
            }
            
        });
	}
	
    void resetLastValueEdited() {
        this.lastEditedOldPropertyName = null;
        this.lastEditedNewPropertyName = null;
        this.lastEditedColumnId = null;
    }
	
	void setLastValueEdited(String oldPropertyName, String newPropertyName, ColumnId columnId) {
	    this.lastEditedOldPropertyName = oldPropertyName;
	    this.lastEditedNewPropertyName = newPropertyName;
	    this.lastEditedColumnId = columnId;
	}
	
	@Override
	public void dispose() {
	    super.dispose();
	    if (listener!=null) {
	        getViewSite().getPage().removeSelectionListener(listener);
	        listener = null;
	    }
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				JcrPropertiesView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(insertAction);
//		manager.add(new Separator());
		manager.add(deleteAction);
        manager.add(showInEditorAction);
        manager.add(pinAction);
        manager.add(synchedAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(insertAction);
        manager.add(deleteAction);
        manager.add(showInEditorAction);
        manager.add(pinAction);
        manager.add(synchedAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(insertAction);
		manager.add(deleteAction);
        manager.add(showInEditorAction);
        manager.add(pinAction);
        manager.add(synchedAction);
	}

	private void makeActions() {
		insertAction = new Action() {
			public void run() {
			    NewRow newRow = new NewRow();
			    viewer.add(newRow);
			    viewer.getTable().setTopIndex(viewer.getTable().getItemCount());
			    viewer.getTable().select(viewer.getTable().getItemCount()-1);
			    viewer.editElement(newRow, 0);
			}
		};
		insertAction.setText("Insert");
		insertAction.setToolTipText("Insert a property");
		insertAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		
		deleteAction = new Action() {
			public void run() {
			    if (viewer.getSelection().isEmpty()) {
			        return;
			    }
			    ISelection sel = viewer.getSelection();
			    if (sel instanceof IStructuredSelection) {
			        IStructuredSelection iss = (IStructuredSelection)sel;
			        Object elem = iss.getFirstElement();
			        if (elem instanceof IPropertyDescriptor) {
			            IPropertyDescriptor pd = (IPropertyDescriptor)elem;
			            JcrNode jcrnode = (JcrNode)viewer.getInput();
			            jcrnode.deleteProperty(pd.getDisplayName());
			            refreshContent();
			        }
			    }
			}
		};
		deleteAction.setText("Delete");
		deleteAction.setToolTipText("Delete a proeprty");
		deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		doubleClickAction = new Action() {
			public void run() {
			    //TODO doesn't do anything currently..
				ISelection selection = viewer.getSelection();
//				Object obj = ((IStructuredSelection)selection).getFirstElement();
//				showMessage("Double-click detected on "+obj.toString());
			}
		};
		
		showInEditorAction = new Action() {
		    public void run() {
		        
		        JcrNode node = (JcrNode)viewer.getInput();
                final IFile file = node.getFileForEditor();
                if (file!=null) {
                    try {
                        IDE.openEditor(getViewSite().getPage(), file, true);
                    } catch (PartInitException e) {
                        e.printStackTrace(System.out);
                    }
                }
		    }
		};
		showInEditorAction.setText("Show in editor");
		showInEditorAction.setToolTipText("Show underlying vault file in editor");
		showInEditorAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		
		pinAction = new Action("pin to selection", IAction.AS_CHECK_BOX) {
		    public void run() {
		        if (!pinAction.isChecked()) {
		            // unpin
		            setContentDescription("");
		            setInput(lastInput);
		        } else {
		            setContentDescription("[pinned]");
		        }
		        // toggle state of syncedAction accordingly
		        synchedAction.setEnabled(!pinAction.isChecked());
		    }
		};
		pinAction.setText("Pin to selection");
		pinAction.setToolTipText("Pin this property view to the current selection");
		pinAction.setImageDescriptor(WorkbenchImages
                .getImageDescriptor(IWorkbenchGraphicConstants.IMG_ETOOL_PIN_EDITOR));
		pinAction.setDisabledImageDescriptor(WorkbenchImages
                .getImageDescriptor(IWorkbenchGraphicConstants.IMG_ETOOL_PIN_EDITOR_DISABLED));
		pinAction.setChecked(false);
		
		synchedAction = new Action("Link with Editor and selection", IAction.AS_CHECK_BOX) {
            public void run() {
                // toggle state of pinAction accordingly
                pinAction.setEnabled(!synchedAction.isChecked());
            }
        };
        synchedAction.setText("Link with Editor and selection");
        synchedAction.setToolTipText("Link with Editor and selection");
        synchedAction.setImageDescriptor(WorkbenchImages
                .getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
        synchedAction.setDisabledImageDescriptor(WorkbenchImages
                .getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED_DISABLED));
        synchedAction.setChecked(true);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

    void refreshContent() {
        final Object input = viewer.getInput();
        if (input!=null && input instanceof JcrNode) {
            JcrNode jcrnode = (JcrNode)input;
            SyncDir syncDir = jcrnode.getSyncDir();
            JcrNode newnode = syncDir.getNode(jcrnode.getJcrPath());
            if (newnode!=null) {
                viewer.setInput(newnode);
                if (lastEditedNewPropertyName!=null) {
                    // set the selection/focus accordingly
                    
                    for(int i=0;;i++) {
                        Object element = viewer.getElementAt(i);
                        if (element==null) {
                            break;
                        }
                        final IPropertyDescriptor pd = (IPropertyDescriptor) element;
                        Map.Entry<String,Object> me = (Entry<String, Object>) pd.getId();
                        String key = me.getKey();
                        if (lastEditedNewPropertyName.equals(key)) {
                            // set the selection to this one
                            final int column;
                            if (lastEditedColumnId==ColumnId.NAME) {
                                column = 0;
                            } else if (lastEditedColumnId==ColumnId.TYPE) {
                                column = 1;
                            } else if (lastEditedColumnId==ColumnId.VALUE) {
                                column = 2;
                            } else if (lastEditedColumnId==ColumnId.MULTIPLE) {
                                column = 5;
                            } else {
                                throw new IllegalStateException("Unknown columnId="+lastEditedColumnId);
                            }
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    try{
                                        // edit
                                        viewer.editElement(pd, column);
                                        // and cancel immediately - to get the selection right
                                        viewer.cancelEditing();
                                    } catch(Exception e) {
                                        Activator.getDefault().getPluginLogger().error("Exception occured on edit/cancel: "+e, e);
                                    }
                                }
                                
                            });
                            break;
                        }
                    }
                    
                }
            }
        }
    }

    private void setInput(JcrNode jcrNode) {
        // reset the last edited values..:
        resetLastValueEdited();

        if (pinAction.isChecked()) {
            lastInput = jcrNode;
        } else {
            if (synchedAction.isChecked()) {
                getViewSite().getPage().bringToTop(this);
            }
            viewer.setInput(jcrNode);
            titleLabel.setText(jcrNode.getJcrPath());
            insertAction.setEnabled(!jcrNode.getPrimaryType().equals("nt:folder"));
            deleteAction.setEnabled(false);
            showInEditorAction.setEnabled(jcrNode.getFileForEditor()!=null);
        }
    }

}