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

import org.apache.sling.ide.eclipse.core.internal.SlingContentModuleFactory;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class SlingContentModuleAdapterTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Test
    public void projectMembersContainContentXmlFirst() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/1_file.txt"), new ByteArrayInputStream(
                new byte[0]));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/2_folder/filler.txt"),
                new ByteArrayInputStream(new byte[0]));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/3_file.txt"), new ByteArrayInputStream(
                new byte[0]));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/4_folder/filler.txt"),
                new ByteArrayInputStream(new byte[0]));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/5_file.txt"), new ByteArrayInputStream(
                new byte[0]));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), new ByteArrayInputStream(
                new byte[0]));

        IModule module = ServerUtil.getModule(contentProject);

        SlingContentModuleFactory moduleFactory = new SlingContentModuleFactory();
        ModuleDelegate moduleDelegate = moduleFactory.getModuleDelegate(module);

        IModuleResource[] members = moduleDelegate.members();

        assertThat("members[0].path", members[0].getModuleRelativePath().toPortableString(), equalTo(""));
        assertThat("members[1].path", members[1].getModuleRelativePath().toPortableString(), equalTo("content"));
        assertThat("members[2].path", members[2].getModuleRelativePath().toPortableString(),
                equalTo("content/.content.xml"));
    }

    @Test
    public void projectMembersContainDotDirFoldersLast() throws Exception {

        // create faceted project
        IProject contentProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(contentProject);
        project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

        // install bundle facet
        project.installFacet("sling.content", "1.0");

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/testproject/_jcr_content/image/file"),
                new ByteArrayInputStream(new byte[0]));
        project.createOrUpdateFile(
                Path.fromPortableString("jcr_root/content/testproject/_jcr_content/image/file.dir/.content.xml"),
                new ByteArrayInputStream(new byte[0]));
        project.createOrUpdateFile(
                Path.fromPortableString("jcr_root/content/testproject/_jcr_content/image/file.dir/_jcr_content/_dam_thumbnails/_dam_thumbnail_319.png"),
                new ByteArrayInputStream(new byte[0]));

        IModule module = ServerUtil.getModule(contentProject);

        SlingContentModuleFactory moduleFactory = new SlingContentModuleFactory();
        ModuleDelegate moduleDelegate = moduleFactory.getModuleDelegate(module);

        IModuleResource[] members = moduleDelegate.members();
        
        int fileIdx = -1;
        int fileDirIdx = -1;
        
        for (int i = 0; i < members.length; i++) {
            String memberName = members[i].getModuleRelativePath().lastSegment();
            if (memberName == null) {
                continue;
            } else if (memberName.equals("file.dir")) {
                fileDirIdx = i;
            } else if (memberName.equals("file")) {
                fileIdx = i;
            }
        }

        Assert.assertTrue("file not sorted before file.dir ; file index = " + fileIdx + ", file.dir index = "
                + fileDirIdx, fileIdx < fileDirIdx);

    }
}
