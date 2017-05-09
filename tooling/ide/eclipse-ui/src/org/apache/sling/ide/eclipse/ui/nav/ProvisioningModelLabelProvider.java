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

import org.apache.sling.ide.eclipse.ui.nav.model.ProvisioningModelRootFolder;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Mostly copied from org.eclipse.ui.internal.ide.dialogs.FileFolderSelectionDialog.FileLabelProvider.
 */
public class ProvisioningModelLabelProvider implements ILabelProvider {

    private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages()
            .getImage(ISharedImages.IMG_OBJ_FOLDER);
	
    @Override
    public void addListener(ILabelProviderListener listener) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {

    }

    @Override
    public Image getImage(Object element) {
    	if ( element instanceof ProvisioningModelRootFolder ) {
    		return IMG_FOLDER;
    	}
    	
    	return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof ProvisioningModelRootFolder) {
        	ProvisioningModelRootFolder rootFolder = (ProvisioningModelRootFolder) element;
            return rootFolder.getProjectRelativePath().toPortableString();
        }
        return null;
    }

}
