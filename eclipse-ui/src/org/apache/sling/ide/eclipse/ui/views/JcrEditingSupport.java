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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.PropertyType;

import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrProperty;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrTextPropertyDescriptor;
import org.apache.sling.ide.eclipse.ui.nav.model.ModifiableProperties;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

public class JcrEditingSupport extends EditingSupport {
    
    static enum ColumnId {
        NAME, TYPE, VALUE, MULTIPLE
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
            if (me.getKey().equals("jcr:primaryType")) {
                return columnId==ColumnId.VALUE;
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
                String rawValue = String.valueOf(me.getValue());
                int index = rawValue.indexOf("}");
                if (index!=-1) {
                    rawValue = rawValue.substring(index+1);
                }
                if ((propertyType!=-1) && (propertyType==PropertyType.BOOLEAN)) {
                    try{
                        if (Boolean.parseBoolean(String.valueOf(rawValue))) {
                            return 1;
                        } else {
                            return 0;
                        }
                    } catch(Exception e) {
                        return 0;
                    }
                } else if ((propertyType!=-1) && (propertyType!=PropertyType.STRING)) {
                    return rawValue;
                }
                return String.valueOf(me.getValue());
            }
            case MULTIPLE: {
                boolean isMultiple = getNode().getProperty(getPropertyName()).isMultiple();
                return isMultiple ? 1 : 0;
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
            JcrTextPropertyDescriptor pd = (JcrTextPropertyDescriptor) element;
            JcrNode jcrNode = getNode();
            Map.Entry me = (Entry) pd.getId();
            
            switch(columnId) {
            case NAME: {
                final String oldKey = String.valueOf(getValue());
                final String newKey = String.valueOf(value);
                pd.setNewPropertyName(newKey);
                Map<String, String> pseudoMap = new HashMap<>();
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
                try{
                    final JcrProperty property = getNode().getProperty(getPropertyName());
                    final int propertyType = property.getType();
                    String encodedValue;
                    if (property.isMultiple()) {
                        Object[] values = (Object[])value;
                        encodedValue = "";
                        for (int i = 0; i < values.length; i++) {
                            Object aValue = values[i];
                            String aValueAsString = PropertyTypeSupport.encodeValueAsString(aValue, propertyType);
                            if (i==0) {
                                encodedValue = aValueAsString;
                            } else {
                                encodedValue = encodedValue+","+aValueAsString;
                            }
                        }
                        encodedValue = "["+encodedValue+"]";
                    } else {
                        encodedValue = PropertyTypeSupport.encodeValueAsString(value, propertyType);
                    }
                    if (propertyType!=PropertyType.STRING && propertyType!=PropertyType.NAME) {
                        encodedValue = "{"+PropertyType.nameFromValue(propertyType)+"}"+encodedValue;
                    }
                    jcrNode.setPropertyValue(me.getKey(), encodedValue);
                } catch(Exception e) {
                    // emergency fallback
                    jcrNode.setPropertyValue(me.getKey(), String.valueOf(value));
                }
                break;
            }
            case MULTIPLE: {
                if (!(value instanceof Integer)) {
                    // value must be an integer
                    return;
                }
                final Integer newIsMultipleValue = (Integer) value;
                final boolean newIsMultiple = newIsMultipleValue==1;
                final JcrProperty property = getNode().getProperty(getPropertyName());
                final boolean oldIsMultiple = property.isMultiple();
                if (newIsMultiple==oldIsMultiple) {
                    // then nothing is to be done
                    return;
                }
                final String oldPropertyValue = getNode().getProperties().getValue(getPropertyName());
                // split {type} prefix from value
                int cPos = oldPropertyValue.indexOf("}");
                final String prefix;
                final String rawValue;
                if (cPos==-1) {
                    prefix = "";
                    rawValue = oldPropertyValue;
                } else {
                    prefix = oldPropertyValue.substring(0, cPos+1);
                    rawValue = oldPropertyValue.substring(cPos+1);
                }
                String newValue;
                if (newIsMultiple) {
                    newValue = prefix + "[" + rawValue + "]";
                } else {
                    newValue = rawValue.substring(1, rawValue.length()-1);
                    int commaPos = newValue.indexOf(",");
                    if (commaPos!=-1) {
                        newValue = newValue.substring(0, commaPos);
                    }
                    newValue = prefix + newValue;
                }
                jcrNode.setPropertyValue(getPropertyName(), newValue);
                break;
            }
            }

            view.refreshContent();
        }

        public int getPropertyType() {
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            Map.Entry me = (Entry) pd.getId();
            String value = String.valueOf(me.getValue());
            return PropertyTypeSupport.propertyTypeOfString(value);
        }

