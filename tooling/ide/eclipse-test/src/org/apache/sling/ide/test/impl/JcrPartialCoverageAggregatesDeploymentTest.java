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

import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasChildrenCount;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasChildrenNames;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPath;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPrimaryType;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPropertyValue;
import static org.hamcrest.CoreMatchers.allOf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.Poller;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.RepositoryAccessor;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
import org.apache.sling.ide.test.impl.helpers.SlingWstServer;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * The <tt>JcrFullCoverageAggregatesDeploymentTest</tt> validates deployment of full-coverage aggregates
 * 
 * @see <a href="https://jackrabbit.apache.org/filevault/vaultfs.html">Vault FS</a>
 *
 */
public class JcrPartialCoverageAggregatesDeploymentTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config)).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void deploySlingFolder() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create a sling:Folder at /content/test-root
        InputStream contentXml = getClass().getResourceAsStream("sling-folder-nodetype.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), contentXml);

        // create a sling:Folder at /content/test-root/nested
        InputStream childContentXml = getClass().getResourceAsStream("sling-folder-nodetype.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/.content.xml"),
                childContentXml);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root"), hasPrimaryType("sling:Folder"),
                hasChildrenCount(1));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root");

            }
        }, postConditions);

        // update jcr:title for /content/test-root
        InputStream updatedContentXml = getClass().getResourceAsStream("sling-folder-nodetype-with-title.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"),
                updatedContentXml);

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root");

            }
        }, allOf(hasPath("/content/test-root"), hasChildrenCount(1), hasPropertyValue("jcr:title", "Some Folder")));

    }

    @Test
    public void deployNodeWithChildrenAndOrderableNodeTypes() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create a sling:Folder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-folder-nodetype.xml"));

        // create a nt:unstructured at /content/test-root/nested
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/.content.xml"),
                getClass().getResourceAsStream("nt-unstructured-nodetype.xml"));

        // create a nt:unstructured at /content/test-root/nested/nested
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/nested/.content.xml"),
                getClass().getResourceAsStream("nt-unstructured-nodetype.xml"));

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/nested"), hasPrimaryType("nt:unstructured"),
                hasChildrenCount(1));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/nested");

            }
        }, postConditions);

        // update jcr:title for /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/.content.xml"),
                getClass().getResourceAsStream("nt-unstructured-nodetype-with-title.xml"));

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/nested");

            }
        }, allOf(hasPath("/content/test-root/nested"), hasChildrenCount(1), hasPropertyValue("jcr:title", "Some Folder")));
    }

    @Test
    public void deploySlingOrderedFolderWithJcrContentNode() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create a nt:file at /content/test-root/file.txt
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/file.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        // create a sling:OrderedFolder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children.xml"));

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root"), hasPrimaryType("sling:OrderedFolder"),
                hasChildrenNames("file.txt", "jcr:content"));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root");

            }
        }, postConditions);

        // reorder the children of the /content/test-root node
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children-reordered.xml"));

        postConditions = allOf(hasPath("/content/test-root"), hasPrimaryType("sling:OrderedFolder"),
                hasChildrenNames("jcr:content", "file.txt"));

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root");

            }
        }, postConditions);
    }

    @Before
    public void ensureCleanState() throws Exception {
        new RepositoryAccessor(config).tryDeleteResource("/content/test-root");
    }

    @After
    public void cleanup() throws Exception {
        new RepositoryAccessor(config).tryDeleteResource("/content/test-root");
    }
}
