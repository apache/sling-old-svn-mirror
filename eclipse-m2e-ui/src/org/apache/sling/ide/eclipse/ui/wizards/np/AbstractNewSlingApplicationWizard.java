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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.m2e.internal.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

public abstract class AbstractNewSlingApplicationWizard extends Wizard implements INewWizard {
	private ChooseArchetypeWizardPage chooseArchetypePage;
	private ArchetypeParametersWizardPage archetypeParametersPage;
	private SetupServerWizardPage setupServerWizardPage;

	// branding
	public abstract ImageDescriptor getLogo();
	public abstract String doGetWindowTitle();
	public abstract void installArchetypes();
	public abstract boolean acceptsArchetype(Archetype archetype2);

	/**
	 * Constructor for AbstractNewSlingApplicationWizard.
	 */
	public AbstractNewSlingApplicationWizard() {
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
		archetypeParametersPage = new ArchetypeParametersWizardPage(this);
		addPage(archetypeParametersPage);
		setupServerWizardPage = new SetupServerWizardPage(this);
		addPage(setupServerWizardPage);
	}
	
    /**
     * 
     * @return the current wizard page, possibly null
     */
    protected WizardPage getCurrentWizardPage() {
        IWizardPage currentPage = getContainer().getCurrentPage();
        if (currentPage instanceof WizardPage) {
            return (WizardPage) currentPage;
        }

        return null;
    }

    protected void reportError(CoreException e) {
        WizardPage currentPage = getCurrentWizardPage();
        if (currentPage != null) {
            currentPage.setMessage(e.getMessage(), IMessageProvider.ERROR);
        } else {
            MessageDialog.openError(getShell(), "Unexpected error", e.getMessage());
        }

        Activator.getDefault().getLog().log(e.getStatus());
    }

    protected void reportError(Throwable t) {
        if ( t instanceof CoreException ) {
            reportError((CoreException) t);
            return;
        }
        
        IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, t.getMessage(), t);
        reportError(new CoreException(status));
    }

	public ChooseArchetypeWizardPage getChooseArchetypePage() {
		return chooseArchetypePage;
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {

        try {
            // create maven projects
            final List<IProject> createdProjects = new ArrayList<IProject>();
            getContainer().run(false, true, new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                        InterruptedException {
                    createdProjects.addAll(createMavenProjects(monitor));
                }
            });

            // configure projects
            final Projects[] projects = new Projects[1];
            getContainer().run(false, true, new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                        InterruptedException {
                    projects[0] = configureCreatedProjects(createdProjects, monitor);
                }
            });

            // deploy the projects on server
            getContainer().run(false, true, new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                        InterruptedException {
                    deployProjectsOnServer(projects[0], monitor);
                }
            });

            // ensure server is started and all modules are published
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        publishModules(createdProjects, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            reportError(e.getTargetException());
            return false;
        }

        return true;
	}

    private List<IProject> createMavenProjects(IProgressMonitor monitor) throws CoreException {

        IPath location = chooseArchetypePage.getLocation();
        Archetype archetype = chooseArchetypePage.getSelectedArchetype();
        String groupId = archetypeParametersPage.getGroupId();
        String artifactId = archetypeParametersPage.getArtifactId();
        String version = archetypeParametersPage.getVersion();
        String javaPackage = archetypeParametersPage.getJavaPackage();
        Properties properties = archetypeParametersPage.getProperties();
        ProjectImportConfiguration configuration = new ProjectImportConfiguration();

        advance(monitor, 1);

        List<IProject> projects = MavenPlugin.getProjectConfigurationManager().createArchetypeProjects(location,
                archetype, groupId, artifactId, version, javaPackage, properties, configuration, monitor);

        monitor.worked(3);

        return projects;

    }

    private Projects configureCreatedProjects(List<IProject> createdProjects, IProgressMonitor monitor)
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

        IServer server = setupServerWizardPage.getOrCreateServer(monitor);
        advance(monitor, 1);

        finishConfiguration(createdProjects, server, monitor);
        advance(monitor, 1);

        return projects;
    }

    private void deployProjectsOnServer(Projects projects, IProgressMonitor monitor) throws CoreException {

        IServer server = setupServerWizardPage.getOrCreateServer(monitor);
        advance(monitor, 1);

        IServerWorkingCopy wc = server.createWorkingCopy();
        // add the bundle and content projects, ie modules, to the server
        List<IModule> modules = new LinkedList<IModule>();
        for (IProject project : projects.getBundleProjects()) {
            IModule module = ServerUtil.getModule(project);
            if (module != null) {
                modules.add(module);
            }
        }
        for (IProject project : projects.getContentProjects()) {
            IModule module = ServerUtil.getModule(project);
            if (module != null) {
                modules.add(module);
            }
        }
        wc.modifyModules(modules.toArray(new IModule[modules.size()]), new IModule[0], monitor);
        wc.save(true, monitor);

        advance(monitor, 2);

        monitor.done();
    }

    private void publishModules(final List<IProject> createdProjects, IProgressMonitor monitor) throws CoreException {
        IServer server = setupServerWizardPage.getOrCreateServer(monitor);
        server.start(ILaunchManager.RUN_MODE, monitor);
        List<IModule[]> modules = new ArrayList<IModule[]>();
        for (IProject project : createdProjects) {
            IModule module = ServerUtil.getModule(project);
            if (module != null) {
                modules.add(new IModule[] { module });
            }
        }

        if (modules.size() > 0) {
            server.publish(IServer.PUBLISH_FULL, modules, null, null);
        }
    }

    private void advance(IProgressMonitor monitor, int step) {

        monitor.worked(step);
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }
	
	protected void finishConfiguration(List<IProject> projects,
			IServer server, IProgressMonitor monitor) throws CoreException {
		// nothing to be done by default - hook for subclasses
	}
	
	protected void configureBundleProject(IProject aBundleProject,
			List<IProject> projects, IProgressMonitor monitor) throws CoreException {
		ConfigurationHelper.convertToBundleProject(aBundleProject);
	}
	
	protected void configureContentProject(IProject aContentProject,
			List<IProject> projects, IProgressMonitor monitor) throws CoreException {
		ConfigurationHelper.convertToContentPackageProject(aContentProject, monitor, "src/main/content/jcr_root");
	}
	
	protected void configureReactorProject(IProject reactorProject, IProgressMonitor monitor) throws CoreException {
		// nothing to be done
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

    private static class Projects {

        private List<IProject> bundleProjects = new ArrayList<IProject>();
        private List<IProject> contentProjects = new ArrayList<IProject>();
        private IProject reactorProject;

        public List<IProject> getBundleProjects() {
            return bundleProjects;
        }

        public List<IProject> getContentProjects() {
            return contentProjects;
        }

        public IProject getReactorProject() {
            return reactorProject;
        }

        public void setReactorProject(IProject reactorProject) {
            this.reactorProject = reactorProject;
        }
    }
}