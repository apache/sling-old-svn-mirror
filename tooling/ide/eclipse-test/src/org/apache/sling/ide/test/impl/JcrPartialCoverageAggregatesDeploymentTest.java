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
import org.apache.sling.ide.test.impl.helpers.FailOnUnsuccessfulEventsRule;
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

    @Rule
    public FailOnUnsuccessfulEventsRule failOnEventsRule = new FailOnUnsuccessfulEventsRule();

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

        // create filter.xml
        project.createVltFilterWithRoots("/content");

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

        // create filter.xml
        project.createVltFilterWithRoots("/content");
        
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
        // create filter.xml
        project.createVltFilterWithRoots("/content");

        // create a nt:file at /content/test-root/file.txt
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/folder/file.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        // create a sling:OrderedFolder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children.xml"));

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root"), hasPrimaryType("sling:OrderedFolder"),
                hasChildrenNames("folder", "jcr:content"));

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
                hasChildrenNames("jcr:content", "folder"));

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root");

            }
        }, postConditions);
    }
    
    @Test
    public void deployNodeWithContentXmlInParentFolder() throws Exception {
        
        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/content");
        
        // the expected repository structure is
        // mapping [sling:Mapping]
        // \- jcr:content [nt:unstructured]
        //  \- par [nt:unstructured]
        //   \- folder [sling:Folder] 
        
        // first create the local structure where the content is completely serialized inside the .content.xml file
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/mapping/.content.xml"),
                getClass().getResourceAsStream("sling-mapping-with-folder-child.xml"));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();

        // wait until the structure is published
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/mapping/jcr:content/par");

            }
        }, hasPrimaryType("nt:unstructured"));

        // now create the folder and its associated .content.xml file ; this will cause
        // intermediate folders to be created whose serialization data is present in the
        // parent's .content.xml file
        project.createOrUpdateFile(
                Path.fromPortableString("jcr_root/content/test-root/mapping/_jcr_content/par/folder/.content.xml"),
                getClass().getResourceAsStream("sling-folder-nodetype.xml"));

        // first wait until the sling:Folder child node is created, to ensure that all changes are processed
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/mapping/jcr:content/par/folder");

            }
        }, hasPrimaryType("sling:Folder"));

        // then, verify that the nt:unstructured node is correctly written
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/mapping/jcr:content/par");

            }
        }, hasPrimaryType("nt:unstructured"));
    }
    
    @Test
    public void deployNodeWithContentXmlInParentFolder_reverse() throws Exception {
        
        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/content");
        
        // the expected repository structure is
        // mapping [sling:Mapping]
        // \- jcr:content [nt:unstructured]
        //  \- par [nt:unstructured]
        //   \- folder [sling:Folder] 
        
        // first create the local structure where the content is completely serialized inside the .content.xml file
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/mapping/.content.xml"),
                getClass().getResourceAsStream("sling-mapping-with-folder-child.xml"));
        // now create the folder and its associated .content.xml file ; this will cause
        // intermediate folders to be created whose serialization data is present in the
        // parent's .content.xml file
        project.createOrUpdateFile(
                Path.fromPortableString("jcr_root/content/test-root/mapping/_jcr_content/par/folder/.content.xml"),
                getClass().getResourceAsStream("sling-folder-nodetype.xml"));        

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();

        // wait until the structure is published
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/mapping/jcr:content/par/folder");

            }
        }, hasPrimaryType("sling:Folder"));

        // change the folder's node type to nt:unstructured and make sure it's covered by the parent
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/mapping/.content.xml"),
                getClass().getResourceAsStream("sling-mapping-with-unstructured-child.xml"));

        // oh boy, where do I start ...
        // this test keeps failing with issues which suggest concurrency problems, e.g. InvalidStateException
        // I've tried to work around this issue by refreshing the session, serialising the executions with a
        // SingleThreadExecutor, inspecting the before/after repo state ... but in the end only this
        // 'harmless' sleep fixes the test. It will have to do for now, at least until SLING-4438 is fixed
        Thread.sleep(1000);

        // delete the sling folder node type since the serialization is now completely covered by
        // the parent node
        project.deleteMember(Path.fromPortableString("jcr_root/content/test-root/mapping/_jcr_content"));

        // validate that the primary type has changed
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/mapping/jcr:content/par/folder");

            }
        }, hasPrimaryType("nt:unstructured"));
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
