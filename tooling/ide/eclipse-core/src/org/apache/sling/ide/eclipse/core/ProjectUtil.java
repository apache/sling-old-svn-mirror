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
		IPath syncDirectoryValue = ProjectUtil.getSyncDirectoryValue(project);
		if (syncDirectoryValue==null || syncDirectoryValue.isEmpty()) {
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
    public static IPath getSyncDirectoryValue(IProject project) {
        String value = null;
        try {
            value = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, PROPERTY_SYNC_ROOT));
            
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error(e.getMessage(), e);
        }

        // TODO central place for defaults
        if (value == null)
             return Path.fromOSString(PROPERTY_SYNC_ROOT_DEFAULT_VALUE);
        else {
             return Path.fromPortableString(value);
        }
    }
    
    public static File getSyncDirectoryFile(IProject project) {
    	return getSyncDirectoryValue(project).toFile();
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
    public static void setSyncDirectoryPath(IProject project, IPath path) {

        try {
            project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, PROPERTY_SYNC_ROOT), path.toPortableString());
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().error(e.getMessage(), e);
        }
    }
    
    /**
     * Loads a filter for the specified project
     * 
     * @param project the project to find a filter for
     * @return the found filter or null
     * @throws CoreException
     */
    public static Filter loadFilter(final IProject project) throws CoreException {

        FilterLocator filterLocator = Activator.getDefault().getFilterLocator();

        IPath filterPath = findFilterPath(project);
        if (filterPath == null) {
            return null;
        }

        IFile filterFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filterPath);
        Filter filter = null;
        if (filterFile != null && filterFile.exists()) {
            InputStream contents = filterFile.getContents();
            try {
                filter = filterLocator.loadFilter(contents);
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Failed loading filter file for project " + project.getName()
                                + " from location " + filterFile, e));
            } finally {
                IOUtils.closeQuietly(contents);
            }
        }
        return filter;
    }

    /**
     * Finds the path to a filter defined for the project
     * 
     * @param project the project
     * @return the path to the filter defined in the project, or null if no filter is found
     */
    public static IPath findFilterPath(final IProject project) {

        FilterLocator filterLocator = Activator.getDefault().getFilterLocator();

        IFolder syncFolder = ProjectUtil.getSyncDirectory(project);
        if (syncFolder == null) {
            return null;
        }
        File filterLocation = filterLocator.findFilterLocation(syncFolder.getLocation().toFile());
        if (filterLocation == null) {
            return null;
        }
        return Path.fromOSString(filterLocation.getAbsolutePath());
    }

    /**
     * Verifies if a resource is inside the content sync root for its defined project
     * 
     * @param resource
     * @return true if the resource is inside the content sync root
     */
    public static boolean isInsideContentSyncRoot(IResource resource) {

        if (resource == null) {
            return false;
        }

        IFolder syncRoot = getSyncDirectory(resource.getProject());
        if (syncRoot == null) {
            return false;
        }

        return syncRoot.getFullPath().isPrefixOf(resource.getFullPath());
    }

    private ProjectUtil() {

    }
}
