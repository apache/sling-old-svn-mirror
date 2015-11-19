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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/** In parts adapted from org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectPage **/
public class ConvertProjectsPage extends WizardPage {
	private IProject[] initialSelection;
	private List<IProject> projects;
	private Label counterLabel;
	private CheckboxTableViewer ctv;
	private List<IProject> resultingSelection;

	public class ProjectLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0 && (element instanceof IProject)) {
				return ((IProject)element).getName();
			} else {
				return "";
			}
		}

		public Image getColumnImage(Object obj, int index) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}
	}

	public ConvertProjectsPage(List<IProject> projects, List<IProject> initialSelection, String title, String description) {
		super("convertSlingProjects"); //$NON-NLS-1$
		setTitle(title);
		setDescription(description);
		this.initialSelection = initialSelection.toArray(new IProject[0]);
		this.projects = projects;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);
		
		Table table = new Table(container, SWT.MULTI | SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);
		ctv = new CheckboxTableViewer(table);
		ctv.setLabelProvider(new ProjectLabelProvider());
		ctv.add(projects.toArray());
		ctv.setCheckedElements(initialSelection);

		setControl(container);
		Dialog.applyDialogFont(container);
	}

	@SuppressWarnings("unchecked")
	public boolean finish() {
		resultingSelection = new LinkedList<>(
				(Collection<? extends IProject>) Arrays.asList(ctv.getCheckedElements()));
		return true;
	}

	public List<IProject> getSelectedProjects() {
		return resultingSelection;
	}

}
