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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/** Testing action which renames a jcr node **/
public class JcrNodeRenameAction implements IObjectActionDelegate {

	private ISelection selection;
	private JcrNode node;
	private Shell shell;

	/**
	 * The constructor.
	 */
	public JcrNodeRenameAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		if (this.node==null) {
			return;
		}
        InputDialog id = new InputDialog(shell, "Change Node Name", "Enter new name for node '" + node.getDescription()
                + "':", node.getLabel(), new IInputValidator() {
					
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
			node.rename(id.getValue());
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iss = (IStructuredSelection) selection;
            if (iss.size()==1) {
    			Object element = iss.getFirstElement();
    			if (element instanceof JcrNode) {
    				final JcrNode n = (JcrNode)element;
    				if (n.canBeRenamed()) {
    					action.setEnabled(true);
    					this.node = n;
    					return;
    				}
    			}
            }
		}
		action.setEnabled(false);
		this.node = null;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.shell = targetPart.getSite().getWorkbenchWindow().getShell();
	}
}