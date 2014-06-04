package org.apache.sling.ide.eclipse.ui.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.RepositoryException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
    private Combo combo;
    private ContentProposalAdapter proposalAdapter;
    private NodeDefinition[] allChildNodeDefs;

    public NewNodeDialog(Shell parentShell, JcrNode node,
            NodeTypeRegistry ntManager) throws RepositoryException {
        super(parentShell, "Enter JCR node name", 
                "Enter name for new node under:\n path: "+node.getJcrPath(), "", null);
        this.parentNodeType = node.getPrimaryType();
        this.ntManager = ntManager;
        final LinkedList<String> ac = new LinkedList<String>(ntManager.getAllowedPrimaryChildNodeTypes(parentNodeType));
        final NodeType parentNt = ntManager.getNodeType(parentNodeType);
        allChildNodeDefs = parentNt.getChildNodeDefinitions();
        Collections.sort(ac);
        this.allowedChildren = ac;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        Control[] children = composite.getChildren();
        Control errorMessageText = children[children.length-1];
        GridData errorMessageGridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL);
        errorMessageGridData.heightHint = convertHeightInCharsToPixels(2);
        errorMessageText.setLayoutData(errorMessageGridData);
        
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

        combo = new Combo(composite, SWT.DROP_DOWN);
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

        SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(combo.getItems());
        proposalProvider.setFiltering(true);
        final ComboContentAdapter controlContentAdapter = new ComboContentAdapter() {
            @Override
            public void insertControlContents(Control control, String text,
                    int cursorPosition) {
                Point selection = combo.getSelection();
                combo.setText(text);
                selection.x = selection.x + cursorPosition;
                selection.y = selection.x;
                combo.setSelection(selection);
            }
            
            @Override
            public Rectangle getInsertionBounds(Control control) {
                final Rectangle insertionBounds = super.getInsertionBounds(control);
                // always insert at start
                insertionBounds.x = 0;
                insertionBounds.y = 0;
                return insertionBounds;
            }
            
            
        };
        // this variant opens auto-complete on each character
        proposalAdapter = new ContentProposalAdapter(combo, controlContentAdapter, proposalProvider, null, null);
        // this variant opens auto-complete only when invoking the auto-complete hotkey
//        proposalAdapter = new ContentAssistCommandAdapter(combo, controlContentAdapter,
//            proposalProvider, null, new char[0], true);
        if (allowedChildren.size()==1) {
            combo.setText(allowedChildren.iterator().next());
        }
        
        return composite;
    }
    
    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        // fix autocomplete popup size:
        Point size = combo.getSize();
        size.x /= 2;
        size.y = 180;
        proposalAdapter.setPopupSize(size);
    }
    
    public String getChosenNodeType() {
        return comboSelection;
    }

    protected void validateInput() {
        final String firstInput = getText().getText();
        final String secondInput = comboSelection;
        try {
            if (secondInput==null || secondInput.length()==0) {
                setErrorMessage("");
            } else if (ntManager.isAllowedPrimaryChildNodeType(parentNodeType, secondInput)) {
                // also check on the name, not only the type
                if (allChildNodeDefs==null) {
                    setErrorMessage("No child node definitions found for "+parentNodeType);
                } else {
                    boolean fail = false;
                    for (int i = 0; i < allChildNodeDefs.length; i++) {
                        NodeDefinition aChildNodeDef = allChildNodeDefs[i];
                        if (aChildNodeDef.getName()!=null && aChildNodeDef.getName().length()>0) {
                            if (firstInput.equals(aChildNodeDef.getName())) {
                                setErrorMessage(null);
                                return;
                            } else {
                                // mark fail if no other child node definition matches
                                fail = true;
                            }
                        }
                    }
                    if (!fail) {
                        setErrorMessage(null);
                        return;
                    }
                    StringBuffer details = new StringBuffer();
                    for (int i = 0; i < allChildNodeDefs.length; i++) {
                        NodeDefinition aChildNodeDef = allChildNodeDefs[i];
                        if (details.length()!=0) {
                            details.append(", ");
                        }
                        details.append("(name="+aChildNodeDef.getName()+", required primary type(s)=");
                        String[] requiredPrimaryTypeNames = aChildNodeDef.getRequiredPrimaryTypeNames();
                        if (requiredPrimaryTypeNames==null) {
                            details.append("null");
                        } else {
                            for (int j = 0; j < requiredPrimaryTypeNames.length; j++) {
                                String rptn = requiredPrimaryTypeNames[j];
                                if (j>0) {
                                    details.append(",");
                                }
                                details.append(rptn);
                            }
                        }
                        details.append(")");
                    }
                    setErrorMessage("No matching child node definition found for "+parentNodeType+". Expected one of: "+details);
                }
            } else {
                setErrorMessage("Error: Invalid child node type of "+parentNodeType);
            }
        } catch(RepositoryException e) {
            setErrorMessage("RepositoryException: "+e);
        }
        
    };
}