        public String getNewPropertyName() {
            JcrTextPropertyDescriptor pd = (JcrTextPropertyDescriptor) element;
            if (pd.getNewPropertyName()!=null) {
                return pd.getNewPropertyName();
            } else {
                return getPropertyName();
            }
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
        public int getPropertyType() {
            return newRow.getType();
        }
        
        @Override
        public Object getValue() {
            if (columnId==ColumnId.NAME) {
                return newRow.getName();
            } else if (columnId==ColumnId.VALUE) {
                final int propertyType = newRow.getType();
                if (propertyType==PropertyType.BOOLEAN) {
                    try{
                        if (Boolean.parseBoolean(String.valueOf(newRow.getValue()))) {
                            return 1;
                        } else {
                            return 0;
                        }
                    } catch(Exception e) {
                        return 0;
                    }
                }
                return newRow.getValue();
            } else if (columnId==ColumnId.TYPE) {
                final int propertyType = newRow.getType();
                if (propertyType!=-1) {
                    return PropertyTypeSupport.indexOfPropertyType(propertyType);
                } else {
                    //TODO: otherwise hardcode to STRING
                    return PropertyTypeSupport.indexOfPropertyType(PropertyType.STRING);
                }
            } else if (columnId==ColumnId.MULTIPLE) {
                //TODO
                return 0;
            } else {
                return null;
            }
        }
        
        @Override
        public String getPropertyName() {
            return String.valueOf(newRow.getName());
        }
        
        @Override
        public String getNewPropertyName() {
            return getPropertyName();
        }
        
        @Override
        public void setValue(Object element, Object value) {
            if (getValue().equals(value)) {
                // then ignore this
                return;
            }
            if (columnId==ColumnId.NAME) {
                String newName = String.valueOf(value);
                ModifiableProperties props = getNode().getProperties();
                int cnt = 1;
                while(props.getValue(newName)!=null) {
                    newName = String.valueOf(value)+(cnt++);
                }
                newRow.setName(newName);
            } else if (columnId==ColumnId.VALUE) {
                newRow.setValue(PropertyTypeSupport.encodeValueAsString(value, getPropertyType()));
            } else if (columnId==ColumnId.TYPE) {
                int propertyType = PropertyTypeSupport.propertyTypeOfIndex((Integer)value);
                newRow.setType(propertyType);
            } else if (columnId==ColumnId.MULTIPLE) {
                // do nothing at the moment
                //TODO
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
        try{
            final CellEditor result = doGetCellEditor(element);
            if (result!=null) {
                Field field = asField(element);
                view.setLastValueEdited(field.getPropertyName(), field.getNewPropertyName(), columnId);
            }
            return result;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    protected CellEditor doGetCellEditor(Object element) {
        if (!canEdit(element)) {
            return null;
        }
        switch(columnId) {
        case NAME: {
            // no validator needed - any string is OK
            return new TextCellEditor(tableViewer.getTable());
        }
        case TYPE: {
            // using a dropdown editor
            final ComboBoxCellEditor editor = new ComboBoxCellEditor(tableViewer.getTable(), 
                    PropertyTypeSupport.PROPERTY_TYPES, SWT.NONE);
            editor.setActivationStyle(ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION | 
                    ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION |
                    ComboBoxCellEditor.DROP_DOWN_ON_TRAVERSE_ACTIVATION);
            return editor;
        }
        case VALUE: {
            final Field field = asField(element);
            if (getNode().getProperty(field.getPropertyName()).isMultiple()) {
                // then launch the MVPEditor instead of returning an editor here
                return new MVNCellEditor(tableViewer.getTable(), getNode(), field.getPropertyName());
            }
            if (field.getPropertyType()==PropertyType.DATE) {
                return new DateTimeCellEditor(tableViewer.getTable(), getNode(), field.getPropertyName());
            }
            if (field.getPropertyType()==PropertyType.BOOLEAN) {
                return new ComboBoxCellEditor(tableViewer.getTable(), new String[] {"false", "true"}, SWT.READ_ONLY);
            }
            CellEditor editor;
            if (field.getPropertyName().equals("jcr:primaryType")) {
                editor = new TextCellEditor(tableViewer.getTable()) {
                    @Override
                    protected Control createControl(Composite parent) {
                        Text text = (Text) super.createControl(parent);
                        Repository repository = ServerUtil.getDefaultRepository(getNode().getProject());
                        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
                        if (ntManager == null) {
                            return text;
                        }
                        try {
                            Collection<String> types = ntManager.getAllowedPrimaryChildNodeTypes(getNode().getParent().getPrimaryType());
                            SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(types.toArray(new String[0]));
                            proposalProvider.setFiltering(true);
                            ContentProposalAdapter adapter = new ContentProposalAdapter(text, new TextContentAdapter(),
                                    proposalProvider, null, null);
                            adapter.setPropagateKeys(true);
                            adapter
                                    .setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
                            return text;
                        } catch (RepositoryException e) {
                            return text;
                        }
                    }
                };
            } else {
                editor = new TextCellEditor(tableViewer.getTable());
            }
            // value might require a validator depending on the property type
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
        case MULTIPLE: {
            if (element instanceof NewRow) {
                return null;
            }
            return new ComboBoxCellEditor(tableViewer.getTable(), new String[] {"false", "true"}, SWT.READ_ONLY);
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
        String newPropertyName;
        if (columnId==ColumnId.NAME) {
            newPropertyName = String.valueOf(value);
        } else {
            newPropertyName = field.getPropertyName();
        }
        view.setLastValueEdited(field.getPropertyName(), newPropertyName, columnId);
        field.setValue(element, value);
    }

    void handleNewRowUpdate(NewRow newRow) {
        if (newRow.isComplete()) {
            tableViewer.remove(newRow);
            final JcrNode jcrNode = (JcrNode)tableViewer.getInput();
            final Integer type = newRow.getType();
            String encodeValueAsString = PropertyTypeSupport.encodeValueAsString(newRow.getValue(), type);
            if (type!=PropertyType.STRING && type!=PropertyType.NAME) {
                encodeValueAsString = "{"+PropertyType.nameFromValue(type)+"}"+encodeValueAsString;
            }
            final String propertyName = String.valueOf(newRow.getName());
            jcrNode.addProperty(propertyName, encodeValueAsString);
            view.refreshContent();
        } else {
            tableViewer.update(newRow, null);
        }
    }

    private JcrNode getNode() {
        return (JcrNode)tableViewer.getInput();
    }
    
}
