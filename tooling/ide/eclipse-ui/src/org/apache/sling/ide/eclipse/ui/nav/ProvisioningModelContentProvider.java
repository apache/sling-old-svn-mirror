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
import org.apache.sling.ide.eclipse.ui.nav.model.ProvisioningModelRootFolder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class ProvisioningModelContentProvider extends BaseRootFolderContentProvider<ProvisioningModelRootFolder> {

    public ProvisioningModelContentProvider() {
    	super(ProvisioningModelRootFolder.class);
	}
    
    @Override
    protected ProvisioningModelRootFolder findRootFolder(IProject project) {
    
    	IPath modelDirPath = ProjectUtil.getProvisioningModelPath(project);
    	
    	IFolder folder = project.getFolder(modelDirPath);
    	if ( !folder.exists() ) {
    		return null;
    	}
    	
    	return new ProvisioningModelRootFolder(folder);
    }
}
