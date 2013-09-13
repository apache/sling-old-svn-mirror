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
package org.apache.sling.ide.eclipse.ui.actions;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class JcrNewNodeAction implements IObjectActionDelegate {

	private ISelection selection;
	private Shell shell;

	public JcrNewNodeAction() {
	}

	@Override
	public void run(IAction action) {
		if (selection==null || !(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ss = (IStructuredSelection)selection;
		JcrNode node = (JcrNode) ss.getFirstElement();
		InputDialog id = new InputDialog(shell, "Enter JCR node name", 
				"Enter name for new JCR node under '"+node.getName()+"':", "", new IInputValidator() {
					
					@Override
					public String isValid(String newText) {
						if (newText!=null && newText.trim().length()>0 && newText.trim().equals(newText)) {
							return null;
						} else {
							return "Invalid input";
						}
					}
				});
		if (id.open() == IStatus.OK) {
			node.createChild(id.getValue());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.shell = targetPart.getSite().getWorkbenchWindow().getShell();
	}

}
