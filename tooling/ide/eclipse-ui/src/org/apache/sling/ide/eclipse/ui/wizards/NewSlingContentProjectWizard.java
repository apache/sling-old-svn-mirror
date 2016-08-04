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
package org.apache.sling.ide.eclipse.ui.wizards;

import static org.apache.sling.ide.eclipse.core.progress.ProgressUtils.advance;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.wst.server.core.IServer;

public class NewSlingContentProjectWizard extends AbstractNewSlingApplicationWizard {

    private WizardNewProjectCreationPage page;

    @Override
    public void addPages() {
        
        page = new WizardNewProjectCreationPage("New Project");
        page.setImageDescriptor(WhitelabelSupport.getProjectWizardBanner());
        page.setDescription("Please select the coordinates for the new content project");
        addPage(page);
        addPage(getSetupServerWizardPage());
    }

    @Override
    protected List<IProject> createProjects(IProgressMonitor monitor) throws CoreException {
        IProject existingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(page.getProjectName());
        if (existingProject!=null && existingProject.exists()) {
            throw new IllegalStateException("Project already exists with name "+page.getProjectName());
        }
        IProject project = page.getProjectHandle();

        List<Operation> ops = new ArrayList<>();

        ops.add(new CreateProject(project));
        ops.add(new OpenProject(project));

        ops.add(new CreateFolder(project, "jcr_root"));

        ops.add(new CreateFolder(project, "jcr_root/content"));
        ops.add(new CreateFile(project, "jcr_root/content/.content.xml", getClass().getResourceAsStream(
                "res/folder.content.xml")));
        ops.add(new CreateFolder(project, "jcr_root/content/example"));
        ops.add(new CreateFile(project, "jcr_root/content/example/.content.xml", getClass().getResourceAsStream(
                "res/.content.xml")));

        ops.add(new CreateFolder(project, "jcr_root/apps"));
        ops.add(new CreateFolder(project, "jcr_root/apps/example"));
        ops.add(new CreateFolder(project, "jcr_root/apps/example/item"));
        ops.add(new CreateFile(project, "jcr_root/apps/example/item/html.jsp", getClass().getResourceAsStream(
                "res/html.jsp")));

        ops.add(new CreateFolder(project, "META-INF"));
        ops.add(new CreateFolder(project, "META-INF/vault"));
        ops.add(new CreateFile(project, "META-INF/vault/filter.xml", getClass().getResourceAsStream("res/filter.xml")));
        ops.add(new CreateFile(project, "META-INF/vault/config.xml", getClass().getResourceAsStream("res/config.xml")));
        ops.add(new CreateFile(project, "META-INF/vault/settings.xml", getClass().getResourceAsStream(
                "res/settings.xml")));

        monitor.beginTask("Creating project", ops.size());
        try {
            for (Operation op : ops) {
                op.execute(monitor);
                advance(monitor, 1);
            }
        } finally {
            monitor.done();
        }

        return Collections.singletonList(project);
    }

    @Override
    protected Projects configureCreatedProjects(List<IProject> createdProjects, IProgressMonitor monitor)
            throws CoreException {
        Projects projects = new Projects();
        for (IProject project : createdProjects) {
            ConfigurationHelper.convertToContentPackageProject(project, monitor, new Path("jcr_root"));
            projects.getContentProjects().add(project);
        }

        IServer server = getSetupServerWizardPage().getOrCreateServer(monitor);
        advance(monitor, 1);

        finishConfiguration(createdProjects, server, monitor);
        advance(monitor, 1);

        return projects;
    }

    @Override
    public String doGetWindowTitle() {
        return "New Content Project";
    }

    private static interface Operation {
        void execute(IProgressMonitor monitor) throws CoreException;
    }

    public static class CreateProject implements Operation {
        private final IProject project;

        public CreateProject(IProject project) {
            this.project = project;
        }

        @Override
        public void execute(IProgressMonitor monitor) throws CoreException {
            project.create(monitor);
        }
    }

    public static class OpenProject implements Operation {

        private final IProject project;

        public OpenProject(IProject project) {
            this.project = project;
        }

        @Override
        public void execute(IProgressMonitor monitor) throws CoreException {
            this.project.open(monitor);
        }
    }

    public static class CreateFolder implements Operation {
        private final IProject project;
        private final String folderName;

        public CreateFolder(IProject project, String folderName) {
            this.project = project;
            this.folderName = folderName;
        }

        @Override
        public void execute(IProgressMonitor monitor) throws CoreException {
            this.project.getFolder(folderName).create(true, true, monitor);
        }

    }

    public static class CreateFile implements Operation {
        private final IProject project;
        private final String fileName;
        private final InputStream input;

        public CreateFile(IProject project, String fileName, InputStream input) {
            this.project = project;
            this.fileName = fileName;
            this.input = input;
        }

        @Override
        public void execute(IProgressMonitor monitor) throws CoreException {
            try {
                this.project.getFile(fileName).create(input, true, monitor);
            } finally {
                IOUtils.closeQuietly(input);
            }
        }

    }

}
