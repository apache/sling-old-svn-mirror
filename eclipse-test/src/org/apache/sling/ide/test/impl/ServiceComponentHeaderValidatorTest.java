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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.sling.ide.eclipse.internal.validation.ServiceComponentHeaderValidator;
import org.apache.sling.ide.test.impl.helpers.OsgiBundleManifest;
import org.apache.sling.ide.test.impl.helpers.Poller;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Rule;
import org.junit.Test;

public class ServiceComponentHeaderValidatorTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DefaultJavaVMInstall jvm = new DefaultJavaVMInstall();

    private ProjectAdapter project;
    private Poller poller;

    @Test
    public void missingDescriptorIsReported() throws CoreException, IOException, InterruptedException {

        String sch = "OSGI-INF/descriptor1.xml, OSGI-INF/descriptor2.xml";
        
        createJavaProject(sch);

        ServiceComponentHeaderValidator validator = new ServiceComponentHeaderValidator();
        List<IFile> missingDescriptors = validator.findMissingScrDescriptors(projectRule.getProject().getFile(
                "bin/META-INF/MANIFEST.MF"));

        assertThat("missingDescriptors.size", missingDescriptors.size(), equalTo(2));
        assertThat("missingDescriptors[0].projectRelativePath", missingDescriptors.get(0).getProjectRelativePath(),
                equalTo(Path.fromPortableString("bin/OSGI-INF/descriptor1.xml")));
        assertThat("missingDescriptors[0].projectRelativePath", missingDescriptors.get(1).getProjectRelativePath(),
                equalTo(Path.fromPortableString("bin/OSGI-INF/descriptor2.xml")));
    }

    private void createJavaProject(String serviceComponentHeader) throws CoreException, IOException,
            InterruptedException {

        project = new ProjectAdapter(projectRule.getProject());
        project.addNatures(JavaCore.NATURE_ID);
        project.configureAsJavaProject();

        OsgiBundleManifest mf = new OsgiBundleManifest("com.example.bundle001").version("1.0.0").serviceComponent(
                serviceComponentHeader);

        project.createOsgiBundleManifest(mf);

        poller = new Poller();
        poller.pollUntil(new Callable<IProject>() {

            @Override
            public IProject call() throws Exception {
                return projectRule.getProject();
            }
        }, hasFile("bin/META-INF/MANIFEST.MF"));
    }

    @Test
    public void wildCardHeaderIsIgnored() throws CoreException, IOException, InterruptedException {

        createJavaProject("OSGI-INF/*.xml");

        ServiceComponentHeaderValidator validator = new ServiceComponentHeaderValidator();
        List<IFile> missingDescriptors = validator.findMissingScrDescriptors(projectRule.getProject().getFile(
                "bin/META-INF/MANIFEST.MF"));

        assertThat("missingDescriptors.size", missingDescriptors.size(), equalTo(0));
    }

    @Test
    public void presentDescriptorIsNotReported() throws CoreException, IOException, InterruptedException {

        createJavaProject("OSGI-INF/descriptor.xml");

        project.createOrUpdateFile(Path.fromPortableString("src/OSGI-INF/descriptor.xml"), new ByteArrayInputStream(
                "<!-- does not matter -->".getBytes()));

        poller.pollUntil(new Callable<IProject>() {

            @Override
            public IProject call() throws Exception {
                return projectRule.getProject();
            }
        }, hasFile("bin/OSGI-INF/descriptor.xml"));

        ServiceComponentHeaderValidator validator = new ServiceComponentHeaderValidator();
        List<IFile> missingDescriptors = validator.findMissingScrDescriptors(projectRule.getProject().getFile(
                "bin/META-INF/MANIFEST.MF"));

        assertThat("missingDescriptors.size", missingDescriptors.size(), equalTo(0));
    }

    @Test
    public void manifestNotUnderOutputDirectoryIsIgnored() throws CoreException, IOException, InterruptedException {

        createJavaProject("OSGI-INF/descriptor.xml");

        ServiceComponentHeaderValidator validator = new ServiceComponentHeaderValidator();
        List<IFile> missingDescriptors = validator.findMissingScrDescriptors(projectRule.getProject().getFile(
                "src/META-INF/MANIFEST.MF"));

        assertThat("missingDescriptors.size", missingDescriptors.size(), equalTo(0));
    }

}
