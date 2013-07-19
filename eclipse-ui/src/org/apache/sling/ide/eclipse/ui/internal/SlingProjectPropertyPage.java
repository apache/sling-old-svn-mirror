package org.apache.sling.ide.eclipse.ui.internal;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.eclipse.ui.dialogs.PropertyPage;

public class SlingProjectPropertyPage extends PropertyPage {


    private Text folderText;

    @Override
    protected Control createContents(Composite parent) {

        Composite c = new Composite(parent, SWT.NONE);

        c.setLayout(new GridLayout(3, false));

        new Label(c, SWT.NONE).setText("Folder to sync");
        folderText = new Text(c, SWT.BORDER);
        folderText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        folderText.setText(ProjectUtil.getSyncDirectoryValue(getProject()));

        folderText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateApplyButton();
            }
        });

        Button browseButton = new Button(c, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final IProject project = getProject();
                ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), project, false, null);
                dialog.showClosedProjects(false);
                dialog.setValidator(new ISelectionValidator() {

                    @Override
                    public String isValid(Object selection) {

                        if (!(selection instanceof IPath)) {
                            return null;
                        }

                        IPath path = (IPath) selection;
                        if (project.getFullPath().isPrefixOf(path)) {
                            return null;
                        }

                        return "The folder must be contained in the " + project.getName() + " project";
                    }
                });

                dialog.open();

                Object[] results = dialog.getResult();
                if (results == null) {
                    return;
                }

                IPath selectedPath = (IPath) results[0];
                folderText.setText(selectedPath.removeFirstSegments(1).toOSString());
            }
        });

        Dialog.applyDialogFont(c);

        return c;
    }

    @Override
    public boolean isValid() {

        String path = folderText.getText();
        IResource member = getProject().findMember(path);

        if (member == null) {
            setErrorMessage("Resource " + path + " is not a part of project " + getProject().getName());
            return false;
        } else if (member.getType() != IResource.FOLDER) {
            setErrorMessage("Resource " + path + " is not a folder");
            return false;
        }

        setErrorMessage(null);

        return true;
    }

    @Override
    public boolean performOk() {

        ProjectUtil.setSyncDirectoryPath(getProject(), folderText.getText());

        return super.performOk();
    }

    private IProject getProject() {
        IProject project = (IProject) getElement().getAdapter(IProject.class);
        return project;
    }
}
