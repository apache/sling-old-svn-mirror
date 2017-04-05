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
package org.apache.sling.ide.eclipse.m2e.internal;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.eclipse.aether.util.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.internal.JavaFacetUtil;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * m2e project configurator that creates the Eclipse project files (e.g .project) from a pom.xml for 
 * the maven packaging type "content-package". The configurator is run automatically 
 * on "Import..." -> "Existing Maven Project" and Right-Click on Project -> "Maven" 
 * -> "Update Project...". 
 * 
 *
 */
public class ContentPackageProjectConfigurator extends AbstractProjectConfigurator {

    // Using these maven properties, the to-be-created eclipse project settings can be configured in pom.xml 
    private static final String M2E_ACTIVE = "sling.ide.m2e.contentpackage.active";
    private static final String M2E_JAVA_FACET_VERSION = "sling.ide.m2e.contentpackage.javaFacetVersion";
    private static final String M2E_WEB_FACET_VERSION = "sling.ide.m2e.contentpackage.webFacetVersion";

    @Override
    public void configure(ProjectConfigurationRequest configRequest, IProgressMonitor progressMonitor) throws CoreException {

        IProject project = configRequest.getProject();
        // delete all previous markers on this pom.xml set by any project configurator
        deleteMarkers(configRequest.getPom());

        if (!getPreferences().isContentPackageProjectConfiguratorEnabled()) {
            trace("M2E project configurator for packing type 'content-package' was disabled through preference.");
            return;
        }

        MavenProject mavenProject = configRequest.getMavenProject();
        boolean active = !"false".equalsIgnoreCase(mavenProject.getProperties().getProperty(M2E_ACTIVE));
        if(!active) {
            trace("M2E project configurator for packing type 'content-package' was disabled with Maven property {0}", M2E_ACTIVE);
            return;
        }
        
        trace("Configuring {0} with packaging 'content-package'...", mavenProject);
        
        // core configuration for sling ide plugin
        
        Resource folder = MavenProjectUtils.guessJcrRootFolder(mavenProject);
        
        java.nio.file.Path contentSyncPath = mavenProject.getBasedir().toPath().relativize(Paths.get(folder.getDirectory()));
        
        String jcrRootPath = contentSyncPath.toString();
        ConfigurationHelper.convertToContentPackageProject(project, progressMonitor, Path.fromOSString(jcrRootPath));   
        
        if (getPreferences().isWtpFacetsEnabledInContentPackageProjectConfigurator()) {
            new WtpProjectConfigurer(configRequest, project, jcrRootPath).configure(progressMonitor);
            trace("WTP facets for {0} added", mavenProject);
        } else {
            trace("WTP facets for packing type 'content-package' are disabled through preferences.");
        }
        
        trace("Done converting .");
    }

    /**
     * Adds the given natures to the project
     * @param project
     * @param natureIdsToAdd
     * @param progressMonitor
     * @throws CoreException
     * @see <a href="http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_natures.htm">Eclipse Help: Project Natures</a>
     */
    private void addNatures(IProject project, String[] natureIdsToAdd, IProgressMonitor progressMonitor, IResource pomResource)
            throws CoreException {
        
        trace("Adding natures {0} to project {1} ", Arrays.toString(natureIdsToAdd), project);

        IProjectDescription description = project.getDescription();
        String[] oldNatureIds = description.getNatureIds();
        Set<String> newNatureIdSet = new TreeSet<String>();
        newNatureIdSet.addAll(Arrays.asList(oldNatureIds));
        newNatureIdSet.addAll(Arrays.asList(natureIdsToAdd));
        String[] newNatureIds = newNatureIdSet.toArray(new String[newNatureIdSet.size()]);
        IStatus status = project.getWorkspace().validateNatureSet(newNatureIds);
        // check the status and decide what to do
        if (status.getCode() == IStatus.OK) {
            description.setNatureIds(newNatureIds);
            project.setDescription(description, IResource.KEEP_HISTORY, progressMonitor);
        } else {
            // add marker
            addMarker(pomResource, "Could not add all necessary WTP natures: " + status.getMessage(), IMarker.SEVERITY_ERROR);
        }
    }

    // Implemented in line with the current m2e-wtp plugin, except that java/web facet version can be configured
    private class WtpProjectConfigurer {

        private final ProjectConfigurationRequest configRequest;
       private final IProject project;
       private final String jcrRootPath;
       
