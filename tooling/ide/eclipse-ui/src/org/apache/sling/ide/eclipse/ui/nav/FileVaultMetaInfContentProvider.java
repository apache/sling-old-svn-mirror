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

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.FileVaultMetaInfRootFolder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class FileVaultMetaInfContentProvider implements ITreeContentProvider {

    private static final String FILEVAULT_METAINF_PATH = "META-INF/vault";
    private static final Object[] NO_CHILDREN = new Object[0];

    private FileVaultMetaInfRootFolder getFileVaultMetaInfFolder(IProject project) {
        if (ProjectHelper.isContentProject(project)) {
            IFolder syncDir = ProjectUtil.getSyncDirectory(project);
            if (syncDir != null) {
                IFolder metaInfFolder = syncDir.getParent().getFolder(new Path(FILEVAULT_METAINF_PATH));
                if (metaInfFolder.exists()) {
                    return new FileVaultMetaInfRootFolder(metaInfFolder);
                }
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

    @Override
    public Object[] getChildren(Object parentElement) {
        final Object[] children;
        // expose the filevault meta-inf directly below the project
        if (parentElement instanceof IProject) {
            IProject project = (IProject) parentElement;
            FileVaultMetaInfRootFolder folder = getFileVaultMetaInfFolder(project);
            if (folder != null) {
                children = new Object[1];
                children[0] = folder;
                return children;
            }
        } else if (parentElement instanceof FileVaultMetaInfRootFolder) {
            FileVaultMetaInfRootFolder fileVaultMetaInfRootFolder = (FileVaultMetaInfRootFolder) parentElement;
            try {
                return fileVaultMetaInfRootFolder.getFolder().members();
            } catch (CoreException e) {
                Activator.getDefault().getPluginLogger().error(
                        "Could not list members of folder " + fileVaultMetaInfRootFolder.getFolder().getName(), e);
            }
        }
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof IResource) {
            IResource resource = (IResource) element;
            FileVaultMetaInfRootFolder fileVaultMetaInfoFolder = getFileVaultMetaInfFolder(resource.getProject());
            if (fileVaultMetaInfoFolder != null) {
                return fileVaultMetaInfoFolder.getFolder().findMember(resource.getFullPath()) != null;
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object parentElement) {
        // the getChildren is not expensive, therefore we leverage that here
        return getChildren(parentElement) != null;
    }

}
