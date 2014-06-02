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

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrProperty;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MVNCellEditor extends DialogCellEditor {

    private final JcrNode node;
    private final String propertyName;

    public MVNCellEditor(Composite parent, JcrNode node, String propertyName) {
        super(parent);
        if (node==null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (propertyName==null || propertyName.length()==0) {
            throw new IllegalArgumentException("propertyName must not be null or empty");
        }
        this.node = node;
        this.propertyName = propertyName;
    }
    
    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        final JcrProperty property = node.getProperty(propertyName);
        final MVPEditor mvpEditor = new MVPEditor(cellEditorWindow.getShell(), property);
        if (mvpEditor.open() == IStatus.OK) {
            return mvpEditor.getLines();
        } else {
            return null;
        }
    }

}
