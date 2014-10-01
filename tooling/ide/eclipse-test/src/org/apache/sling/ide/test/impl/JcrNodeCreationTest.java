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

import static org.apache.sling.ide.test.impl.helpers.EclipseResourceMatchers.hasFile;
import static org.apache.sling.ide.test.impl.helpers.EclipseResourceMatchers.hasFolder;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.RepositoryAccessor;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
import org.apache.sling.ide.test.impl.helpers.SlingWstServer;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * The <tt>JcrNodeCreationTest</tt> tests node creation scenarios
 *
 */
public class JcrNodeCreationTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config)).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    private SyncDir syncDirNode;

    @Before
    public void prepareProjectAndContent() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // directly create the root node
        syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

    }

    @Test
    public void createNtFolderNode() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("folder", "nt:folder");

        assertThat(projectRule.getProject(), hasFolder("/jcr_root/content/test-root/folder"));
    }

    @Test
    public void createNtFileNode() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("hello.txt", "nt:file");

        assertThat(projectRule.getProject(), hasFile("/jcr_root/content/test-root/hello.txt"));
    }

    @Test
    public void createFullCoverageNodeUnderFolder() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("messages", "sling:OsgiConfig");

        assertThat(projectRule.getProject(), hasFile("/jcr_root/content/test-root/messages.xml"));
    }

    @Test
    public void createFullCoverageNodeUnderPartialCoverageNode() throws Exception {

        IProject project = projectRule.getProject();
        new ProjectAdapter(project).createOrUpdateFile(Path
                .fromPortableString("jcr_root/content/test-root/holder/.content.xml"),
                getClass().getResourceAsStream("nt-unstructured-nodetype.xml"));

        JcrNode contentNode = syncDirNode.getNode("/content/test-root/holder");
        contentNode.createChild("org.apache.sling.SomeComponent", "sling:OsgiConfig");

        assertThat(project, hasFile("/jcr_root/content/test-root/holder/org.apache.sling.SomeComponent.xml"));
    }

    @Test
    public void createUnstructuredNodeWithSpecialName() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("sling:stuff", "nt:unstructured");

        assertThat(projectRule.getProject(), hasFile("/jcr_root/content/test-root/_sling_stuff/.content.xml"));
    }

    @Test
    public void createFullCoverageNodeWithSpecialName() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("sling:stuff", "sling:OsgiConfig");

        assertThat(projectRule.getProject(), hasFile("/jcr_root/content/test-root/_sling_stuff.xml"));
    }

    @Test
    public void createSlingFolderNodeWithSpecialName() throws Exception {

        JcrNode contentNode = syncDirNode.getNode("/content/test-root");
        contentNode.createChild("sling:stuff", "sling:Folder");

        assertThat(projectRule.getProject(), hasFile("/jcr_root/content/test-root/_sling_stuff/.content.xml"));
    }

    @After
    public void cleanup() throws Exception {
        new RepositoryAccessor(config).tryDeleteResource("/content/test-root");
    }
}
