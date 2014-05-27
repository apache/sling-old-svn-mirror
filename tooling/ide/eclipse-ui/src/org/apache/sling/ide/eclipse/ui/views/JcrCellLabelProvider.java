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

import javax.jcr.PropertyType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

public class JcrCellLabelProvider extends CellLabelProvider {

    private final TableViewer viewer;
    private Font italic;
    private Font normal;
    private Font bold;
    private Color greyColor;
    private Color normalColor;

    public JcrCellLabelProvider(TableViewer viewer) {
        this.viewer = viewer;

        Display display = viewer.getControl().getDisplay();
        
        FontData fontData = viewer.getTable().getFont().getFontData()[0];
        italic = new Font(display, new FontData(fontData.getName(), fontData
                .getHeight(), SWT.ITALIC));
        normal = new Font(display, new FontData(fontData.getName(), fontData
                .getHeight(), SWT.NORMAL));
        bold = new Font(display, new FontData(fontData.getName(), fontData
                .getHeight(), SWT.BOLD));
        greyColor = new Color(display, 100, 100, 100);
        normalColor = viewer.getTable().getForeground();
    }
    
    @Override
    public void update(ViewerCell cell) {
        int index = cell.getColumnIndex();
        if (isNewRow(cell)) {
            cell.setFont(italic);
        } else {
            cell.setFont(normal);
        }
        if (canEdit(cell)) {
            cell.setForeground(normalColor);
        } else {
            cell.setForeground(greyColor);
        }
        if (index==0) {
            updateName(cell);
            return;
        } else if (index==1) {
            final Object element = cell.getElement();
            if (element instanceof NewRow) {
                NewRow newRow = (NewRow)element;
                cell.setText("");
            } else if (element instanceof IPropertyDescriptor) {
                IPropertyDescriptor pd = (IPropertyDescriptor)element;
                JcrNode jcrNode = (JcrNode)viewer.getInput();
                Map.Entry me = (Entry) pd.getId();
                int propertyType = jcrNode.getPropertyType(String.valueOf(me.getKey()));
                if (propertyType<=-1 || propertyType==PropertyType.UNDEFINED) {
                    cell.setText("");
                } else {
                    cell.setText(PropertyType.nameFromValue(propertyType));
                }
            } else {
                cell.setText("");
            }
            return;
        } else if (index==2) {
            updateValue(cell);
            return;
        } else {
            final Object element = cell.getElement();
            if (element instanceof NewRow) {
                NewRow newRow = (NewRow)element;
                cell.setText("");
            } else if (element instanceof IPropertyDescriptor) {
                IPropertyDescriptor pd = (IPropertyDescriptor)element;
                JcrNode jcrNode = (JcrNode)viewer.getInput();
                Map.Entry me = (Entry) pd.getId();
                PropertyDefinition prd = jcrNode.getPropertyDefinition(String.valueOf(me.getKey()));
                if (prd==null) {
                    cell.setText("false");
                } else if (index==3) {
                    // protected
                    cell.setText(String.valueOf(prd.isProtected()));
                } else if (index==4) {
                    // mandatory
                    cell.setText(String.valueOf(prd.isMandatory()));
                } else if (index==5) {
                    // multiple
                    cell.setText(String.valueOf(prd.isMultiple()));
                } else if (index==6) {
                    // auto creatd
                    cell.setText(String.valueOf(prd.isAutoCreated()));
                } else {
                    cell.setText("n/a");
                    return;
                }
            }
            
        }
    }

    private boolean canEdit(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof NewRow) {
            return (cell.getColumnIndex()==0 || cell.getColumnIndex()==2);
        } else if (element instanceof IPropertyDescriptor){
            IPropertyDescriptor pd = (IPropertyDescriptor)element;
            JcrNode jcrNode = (JcrNode)viewer.getInput();
            Map.Entry me = (Entry) pd.getId();
            if (me.getKey().equals("jcr:primaryType")) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isNewRow(ViewerCell cell) {
        return (cell.getElement() instanceof NewRow);
    }

    private void updateValue(ViewerCell cell) {
        final Object element = cell.getElement();
        if (element instanceof NewRow) {
            NewRow newRow = (NewRow)element;
            cell.setText(String.valueOf(newRow.getValue()));
        } else if (element instanceof IPropertyDescriptor) {
            IPropertyDescriptor pd = (IPropertyDescriptor)element;
            JcrNode jcrNode = (JcrNode)viewer.getInput();
//            jcrNode.getProperties().getPropertyValue(pd); 
            Map.Entry me = (Entry) pd.getId();
            final String rawValue = String.valueOf(me.getValue());
            int index = rawValue.indexOf("}");
            if (index!=-1) {
                cell.setText(rawValue.substring(index+1));
            } else {
                cell.setText(rawValue);
            }
        } else {
            cell.setText(String.valueOf(element));
        }
    }

    private void updateName(ViewerCell cell) {
        final Object element = cell.getElement();
        if (element instanceof NewRow) {
            NewRow newRow = (NewRow)element;
            cell.setText(String.valueOf(newRow.getName()));
        } else if (element instanceof IPropertyDescriptor) {
            IPropertyDescriptor pd = (IPropertyDescriptor)element;
            cell.setText(pd.getDisplayName());
        } else {
            cell.setText(String.valueOf(element));
        }
    }

}
