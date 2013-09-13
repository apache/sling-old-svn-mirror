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
package org.apache.sling.ide.eclipse.ui.nav;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/** current prototype of how to support double-click on jcr nodes to open the file in the editor **/
public class PackageExplorerOpenActionProvider extends CommonActionProvider {

//	private IAction fOpenAndExpand;
//	private OpenEditorActionGroup fOpenGroup;

	private boolean fInViewPart = true;
private OpenFileAction action;
private TreeViewer treeViewer;
	private IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		return activeWorkbenchWindow.getActivePage();
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (fInViewPart) {
			ISelection selection = treeViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection iss = (IStructuredSelection)selection;
				if (iss.getFirstElement() instanceof JcrNode) {
					final JcrNode node = (JcrNode)iss.getFirstElement();
					final IFile file = node.getFileForEditor();
					if (file!=null) {
						actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, new OpenFileAction(getActivePage()) {
							@Override
							public void run() {
								try {
									IDE.openEditor(getActivePage(), file, true);
								} catch (PartInitException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						});
					}
					return;
				}
			}
//			fOpenGroup.fillActionBars(actionBars);
//
//			if (fOpenAndExpand == null && fOpenGroup.getOpenAction().isEnabled()) // TODO: is not updated!
//				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenGroup.getOpenAction());
//			else if (fOpenAndExpand != null && fOpenAndExpand.isEnabled())
		}
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, action);

	}

	@Override
	public void fillContextMenu(IMenuManager menu) {

		if (fInViewPart) {
//			if (fOpenGroup.getOpenAction().isEnabled()) {
//				fOpenGroup.fillContextMenu(menu);
//			}
		}
	}

	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);
		action = new OpenFileAction(getActivePage());
		ICommonViewerWorkbenchSite workbenchSite = null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();

		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart = (IViewPart) workbenchSite.getPart();

				if (site.getStructuredViewer() instanceof TreeViewer) {
					 treeViewer = (TreeViewer) site.getStructuredViewer();
					 treeViewer.addSelectionChangedListener(action);
				}
				fInViewPart = true;
			}
		}
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);
//		if (fInViewPart) {
//			fOpenGroup.setContext(context);
//		}
	}

	/*
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 * @since 3.5
	 */
	@Override
	public void dispose() {
//		if (fOpenGroup != null)
//			fOpenGroup.dispose();
		super.dispose();
	}

}
