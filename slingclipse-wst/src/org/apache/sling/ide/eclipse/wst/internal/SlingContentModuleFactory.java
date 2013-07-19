package org.apache.sling.ide.eclipse.wst.internal;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

public class SlingContentModuleFactory extends ProjectModuleFactoryDelegate {

    private static final String NATURE_ID = "sling.content";

    @Override
    public ModuleDelegate getModuleDelegate(IModule arg0) {

        System.out.println("SlingContentModuleFactory.getModuleDelegate()");

        return new SlingContentModuleDelegate();
    }

    @Override
    protected IModule createModule(IProject project) {

        try {
            IFacetedProject facetedProject = ProjectFacetsManager.create(project);
            for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
                System.out.println("Project " + project + " has facet " + facet);
                if (facet.getProjectFacet().getId().equals(NATURE_ID)) {
                    return createModule(project.getName(), project.getName(), NATURE_ID, "1.0", project);
                }
            }
        } catch (CoreException ce) {
            // TODO logging
            }


        return null;
    }

    static class SlingContentModuleDelegate extends ModuleDelegate {
        @Override
        public IStatus validate() {
            return Status.OK_STATUS; // TODO actually validate
        }

        @Override
        public IModuleResource[] members() throws CoreException {
            return new IModuleResource[0]; // TODO revisit, do we have members?
        }

        @Override
        public IModule[] getChildModules() {
            return new IModule[0]; // TODO revisit, do we need child modules?
        }
    }
}
