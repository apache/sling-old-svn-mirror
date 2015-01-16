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

import static org.apache.sling.ide.test.impl.helpers.EclipseResourceMatchers.hasFile;
import static org.apache.sling.ide.test.impl.helpers.EclipseResourceMatchers.hasFolder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.ImportRepositoryContentAction;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ExternalSlingLaunchpad;
import org.apache.sling.ide.test.impl.helpers.FailOnModificationEventsRule;
import org.apache.sling.ide.test.impl.helpers.LaunchpadConfig;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.RepositoryAccessor;
import org.apache.sling.ide.test.impl.helpers.RepositoryAccessor.SessionRunnable;
import org.apache.sling.ide.test.impl.helpers.ServerAdapter;
import org.apache.sling.ide.test.impl.helpers.SlingWstServer;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;


public class ContentImportTest {

    private final LaunchpadConfig config = LaunchpadConfig.getInstance();

    private final SlingWstServer wstServer = new SlingWstServer(config);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ExternalSlingLaunchpad(config)).around(wstServer);

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    @Rule
    public FailOnModificationEventsRule deh = new FailOnModificationEventsRule();

    @Test
    public void importFilesAndFolders() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root/en");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/en", "nt:folder");
        repo.createNode("/content/test-root/en/files", "nt:folder");
        repo.createFile("/content/test-root/en/files/first.txt", "first file".getBytes());

        runImport(contentProject);

        assertThat(contentProject, hasFolder("jcr_root/content/test-root/en/files"));
        assertThat(contentProject, hasFile("jcr_root/content/test-root/en/files/first.txt", "first file".getBytes()));
    }

    @Test
    public void importFilesAndFoldersRespectsVltFilters() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root/en");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/de", "nt:folder");
        repo.createNode("/content/test-root/de/files", "nt:folder");
        repo.createFile("/content/test-root/de/files/first.txt", "first file".getBytes());

        deh.clearUnexpectedEventsAfterSettling();

        runImport(contentProject);

        assertThat(contentProject.findMember("jcr_root/content/test-root/de"), nullValue());
    }

    @Test
    public void importFilesAndFoldersRespectsVltIgnore() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root/en");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.vltignore"),
                new ByteArrayInputStream("en\n".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/en", "nt:folder");
        repo.createNode("/content/test-root/en/files", "nt:folder");
        repo.createFile("/content/test-root/en/files/first.txt", "first file".getBytes());

        runImport(contentProject);

        assertThat(contentProject.findMember("jcr_root/content/test-root/en"), nullValue());
    }

    @Test
    public void importFilesAndFoldersRespectsVltIgnoreNotUnderImportRoot() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root/en");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/hello.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/.vltignore"), new ByteArrayInputStream(
                "content/test-root/en\n".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/en", "nt:folder");
        repo.createNode("/content/test-root/en/files", "nt:folder");
        repo.createFile("/content/test-root/en/files/first.txt", "first file".getBytes());

        runImport(contentProject);

        assertThat(contentProject.findMember("jcr_root/content/test-root/en"), nullValue());
    }

    private void runImport(IProject contentProject) throws SerializationException, InvocationTargetException,
            InterruptedException, CoreException {

        ImportRepositoryContentAction action = new ImportRepositoryContentAction(wstServer.getServer(),
                Path.fromPortableString("/content/test-root"), contentProject, Activator.getDefault()
                        .getSerializationManager());

        action.run(new NullProgressMonitor());
    }
    
    @Test
    public void importFilesWithExtraNodesUnderJcrContent() throws Exception {
        
        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        // create filter.xml
        project.createVltFilterWithRoots("/content/test-root");

        // create sling:Folder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"),
                getClass().getResourceAsStream("sling-folder-nodetype.xml"));

        wstServer.waitForServerToStart();

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.tryDeleteResource("/content/test-root");
        repo.createNode("/content/test-root", "sling:Folder");
        repo.createFile("/content/test-root/file.txt", "hello, world".getBytes());
        repo.doWithSession(new SessionRunnable<Void>() {
            @Override
            public Void doWithSession(Session session) throws RepositoryException {

                ValueFactory valueFactory = session.getValueFactory();
                
                Node contentNode = session.getNode("/content/test-root/file.txt/jcr:content");
                contentNode.addMixin("sling:chunks");

                Node chunkNode = contentNode.addNode("firstChunk", "sling:chunk");
                chunkNode.setProperty("sling:offset", valueFactory.createValue(0));
                chunkNode.setProperty( "jcr:data",
                        valueFactory.createValue( valueFactory.createBinary(
                                        new ByteArrayInputStream("hello, world".getBytes()))));

                session.save();

                return null;
            }
        });
        
        assertThat(repo.getNode("/content/test-root").getPrimaryNodeType().getName(), equalTo("sling:Folder"));

        runImport(contentProject);

        assertThat("File not properly imported", contentProject,
                hasFile("jcr_root/content/test-root/file.txt", "hello, world".getBytes()));
        assertThat("File extra serialization dir not imported", contentProject,
                hasFolder("jcr_root/content/test-root/file.txt.dir"));
        assertThat("File jcr:content data not serialized in .content.xml", contentProject,
                hasFile("jcr_root/content/test-root/file.txt.dir/.content.xml"));
        assertThat("File jcr:content extra dir not serialized as _jcr_content", contentProject,
                hasFolder("jcr_root/content/test-root/file.txt.dir/_jcr_content"));
        assertThat("First chunk dir not serialized", contentProject,
                hasFolder("jcr_root/content/test-root/file.txt.dir/_jcr_content/firstChunk"));
        assertThat("First chunk properties not serialized", contentProject,
                hasFile("jcr_root/content/test-root/file.txt.dir/_jcr_content/firstChunk/.content.xml"));
        assertThat("Sling folder serialization file was deleted after import", contentProject,
                hasFile("jcr_root/content/test-root/.content.xml"));
    }

    @Test
    public void importFullCoverageNode() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.doWithSession(new SessionRunnable<Void>() {

            @Override
            public Void doWithSession(Session session) throws RepositoryException {
                Node configNode = session.getRootNode().addNode(
                        "content/test-root/org.apache.sling.example.Component.config", "sling:OsgiConfig");
                configNode.setProperty("enabled", Boolean.TRUE.toString());

                session.save();

                return null;

            }
        });

        runImport(contentProject);

        assertThat(contentProject,
                hasFile("jcr_root/content/test-root/org.apache.sling.example.Component.config.xml"));
    }

    @Test
    public void importDeletesNoLongerPresentFile() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        // create server-side content
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/en", "nt:folder");
        repo.createNode("/content/test-root/en/files", "nt:folder");
        repo.createFile("/content/test-root/en/files/first.txt", "first file".getBytes());
        repo.createFile("/content/test-root/en/files/second.txt", "second file".getBytes());

        // delete file since it was deployed when the module was added to the server
        repo.tryDeleteResource("/content/test-root/hello.txt");

        // run initial import
        runImport(contentProject);

        // first.txt and second.txt should be present
        assertThat(contentProject, hasFile("jcr_root/content/test-root/en/files/first.txt"));
        assertThat(contentProject, hasFile("jcr_root/content/test-root/en/files/second.txt"));
        // hello.txt is not present in the repo and should be deleted
        assertThat(contentProject, not(hasFile("jcr_root/content/test-root/hello.txt")));

    }
    
    @Test
    public void importFromRepositoryWithNtUnstructuredNodeWithNoPropertiesCausesSpuriousLocalDeletion()
            throws Exception {

        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content/test-root/en", "nt:folder");

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        wstServer.waitForServerToStart();

        project.createVltFilterWithRoots("/content/test-root");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));
        
        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        repo.createNode("/content/test-root/folder", "sling:Folder");
        repo.createNode("/content/test-root/folder/jcr:content", "nt:unstructured");
        repo.createFile("/content/test-root/folder/jcr:content/some_file.txt", "dummy contents".getBytes());
        repo.doWithSession(new SessionRunnable<Void>() {

            @Override
            public Void doWithSession(Session session) throws RepositoryException {
                session.getRootNode().getNode("content/test-root/folder/jcr:content/some_file.txt/jcr:content")
                        .setProperty("jcr:mimeType", "x-vendor-reserved");
                session.save();

                return null;
            }
        });

        // run initial import
        runImport(contentProject);

        assertThat(contentProject,
                hasFile("jcr_root/content/test-root/folder/_jcr_content/some_file.txt.dir/.content.xml"));

        // run second import
        runImport(contentProject);

        assertThat(contentProject,
                hasFile("jcr_root/content/test-root/folder/_jcr_content/some_file.txt.dir/.content.xml"));
        
    }
    
    @Test
    public void importFileWithNamespacedName() throws Exception {

        RepositoryAccessor repo = new RepositoryAccessor(config);

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        project.createVltFilterWithRoots("/content/test-root");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/hello.txt"),
                new ByteArrayInputStream("hello, world".getBytes()));

        wstServer.waitForServerToStart();

        ServerAdapter server = new ServerAdapter(wstServer.getServer());
        server.installModule(contentProject);

        deh.clearUnexpectedEventsAfterSettling();

        repo.createFile("/content/test-root/sling:file", "some_content".getBytes());

        runImport(contentProject);

        assertThat(contentProject, hasFile("jcr_root/content/test-root/_sling_file"));
    }

    @Before
    public void setUp() throws Exception {
        RepositoryAccessor repo = new RepositoryAccessor(config);
        repo.createNode("/content", "nt:folder");
        repo.createNode("/content/test-root", "nt:folder");
    }

    @After
    public void cleanUp() throws Exception {
        new RepositoryAccessor(config).tryDeleteResource("/content/test-root");
    }
}
