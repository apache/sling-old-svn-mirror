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
package org.apache.sling.ide.test.impl.sightly;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.validation.ValidationFramework;
import org.junit.Rule;
import org.junit.Test;

public class SightlyNatureTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();
    
    @Test
    public void slyTagDoesNotCreateProblemMarker() throws Exception {

        testValidationMarkers(false);
    }
    
    private void testValidationMarkers(boolean invalidTag) throws Exception {
        
        final IProject project = projectRule.getProject();
        
        // create faceted project
        ProjectAdapter projectAdapter = new ProjectAdapter(project);
        projectAdapter.addNatures("org.eclipse.wst.common.project.facet.core.nature");
        projectAdapter.installFacet("sling.content", "1.0");
        projectAdapter.installFacet("sightly", "1.1");
        
        final IPath sightlyTemplatePath = Path.fromPortableString("/jcr_root/libs/my/component/html.html");
        String tagName = invalidTag ? "invalid-tag" : "sly";
        projectAdapter.createOrUpdateFile(sightlyTemplatePath, new ByteArrayInputStream(("<" + tagName + " />").getBytes()));
        
        ValidationFramework.getDefault().join(new NullProgressMonitor());
        
        IMarker[] markers = project.findMember(sightlyTemplatePath).findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
        
        if ( invalidTag ) {
            assertThat("markers.length", markers.length, equalTo(1));
            // might be overspecifying
            assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), equalTo("Unknown tag (invalid-tag)."));
        } else {
            assertThat("markers.length", markers.length, equalTo(0));
        }
        
    }

    @Test
    public void invalidTagDoesCreatesProblemMarker() throws Exception {
        
        testValidationMarkers(false);
    }
}
