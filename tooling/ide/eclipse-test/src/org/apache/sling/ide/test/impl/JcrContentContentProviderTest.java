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
import java.io.InputStream;

import org.apache.sling.ide.eclipse.ui.nav.JcrContentContentProvider;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

public class JcrContentContentProviderTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void listChildrenInNestedStructure() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // instantiate the content provider
        JcrContentContentProvider contentProvider = new JcrContentContentProvider();

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

        // test children of '/'
        Object[] children = contentProvider.getChildren(syncDirNode);
        assertChildrenHavePaths(children, "/content");

        // test children of '/content'
        JcrNode contentNode = (JcrNode) children[0];
        Object[] children2 = contentProvider.getChildren(contentNode);
        assertChildrenHavePaths(children2, "/content/test-root");

        // test children of '/content/test-root'
        JcrNode testRootNode = (JcrNode) children2[0];
        Object[] children3 = contentProvider.getChildren(testRootNode);
        assertChildrenHavePaths(children3, "/content/test-root/en");

        // test children of '/content/test-root/en'
        JcrNode enNode = (JcrNode) children3[0];
        Object[] children4 = contentProvider.getChildren(enNode);
        assertChildrenHavePaths(children4, "/content/test-root/en/message", "/content/test-root/en/error",
                "/content/test-root/en/warning");

        // test children of '/content/test-root/en/message'
        JcrNode messageNode = (JcrNode) children4[0];
        Object[] children5 = contentProvider.getChildren(messageNode);
        assertChildrenHavePaths(children5); // no children
    }

    @Test
    public void listChildrenWithNestedContentXmlInEscapedDir() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create .content.xml structure
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), getClass()
                .getResourceAsStream("sling-folder-nodetype.xml"));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/_sling_stuff/.content.xml"), getClass()
                .getResourceAsStream("nt-unstructured-nodetype.xml"));

        // instantiate the content provider
        JcrContentContentProvider contentProvider = new JcrContentContentProvider();

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

        // test children of '/'
        Object[] children = contentProvider.getChildren(syncDirNode);
        assertChildrenHavePaths(children, "/content");

        // test children of '/content'
        JcrNode contentNode = (JcrNode) children[0];
        Object[] children2 = contentProvider.getChildren(contentNode);
        assertChildrenHavePaths(children2, "/content/sling:stuff");

        // test children of '/content/sling:stuff
        JcrNode slingStuffNode = (JcrNode) children2[0];
        Object[] children3 = contentProvider.getChildren(slingStuffNode);
        assertChildrenHavePaths(children3); // no children

    }

    @Test
    public void listChildrenWhenContentXmlIsBroken() throws Exception {
        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create .content.xml structure
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), new ByteArrayInputStream(
                "invalid".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/child1.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/child2.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // instantiate the content provider
        JcrContentContentProvider contentProvider = new JcrContentContentProvider();

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

        // test children of '/'
        Object[] children = contentProvider.getChildren(syncDirNode);
        assertChildrenHavePaths(children, "/content");

        // test children of '/content'
        JcrNode contentNode = (JcrNode) children[0];
        Object[] children2 = contentProvider.getChildren(contentNode);
        assertChildrenHavePaths(children2, "/content/child1.txt", "/content/child2.txt");
    }

    private void assertChildrenHavePaths(Object[] children, String... paths) {
        assertThat("Unexpected number of children found", children.length, CoreMatchers.equalTo(paths.length));
        for (int i = 0; i < children.length; i++) {

            Object child = children[i];
            assertThat("Unexpected type of child", child, CoreMatchers.instanceOf(JcrNode.class));

            JcrNode node = (JcrNode) child;
            assertThat("Unexpected path for child at index " + i, node.getJcrPath(), CoreMatchers.equalTo(paths[i]));
        }
    }

}
