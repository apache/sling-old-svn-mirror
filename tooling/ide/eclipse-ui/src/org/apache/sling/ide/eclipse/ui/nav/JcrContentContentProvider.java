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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider2;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

/** WIP: content provider for content package view in project explorer **/
public class JcrContentContentProvider implements ITreeContentProvider, IPipelinedTreeContentProvider2, IResourceChangeListener {

	private Object input;
	private TreeViewer viewer;

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	    if (viewer.getTree().isDisposed()) {
	        return;
	    }
		try {
            final Set<IProject> toBeRefreshed = new HashSet<>();
			event.getDelta().accept(new IResourceDeltaVisitor() {
				
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (delta.getResource() instanceof IContainer) {
						return true;
					}
					IProject p = delta.getResource().getProject();
					IFolder syncDir = getSyncDir(p);
					if (syncDir==null) {
						return false;
					}
					toBeRefreshed.add(syncDir.getProject());
					return true;
				}
			});

            for (final IProject project : toBeRefreshed) {
				viewer.getTree().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
                        long start = System.currentTimeMillis();
                        viewer.refresh(project, true);
                        long end = System.currentTimeMillis();
                        Activator.getDefault().getPluginLogger()
                                .tracePerformance("viewer.refresh({0},true)", (end - start), project);
					}
				});
			}
		} catch (CoreException e) {
			//TODO proper logging
			e.printStackTrace();
		}
	}
	
	@Override
	public void dispose() {
		// nothing to be done here
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.input = newInput;
		this.viewer = (TreeViewer)viewer;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			return projectGetChildren((IProject)parentElement);
		} else if (parentElement instanceof JcrNode) {
            long start = System.currentTimeMillis();
			JcrNode node = (JcrNode)parentElement;
            Object[] children = node.getChildren(true);
            long end = System.currentTimeMillis();
            Activator.getDefault().getPluginLogger()
                    .tracePerformance("node.getChildren for node at {0}", (end - start), node.getJcrPath());
            return children;
		} else {
			return null;
		}
	}

	private Object[] projectGetChildren(IProject parentElement) {
		IFolder syncDir = getSyncDir(parentElement);
		if (syncDir!=null && syncDir.exists()) {
			return new Object[] {new SyncDir(syncDir)};
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof JcrNode)) {
			return null;
		} else if (element instanceof SyncDir) {
			SyncDir syncDir = (SyncDir) element;
			return syncDir.getFolder().getProject();
		}
		JcrNode node = (JcrNode) element;
		return node.getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			return projectHasChildren((IProject)element);
		} else if (element instanceof JcrNode) {
			JcrNode jcrNode = (JcrNode) element;
			return jcrNode.hasChildren();
		}
		return false;
	}

	private boolean projectHasChildren(IProject project) {
		IFolder syncDir = getSyncDir(project);
		if (syncDir!=null && syncDir.exists()) {
			return true;
		}
		return false;
	}

	private IFolder getSyncDir(IProject project) {
		return ProjectUtil.getSyncDirectory(project);
	}

	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		// we're not adding any elements at the root level
		// hence nothing to be done here
	}


	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		// not fiddling with the parent
		// hence returning the suggested parent
		return aSuggestedParent;
	}


	@Override
	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification anAddModification) {
		// nothing to do here
		// passing along the parameter unchanged
		return anAddModification;
	}


	@Override
	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification aRemoveModification) {
		// nothing to do here
		// passing along the parameter unchanged
		return aRemoveModification;
	}


	@Override
	public boolean interceptRefresh(
			PipelinedViewerUpdate aRefreshSynchronization) {
		// nothing to do here
		return false;
	}


	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		// nothing to do here
		return false;
	}


	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				this,
				IResourceChangeEvent.POST_CHANGE);
	}


	@Override
	public void restoreState(IMemento aMemento) {
		// nothing to do here
	}


	@Override
	public void saveState(IMemento aMemento) {
		// nothing to do here
	}

	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		if (aParent instanceof IProject) {
			IProject project = (IProject)aParent;
			if (ProjectHelper.isContentProject(project)) {
				for (Iterator<?> it = theCurrentChildren.iterator(); it
						.hasNext();) {
					Object aChild = (Object) it.next();
					if (aChild instanceof IPackageFragmentRoot) {
						IPackageFragmentRoot ipfr = (IPackageFragmentRoot)aChild;
						IResource res = ipfr.getResource();
						IFolder syncDir = getSyncDir(project);
						if (res!=null && syncDir!=null && res.equals(syncDir)) {
							// then remove this one folder provided via j2ee content provider
							// reason: we are showing it too via the sling content provider
							it.remove();
							// and we can break here since there's only one syncdir currently
							break;
						}
						
					}
				}
			}
			Object[] children = projectGetChildren(project);
			if (children!=null && children.length>0) {
				theCurrentChildren.addAll(Arrays.asList(children));
			}
			return;
		} else if (aParent instanceof SyncDir) {
			theCurrentChildren.clear();
			Object[] children = getChildren(aParent);
			if (children!=null) {
				theCurrentChildren.addAll(Arrays.asList(children));
			}
		}
	}

	@Override
	public boolean hasPipelinedChildren(Object anInput,
			boolean currentHasChildren) {
		if (anInput instanceof IResource) {
			// then typically the 'currentHasChildren' is correct
			return currentHasChildren;
		}
		return true;
	}

}
