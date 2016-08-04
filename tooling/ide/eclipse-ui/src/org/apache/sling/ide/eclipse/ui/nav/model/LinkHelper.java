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
package org.apache.sling.ide.eclipse.ui.nav.model;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.ILinkHelper;

import de.pdark.decentxml.Element;

public class LinkHelper implements ILinkHelper {

	@Override
	public IStructuredSelection findSelection(IEditorInput anInput) {
		IFile file = ResourceUtil.getFile(anInput);
        if (file == null) {
            return null;
        }
		IProject project = file.getProject();
		if (!ProjectHelper.isContentProject(project)) {
			return null;
		}
		IPath syncDirFullPath = ProjectUtil.getSyncDirectoryFullPath(project);
		if (syncDirFullPath==null) {
			return null;
		}
		if (!syncDirFullPath.isPrefixOf(file.getFullPath())) {
			return null;
		}
		JcrNode selectedNode = createSelectionNode(file);
		if (selectedNode!=null) {
			return new StructuredSelection(selectedNode);
		} else {
			return null;
		}
	}

	private JcrNode createSelectionNode(IResource resource) {
		if (resource==null) {
			return null;
		}
		final IContainer resourceParent = resource.getParent();
		if (resource instanceof IFolder) {
			IFolder container = (IFolder)resource;
			IPath syncDirFullPath = ProjectUtil.getSyncDirectoryFullPath(resource.getProject());
			if (syncDirFullPath.equals(container.getFullPath())) {
				// then we've reached the syncdir
				return new SyncDir(container);
			}
		} else if (!(resource instanceof IFile)) {
			return null;
		}
		JcrNode parent = createSelectionNode(resourceParent);
		if (parent==null) {
			return null;
		}
		Element domNode = null;
		JcrNode selectedNode = new JcrNode(parent, domNode, resource);
		return selectedNode;
	}

	@Override
	public void activateEditor(IWorkbenchPage aPage,
			IStructuredSelection aSelection) {
		final Object selectedElement = aSelection.getFirstElement();
		if (!(selectedElement instanceof JcrNode)) {
			return;
		}
		final JcrNode node = (JcrNode) selectedElement;
		// bring properties view to top, if it is open
		// SLING-3641 : moved link-with-editor behavior to the JCR Properties view atm
		//TODO: to be reviewed at a later stage with SLING-3641
//		IViewPart propertiesView = aPage.findView(IPageLayout.ID_PROP_SHEET);
//		if (propertiesView!=null) {
//			aPage.bringToTop(propertiesView);
//		}
		final IResource resource = node.getResource();
		if (resource==null || !(resource instanceof IFile)) {
			return;
		}
		final IFile selectedFile = (IFile)resource;
		for (final IEditorReference reference : aPage.getEditorReferences()) {
			if (reference==null) {
				continue;
			}
			final IEditorInput editorInput;
			try {
				editorInput = reference.getEditorInput();
			} catch (PartInitException e) {
				//TODO proper logging
				e.printStackTrace();
				continue;
			}
			if (editorInput==null) {
				continue;
			}
			if (!(editorInput instanceof IFileEditorInput)) {
				continue;
			}
			final IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
			final IFile file = fileEditorInput.getFile();
			if (file==null) {
				continue;
			}
			if (file.equals(selectedFile)) {
				aPage.bringToTop(reference.getEditor(true));
			}
		}
	}

}
