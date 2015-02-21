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
package org.apache.sling.ide.eclipse.ui.browser;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * The <tt>AbstractOpenInBrowserAction</tt> offers support for easily opening a node in a browser
 *
 */
public abstract class AbstractOpenInBrowserAction implements IObjectActionDelegate {

    private ISelection selection;
    private Shell shell;
    private IWorkbenchPart targetPart;

    @Override
    public void run(IAction action) {
    	if (selection==null || !(selection instanceof IStructuredSelection)) {
    		return;
    	}
    	IStructuredSelection ss = (IStructuredSelection)selection;
    	JcrNode node = (JcrNode) ss.getFirstElement();
    	
        IModule module = ServerUtil.getModule(node.getProject());
        if (module==null) {
    		MessageDialog.openWarning(shell, "Cannot open browser", "Not configured for any server");
        	return;
        }
        IServer[] servers = ServerUtil.getServersByModule(module, new NullProgressMonitor());
        if (servers==null || servers.length==0) {
    		MessageDialog.openWarning(shell, "Cannot open browser", "Not configured for any server");
        	return;
        }
        IServer server = servers[0];
        URL url;
        try {
            url = getUrlToOpen(node, server);
        } catch (MalformedURLException e) {
            StatusManager.getManager().handle(new Status(Status.WARNING, Activator.PLUGIN_ID, "Url is invalid", e),
                    StatusManager.SHOW);
            return;
        }
    	try {
            IWorkbenchBrowserSupport browserSupport = targetPart.getSite().getWorkbenchWindow().getWorkbench()
                    .getBrowserSupport();
            browserSupport.createBrowser("org.apache.sling.ide.openOnServer").openURL(url);
        } catch (PartInitException e) {
            StatusManager.getManager().handle(
                    new Status(Status.WARNING, Activator.PLUGIN_ID, "Failed creating browser instance", e),
                    StatusManager.SHOW | StatusManager.LOG);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    	this.selection = selection;
    	action.setEnabled(true);
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    	this.targetPart = targetPart;
    	this.shell = targetPart.getSite().getWorkbenchWindow().getShell();
    }

    protected abstract URL getUrlToOpen(JcrNode node, IServer server) throws MalformedURLException;

}