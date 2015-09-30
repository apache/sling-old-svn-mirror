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

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.ide.eclipse.core.internal.ResourceChangeCommandFactory;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.SpyCommand;
import org.apache.sling.ide.test.impl.helpers.SpyRepository;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.util.PathUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceChangeCommandFactoryTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();
    private IProject contentProject;
    private ProjectAdapter project;
    private ResourceChangeCommandFactory factory;
    private Repository spyRepo;

    @Before
    public void setUp() throws Exception {

        // create faceted project
        contentProject = projectRule.getProject();

        project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");

        factory = new ResourceChangeCommandFactory(Activator.getDefault().getSerializationManager());

        spyRepo = new SpyRepository();

    }

    @Test
    public void commandForAddedOrUpdatedNtFolder() throws CoreException {

        // create a sling:Folder at /content/test-root/nested
        InputStream childContentXml = getClass().getResourceAsStream("sling-folder-nodetype.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/.content.xml"),
                childContentXml);

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(spyRepo,
                contentProject.findMember("jcr_root/content/test-root"));

        assertThat("command.path", command.getPath(), nullValue());
        assertThat("command.resource.path", command.getResourceProxy().getPath(), equalTo("/content/test-root"));
        assertThat("command.resource.properties", command.getResourceProxy().getProperties(),
                equalTo(singletonMap("jcr:primaryType", (Object) "nt:folder")));
        assertThat("command.fileinfo", command.getFileInfo(), nullValue());
        assertThat("command.kind", command.getSpyKind(), equalTo(SpyCommand.Kind.ADD_OR_UPDATE));
    }

    @Test
    public void commandForAddedOrUpdatedSlingFolder() throws CoreException {

        // create a sling:Folder at /content/test-root/nested
        InputStream childContentXml = getClass().getResourceAsStream("sling-folder-nodetype-with-title.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/nested/.content.xml"),
                childContentXml);

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(spyRepo,
                contentProject.findMember("jcr_root/content/test-root/nested"));

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("jcr:primaryType", "sling:Folder");
        props.put("jcr:title", "Some Folder");

        assertThat("command.path", command.getPath(), nullValue());
        assertThat("command.resource.path", command.getResourceProxy().getPath(), equalTo("/content/test-root/nested"));
        assertThat("command.resource.properties", command.getResourceProxy().getProperties(), equalTo(props));
        assertThat("command.fileinfo", command.getFileInfo(), nullValue());
        assertThat("command.kind", command.getSpyKind(), equalTo(SpyCommand.Kind.ADD_OR_UPDATE));
    }

    @Test
    public void commandForSlingOrderedFolder_children() throws CoreException {

        // create a sling:OrderedFolder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children.xml"));
        // create the child folder listed in the .content.xml file
        contentProject.getFolder("jcr_root/content/test-root/folder").create(true, true, new NullProgressMonitor());

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(spyRepo,
                contentProject.findMember("jcr_root/content/test-root"));

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(2));
    }

    @Test
    public void commandForSlingOrderedFolder_childrenMissingFromFilesystem() throws CoreException {

        // create a sling:OrderedFolder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children.xml"));

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(spyRepo,
                contentProject.findMember("jcr_root/content/test-root"));

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(1));
    }

    @Test
    public void commandForSlingOrderedFolder_extraChildrenInTheFilesystem() throws CoreException {

        // create a sling:OrderedFolder at /content/test-root
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/.content.xml"), getClass()
                .getResourceAsStream("sling-ordered-folder-with-children.xml"));
        // create the child folder listed in the .content.xml file
        contentProject.getFolder("jcr_root/content/test-root/folder").create(true, true, new NullProgressMonitor());
        // create an extra folder not listed in the .content.xml file
        contentProject.getFolder("jcr_root/content/test-root/folder2").create(true, true, new NullProgressMonitor());

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(spyRepo,
                contentProject.findMember("jcr_root/content/test-root"));

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(3));
        assertThat("command.resource.children[2].name", PathUtil.getName(children.get(2).getPath()), equalTo("folder2"));
    }
}
