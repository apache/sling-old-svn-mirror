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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
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
public class ConnectionTest {

    private final SlingWstServer wstServer = new SlingWstServer();

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad()).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Test
    public void deployBundleOnServer() throws CoreException, InterruptedException, BackingStoreException, IOException {

        // prevent status prompts, since it can lead to the test Eclipse instance hanging
        // TODO - move to rule/utility class
        IEclipsePreferences debugPrefs = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
        debugPrefs.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false);
        debugPrefs.flush();

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
            simpleServlet = getClass().getResourceAsStream("SimpleServlet.java.txt");
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

        HttpClient c = new HttpClient();
        GetMethod gm = new GetMethod("http://localhost:" + LaunchpadUtils.getLaunchpadPort() + "/simple-servlet");
        try {
            int status = c.executeMethod(gm);

            assertThat("Unexpected status code for " + gm.getURI(), status, equalTo(200));
            assertThat("Unexpected response for " + gm.getURI(), gm.getResponseBodyAsString(), equalTo("Version 1"));

        } finally {
            gm.releaseConnection();
        }
    }
}
