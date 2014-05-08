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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
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
        assertSimpleCallsSucceeds("hello, world");

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/hello.txt"), new ByteArrayInputStream(
                "goodbye, world".getBytes()));

        Thread.sleep(2000); // for good measure, make sure the output is there - TODO replace with polling

        // verify that file is updated
        assertSimpleCallsSucceeds("goodbye, world");

        project.deleteMember(Path.fromPortableString("jcr_root/hello.txt"));

        Thread.sleep(2000); // for good measure, make sure the output is there - TODO replace with polling
        // verify that the file is deleted
        assertResourceNotFound();
    }

    private void assertSimpleCallsSucceeds(String expectedOutput) throws IOException, HttpException, URIException {
        HttpClient c = new HttpClient();
        GetMethod gm = new GetMethod(config.getUrl() + "hello.txt");
        try {
            int status = c.executeMethod(gm);

            assertThat("Unexpected status code for " + gm.getURI(), status, equalTo(200));
            assertThat("Unexpected response for " + gm.getURI(), gm.getResponseBodyAsString(), equalTo(expectedOutput));

        } finally {
            gm.releaseConnection();
        }
    }

    private void assertResourceNotFound() throws IOException, HttpException, URIException {
        HttpClient c = new HttpClient();
        GetMethod gm = new GetMethod(config.getUrl() + "hello.txt");
        try {
            int status = c.executeMethod(gm);

            assertThat("Unexpected status code for " + gm.getURI(), status, equalTo(404));
        } finally {
            gm.releaseConnection();
        }
    }

    @After
    public void cleanUp() throws HttpException, IOException {
        HttpClient c = new HttpClient();
        c.getParams().setAuthenticationPreemptive(true);
        c.getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
        PostMethod pm = new PostMethod(config.getUrl() + "hello.txt");
        Part[] parts = { new StringPart(":operation", "delete") };
        pm.setRequestEntity(new MultipartRequestEntity(parts, pm.getParams()));
        try {
            int status = c.executeMethod(pm);

            System.out.println("Delete operation got status " + status);
        } finally {
            pm.releaseConnection();
        }
    }
}
