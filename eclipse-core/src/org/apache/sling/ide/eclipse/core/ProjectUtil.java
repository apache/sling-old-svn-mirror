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
package org.apache.sling.ide.eclipse.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

public abstract class ProjectUtil {

    private static final String PROPERTY_SYNC_ROOT = "sync_root";
    private static final String PROPERTY_SYNC_ROOT_DEFAULT_VALUE = "jcr_root";

    public static IFolder getSyncDirectory(IProject project) {
    	if (project==null) {
    		return null;
    	}
		if (!project.isOpen()) {
			return null;
		} else if (!ProjectHelper.isContentProject(project)) {
			return null;
		}
		String syncDirectoryValue = ProjectUtil.getSyncDirectoryValue(project);
		if (syncDirectoryValue==null || syncDirectoryValue.length()==0) {
			return null;
		}
		IResource syncDir = project.findMember(syncDirectoryValue);
		if (syncDir==null || !(syncDir instanceof IFolder)) {
			return null;
		}
		return (IFolder) syncDir;
    }
    
    /**
     * Returns the value of the sync directory configured for a project.
     * 
     * <p>
     * The value is returned as a relative path to the project's location. If the property value is not set, it defaults
     * to {@value #PROPERTY_SYNC_ROOT_DEFAULT_VALUE}.
     * </p>
     * 
     * @param project the project, must not be null
     * @return the value of the sync directory
     */
    public static String getSyncDirectoryValue(IProject project) {
        String value = null;
        try {
            value = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, PROPERTY_SYNC_ROOT));
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error(e.getMessage(), e);
        }

        // TODO central place for defaults
        if (value == null)
            value = PROPERTY_SYNC_ROOT_DEFAULT_VALUE;

        return value;
    }
    
    public static File getSyncDirectoryFile(IProject project) {
    	return new File(project.getLocation().toFile(), getSyncDirectoryValue(project));
    }

    public static IPath getSyncDirectoryFullPath(IProject project) {

        return project.getFolder(getSyncDirectoryValue(project)).getFullPath();
    }

    /**
     * Sets the value of the sync directory configured for a project
     * 
     * <p>
     * The value must be a path relative to the project's location.
     * </p>
     * 
     * @param project the project, must not be null
     * @param path the value
     */
    public static void setSyncDirectoryPath(IProject project, String path) {

        try {
            project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, PROPERTY_SYNC_ROOT), path);
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error(e.getMessage(), e);
        }
    }
    
    public static Filter loadFilter(final IProject project) throws CoreException {
        IFolder syncFolder = ProjectUtil.getSyncDirectory(project);

        FilterLocator filterLocator = Activator.getDefault().getFilterLocator();
        File filterLocation = filterLocator.findFilterLocation(syncFolder.getLocation().toFile());
        if (filterLocation == null) {
            return null;
        }
        IPath filterPath = Path.fromOSString(filterLocation.getAbsolutePath());
        IFile filterFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filterPath);
        Filter filter = null;
        if (filterFile != null && filterFile.exists()) {
            InputStream contents = filterFile.getContents();
            try {
                filter = filterLocator.loadFilter(contents);
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Failed loading filter file for project " + syncFolder.getProject().getName()
                                + " from location " + filterFile, e));
            } finally {
                IOUtils.closeQuietly(contents);
            }
        }
        return filter;
    }


    private ProjectUtil() {

    }
}
