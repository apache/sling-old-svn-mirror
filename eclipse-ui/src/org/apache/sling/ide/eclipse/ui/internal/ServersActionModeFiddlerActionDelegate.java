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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/** prototype for changing icons and tooltip in the wst servers view - tbd properly **/
public class ServersActionModeFiddlerActionDelegate implements
		IViewActionDelegate {

	private IActionBars actionBars;
	private IViewPart view;
	private IPropertyChangeListener runTooltipListener;
	private IPropertyChangeListener debugTooltipListener;
	private IPropertyChangeListener disconnectTooltipListener;

	@Override
	public void run(IAction action) {

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
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

	@Override
	public void init(IViewPart view) {
		this.view = view;
		actionBars = view.getViewSite().getActionBars();
	}

}
