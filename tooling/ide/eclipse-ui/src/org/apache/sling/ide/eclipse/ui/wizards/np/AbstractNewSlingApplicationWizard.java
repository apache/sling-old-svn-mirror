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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.apache.sling.ide.eclipse.core.MavenLaunchHelper;
import org.apache.sling.ide.eclipse.ui.wizards.ConfigurationHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
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
			getContainer().run(false, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						performFinish(monitor);
					} catch (Exception e) {
						// TODO proper logging
						e.printStackTrace();
						MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), 
			                    SWT.OK | SWT.ICON_ERROR);
			            messageBox.setText("Creating application failed");
			            StringBuffer sb = new StringBuffer();
			            Throwable t = e;
			            while(t!=null) {
			            	if (sb.length()!=0) {
			            		sb.append(System.getProperty("line.separator"));
			            	}
			            	sb.append(t.getMessage());
			            	t = t.getCause();
			            }
			            messageBox.setMessage(sb.toString());
			            messageBox.open();
					}
				}
				
			});
			return true;
        } catch (InterruptedException e) {
        	// that's fine, the user interrupted - dont complain
        	return false;
		} catch (InvocationTargetException e) {
			// TODO proper logging
			e.printStackTrace();
			MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), 
                    SWT.OK | SWT.ICON_ERROR);
            messageBox.setText("Creating application failed");
            StringBuffer sb = new StringBuffer();
            Throwable t = e;
            while(t!=null) {
            	if (sb.length()!=0) {
            		sb.append(System.getProperty("line.separator"));
            	}
            	sb.append(t.getMessage());
            	t = t.getCause();
            }
            messageBox.setMessage(sb.toString());
            messageBox.open();
			return false;
		}
	}
        
	private boolean performFinish(IProgressMonitor monitor) throws Exception {

		IPath location = chooseArchetypePage.getLocation();
		Archetype archetype = chooseArchetypePage.getSelectedArchetype();
		String groupId = archetypeParametersPage.getGroupId();
		String artifactId = archetypeParametersPage.getArtifactId();
		String version = archetypeParametersPage.getVersion();
		String javaPackage = archetypeParametersPage.getJavaPackage();
		Properties properties = archetypeParametersPage.getProperties();
		ProjectImportConfiguration configuration = new ProjectImportConfiguration();

		monitor.worked(1);
		if (monitor.isCanceled()) {
			return false;
		}
		IServer server = setupServerWizardPage.getOrCreateServer();
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return false;
		}
		
		List<IProject> projects = MavenPlugin.getProjectConfigurationManager().createArchetypeProjects(
				location, archetype, groupId, artifactId, version, javaPackage, properties, configuration, monitor);
		
		monitor.worked(3);
		if (monitor.isCanceled()) {
			return false;
		}
		
		List<IProject> contentProjects = new LinkedList<IProject>();
		List<IProject> bundleProjects = new LinkedList<IProject>();
		IProject reactorProject = null;
		for (Iterator<IProject> it = projects.iterator(); it.hasNext();) {
			IProject project = it.next();
			IFile pomFile = project.getFile("pom.xml");
			if (!pomFile.exists()) {
				// then ignore this project - we only deal with maven projects
				continue;
			}
			final Model model = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
			final String packaging = model.getPackaging();

			if ("content-package".equals(packaging)) {
				contentProjects.add(project);
			} else if ("bundle".equals(packaging)) {
				bundleProjects.add(project);
			} else if ("pom".equals(packaging)) {
				if (reactorProject==null) {
					reactorProject = project;
				} else {
					IPath currLocation = project.getFullPath();
					IPath prevLocation = reactorProject.getFullPath();
					if (currLocation.isPrefixOf(prevLocation)) {
						// assume reactor is up in the folder structure
						reactorProject = project;
					}
				}
			}
		}
		
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return false;
		}
		
		for (Iterator<IProject> it = contentProjects.iterator(); it.hasNext();) {
			IProject aContentProject = it.next();
			configureContentProject(aContentProject, projects, monitor);
		}
		for (Iterator<IProject> it = bundleProjects.iterator(); it.hasNext();) {
			IProject aBundleProject = it.next();
			configureBundleProject(aBundleProject, projects, monitor);
		}
		
		if (reactorProject!=null) {
			configureReactorProject(reactorProject, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return false;
			}
		}
		
		finishConfiguration(projects, server, monitor);
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return false;
		}
		
		updateProjectConfigurations(projects, true, monitor);
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return false;
		}
		
		IServerWorkingCopy wc = server.createWorkingCopy();
		// add the bundle and content projects, ie modules, to the server
		List<IModule> modules = new LinkedList<IModule>();
		for (Iterator<IProject> it = bundleProjects.iterator(); it.hasNext();) {
			IProject project = it.next();
			IModule module = ServerUtil.getModule(project);
			modules.add(module);
		}
		for (Iterator<IProject> it = contentProjects.iterator(); it.hasNext();) {
			IProject project = it.next();
			IModule module = ServerUtil.getModule(project);
			modules.add(module);
		}
		wc.modifyModules(modules.toArray(new IModule[modules.size()]), new IModule[0], monitor);
		IServer newServer = wc.save(true, monitor);
		newServer.start(ILaunchManager.RUN_MODE, monitor);
		
		monitor.worked(2);
		if (monitor.isCanceled()) {
			return false;
		}
		
		if (reactorProject!=null) {
			ILaunchConfiguration launchConfig = 
					DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(reactorProject.getFolder(".settings").getFolder(".launches").getFile("initial_install.launch"));
			if (launchConfig!=null) {
				ILaunch theLaunch = launchConfig.launch(ILaunchManager.RUN_MODE, monitor, true);
				monitor.setTaskName("mvn install");
				while(!theLaunch.isTerminated()) {
					Thread.sleep(500);
					monitor.worked(1);
				}
			}
		}
		
		wc.getOriginal().publish(IServer.PUBLISH_FULL, monitor);
		
		// also add 'java 1.6' and 'jst.ejb 3.1'
