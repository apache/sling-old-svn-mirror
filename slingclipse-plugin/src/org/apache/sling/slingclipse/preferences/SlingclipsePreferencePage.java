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
package org.apache.sling.slingclipse.preferences;

import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

public class SlingclipsePreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public SlingclipsePreferencePage() {
		super(GRID);
		setPreferenceStore(SlingclipsePlugin.getDefault().getPreferenceStore());
		setDescription("Version 0.0.1");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		addField(new StringFieldEditor(
				PreferencesMessages.REPOSITORY_URL.getKey(),
				"Sling Repository URL:", composite));
		addField(new StringFieldEditor(PreferencesMessages.USERNAME.getKey(),
				"Username:", composite));
		addField(new StringFieldEditor(PreferencesMessages.PASSWORD.getKey(),
				"Password:", composite));
		addField(new BooleanFieldEditor(
				PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey(),
				"&Sync the repository at every save",
				composite));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}