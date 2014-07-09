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
package org.apache.sling.ide.eclipse.ui.dnd.assistant;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

public class JcrDropAdapterAssistant extends CommonDropAdapterAssistant {

    public JcrDropAdapterAssistant() {
    }
    
    @Override
    public boolean isSupportedType(TransferData aTransferType) {
        // support plain OS file drag'n'drop
        return super.isSupportedType(aTransferType) || FileTransfer.getInstance().isSupportedType(aTransferType);
    }
    
    @Override
    public IStatus validateDrop(Object target, int operation,
            TransferData transferType) {
        if (target instanceof JcrNode) {
            JcrNode jcrNode = (JcrNode)target;
            IStatus result = jcrNode.validateDrop(operation, transferType);
            if (!result.isOK()) {
                // check for details to be shown in the status bar
                final String message = result.getMessage();
                if (message!=null && message.trim().length()>0) {
                    StatusLineUtils.setErrorMessage(2000, message);
                } else {
                    StatusLineUtils.resetErrorMessage();
                }
            } else {
                StatusLineUtils.resetErrorMessage();
            }
            return result;
        }
        return Status.CANCEL_STATUS;
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter aDropAdapter,
            DropTargetEvent aDropTargetEvent, Object aTarget) {
        if (aTarget==null) {
            return Status.CANCEL_STATUS;
        }
        if (!(aTarget instanceof JcrNode)) {
            StatusLineUtils.setErrorMessage(5000, "Cannot drop on this type of element");
            return Status.CANCEL_STATUS;
        }
        JcrNode node = (JcrNode)aTarget;
        if (LocalSelectionTransfer.getTransfer().isSupportedType(aDropAdapter.getCurrentTransfer())) {
            try {
                IStatus status = node.handleDrop(aDropTargetEvent.data, aDropTargetEvent.detail);
                if (!status.isOK()) {
                    final String message = status.getMessage();
                    if (message!=null && message.trim().length()>0) {
                        StatusLineUtils.setErrorMessage(5000, message);
                    }
                }
                return status;
            } catch(Exception e) {
                Activator.getDefault().getPluginLogger().error("Error handling drop: "+e, e);
                StatusLineUtils.setErrorMessage(5000, "Could not drop due to: "+e);
                return Status.CANCEL_STATUS;
            }
        } else if (FileTransfer.getInstance().isSupportedType(aDropAdapter.getCurrentTransfer())) {
            final IContainer targetContainer = node.getDropContainer();
            if (targetContainer == null)
                return Status.CANCEL_STATUS;

            getShell().forceActive();
            final Object data= FileTransfer.getInstance().nativeToJava(aDropAdapter.getCurrentTransfer());
            new CopyFilesAndFoldersOperation(getShell()).copyOrLinkFiles((String[])data, targetContainer, aDropAdapter.getCurrentOperation());
            return Status.OK_STATUS;
        }
        return Status.CANCEL_STATUS;
        
        
        
    }

}
