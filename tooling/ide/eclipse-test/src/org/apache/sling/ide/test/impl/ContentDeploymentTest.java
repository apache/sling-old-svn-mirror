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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
import org.apache.sling.ide.test.impl.helpers.RepositoryAccessor;
import org.apache.sling.ide.test.impl.helpers.SlingWstServer;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
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
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void deployFile() throws CoreException, InterruptedException, URIException, HttpException, IOException {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/hello.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        Thread.sleep(1000); // for good measure, make sure the output is there - TODO replace with polling

        // verify that file is created
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.assertGetIsSuccessful("hello.txt", "hello, world");

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/hello.txt"), new ByteArrayInputStream(
                "goodbye, world".getBytes()));

        Thread.sleep(2000); // for good measure, make sure the output is there - TODO replace with polling

        // verify that file is updated
        repo.assertGetIsSuccessful("hello.txt", "goodbye, world");

        project.deleteMember(Path.fromPortableString("jcr_root/hello.txt"));

        Thread.sleep(2000); // for good measure, make sure the output is there - TODO replace with polling

        // verify that the file is deleted
        repo.assertGetReturns404("hello.txt");
    }

    @After
    public void cleanUp() throws HttpException, IOException {

        new RepositoryAccessor(config).tryDeleteResource("hello.txt");
    }
}
