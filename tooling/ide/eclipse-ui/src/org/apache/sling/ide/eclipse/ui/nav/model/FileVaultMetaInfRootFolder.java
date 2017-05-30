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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class FileVaultMetaInfRootFolder implements RootFolder{

    private final IFolder folder;

    public FileVaultMetaInfRootFolder(IFolder folder) {
        this.folder = folder;
    }

    @Override
    public IResource[] members() throws CoreException {
    	return folder.members();
    }
    
    @Override
    public IResource findMember(IPath path) {
    	return folder.findMember(path);
    }

	@Override
	public IPath getProjectRelativePath() {
		return folder.getProjectRelativePath();
	}
    
    @Override
    public String toString() {
        return folder.getProjectRelativePath().toString();
    }

    
}
