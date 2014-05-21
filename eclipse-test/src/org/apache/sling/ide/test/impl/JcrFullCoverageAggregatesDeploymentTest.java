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
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasMixinTypes;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPath;
import static org.apache.sling.ide.test.impl.helpers.jcr.JcrMatchers.hasPrimaryType;
import static org.hamcrest.CoreMatchers.allOf;

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
import org.junit.Ignore;
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
public class JcrFullCoverageAggregatesDeploymentTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config)).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void deployNestedFullCoverageAggregate() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(3));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);
    }

    @Test
    public void deleteNodeFromNestedFullCoverageAggreate() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(3));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);

        // update .content.xml structure
        InputStream updatedContentXml = getClass().getResourceAsStream("content-nested-structure-deleted-node.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), updatedContentXml);

        // poll until we only have 2 child nodes left
        postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(2));
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);
    }

    @Test
    @Ignore(value = "SLING-3591")
    public void deleteAllNodesFromNestedFullCoverageAggreate() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(3));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);

        // update .content.xml structure
        InputStream updatedContentXml = getClass()
                .getResourceAsStream("content-nested-structure-deleted-all-nodes.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), updatedContentXml);

        // poll until we only have no child nodes left
        postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(0));
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);
    }

    @Test
    public void reorderNodesFromNestedFullCoverageAggregate() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // install content facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenNames("message", "error", "warning"));

        final RepositoryAccessor repo = new RepositoryAccessor(config);
        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);

        // update .content.xml structure
        InputStream updatedContentXml = getClass().getResourceAsStream("content-nested-structure-reordered-nodes.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), updatedContentXml);

        // poll until we have the child nodes reordered
        postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:OrderedFolder"),
                hasMixinTypes("mix:language"), hasChildrenNames("message", "warning", "error"));

        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);

    }

    @Test
    public void deployNestedFullCoverageAggregateAtFilterRoot() throws Exception {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // create filter.xml
        InputStream filterXml = getClass().getResourceAsStream("filter-only-content-test-root-en.xml");
        project.createOrUpdateFile(Path.fromPortableString("META-INF/vault/filter.xml"), filterXml);

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // install content facet
        project.installFacet("sling.content", "1.0");

        // create prerequisite data
        final RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content", "sling:Folder");
        repo.createNode("/content/test-root", "sling:Folder");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Matcher<Node> postConditions = allOf(hasPath("/content/test-root/en"), hasPrimaryType("sling:Folder"),
                hasMixinTypes("mix:language"), hasChildrenCount(3));

        Poller poller = new Poller();
        poller.pollUntil(new Callable<Node>() {
            @Override
            public Node call() throws RepositoryException {
                return repo.getNode("/content/test-root/en");

            }
        }, postConditions);
    }

    @After
    public void cleanup() throws Exception {
        new RepositoryAccessor(config).tryDeleteResource("/content/test-root");
    }
}
