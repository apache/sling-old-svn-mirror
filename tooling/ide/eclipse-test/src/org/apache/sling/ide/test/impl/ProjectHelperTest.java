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
package org.apache.sling.ide.test.impl;

import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

public class ProjectHelperTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Test
    public void inferContentProjectContentRootAtTheTopLevel() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.txt"), new ByteArrayInputStream(
                "goodbye, world".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("META-INF/vault/filter.xml"), new ByteArrayInputStream(
                "<workspaceFilter version=\"1.0\"/>".getBytes()));

        assertThat(ProjectHelper.getInferredContentProjectContentRoot(contentProject),
                CoreMatchers.<IContainer> equalTo(contentProject));

    }

    @Test
    public void inferContentProjectContentRootNested() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("src/main/content/jcr_root/test/hello.txt"),
                new ByteArrayInputStream("goodbye, world".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("src/main/content/META-INF/vault/filter.xml"),
                new ByteArrayInputStream("<workspaceFilter version=\"1.0\"/>".getBytes()));

        assertThat(ProjectHelper.getInferredContentProjectContentRoot(contentProject),
                CoreMatchers.<IContainer> equalTo(contentProject.getFolder("src/main/content")));

    }

    @Test
    public void inferContentProjectContentMissingFilter() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("src/main/content/jcr_root/test/hello.txt"),
                new ByteArrayInputStream("goodbye, world".getBytes()));

        assertThat(ProjectHelper.getInferredContentProjectContentRoot(contentProject), CoreMatchers.nullValue());
    }

    @Test
    public void inferContentProjectContentMissingJcrRoot() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("src/main/content/META-INF/vault/filter.xml"),
                new ByteArrayInputStream("<workspaceFilter version=\"1.0\"/>".getBytes()));

        assertThat(ProjectHelper.getInferredContentProjectContentRoot(contentProject), CoreMatchers.nullValue());
    }

    @Test
    public void projectWithInstalledFacetIsCandidate() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("src/main/content/jcr_root/test/hello.txt"),
                new ByteArrayInputStream("goodbye, world".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("src/main/content/META-INF/vault/filter.xml"),
                new ByteArrayInputStream("<workspaceFilter version=\"1.0\"/>".getBytes()));

        assertThat(ProjectHelper.isPotentialContentProject(contentProject), CoreMatchers.equalTo(true));
    }

    @Test
    public void projectWithoutInstalledFacetIsNotCandidate() throws CoreException, InterruptedException {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create files
        project.createOrUpdateFile(Path.fromPortableString("src/main/content/jcr_root/test/hello.txt"),
                new ByteArrayInputStream("goodbye, world".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("src/main/content/META-INF/vault/filter.xml"),
                new ByteArrayInputStream("<workspaceFilter version=\"1.0\"/>".getBytes()));

        assertThat(ProjectHelper.isPotentialContentProject(contentProject), CoreMatchers.equalTo(false));
    }
}
