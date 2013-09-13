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
package org.apache.sling.ide.eclipse.ui.wizards;

import java.util.List;

import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.SharedImages;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

/** In parts adapted from org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectWizard **/
public class ConvertProjectsWizard extends Wizard {
	private ConvertProjectsPage mainPage;
	private List<IProject> initialSelection;
	private List<IProject> projects;
	private String title;
	private String description;

	public ConvertProjectsWizard(List<IProject> projects, List<IProject> initialSelection, String title, String description) {
		setDefaultPageImageDescriptor(SharedImages.SLING_LOG);
		setWindowTitle(title);
		setDialogSettings(Activator.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		this.title = title;
		this.description = description;
		this.initialSelection = initialSelection;
		this.projects = projects;
	}

	public void addPages() {
		mainPage = new ConvertProjectsPage(projects, initialSelection, title, description);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}

	public List<IProject> getSelectedProjects() {
		return mainPage.getSelectedProjects();
	}
}
