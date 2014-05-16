package org.apache.sling.ide.eclipse.ui.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.RepositoryException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class NewNodeDialog extends InputDialog {

    private final String parentNodeType;
    private final NodeTypeRegistry ntManager;
    protected String comboSelection;
    private Collection<String> allowedChildren;

    public NewNodeDialog(Shell parentShell, JcrNode node,
            NodeTypeRegistry ntManager) throws RepositoryException {
        super(parentShell, "Enter JCR node name", 
                "Enter name for new node under:\n path: "+node.getJcrPath(), "", null);
        this.parentNodeType = node.getPrimaryType();
        this.ntManager = ntManager;
        final LinkedList<String> ac = new LinkedList<String>(ntManager.getAllowedPrimaryChildNodeTypes(parentNodeType));
        Collections.sort(ac);
        this.allowedChildren = ac;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        Control[] children = composite.getChildren();
        Control errorMessageText = children[children.length-1];
        
        // now add the node type dropbox-combo
        Label label = new Label(composite, SWT.WRAP);
        label.moveAbove(errorMessageText);
        label.setText("Define node type");
        GridData data = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_CENTER);
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
        label.setLayoutData(data);
        label.setFont(parent.getFont());

        final Combo combo = new Combo(composite, SWT.DROP_DOWN);
        combo.moveAbove(errorMessageText);
        combo.setItems(allowedChildren.toArray(new String[0]));
        combo.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comboSelection = combo.getText();
                validateInput();
            }
        });
        combo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                comboSelection = combo.getText();
                validateInput();
            }
            
        });
        if (allowedChildren.size()==1) {
            combo.setText(allowedChildren.iterator().next());
        }
        
        return composite;
    }
    
    public String getChosenNodeType() {
        return comboSelection;
    }

    protected void validateInput() {
        final String firstInput = getValue();
        final String secondInput = comboSelection;
        try {
            if (secondInput==null || secondInput.length()==0) {
                setErrorMessage("");
            } else if (ntManager.isAllowedPrimaryChildNodeType(parentNodeType, secondInput)) {
                setErrorMessage(null);
            } else {
                setErrorMessage("Error: Invalid child node type of "+parentNodeType);
            }
        } catch(RepositoryException e) {
            setErrorMessage("RepositoryException: "+e);
        }
        
    };
}
