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
package org.apache.sling.ide.eclipse.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ModuleFolder;
import org.eclipse.wst.server.core.util.ProjectModule;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

public class SlingContentModuleFactory extends ProjectModuleFactoryDelegate {

    static final String SLING_CONTENT_FACET_ID = "sling.content";
	private static final IPath[] SETTINGS_PATHS = new IPath[] {new Path(".settings")};
    
	@Override
	public IModule[] getModules(IProject project) {
	    final IModule[] result = super.getModules(project);
	    if (result!=null && result.length>0) {
	        return result;
	    } else {
	        // try clearing the cache
	        // might fix SLING-3663 which could be due to a synchronization issue at first access time
	        clearCache(project);
	        return super.getModules(project);
	    }
	}
	
    @Override
    protected IPath[] getListenerPaths() {
    	// returning the .settings path instead of null (as done by the parent)
    	// results in clearing the cache on changes to .settings - which in turn
    	// results in re-evaluating facet changes.
    	// we could be more specific here but .settings changes are infrequent anyway.
    	return SETTINGS_PATHS;
    }

    @Override
    public ModuleDelegate getModuleDelegate(IModule module) {

        return new SlingContentModuleDelegate(module);
    }

    @Override
    protected IModule createModule(IProject project) {

        try {
            IFacetedProject facetedProject = ProjectFacetsManager.create(project);
            if (facetedProject == null) {
                return null;
            }
            for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
                if (facet.getProjectFacet().getId().equals(SLING_CONTENT_FACET_ID)) {
                    return createModule(project.getName(), project.getName(), SLING_CONTENT_FACET_ID, "1.0", project);
                }
            }
        } catch (CoreException ce) {
            // TODO logging
        }

        return null;
    }

    static class SlingContentModuleDelegate extends ProjectModule {


        private static final IModuleResource[] EMPTY_MODULE_RESOURCES = new IModuleResource[0];

        public SlingContentModuleDelegate(IModule module) {
            super(module.getProject());
        }

        @Override
        public IModuleResource[] members() throws CoreException {
            final List<IModuleResource> resources = new ArrayList<>();
            final IFolder syncFolder = ProjectUtil.getSyncDirectory(getProject());

            if (syncFolder == null || !syncFolder.exists()) {
                return EMPTY_MODULE_RESOURCES;
            }

            syncFolder.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {

                    IPath relativePath = resource.getProjectRelativePath();

                    IPath modulePath = relativePath.removeFirstSegments(syncFolder.getProjectRelativePath()
                            .segmentCount()); // remove sync dir

                    IModuleResource moduleFile = null;

                    if (resource.getType() == IResource.FILE) {
                        moduleFile = new ModuleFile((IFile) resource, resource.getName(), modulePath);
                    } else if (resource.getType() == IResource.FOLDER) {
                        moduleFile = new ModuleFolder((IFolder) resource, resource.getName(), modulePath);
                    }

                    if (moduleFile != null)
                        resources.add(moduleFile);

                    return true;
                }
            });

            // sort files before folders in the same folder
            // the reason is that we want to process .content.xml files before we
            // descend down folders
            Collections.sort(resources, new Comparator<IModuleResource>() {

                @Override
                public int compare(IModuleResource o1, IModuleResource o2) {

                    IPath p1 = o1.getModuleRelativePath();
                    IPath p2 = o2.getModuleRelativePath();

                    // shorter paths first
                    if (p1.segmentCount() != p2.segmentCount()) {
                        return p1.segmentCount() - p2.segmentCount();
                    }

                    // special-case the situation where they share a parent
                    // to implement the 'files before folders' logic

                    if (p1.removeLastSegments(1).equals(p2.removeLastSegments(1))) {

                        if (o1.getClass() != o2.getClass()) {
                            if (o1 instanceof IModuleFile) {
                                return -1; // files first
                            } else {
                                return 1;
                            }
                        }

                        return p1.segment(p1.segmentCount() - 1).compareTo(p2.segment(p2.segmentCount() - 1));
                    }

                    // find the first different segment path and return that
                    for (int i = 0; i < p1.segmentCount(); i++) {
                        String s1 = p1.segment(i);
                        String s2 = p2.segment(i);

                        int res = s1.compareTo(s2);

                        if (res != 0) {
                            return res;
                        }
                    }

                    throw new IllegalArgumentException("Could not sort " + o1 + " and " + o2);
                }
            });

            return resources.toArray(new IModuleResource[resources.size()]);
        }
    }
}
