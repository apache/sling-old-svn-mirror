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
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasFileContent;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPath;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPrimaryType;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPropertyValue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * The <tt>ContentDeploymentTest</tt> validates simple content deployment based on the resources changes in the
 * workspace
 *
 */
public class ContentDeploymentTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config)).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public FailOnUnsuccessfulEventsRule failOnEventsRule = new FailOnUnsuccessfulEventsRule();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void deployFile() throws CoreException, InterruptedException, URIException, HttpException, IOException {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // verify that file is created
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Void>() {
            @Override
            public Void call() throws HttpException, IOException {
                repo.assertGetIsSuccessful("test/hello.txt", "hello, world");
                return null;
            }
        }, nullValue(Void.class));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.txt"), new ByteArrayInputStream(
                "goodbye, world".getBytes()));

        // verify that file is updated
        poller.pollUntil(new Callable<Void>() {
            @Override
            public Void call() throws HttpException, IOException {
                repo.assertGetIsSuccessful("test/hello.txt", "goodbye, world");
                return null;
            }
        }, nullValue(Void.class));


        project.deleteMember(Path.fromPortableString("jcr_root/test/hello.txt"));

        // verify that file is deleted
        poller.pollUntil(new Callable<Void>() {
            @Override
            public Void call() throws HttpException, IOException {
                repo.assertGetReturns404("test/hello.txt");
                return null;
            }
        }, nullValue(Void.class));

    }

    @Test
    public void changeNodePrimaryType() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // verifications
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        assertThatNode(repo, poller, "/test", allOf(hasPath("/test"), hasPrimaryType("nt:folder"), hasChildrenCount(1)));

        // change node type to sling:Folder
        InputStream contentXml = getClass().getResourceAsStream("sling-folder-nodetype.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/.content.xml"), contentXml);

        // verifications (2)
        assertThatNode(repo, poller, "/test",
                allOf(hasPath("/test"), hasPrimaryType("sling:Folder"), hasChildrenCount(1)));
    }

    @Test
    public void deployFileWithAttachedMetadata() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.esp"), new ByteArrayInputStream(
                "// not really javascript".getBytes()));

        // verify that file is created
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        assertThatNode(repo, poller, "/test/hello.esp", hasPrimaryType("nt:file"));

        InputStream contentXml = getClass().getResourceAsStream("file-custom-mimetype.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.esp.dir/.content.xml"), contentXml);

        assertThatNode(repo, poller, "/test/hello.esp/jcr:content", hasPropertyValue("jcr:mimeType", "text/javascript"));
    }

    @Test
    public void fileDeployedBeforeAddingModuleToServerIsPublished() throws Throwable {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/hello.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // verify that file is created
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/test/hello.txt");
            }
        }, hasFileContent("hello, world"));

    }

    /**
     * This test validates that if the parent of a resource that does not exist in the repository the resource is
     * successfully created
     * 
     * @throws Exception
     */
    @Test
    public void deployFileWithMissingParentFromRepository() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        // create file
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/demo/nested/structure/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        // verify that file is created
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/test/demo/nested/structure/hello.txt");
            }
        }, hasFileContent("hello, world"));
    }

    @Test
    public void filedDeployedWithFullCoverageSiblingDoesNotCauseSpuriousDeletion() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create filter.xml
        project.createVltFilterWithRoots("/test");
        // create sling:Folder at /test/folder
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/folder/.content.xml"), getClass()
                .getResourceAsStream("sling-folder-nodetype.xml"));

        // create nt:file at /test/folder/hello.esp
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/folder/hello.esp"), new ByteArrayInputStream(
                "// not really javascript".getBytes()));

        // create sling:OsgiConfig at /test/folder/config.xml
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/folder/config.xml"), getClass()
                .getResourceAsStream("com.example.some.Component.xml"));

        // verify that config node is created
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();

        assertThatNode(repo, poller, "/test/folder/config", hasPrimaryType("sling:OsgiConfig"));

        // update file at /test/folder/hello.esp
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/test/folder/hello.esp"), new ByteArrayInputStream(
                "// maybe javascript".getBytes()));

        // wait until the file is updated
        assertThatNode(repo, poller, "/test/folder/hello.esp", hasFileContent("// maybe javascript"));

        // verify that the sling:OsgiConfig node is still present
        assertThatNode(repo, poller, "/test/folder/config", hasPrimaryType("sling:OsgiConfig"));
    }

    private void assertThatNode(final RepositoryAccessor repo, Poller poller, final String nodePath, Matcher<Node> matcher)
            throws InterruptedException {

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                Node node = repo.getNode(nodePath);
                return node;

            }
        }, matcher);
    }

    @After
    public void cleanUp() throws Exception {

        new RepositoryAccessor(config).tryDeleteResource("/test");
    }
}
