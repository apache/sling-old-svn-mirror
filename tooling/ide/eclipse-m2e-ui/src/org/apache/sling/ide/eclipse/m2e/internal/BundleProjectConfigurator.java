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

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.log.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class BundleProjectConfigurator extends AbstractProjectConfigurator {

    /**
     *  the plugin ID consists of <code>groupId:artifactId</code>, see {@link Plugin#constructKey(String, String)}
     */
    private static final String MAVEN_SLING_PLUGIN_KEY = "org.apache.sling:maven-sling-plugin";
    private static final String MAVEN_BUNDLE_PLUGIN_KEY ="org.apache.felix:maven-bundle-plugin";
    private static final String BND_MAVEN_PLUGIN_KEY = "biz.aQute.bnd:bnd-maven-plugin";
    
    private static final String MARKER_TYPE_BUNDLE_NOT_SUPPORTING_M2E = "org.apache.sling.ide.eclipse-m2e-ui.bundleprojectnotsupportingm2e";

    @Override
    public void configure(ProjectConfigurationRequest configRequest, IProgressMonitor monitor) throws CoreException {
        
        // at this point the JDT project is already created by the tycho plugin
        // we just need to setup the appropriate facets
        Logger logger = Activator.getDefault().getPluginLogger();
        IProject project = configRequest.getProject();
        logger.trace("BundleProjectActivator called for POM {0} and project {1}", configRequest.getPom().getFullPath(),
                project.getName());
        markerManager.deleteMarkers(project.getFile(IMavenConstants.POM_FILE_NAME), MARKER_TYPE_BUNDLE_NOT_SUPPORTING_M2E);

        // check for maven-sling-plugin as well (to make sure this is a Sling project)
        MavenProject mavenProject = configRequest.getMavenProject();
        if (mavenProject.getPlugin(MAVEN_SLING_PLUGIN_KEY) != null) {
            logger.trace(
                    "Found maven-sling-plugin in build plugins for project {0}, therefore adding sling bundle facets!",
                    project.getName());
            ConfigurationHelper.convertToBundleProject(project);
        } else {
            logger.trace("Couldn't find maven-sling-plugin in build plugins for project {0}, therefore not adding the sling bundle facets!", project.getName());
        }
        
        if (!isSupportingM2EIncrementalBuild(mavenProject, logger)) {
            markerManager.addMarker(project.getFile(IMavenConstants.POM_FILE_NAME), MARKER_TYPE_BUNDLE_NOT_SUPPORTING_M2E, "Missing m2e incremental support for generating the bundle manifest", -1,
                    IMarker.SEVERITY_ERROR);
        }
    }
    
    /**
     * @param mavenProject
     * @param logger
     * @return {@code true} in case the pom.xml is correctly configured to support incremental build on the bundle's manifest, otherwise {@code false}
     */
    private boolean isSupportingM2EIncrementalBuild(MavenProject mavenProject, Logger logger) {
        Plugin bundlePlugin = mavenProject.getPlugin(MAVEN_BUNDLE_PLUGIN_KEY);
        if (bundlePlugin == null) {
            Plugin bndPlugin = mavenProject.getPlugin(BND_MAVEN_PLUGIN_KEY);
            if (bndPlugin != null) {
                logger.trace("Using bnd-maven-plugin which supports incremental builds.");
                return true;
            }
            logger.warn("Neither maven-bundle-plugin nor bnd-maven-plugin configured!");
            return false;
        } else {
            String version = bundlePlugin.getVersion();
            if (version == null) {
                logger.warn("Could not retrieve used version of maven-bundle-plugin!");
                return false;
            }
            ComparableVersion comparableVersion = new ComparableVersion(version);
            // with https://issues.apache.org/jira/browse/FELIX-4009 m2e support for incremental builds was added to maven-bundle-plugin in version 3.2.0
            if (comparableVersion.compareTo(new ComparableVersion("3.2.0")) >= 0) {
                // but only if explicitly configured, see http://felix.apache.org/documentation/faqs/apache-felix-bundle-plugin-faq.html#use-scr-metadata-generated-by-bnd-in-unit-tests
                // therefore check configuration
                for (PluginExecution pluginExecution : bundlePlugin.getExecutions()) {
                    if (!pluginExecution.getGoals().contains("manifest")) {
                        continue;
                    }
                    Xpp3Dom configuration = (Xpp3Dom)pluginExecution.getConfiguration();
                    Xpp3Dom supportIncrementalBuildConfiguration = configuration.getChild("supportIncrementalBuild");
                    // https://issues.apache.org/jira/browse/FELIX-3324
                    Xpp3Dom exportScrConfiguration = configuration.getChild("exportScr");
                    if (supportIncrementalBuildConfiguration == null || !Boolean.parseBoolean(supportIncrementalBuildConfiguration.getValue())) {
                        logger.warn("Although using maven-bundle-plugin in a version >= 3.2.0, the incremental build support was not enabled.");
                    } else if (exportScrConfiguration == null || !Boolean.parseBoolean(exportScrConfiguration.getValue())) {
                        logger.warn("Although using maven-bundle-plugin in a version >= 3.2.0, component descriptions are not exported (exportScr=false) .");
                    } else {
                        logger.trace("Using maven-bundle-plugin in a version >= 3.2.0 with the incremental build support correctly enabled.");
                        return true;
                    }
                }
            } else {
                logger.warn("maven-bundle-plugin in a version < 3.2.0 does not support incremental builds.");
                return false;
            }
        }
        return false;
    }
}
