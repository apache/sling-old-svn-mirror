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

import java.util.Collections;
import java.util.List;

import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.ui.internal.SharedImages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.wst.server.core.IServer;

public class NewSlingContentProjectWizard extends AbstractNewSlingApplicationWizard {

    private WizardNewProjectCreationPage page;

    @Override
    public void addPages() {
        
        page = new WizardNewProjectCreationPage("New Project");
        page.setImageDescriptor(SharedImages.SLING_LOG);
        page.setDescription("Please select the coordinates for the new Sling content project");
        addPage(page);
        addPage(getSetupServerWizardPage());
    }

    @Override
    protected List<IProject> createProjects(IProgressMonitor monitor) throws CoreException {
        IProject project = page.getProjectHandle();
        project.create(monitor);
        project.open(monitor);
        project.getFolder("jcr_root").create(true, true, monitor);
        project.getFolder("jcr_root/content").create(true, true, monitor);
        project.getFolder("jcr_root/content/example").create(true, true, monitor);
        project.getFolder("jcr_root/apps").create(true, true, monitor);
        project.getFolder("jcr_root/apps/example").create(true, true, monitor);
        project.getFolder("jcr_root/apps/example/item").create(true, true, monitor);
        project.getFolder("META-INF").create(true, true, monitor);
        project.getFolder("META-INF/vault").create(true, true, monitor);
        // TODO - we need to add more content here
        // - a default html.jsp script
        // - the default files from under META-INF/vault
        // - a little content (nt:unstructured nodes of resource type example/item) under /content/example
        return Collections.singletonList(project);
    }

    @Override
    protected Projects configureCreatedProjects(List<IProject> createdProjects, IProgressMonitor monitor)
            throws CoreException {
        Projects projects = new Projects();
        for (IProject project : createdProjects) {
            ConfigurationHelper.convertToContentPackageProject(project, monitor, "jcr_root");
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
        return "New Sling Content Project";
    }

}
