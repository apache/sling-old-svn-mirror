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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.PropertyType;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

public class JcrEditingSupport extends EditingSupport {
    
    static enum ColumnId {
        NAME, TYPE, VALUE
    }

    private final ColumnId columnId;
    private final TableViewer tableViewer;
    private final JcrPropertiesView view;
    
    private class Field {
        private final Object element;

        Field(Object element) {
            this.element = element;
        }

        public boolean canEdit() {
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            Map.Entry me = (Entry) pd.getId();
            if (me.getKey().equals("jcr:primaryType") && (columnId==ColumnId.NAME)) {
                return false;
            }
            return true;
        }
        
        public Object getValue() {
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            JcrNode jcrNode = getNode();
            Map.Entry me = (Entry) pd.getId();
            
            switch(columnId) {
            case NAME: {
                return String.valueOf(me.getKey());
            }
            case TYPE: {
                final int propertyType = getNode().getPropertyType(getPropertyName());
                if (propertyType!=-1) {
                    return PropertyTypeSupport.indexOfPropertyType(propertyType);
                } else {
                    //TODO: otherwise hardcode to STRING
                    return PropertyTypeSupport.indexOfPropertyType(PropertyType.STRING);
                }
            }
            case VALUE: {
                final int propertyType = getNode().getPropertyType(getPropertyName());
                if ((propertyType!=-1) && (propertyType!=PropertyType.STRING)) {
                    String rawValue = String.valueOf(me.getValue());
                    int index = rawValue.indexOf("}");
                    if (index!=-1) {
                        String actualValue = rawValue.substring(index+1);
                        return actualValue;
                    }
                }
                return String.valueOf(me.getValue());
            }
            default: {
                throw new IllegalStateException("Unknown columnId: "+columnId);
            }
            }
        }
        
        public String getPropertyName() {
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            Map.Entry me = (Entry) pd.getId();
            return String.valueOf(me.getKey());
        }

        public void setValue(Object element, Object value) {
            if (getValue().equals(value)) {
                // then ignore this
                return;
            }
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            JcrNode jcrNode = getNode();
            Map.Entry me = (Entry) pd.getId();
            
            switch(columnId) {
            case NAME: {
                final String oldKey = String.valueOf(getValue());
                final String newKey = String.valueOf(value);
                Map<String, String> pseudoMap = new HashMap<String, String>();
                final String propertyValue = jcrNode.getProperties().getValue(oldKey);
                pseudoMap.put(newKey, propertyValue);
                final Entry<String, String> mapEntry = pseudoMap.entrySet().iterator().next();
                element = new TextPropertyDescriptor(mapEntry, propertyValue);
                jcrNode.renameProperty(oldKey, newKey);
                break;
            }
            case TYPE: {
                int propertyType = PropertyTypeSupport.propertyTypeOfIndex((Integer)value);
                jcrNode.changePropertyType(String.valueOf(me.getKey()), propertyType);
                break;
            }
            case VALUE: {
                jcrNode.setPropertyValue(me.getKey(), value);
                break;
            }
            }

            view.refreshContent();
        }
    }
    
    private class NewRowField extends Field {

        private final NewRow newRow;

        NewRowField(NewRow newRow) {
            super(newRow);
            this.newRow = newRow;
        }
        
        @Override
        public boolean canEdit() {
            return true;
        }
        
        @Override
        public Object getValue() {
            if (columnId==ColumnId.NAME) {
                return newRow.getName();
            } else if (columnId==ColumnId.VALUE) {
                return newRow.getValue();
            } else if (columnId==ColumnId.TYPE) {
                return newRow.getType();
            } else {
                return null;
            }
        }
        
        @Override
        public String getPropertyName() {
            return String.valueOf(newRow.getName());
        }
        
        @Override
        public void setValue(Object element, Object value) {
            if (getValue().equals(value)) {
                // then ignore this
                return;
            }
            if (columnId==ColumnId.NAME) {
                newRow.setName(String.valueOf(value));
            } else if (columnId==ColumnId.VALUE) {
                newRow.setValue(String.valueOf(value));
            } else if (columnId==ColumnId.TYPE) {
                newRow.setType(1);
            } else {
                // otherwise non-editable
                return;
            }
            handleNewRowUpdate(newRow);
        }
    }
    
    private class DecimalValidator implements ICellEditorValidator {

        private final CellEditor editor;

        DecimalValidator(CellEditor editor) {
            this.editor = editor;
        }
        
        @Override
        public String isValid(Object value) {
            Control cn = editor.getControl();
            TableViewer tw = tableViewer;
            Color red = new Color(Display.getCurrent(), new RGB(255, 100, 100));
            cn.setBackground(red);
            return null;
        }
        
    }

    public JcrEditingSupport(JcrPropertiesView view, TableViewer viewer, ColumnId columnType) {
        super(viewer);
        this.view = view;
        this.columnId = columnType;
        this.tableViewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        switch(columnId) {
        case NAME: {
            // no validator needed - any string is OK
            return new TextCellEditor(tableViewer.getTable());
        }
        case TYPE: {
            // using a dropdown editor
            final ComboBoxCellEditor editor = new ComboBoxCellEditor(tableViewer.getTable(), 
                    PropertyTypeSupport.SUPPORTED_PROPERTY_TYPES, SWT.READ_ONLY);
            return editor;
        }
        case VALUE: {
            CellEditor editor = new TextCellEditor(tableViewer.getTable());
            // value might require a validator depending on the property type
            Field field = asField(element);
            int propertyType = getNode().getPropertyType(field.getPropertyName());
            switch(propertyType) {
            case PropertyType.STRING:
            case PropertyType.NAME: {
                // no validator needed, any string is OK (for now)
                //TODO: check jcr rules for name
                break;
            }
            case PropertyType.DECIMAL: {
                editor.setValidator(new DecimalValidator(editor));
                break;
            }
            default: {
                // for the rest, no check implemented yet
                //TODO
                break;
            }
            }
            return editor;
        }
        default: {
            throw new IllegalStateException("Unknown columnId: "+columnId);
        }
        }
    }

    @Override
    protected boolean canEdit(Object element) {
        return asField(element).canEdit();
    }
    
    private Field asField(Object element) {
        if (element instanceof NewRow) {
            return new NewRowField((NewRow)element);
        } else {
            return new Field(element);
        }
    }

    @Override
    protected Object getValue(Object element) {
        return asField(element).getValue();
    }

    @Override
    protected void setValue(Object element, Object value) {
        Field field = asField(element);
        if (!field.canEdit()) {
            return;
        }
        field.setValue(element, value);
    }

    void handleNewRowUpdate(NewRow newRow) {
        if (newRow.isComplete()) {
            tableViewer.remove(newRow);
            JcrNode jcrNode = (JcrNode)tableViewer.getInput();
            jcrNode.addProperty(String.valueOf(newRow.getName()), String.valueOf(newRow.getValue()));
            view.refreshContent();
        } else {
            tableViewer.update(newRow, null);
        }
    }

    private JcrNode getNode() {
        return (JcrNode)tableViewer.getInput();
    }
    
}
