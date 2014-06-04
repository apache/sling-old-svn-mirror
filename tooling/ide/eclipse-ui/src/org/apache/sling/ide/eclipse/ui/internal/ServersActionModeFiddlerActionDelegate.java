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
package org.apache.sling.ide.eclipse.ui.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.ImageResource;
import org.eclipse.wst.server.ui.internal.Messages;

/** prototype for changing icons and tooltip in the wst servers view - tbd properly **/
public class ServersActionModeFiddlerActionDelegate implements
		IViewActionDelegate {

	private IActionBars actionBars;
	private IViewPart view;
	private IPropertyChangeListener runTooltipListener;
	private IPropertyChangeListener debugTooltipListener;
	private IPropertyChangeListener disconnectTooltipListener;

	private List<IAction> prependedToolbarActions = new LinkedList<IAction>();
	private List<IAction> appendedToolbarActions = new LinkedList<IAction>();
    private IServer server;
    private Action cleanAction;
	
	@Override
	public void run(IAction action) {

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	    if (selection!=null && (selection instanceof IStructuredSelection) &&
	            (((IStructuredSelection)selection).getFirstElement() instanceof IServer)) {
            server = (IServer)(((IStructuredSelection)selection).getFirstElement());
            cleanAction.setEnabled(true);
	    } else {
	        cleanAction.setEnabled(false);
	    }
		action.setEnabled(true);
		final IAction serverRunAction = actionBars.getGlobalActionHandler("org.eclipse.wst.server.run");
		final IAction serverDebugAction = actionBars.getGlobalActionHandler("org.eclipse.wst.server.debug");
		IAction stopRunAction = actionBars.getGlobalActionHandler("org.eclipse.wst.server.stop");
		if (serverRunAction==null || stopRunAction==null || serverDebugAction==null) {
			return;
		}
//		serverRunAction.setHoverImageDescriptor(SharedImages.SLING_LOG);
		serverRunAction.setHoverImageDescriptor(SharedImages.RUN_CONNECT);
		serverDebugAction.setHoverImageDescriptor(SharedImages.DEBUG_CONNECT);
		stopRunAction.setHoverImageDescriptor(SharedImages.DISCONNECT);
		
		for (Iterator it = appendedToolbarActions.iterator(); it.hasNext();) {
            IAction appendedAction = (IAction) it.next();
            if (!actionAdded(appendedAction)) {
                actionBars.getToolBarManager().add(appendedAction);
            }
        }
		
		final String runText = "Connect to server in run mode";
		if (runTooltipListener==null) {
			runTooltipListener = new IPropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(IAction.TOOL_TIP_TEXT) ) {
						if (!event.getNewValue().equals(runText)) {
							serverRunAction.setToolTipText(runText);
						}
					}
				}
			};
			serverRunAction.addPropertyChangeListener(runTooltipListener);
		}
		final String debugText = "Connect to server in debug mode";
		if (debugTooltipListener==null) {
			debugTooltipListener = new IPropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(IAction.TOOL_TIP_TEXT)) {
						if (!event.getNewValue().equals(debugText)) {
							serverDebugAction.setToolTipText(debugText);
						}
					}
				}
			};
			serverDebugAction.addPropertyChangeListener(debugTooltipListener);
		}
		final String disconnectText = "Disconnect from server";
		if (disconnectTooltipListener==null) {
			disconnectTooltipListener = new IPropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(IAction.TOOL_TIP_TEXT)) {
						if (!event.getNewValue().equals(disconnectText)) {
							serverRunAction.setToolTipText(disconnectText);
						}
					}
				}
			};
			stopRunAction.addPropertyChangeListener(disconnectTooltipListener);
		}
		
		serverRunAction.setToolTipText(runText);
		serverDebugAction.setToolTipText(debugText);
		stopRunAction.setToolTipText(disconnectText);
		
	}

	private boolean actionAdded(IAction action) {
        IContributionItem[] items = actionBars.getToolBarManager().getItems();
        for (int i = 0; i < items.length; i++) {
            IContributionItem iContributionItem = items[i];
            final String id = iContributionItem.getId();
            if (id==null) {
                continue;
            }
            if (id.equals(action.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
	public void init(IViewPart view) {
		this.view = view;
		actionBars = view.getViewSite().getActionBars();
		initToolbarContributedActions();
		for (Iterator it = prependedToolbarActions.iterator(); it.hasNext();) {
            IAction action = (IAction) it.next();
            final ActionContributionItem contribution = new ActionContributionItem(action);
            actionBars.getToolBarManager().add(contribution);
        }
	}

    private void initToolbarContributedActions() {
        cleanAction = new Action("Clean Publish...", IAction.AS_PUSH_BUTTON) {
            public void run() {
                if (MessageDialog.openConfirm(view.getSite().getShell(), Messages.defaultDialogTitle, Messages.dialogPublishClean)) {
                    IAdaptable info = new IAdaptable() {
                        public Object getAdapter(Class adapter) {
                            if (Shell.class.equals(adapter))
                                return view.getSite().getShell();
                            if (String.class.equals(adapter))
                                return "user";
                            return null;
                        }
                    };
                    
                    server.publish(IServer.PUBLISH_CLEAN, null, info, null);
                }
            }
        };
        cleanAction.setText("Clean Publish...");
        cleanAction.setToolTipText("Clean and Publish...");
        ImageDescriptor cleanAndPublishImageDesc = new DecorationOverlayIcon(
                ImageResource.getImageDescriptor(ImageResource.IMG_ELCL_PUBLISH).createImage(), 
                ImageDescriptor.createFromFile(SharedImages.class, "refresh.png"), IDecoration.BOTTOM_RIGHT);
        cleanAction.setImageDescriptor(cleanAndPublishImageDesc);
        cleanAction.setId("org.apache.sling.ide.eclipse.ui.actions.ClearAction");  
        appendedToolbarActions.add(cleanAction);
    }

}
