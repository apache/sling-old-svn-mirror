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

import javax.jcr.nodetype.NodeType;

import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class JcrNewNodeAction implements IObjectActionDelegate {

	private ISelection selection;
	private Shell shell;
    private boolean doNotAskAgain;

	public JcrNewNodeAction() {
	}

	@Override
    public void run(IAction action) {
        if (selection==null || !(selection instanceof IStructuredSelection)) {
            return;
        }
        IStructuredSelection ss = (IStructuredSelection)selection;
        JcrNode node = (JcrNode) ss.getFirstElement();
        if (!node.canCreateChild()) {
            MessageDialog.openInformation(shell, "Cannot create node",
                    "Node is not covered by the workspace filter as defined in filter.xml");
            return;
        }
        Repository repository = ServerUtil.getDefaultRepository(node.getProject());
        NodeTypeRegistry ntManager = (repository==null) ? null : repository.getNodeTypeRegistry();
        if (ntManager == null) {
            
            if (!doNotAskAgain) {
                MessageDialog dialog = new MessageDialog(null,  "Unable to validate node type", null,
                        "Unable to validate node types since project " + node.getProject().getName() + " is not associated with a server or the server is not started.",
                        MessageDialog.QUESTION_WITH_CANCEL, 
                        new String[] {"Cancel", "Continue (do not ask again)", "Continue"}, 1) {
                    @Override
                    protected void configureShell(Shell shell) {
                        super.configureShell(shell);
                        setShellStyle(getShellStyle() | SWT.SHEET);
                    }
                };
                int choice = dialog.open();
                if (choice <= 0) {
                    return;
                }
                if (choice==1) {
                    doNotAskAgain = true;
                }
            }

        }
        final NodeType nodeType = node.getNodeType();
        if (nodeType!=null && nodeType.getName()!=null && nodeType.getName().equals("nt:file")) {
            MessageDialog.openInformation(shell, "Cannot create node", "Node of type nt:file cannot have children");
            return;
        }
        
        try {
            final NewNodeDialog nnd = new NewNodeDialog(shell, node, ntManager);
            if (nnd.open() == IStatus.OK) {
                node.createChild(nnd.getValue(), nnd.getChosenNodeType());
                return;
            }
        } catch (RepositoryException e1) {
            Activator.getDefault().getPluginLogger().warn(
                    "Could not open NewNodeDialog due to "+e1, e1);
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
