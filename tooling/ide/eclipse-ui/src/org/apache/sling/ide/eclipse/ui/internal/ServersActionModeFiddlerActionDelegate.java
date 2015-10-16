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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
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

	private List<ActionContributionItem> prependedToolbarActions = new LinkedList<>();
	private List<ActionContributionItem> appendedToolbarActionContributionItems = new LinkedList<>();
    private IServer server;
    private List<IModule[]> modules;
    private Action cleanAction;
    private Action publishAction;
    private ActionContributionItem wstPublishAction;
    private ActionContributionItem cleanActionContributionItem;
    private ActionContributionItem publishActionContributionItem;
    protected boolean doNotAskAgain = false; //TODO: move to preferences
	
	@Override
	public void run(IAction action) {

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
        server = null;
        modules = null;
        if (selection!=null && (selection instanceof IStructuredSelection)) {
	        IStructuredSelection iss = (IStructuredSelection) selection;
	        Object first = iss.getFirstElement();
	        if (first instanceof IServer) {
	            server = (IServer)first;
	            modules = null;
	            if (iss.size()>1) {
	                // verify that all selected elements are of type IServer
	                Iterator<?> it = iss.iterator();
	                it.next(); // skip the first, we have that above already
	                while(it.hasNext()) {
	                    Object next = it.next();
	                    if (!(next instanceof IServer)) {
	                        server = null;
	                        modules = null;
	                        break;
	                    }
	                }
	            }
	        } else if (first instanceof IServerModule) {
	            modules = new LinkedList<>();
	            IServerModule module = (IServerModule)first;
	            modules.add(module.getModule());
	            server = module.getServer();
                if (iss.size()>1) {
                    // verify that all selected elements are of type IServerModule
                    // plus add the module[] to the modules list
                    Iterator<?> it = iss.iterator();
                    it.next(); // skip the first, we have that above already
                    while(it.hasNext()) {
                        Object next = it.next();
                        if (!(next instanceof IServerModule)) {
                            server = null;
                            module = null;
                            break;
                        } else {
                            module = (IServerModule) next;
                            modules.add(module.getModule());
                        }
                    }
                }
	        }
	    }
        
        if (server!=null) {
            if (server.getServerState() != IServer.STATE_STARTED) {
                server = null;
                modules = null;
            }
        }
        cleanAction.setEnabled(server!=null);
        publishAction.setEnabled(server!=null);

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
		
		findWstPublishAction();
		
		for (ActionContributionItem appendedAction : appendedToolbarActionContributionItems) {
            if (!contributionAdded(appendedAction)) {
                actionBars.getToolBarManager().add(appendedAction);
            }
        }
		if (wstPublishAction!=null) {
		    wstPublishAction.setVisible(false);
		    publishActionContributionItem.setVisible(true);
		} else {
		    // otherwise hide it, as it is an unexpected situation
		    publishActionContributionItem.setVisible(false);
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

	private void findWstPublishAction() {
	    if (wstPublishAction!=null) {
	        return;
	    }
        IContributionItem[] items = actionBars.getToolBarManager().getItems();
        for (IContributionItem item : items) {
            if (item instanceof ActionContributionItem) {
                ActionContributionItem actionItem = (ActionContributionItem) item;
                IAction a = actionItem.getAction();
                if ("org.eclipse.wst.server.publish".equals(a.getActionDefinitionId())) {
                    wstPublishAction = actionItem;
//                    item.setVisible(false);
//                    actionBars.getToolBarManager().remove(item);
                }
            }
        }
        
    }

    private boolean contributionAdded(ActionContributionItem action) {
        IContributionItem[] items = actionBars.getToolBarManager().getItems();
        for (IContributionItem iContributionItem : items) {
            if (iContributionItem==action) {
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
		for (ActionContributionItem actionContributionItem : prependedToolbarActions) {
		    // TODO - this looks wrong
            IAction action = (IAction) actionContributionItem;
            final ActionContributionItem contribution = new ActionContributionItem(action);
            actionBars.getToolBarManager().add(contribution);
        }
	}

    private void initToolbarContributedActions() {
        cleanAction = new Action("Clean Publish...", IAction.AS_PUSH_BUTTON) {
            public void run() {
                if (server==null) {
                    MessageDialog.openInformation(view.getSite().getShell(), "No server selected", "A server must be selected");
                    return;
                }
                int selection = 2;
                if (!doNotAskAgain) {
                    MessageDialog dialog = new MessageDialog(view.getSite().getShell(), Messages.defaultDialogTitle, null, Messages.dialogPublishClean,
                            MessageDialog.QUESTION_WITH_CANCEL, 
                            new String[] {"Cancel", "OK (do not ask again)", "OK"}, 1) {
                        @Override
                        protected void configureShell(Shell shell) {
                            super.configureShell(shell);
                            setShellStyle(getShellStyle() | SWT.SHEET);
                        }
                    };
                    selection = dialog.open();
                }
                if (selection != 0) {
                    if (selection==1) {
                        doNotAskAgain = true;
                    }
                    IAdaptable info = new IAdaptable() {
                        public Object getAdapter(Class adapter) {
                            if (Shell.class.equals(adapter))
                                return view.getSite().getShell();
                            if (String.class.equals(adapter))
                                return "user";
                            return null;
                        }
                    };
                    
                    server.publish(IServer.PUBLISH_CLEAN, modules, info, null);
                }
            }
        };
        cleanAction.setText("Clean Publish...");
        cleanAction.setToolTipText("Clean and Publish...");
        ImageDescriptor cleanAndPublishImageDesc = new DecorationOverlayIcon(
                ImageResource.getImageDescriptor(ImageResource.IMG_CLCL_PUBLISH).createImage(), 
                ImageDescriptor.createFromFile(SharedImages.class, "refresh.gif"), IDecoration.BOTTOM_RIGHT);
        cleanAction.setImageDescriptor(cleanAndPublishImageDesc);
        cleanAction.setId("org.apache.sling.ide.eclipse.ui.actions.CleanPublishAction");
        publishAction = new Action("Publish", IAction.AS_PUSH_BUTTON) {
            public void run() {
                if (server==null) {
                    MessageDialog.openInformation(view.getSite().getShell(), "No server selected", "A server must be selected");
                    return;
                }
                IAdaptable info = new IAdaptable() {
                    public Object getAdapter(Class adapter) {
                        if (Shell.class.equals(adapter))
                            return view.getSite().getShell();
                        if (String.class.equals(adapter))
                            return "user";
                        return null;
                    }
                };
                
                server.publish(IServer.PUBLISH_INCREMENTAL, modules, info, null);
            }
        };
        publishAction.setText("Publish");
        publishAction.setToolTipText("Publish");
        publishAction.setImageDescriptor(ImageResource.getImageDescriptor(ImageResource.IMG_CLCL_PUBLISH));
        publishAction.setId("org.apache.sling.ide.eclipse.ui.actions.PublishAction");  
        cleanAction.setEnabled(false);
        publishAction.setEnabled(false);

        cleanActionContributionItem = new ActionContributionItem(cleanAction);
        publishActionContributionItem = new ActionContributionItem(publishAction);
        
        appendedToolbarActionContributionItems.add(publishActionContributionItem);
        appendedToolbarActionContributionItems.add(cleanActionContributionItem);
    }

}
