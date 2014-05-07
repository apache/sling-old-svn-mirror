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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.hamcrest.CoreMatchers;
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

    private final SlingWstServer server = new SlingWstServer();

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad()).around(server);

    @Test
    public void deployBundleOnServer() throws CoreException, InterruptedException, BackingStoreException, IOException {

        // prevent status prompts, since it can lead to the test Eclipse instance hanging
        // TODO - move to rule/utility class
        IEclipsePreferences debugPrefs = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
        debugPrefs.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false);
        debugPrefs.flush();

        server.waitForServerToStart();

        // create faceted project
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject bundleProject = root.getProject("bundle0001");
        bundleProject.create(new NullProgressMonitor());
        bundleProject.open(new NullProgressMonitor());

        IProjectDescription desc = bundleProject.getDescription();
        String[] natures = desc.getNatureIds();
        String[] newNatures = new String[natures.length + 2];
        newNatures[newNatures.length - 2] = JavaCore.NATURE_ID;
        newNatures[newNatures.length - 1] = "org.eclipse.wst.common.project.facet.core.nature";
        desc.setNatureIds(newNatures);
        bundleProject.setDescription(desc, new NullProgressMonitor());

        // get dependency to required artifacts
        Artifact slingApiJar = MavenPlugin.getMaven().resolve("org.apache.sling", "org.apache.sling.api", "2.2.0",
                "jar", "", MavenPlugin.getMaven().getArtifactRepositories(), new NullProgressMonitor());
        Artifact servletApiJar = MavenPlugin.getMaven().resolve("javax.servlet", "servlet-api", "2.4", "jar", "",
                MavenPlugin.getMaven().getArtifactRepositories(), new NullProgressMonitor());

        // create java project
        IJavaProject javaProject = JavaCore.create(bundleProject);
        Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
        entries.add(JavaRuntime.getDefaultJREContainerEntry());
        entries.add(JavaCore.newLibraryEntry(Path.fromOSString(slingApiJar.getFile().getAbsolutePath()), null, null));
        entries.add(JavaCore.newLibraryEntry(Path.fromOSString(servletApiJar.getFile().getAbsolutePath()), null, null));

        IFolder src = bundleProject.getFolder("src");
        src.create(true, true, new NullProgressMonitor());
        entries.add(JavaCore.newSourceEntry(src.getFullPath()));

        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), new NullProgressMonitor());

        IFolder bin = bundleProject.getFolder("bin");
        if (!bin.exists()) { // TODO - not sure why this exists...
            bin.create(true, true, new NullProgressMonitor());
        }
        javaProject.setOutputLocation(bin.getFullPath(), new NullProgressMonitor());

        IFolder example = src.getFolder("example");
        example.create(true, true, new NullProgressMonitor());
        IFile servletFile = example.getFile("SimpleServlet.java");
        InputStream simpleServlet = getClass().getResourceAsStream("SimpleServlet.java.txt");
        try {
            servletFile.create(simpleServlet, true, new NullProgressMonitor());
        } finally {
            IOUtils.closeQuietly(simpleServlet);
            ;
        }

        IFolder osgiInf = src.getFolder("OSGI-INF");
        osgiInf.create(true, true, new NullProgressMonitor());
        
        IFile servletDSDescriptor = osgiInf.getFile("SimpleServlet.xml");
        InputStream servletDSDescriptorFile = getClass().getResourceAsStream("SimpleServlet.xml");
        try {
            servletDSDescriptor.create(servletDSDescriptorFile, true, new NullProgressMonitor());
        } finally {
            IOUtils.closeQuietly(servletDSDescriptorFile);
        }

        IFolder metaInf = src.getFolder("META-INF");
        metaInf.create(true, true, new NullProgressMonitor());

        IFile manifest = metaInf.getFile("MANIFEST.MF");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Manifest m = new Manifest();
        m.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        m.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
        m.getMainAttributes().putValue("Bundle-Name", "Test bundle");
        m.getMainAttributes().putValue("Bundle-SymbolicName", "test.bundle001");
        m.getMainAttributes().putValue("Bundle-Version", "1.0.0.SNAPSHOT");
        m.getMainAttributes().putValue("Import-Package",
                "javax.servlet,org.apache.sling.api,org.apache.sling.api.servlets");
        m.getMainAttributes().putValue("Service-Component", "OSGI-INF/SimpleServlet.xml");
        m.write(baos);

        manifest.create(new ByteArrayInputStream(baos.toByteArray()), false, null);

        // install bundle facet
        IFacetedProject facetedProject = ProjectFacetsManager.create(bundleProject);
        IProjectFacet slingBundleFacet = ProjectFacetsManager.getProjectFacet("sling.bundle");
        IProjectFacetVersion projectFacetVersion = slingBundleFacet.getVersion("1.0");

        facetedProject.installProjectFacet(projectFacetVersion, null, new NullProgressMonitor());

        // deploy on server
        IModule bundleModule = ServerUtil.getModule(bundleProject);
        // TODO - why does this fail?
        if (bundleModule == null) {
            for (int i = 0; i < 5; i++) {
                System.out.println("bundleModule is null!");
                Thread.sleep(1000);
                bundleModule = ServerUtil.getModule(bundleProject);
                if (bundleModule != null) {
                    break;
                }
            }
        }

        IServerWorkingCopy serverWorkingCopy = server.getServer().createWorkingCopy();
        serverWorkingCopy.modifyModules(new IModule[] { bundleModule }, new IModule[0], new NullProgressMonitor());
        serverWorkingCopy.save(false, new NullProgressMonitor());

        System.out.println("You can inspect the project output " + bin.getLocation());

        Thread.sleep(1000); // for good measure, make sure the output is there - TODO remove

        HttpClient c = new HttpClient();
        GetMethod gm = new GetMethod("http://localhost:" + LaunchpadUtils.getLaunchpadPort() + "/simple-servlet");
        int status = c.executeMethod(gm);

        assertThat(status, CoreMatchers.equalTo(200));
        assertThat(gm.getResponseBodyAsString(), CoreMatchers.equalTo("Version 1"));
    }
}
