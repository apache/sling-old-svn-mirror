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
package org.apache.sling.ide.eclipse.ui;

import org.apache.sling.ide.eclipse.core.Preferences;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SlingIdePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    // a list with options to add/remove
    private ListEditor ignoredFileNamesForSyncEditor;
    
    public SlingIdePreferencePage() {
        super(GRID);
    }

    private final static class PatternValidator implements IInputValidator {

        @Override
        public String isValid(String newText) {
            // may not contain "/" or "\"
            if (newText.matches(".*(\\\\|\\/).*")) {
                return "Name must not contain '/' or '\'";
            };
            return null;
        }
        
    }
    @Override
    protected void createFieldEditors() {
        ignoredFileNamesForSyncEditor = new ListEditor(Preferences.IGNORED_FILE_NAMES_FOR_SYNC, "Ignored file names for the server sync", getFieldEditorParent()) {

            @Override
            protected String createList(String[] items) {
                StringBuffer path = new StringBuffer("");//$NON-NLS-1$
                for (int i = 0; i < items.length; i++) {
                    path.append(items[i]);
                    path.append(Preferences.LIST_SEPARATOR);
                }
                return path.toString();
            }

            @Override
            protected String getNewInputObject() {
                InputDialog dialog = new InputDialog(getShell(), "Add file name", "Enter a file name to ignore during the server sync ...", "",  new PatternValidator());
                if (dialog.open() == Window.OK) {
                    return dialog.getValue();
                } else {
                    return null;
                }
               
            }

            @Override
            protected String[] parseString(String stringList) {
                return stringList.split(Preferences.LIST_SEPARATOR);
            }
            
        };
        addField(ignoredFileNamesForSyncEditor);
        
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return Activator.getDefault().getPreferenceStore();
    }

    @Override
    public void init(IWorkbench workbench) {
    }

}
