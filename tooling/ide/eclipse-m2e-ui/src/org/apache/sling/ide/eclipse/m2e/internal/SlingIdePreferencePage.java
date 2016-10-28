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
package org.apache.sling.ide.eclipse.m2e.internal;

import org.apache.sling.ide.eclipse.m2e.internal.preferences.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SlingIdePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private BooleanFieldEditor enableProjectConfiguratorEditor;
    private BooleanFieldEditor enableExtendedProjectConfigurationEditor;
    private Group m2eProjectConfiguratorsForContentPackagesGroup;
    
    public SlingIdePreferencePage() {
        super(GRID);
    }

    @Override
    protected void createFieldEditors() {
        m2eProjectConfiguratorsForContentPackagesGroup = new Group(getFieldEditorParent(), SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        m2eProjectConfiguratorsForContentPackagesGroup.setLayoutData(gd);
        m2eProjectConfiguratorsForContentPackagesGroup.setLayout(new GridLayout());
        m2eProjectConfiguratorsForContentPackagesGroup.setText("Maven Project Configurator for Content-Packages");
        enableProjectConfiguratorEditor = new BooleanFieldEditor(Preferences.ENABLE_CONTENT_PACKAGE_PROJECT_CONFIGURATOR, "Enable",
                m2eProjectConfiguratorsForContentPackagesGroup);
        enableExtendedProjectConfigurationEditor = new BooleanFieldEditor(Preferences.ENABLE_CONTENT_PACKAGE_PROJECT_CONFIGURATOR_ADDITIONAL_WTP_FACETS, "Add additional WTP natures and facets",
                m2eProjectConfiguratorsForContentPackagesGroup);

        addField(enableProjectConfiguratorEditor);
        addField(enableExtendedProjectConfigurationEditor);
        addField(new BooleanFieldEditor(Preferences.ENABLE_BUNDLE_PROJECT_CONFIGURATOR, "Enable Maven Project Configurator for Bundles", getFieldEditorParent()));
    }

    @Override
    protected void initialize() {
        super.initialize();
        // make sure that the dependent boolean field is conditionally disabled (depending on the initial value of the
        // other field)
        enableExtendedProjectConfigurationEditor.setEnabled(enableProjectConfiguratorEditor.getBooleanValue(),
                m2eProjectConfiguratorsForContentPackagesGroup);
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return Activator.getDefault().getPreferenceStore();
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        // http://stackoverflow.com/questions/26909856/own-preferencepage-enable-and-disable-fieldeditor-by-booleanfieldeditor
        if (event.getSource() == enableProjectConfiguratorEditor) {
            Boolean enabled = (Boolean) event.getNewValue();
            enableExtendedProjectConfigurationEditor.setEnabled(enabled,
                    m2eProjectConfiguratorsForContentPackagesGroup);
        }
    }

}