//		IFacetedProject fp2 = ProjectFacetsManager.create(uiProject, true, null);
//		IProjectFacet java = ProjectFacetsManager.getProjectFacet("java");
//		fp2.installProjectFacet(java.getVersion("1.6"), null, null);
//		IProjectFacet dynamicWebModule = ProjectFacetsManager.getProjectFacet("jst.web");
//		fp2.installProjectFacet(dynamicWebModule.getLatestVersion(), null, null);

		monitor.worked(2);
		updateProjectConfigurations(projects, false, monitor);
		monitor.worked(1);
		monitor.done();
		return true;
	}
	
	protected void finishConfiguration(List<IProject> projects,
			IServer server, IProgressMonitor monitor) throws CoreException {
		// nothing to be done by default - hook for subclasses
	}
	
	protected void updateProjectConfigurations(List<IProject> projects, boolean forceDependencyUpdate, IProgressMonitor monitor) throws CoreException {
		for (Iterator<IProject> it = projects.iterator(); it.hasNext();) {
			IProject project = it.next();
			MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(new MavenUpdateRequest(project, /*mavenConfiguration.isOffline()*/false, forceDependencyUpdate), monitor);
		}
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
		// temp hack: install the launch file
		IFolder dotLaunches = reactorProject.getFolder(".settings").getFolder(".launches");
		dotLaunches.create(true, true, monitor);
		IFile launchFile = dotLaunches.getFile("initial_install.launch");
		String l = MavenLaunchHelper.createMavenLaunchConfigMemento(reactorProject.getLocation().toOSString(), 
				"install", null, false, null);
		InputStream in = new ByteArrayInputStream(l.getBytes());
		launchFile.create(in, true, monitor);
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
}