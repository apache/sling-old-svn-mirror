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
package org.apache.sling.ide.eclipse.sightly;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class FacetHelper {


    private static final String SIGHTLY_FACET = "sightly";

    public static boolean hasSightlyFacet(IProject project) {
        
        return containsFacet(project, SIGHTLY_FACET);
    }
    
    // TODO - copied from eclipse-core/.../ProjectHelper
    private static boolean containsFacet(IProject project, String facetId) {
        // deleted modules can trigger a publish call without having an attached project
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
