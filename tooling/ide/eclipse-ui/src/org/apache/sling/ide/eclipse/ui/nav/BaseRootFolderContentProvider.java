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

import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.RootFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public abstract class BaseRootFolderContentProvider<T extends RootFolder> implements ITreeContentProvider {
	
	protected static final Object[] NO_CHILDREN = new Object[0];
	private final Class<T> klazz;
	
	public BaseRootFolderContentProvider(Class<T> klazz) {
		this.klazz = klazz;
	}

    @Override
    public boolean hasChildren(Object parentElement) {
        // the getChildren is not expensive, therefore we leverage that here
        return getChildren(parentElement) != null;
    }
    
    @Override
    public Object[] getChildren(Object parentElement) {
    	if ( parentElement == null ) {
    		return NO_CHILDREN;
    	}
    	
        // expose the contribution directly below the project
        if (parentElement instanceof IProject) {
            IProject project = (IProject) parentElement;
            RootFolder folder = findRootFolder(project);
            if (folder != null) {
            	return new Object[] { folder };
            }
        } else if (klazz.isAssignableFrom(parentElement.getClass())) {
        	RootFolder rootFolder = (RootFolder) parentElement;
            try {
                return rootFolder.members();
            } catch (CoreException e) {
                Activator.getDefault().getPluginLogger().error(
                        "Could not list members of  " + rootFolder, e);
            }
        }
        return NO_CHILDREN;
    }
    
    @Override
    public Object getParent(Object element) {
        if (element instanceof IResource) {
            IResource resource = (IResource) element;
            RootFolder rootFolder = findRootFolder(resource.getProject());
            if (rootFolder != null) {
                return rootFolder.findMember(resource.getFullPath());
            }
        }
        return null;
    }
    

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	@Override
	public Object[] getElements(Object inputElement) {
		return null;
	}
    
    protected abstract T findRootFolder(IProject project);
}
