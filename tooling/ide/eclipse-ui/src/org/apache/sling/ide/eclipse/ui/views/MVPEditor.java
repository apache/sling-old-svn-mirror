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

import java.util.ArrayList;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrProperty;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class MVPEditor extends Dialog {

    private final JcrProperty property;
    private java.util.List<Line> lines = new ArrayList<>();
    private TableViewer viewer;
    private final Color greyColor;
    
    
    private class Line {

        private String value;
        
        Line(String value) {
            this.value = value;
        }
        
        void setValue(String value) {
            this.value = value;
        }
        
        String getValue() {
            return value;
        }
    }

    protected MVPEditor(Shell parentShell, JcrProperty property) {
        super(parentShell);
        this.property = property;
        if (!property.isMultiple()) {
            throw new IllegalArgumentException("Property "+property.getName()+" is not a Multi-Value Property");
        }
        greyColor = new Color(parentShell.getDisplay(), 100, 100, 100);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Modify multi value property");
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        // now add the node type dropbox-combo
        Composite header = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.numColumns = 3;
        header.setLayout(layout);
        
        Label label = new Label(header, SWT.WRAP);
        label.setText("Modify property "+property.getName()+":");
        GridData data = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_CENTER);
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
        label.setLayoutData(data);
        label.setFont(parent.getFont());
        
        ToolBar buttonBar = new ToolBar(header, SWT.NONE);
        ToolItem invisible = new ToolItem(buttonBar, SWT.NONE);
        
        ToolItem plus = new ToolItem(buttonBar, SWT.NONE);
        plus.setImage(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_OBJ_ADD).createImage());
        plus.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                plus();
            }
        });
        
        final ToolItem minus = new ToolItem(buttonBar, SWT.NONE);
        minus.setImage(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());
        minus.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                minus();
            }
        });
        minus.setEnabled(false);

        Composite tableParent = new Composite(composite, SWT.NONE);
        final GridData layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.heightHint = 150;
        tableParent.setLayoutData(layoutData);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableParent.setLayout(tableLayout);
        viewer = new TableViewer(tableParent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER );
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        
        // accessing property here directly, instead of going via (JcrProperty)inputElement;
        String[] rawLines = property.getValuesAsString();
        // convert raw lines to Line objects for easier editing management
        for (int i = 0; i < rawLines.length; i++) {
            lines.add(new Line(rawLines[i]));
        }
        
        viewer.setContentProvider(new IStructuredContentProvider() {
            
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            
            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return lines.toArray();
            }
        });
        
        TableViewerColumn column0 = new TableViewerColumn(viewer, SWT.NONE);
        column0.getColumn().setText("Type");
        column0.getColumn().setResizable(true);
        column0.getColumn().setWidth(100);
        tableLayout.setColumnData(column0.getColumn(), new ColumnWeightData(20, 100));
        column0.setLabelProvider(new CellLabelProvider() {
            
            @Override
            public void update(ViewerCell cell) {
                try{
                    cell.setText(property.getTypeAsString());
                    cell.setForeground(greyColor);
                } catch(Exception e) {
                    cell.setText("n/a");
                    cell.setForeground(greyColor);
                }
            }
        });

        TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.NONE);
        column1.getColumn().setText("Value");
        column1.getColumn().setResizable(true);
        column1.getColumn().setWidth(200);
        tableLayout.setColumnData(column1.getColumn(), new ColumnWeightData(80, 200));

        column1.setLabelProvider(new CellLabelProvider() {
            
            @Override
            public void update(ViewerCell cell) {
                Line line = (Line) cell.getElement();
                cell.setText(line.getValue());
            }
        });
        column1.setEditingSupport(new EditingSupport(viewer) {
            
            @Override
            protected void setValue(Object element, Object value) {
                Line line = (Line)element;
                line.setValue(String.valueOf(value));
                // trigger a refresh:
                viewer.setInput(property);
            }
            
            @Override
            protected Object getValue(Object element) {
                final Line line = (Line)element;
                final String value = line.getValue();
                System.out.println("Value="+value);
                return value;
            }
            
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor(viewer.getTable());
            }
            
            @Override
            protected boolean canEdit(Object element) {
                // all values are editable
                return true;
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                final ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection iss = (IStructuredSelection)selection;
                    if (iss.isEmpty()) {
                        minus.setEnabled(false);
                    } else {
                        minus.setEnabled(true);
                    }
                } else {
                    minus.setEnabled(false);
                }
            }
        });
        
        viewer.setInput(property);
        
        return composite;
    }
    
    protected void minus() {
        ISelection selection = viewer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection iss = (IStructuredSelection)selection;
            if (!iss.isEmpty()) {
                Object element = iss.getFirstElement();
                if (element instanceof Line) {
                    Line line = (Line)element;
                    lines.remove(line);
                }
            }
        }
        viewer.setInput(property);
    }

    protected void plus() {
        Line newLine = new Line("");
        lines.add(newLine);
        viewer.setInput(property);
    }

    public String[] getLines() {
        final String[] result = new String[lines.size()];
        for(int i=0; i<result.length; i++) {
            result[i] = lines.get(i).getValue();
        }
        return result;
    }
    
    @Override
    protected void okPressed() {
        boolean active = viewer.isCellEditorActive();
        if (active) {
            // force applyEditorValue to be called
            viewer.setInput(property);
        }
        super.okPressed();
    }
}
