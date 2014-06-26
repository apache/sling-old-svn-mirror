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
package org.apache.sling.ide.eclipse.ui.wizards.np;

import static org.apache.sling.ide.eclipse.core.progress.ProgressUtils.advance;

import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.apache.sling.ide.eclipse.ui.wizards.AbstractNewSlingApplicationWizard;
import org.apache.sling.ide.eclipse.ui.wizards.Projects;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.wst.server.core.IServer;

public abstract class AbstractNewMavenBasedSlingApplicationWizard extends AbstractNewSlingApplicationWizard {
	private ChooseArchetypeWizardPage chooseArchetypePage;
	private ArchetypeParametersWizardPage archetypeParametersPage;
	public abstract void installArchetypes();
	public abstract boolean acceptsArchetype(Archetype archetype2);

	/**
	 * Constructor for AbstractNewMavenBasedSlingApplicationWizard.
	 */
	public AbstractNewMavenBasedSlingApplicationWizard() {
		super();
		setWindowTitle(doGetWindowTitle());
		setNeedsProgressMonitor(true);
	}
	
	
	/**
	 * Adding the page to the wizard.
	 */
	public void addPages() {
		chooseArchetypePage = new ChooseArchetypeWizardPage(this);
		addPage(chooseArchetypePage);
		archetypeParametersPage = createArchetypeParametersWizardPage();
		addPage(archetypeParametersPage);
        addPage(getSetupServerWizardPage());
	}
	
    protected ArchetypeParametersWizardPage createArchetypeParametersWizardPage() {
        return new ArchetypeParametersWizardPage(this);
    }
	
    public ChooseArchetypeWizardPage getChooseArchetypePage() {
		return chooseArchetypePage;
	}

	protected List<IProject> createProjects(IProgressMonitor monitor) throws CoreException {

        IPath location = chooseArchetypePage.getLocation();
        Archetype archetype = chooseArchetypePage.getSelectedArchetype();
        String groupId = archetypeParametersPage.getGroupId();
        String artifactId = archetypeParametersPage.getArtifactId();
        String version = archetypeParametersPage.getVersion();
        String javaPackage = archetypeParametersPage.getJavaPackage();
        Properties properties = archetypeParametersPage.getProperties();
        ProjectImportConfiguration configuration = new ProjectImportConfiguration();

        IProject existingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(artifactId);
        if (existingProject!=null && existingProject.exists()) {
            throw new IllegalStateException("Project already exists with name "+artifactId);
        }

        advance(monitor, 1);

        List<IProject> projects = MavenPlugin.getProjectConfigurationManager().createArchetypeProjects(location,
                archetype, groupId, artifactId, version, javaPackage, properties, configuration, monitor);

        monitor.worked(3);

        return projects;

    }

    protected Projects configureCreatedProjects(List<IProject> createdProjects, IProgressMonitor monitor)
            throws CoreException {

        Projects projects = new Projects();

        for (IProject project : createdProjects) {
            IFile pomFile = project.getFile("pom.xml");
            if (!pomFile.exists()) {
                // then ignore this project - we only deal with maven projects
                continue;
            }
            final Model model = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
            final String packaging = model.getPackaging();

            if ("content-package".equals(packaging)) {
                projects.getContentProjects().add(project);
            } else if ("bundle".equals(packaging)) {
                projects.getBundleProjects().add(project);
            } else if ("pom".equals(packaging)) {
                if (projects.getReactorProject() == null) {
                    projects.setReactorProject(project);
                } else {
                    IPath currLocation = project.getFullPath();
                    IPath prevLocation = projects.getReactorProject().getFullPath();
                    if (currLocation.isPrefixOf(prevLocation)) {
                        // assume reactor is up in the folder structure
                        projects.setReactorProject(project);
                    }
                }
            }
        }

        advance(monitor, 1);

        for (IProject contentProject : projects.getContentProjects()) {
            configureContentProject(contentProject, createdProjects, monitor);
        }
        for (IProject bundleProject : projects.getBundleProjects()) {
            configureBundleProject(bundleProject, createdProjects, monitor);
        }

        if (projects.getReactorProject() != null) {
            configureReactorProject(projects.getReactorProject(), monitor);
            advance(monitor, 1);
        }

        IServer server = getSetupServerWizardPage().getOrCreateServer(monitor);
        advance(monitor, 1);

        finishConfiguration(createdProjects, server, monitor);
        advance(monitor, 1);

        return projects;
    }
}