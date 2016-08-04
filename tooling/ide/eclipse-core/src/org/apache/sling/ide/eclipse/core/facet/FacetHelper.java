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
package org.apache.sling.ide.eclipse.core.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Groups common utility methods related to project facets
 */
public abstract class FacetHelper {

    private FacetHelper() {
        
    }
    
    /**
     * Checks if the specified project has the specified facet
     * 
     * @param project the project, may be <code>null</code>
     * @param facetId the facet to check for
     * @return true if the specified <tt>project</tt> has the specified <tt>facetId</tt>
     */
    public static boolean containsFacet(IProject project, String facetId) {

        if (project == null) {
            return false;
        }
        
        IFacetedProject facetedProject = (IFacetedProject) project.getAdapter(IFacetedProject.class);
        if (facetedProject==null ) {
            return false;
        }
        
        IProjectFacet facet = ProjectFacetsManager.getProjectFacet(facetId);
        return facetedProject.hasProjectFacet(facet);
    }    
}
