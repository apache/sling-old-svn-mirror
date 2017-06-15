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
import org.apache.sling.ide.eclipse.ui.nav.model.FileVaultMetaInfRootFolder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

public class FileVaultMetaInfContentProvider extends BaseRootFolderContentProvider<FileVaultMetaInfRootFolder> {

	private static final String FILEVAULT_METAINF_PATH = "META-INF/vault";
	
    public FileVaultMetaInfContentProvider() {
		super(FileVaultMetaInfRootFolder.class);
	}

    @Override
    protected FileVaultMetaInfRootFolder findRootFolder(IProject project) {
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
}