        WtpProjectConfigurer(ProjectConfigurationRequest configRequest,
               IProject project, String jcrRootPath) {

            this.configRequest = configRequest;
           this.project = project;
           this.jcrRootPath = jcrRootPath;
       }


       void configure(IProgressMonitor progressMonitor) throws CoreException {
           
           trace("Configuring content-package with WTP facets/natures");
           
            addNatures(project, getDefaultWtpNatures(), progressMonitor, configRequest.getPom());
           addWtpFacets(progressMonitor);
       }

       void addWtpFacets( IProgressMonitor progressMonitor) throws CoreException {
           MavenProject mavenProject = configRequest.getMavenProject();
           String javaFacetVersion;
           if ( !StringUtils.isEmpty(mavenProject.getProperties().getProperty(M2E_JAVA_FACET_VERSION))) {
               javaFacetVersion = mavenProject.getProperties().getProperty(M2E_JAVA_FACET_VERSION);
               trace("Configured Java facet version {0} from pom property {1}", javaFacetVersion, M2E_JAVA_FACET_VERSION);
           } else {
               javaFacetVersion = JavaFacetUtil.getCompilerLevel(project);
               trace("Configured Java facet version {0} using JavaFacetUtil", javaFacetVersion);
           }
           
           String webFacetVersion;
           if ( !StringUtils.isEmpty(mavenProject.getProperties().getProperty(M2E_WEB_FACET_VERSION))) {
               webFacetVersion = mavenProject.getProperties().getProperty(M2E_WEB_FACET_VERSION);
               trace("Configured Web facet version {0} from pom property {1}", webFacetVersion, M2E_WEB_FACET_VERSION);
           } else {
               webFacetVersion = MavenProjectUtils.guessServletApiVersion(mavenProject);
               trace("Configured Web facet version {0} from project dependencies", webFacetVersion);
           }

            // web facets
            IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, progressMonitor);
            Set<Action> actions = new LinkedHashSet<Action>();
            installJavaFacet(actions, project, facetedProject, javaFacetVersion);

            IProjectFacetVersion webFv = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.DYNAMIC_WEB).getVersion(webFacetVersion);
            IDataModel webModelCfg = getWebModelConfig(jcrRootPath);
            if (!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
                removeConflictingFacets(facetedProject, webFv, actions);
                actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, webFv, webModelCfg));
            } else {
                IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET);
                if (webFv.getVersionString() != null && !webFv.getVersionString().equals(projectFacetVersion.getVersionString())) {
                    actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, webFv, webModelCfg));
                }
            }
            
            if (!actions.isEmpty()) {
                try {
                    facetedProject.modify(actions, progressMonitor);
                } catch (Exception e) {
                   Activator.getDefault().getPluginLogger().warn("Could not add facets to project: "+e, e);
                }

            }
       }

        private IDataModel getWebModelConfig(String warSourceDirectory) {
            IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
            webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, warSourceDirectory);
            webModelCfg.setProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT, "/");
            webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.GENERATE_DD, false);
            return webModelCfg;
        }

        private void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject, String javaFacetVersion) {
            IProjectFacetVersion javaFv = JavaFacet.FACET.getVersion(javaFacetVersion);
            if (!facetedProject.hasProjectFacet(JavaFacet.FACET)) {
                actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, javaFv, null));
            } else if (!facetedProject.hasProjectFacet(javaFv)) {
                actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, javaFv, null));
            }
        }

        /**
         * Adds uninstall actions of facets from the faceted project that conflict with the given facetVersion.
         */
        private void removeConflictingFacets(IFacetedProject project, IProjectFacetVersion facetVersion, Set<Action> actions) {
            if (project == null) {
                throw new IllegalArgumentException("project can not be null");
            }
            if (facetVersion == null) {
                throw new IllegalArgumentException("Facet version can not be null");
            }
            if (actions == null) {
                throw new IllegalArgumentException("actions can not be null");
            }
            for (IProjectFacetVersion existingFacetVersion : project.getProjectFacets()) {
                if (facetVersion.conflictsWith(existingFacetVersion)) {
                    actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL, existingFacetVersion, null));
                }
            }
        }
       
        
        private String[] getDefaultWtpNatures() {
           return new String[] {
               "org.eclipse.wst.jsdt.core.jsNature", 
               "org.eclipse.wst.common.project.facet.core.nature",
               "org.eclipse.wst.common.modulecore.ModuleCoreNature",
               "org.eclipse.jem.workbench.JavaEMFNature"
            };
        }
    }
}