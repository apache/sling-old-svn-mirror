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
package org.apache.sling.ide.test.impl.ui.sightly;

import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.sling.ide.eclipse.sightly.ui.internal.JavaUtils;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

public class JavaUtilsTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();    
    
    @Test
    public void inferPackageName() throws Exception {
        
        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install facets
        project.installFacet("sling.content", "1.0");
        project.installFacet("sightly", "1.1");
        
        // create basic html file
        IPath path = Path.fromOSString("jcr_root/apps/components/app/forum");
        IPath fullPath = project.ensureDirectoryExists(path).getFullPath();
        
        String inferredPackage = JavaUtils.inferPackage(fullPath);
        assertThat(inferredPackage, CoreMatchers.equalTo("apps.components.app.forum"));
    }
}
