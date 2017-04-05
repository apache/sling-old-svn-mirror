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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.apache.sling.ide.eclipse.sightly.internal.SightlyNatureTester;
import org.apache.sling.ide.eclipse.ui.nav.JcrContentContentProvider;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SightlyNatureTesterTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    private SightlyNatureTester tester;
    private ProjectAdapter projectAdapter;

    @Before
    public void prepare() throws Exception {

        final IProject project = projectRule.getProject();

        projectAdapter = new ProjectAdapter(project);
        projectAdapter.addNatures("org.eclipse.wst.common.project.facet.core.nature");
        projectAdapter.installFacet("sling.content", "1.0");

        tester = new SightlyNatureTester();
    }

    @Test
    public void testOnSightlyProject() throws Exception {

        testOnProject(true);
    }

    private void testOnProject(boolean hasSightlyNature) throws CoreException {

        if (hasSightlyNature) {
            projectAdapter.installFacet("sightly", "1.1");
        }

        final IPath sightlyTemplatePath = Path.fromPortableString("/jcr_root/libs/my/component/html.html");
        projectAdapter.createOrUpdateFile(sightlyTemplatePath, new ByteArrayInputStream(("<html />").getBytes()));

        // test on resources directly
        assertEquals("Test on project", hasSightlyNature,
                tester.test(projectRule.getProject(), "sightlyNature", new Object[0], null));
        assertEquals("Test on folder", hasSightlyNature,
                tester.test(projectRule.getProject().getFolder("jcr_root"), "sightlyNature", new Object[0], null));
        assertEquals("Test on file", hasSightlyNature, tester
                .test(projectRule.getProject().getFile(sightlyTemplatePath), "sightlyNature", new Object[0], null));
        
        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) projectRule.getProject().findMember("jcr_root"));
        assertEquals("Test on sync dir node", hasSightlyNature,
                tester.test(syncDirNode, "sightlyNature", new Object[0], null));
        
        // test on jcr nodes
        JcrContentContentProvider contentProvider = new JcrContentContentProvider();
        JcrNode firstChild = (JcrNode) contentProvider.getChildren(syncDirNode)[0];
        assertEquals("Test on jcr node", hasSightlyNature,
                tester.test(firstChild, "sightlyNature", new Object[0], null));

    }

    @Test
    public void testOnNonSightlyProject() throws Exception {

        testOnProject(false);
    }
}
