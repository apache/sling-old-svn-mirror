package org.apache.sling.ide.eclipse.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public class SlingContentFacetUninstallDelegate implements IDelegate {

    @Override
    public void execute(IProject arg0, IProjectFacetVersion arg1, Object arg2, IProgressMonitor arg3)
            throws CoreException {
    }

}
