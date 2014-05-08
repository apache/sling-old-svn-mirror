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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.MavenDependency;
import org.apache.sling.ide.test.impl.helpers.OsgiBundleManifest;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
import org.apache.sling.ide.test.impl.helpers.SlingWstServer;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.apache.sling.ide.test.impl.helpers.ToolingSupportBundle;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.osgi.service.prefs.BackingStoreException;

// http://www.eclipse.org/forums/index.php/t/457988/
// https://github.com/jbosstools/jbosstools-server/blob/master/as/tests/org.jboss.ide.eclipse.as.test/src/org/jboss/ide/eclipse/as/test/util/ServerRuntimeUtils.java
// https://github.com/jbosstools/jbosstools-server/blob/master/as/tests/org.jboss.ide.eclipse.as.archives.integration.test/src/org/jboss/ide/eclipse/as/archives/integration/test/BuildDeployTest.java
// https://stackoverflow.com/questions/6660155/eclipse-plugin-java-based-project-how-to
/**
 * The <tt>BundleDeploymentTest</tt> validates the basic workflows behind working with OSGi bundles
 *
 */
public class BundleDeploymentTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config))
            .around(new ToolingSupportBundle(config))
            .around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Test
    public void deployBundleOnServer() throws CoreException, InterruptedException, BackingStoreException, IOException,
            OsgiClientException {

        wstServer.waitForServerToStart();

        // create faceted project
        IProject bundleProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(bundleProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // configure java project with dependencies
        MavenDependency slingApiDep = new MavenDependency().groupId("org.apache.sling")
                .artifactId("org.apache.sling.api").version("2.2.0");
        MavenDependency servletApiDep = new MavenDependency().groupId("javax.servlet").artifactId("servlet-api")
                .version("2.4");
        project.configureAsJavaProject(slingApiDep, servletApiDep);

        // create DS component class
        InputStream simpleServlet = null;
        try {
            simpleServlet = getClass().getResourceAsStream("SimpleServlet.java.v1.txt");
            project.createOrUpdateFile(Path.fromPortableString("src/example/SimpleServlet.java"), simpleServlet);
        } finally {
            IOUtils.closeQuietly(simpleServlet);
        }
        
        // create DS component descriptor
        InputStream servletDescriptor = null;
        try {
            servletDescriptor = getClass().getResourceAsStream("SimpleServlet.xml");
            project.createOrUpdateFile(Path.fromPortableString("src/OSGI-INF/SimpleServlet.xml"), servletDescriptor);
        } finally {
            IOUtils.closeQuietly(servletDescriptor);
        }

        // create manifest
        OsgiBundleManifest manifest = OsgiBundleManifest.symbolicName("test.bundle001").version("1.0.0.SNAPSHOT")
                .name("Test bundle").serviceComponent("OSGI-INF/SimpleServlet.xml")
                .importPackage("javax.servlet,org.apache.sling.api,org.apache.sling.api.servlets");
        project.createOsgiBundleManifest(manifest);

        // install bundle facet
        project.installFacet("sling.bundle", "1.0");

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(bundleProject);

        Thread.sleep(1000); // for good measure, make sure the output is there - TODO replace with polling

        assertSimpleServletCallsSucceeds("Version 1");

        // create DS component class
        InputStream simpleServlet2 = null;
        try {
            simpleServlet2 = getClass().getResourceAsStream("SimpleServlet.java.v2.txt");
            project.createOrUpdateFile(Path.fromPortableString("src/example/SimpleServlet.java"), simpleServlet2);
        } finally {
            IOUtils.closeQuietly(simpleServlet2);
        }

        Thread.sleep(1000); // for good measure, make sure the output is there - TODO replace with polling

        assertSimpleServletCallsSucceeds("Version 2");
    }

    private void assertSimpleServletCallsSucceeds(String expectedOutput) throws IOException, HttpException, URIException {
        HttpClient c = new HttpClient();
        GetMethod gm = new GetMethod(config.getUrl() + "simple-servlet");
        try {
            int status = c.executeMethod(gm);

            assertThat("Unexpected status code for " + gm.getURI(), status, equalTo(200));
            assertThat("Unexpected response for " + gm.getURI(), gm.getResponseBodyAsString(), equalTo(expectedOutput));

        } finally {
            gm.releaseConnection();
        }
    }
}
