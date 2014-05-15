package org.apache.sling.ide.eclipse.ui.views;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

public class JcrEditingSupport extends EditingSupport {
    
    static enum ColumnType {
        NAME, VALUE
    }

    private final ColumnType columnType;
    private final TableViewer tableViewer;
    private final JcrPropertiesView view;
    
    private class Field {
        private final Object element;

        Field(Object element) {
            this.element = element;
        }

        public boolean canEdit() {
            if (element instanceof NewRow) {
                return true;
            }
            if (columnType==ColumnType.NAME) {
                return false;
            }
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            Map.Entry me = (Entry) pd.getId();
            if (me.getKey().equals("jcr:primaryType")) {
                return false;
            }
            return true;
        }
        
        public Object getValue() {
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            JcrNode jcrNode = getNode();
            Map.Entry me = (Entry) pd.getId();
            
            return String.valueOf(me.getValue());
        }

        public void setValue(Object element, Object value) {
            if (getValue().equals(value)) {
                // then ignore this
                return;
            }
            IPropertyDescriptor pd = (IPropertyDescriptor) element;
            JcrNode jcrNode = getNode();
            Map.Entry me = (Entry) pd.getId();

            jcrNode.setPropertyValue(me.getKey(), value);

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
            return (columnType==ColumnType.NAME || columnType==ColumnType.VALUE);
        }
        
        @Override
        public Object getValue() {
            if (columnType==ColumnType.NAME) {
                return newRow.getName();
            } else if (columnType==ColumnType.VALUE) {
                return newRow.getValue();
            } else {
                return null;
            }
        }
        
        @Override
        public void setValue(Object element, Object value) {
            if (getValue().equals(value)) {
                // then ignore this
                return;
            }
            if (columnType==ColumnType.NAME) {
                newRow.setName(String.valueOf(value));
            } else if (columnType==ColumnType.VALUE) {
                newRow.setValue(String.valueOf(value));
            } else {
                // otherwise non-editable
                return;
            }
            handleNewRowUpdate(newRow);
        }
    }

    public JcrEditingSupport(JcrPropertiesView view, TableViewer viewer, ColumnType columnType) {
        super(viewer);
        this.view = view;
        this.columnType = columnType;
        this.tableViewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        return new TextCellEditor(tableViewer.getTable());
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
