package org.apache.sling.ide.eclipse.ui.internal;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExportContentAction extends AbstractHandler implements IObjectActionDelegate, IExecutableExtension {

    private ISelection selection;

    @Override
    public void run(IAction action) {
        run(selection);
    }

    private void run(ISelection selection) {

        if (!(selection instanceof IStructuredSelection)) {
            return;
        }

        IStructuredSelection structuredSelection = (IStructuredSelection) selection;

        for (Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
            Object selected = it.next();
            if (selected instanceof IResource) {
                IResource resource = (IResource) selected;

                Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();

                ExportWizard wiz = new ExportWizard();
                wiz.init(PlatformUI.getWorkbench(), resource);

                WizardDialog dialog = new WizardDialog(activeShell, wiz);
                dialog.open();
            }
        }

    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        run(HandlerUtil.getCurrentSelection(event));
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement
     * , java.lang.String, java.lang.Object)
     */
    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
     * org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

}
