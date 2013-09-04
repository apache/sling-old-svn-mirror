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
