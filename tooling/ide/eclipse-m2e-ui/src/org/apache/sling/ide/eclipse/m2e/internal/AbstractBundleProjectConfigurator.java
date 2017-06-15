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

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public abstract class AbstractBundleProjectConfigurator extends AbstractProjectConfiguratorRunningDefaultOnIncrementalBuilds {

    public AbstractBundleProjectConfigurator(boolean runOnIncremental) {
        super(runOnIncremental);
    }

    /**
     *  the plugin ID consists of <code>groupId:artifactId</code>, see {@link Plugin#constructKey(String, String)}
     */
    private static final String MAVEN_SLING_PLUGIN_KEY = "org.apache.sling:maven-sling-plugin";

    private static final String MARKER_TYPE_BUNDLE_NOT_SUPPORTING_M2E = "org.apache.sling.ide.eclipse-m2e-ui.bundleprojectnotsupportingm2e";

    @SuppressWarnings("restriction")
    @Override
    public void configure(ProjectConfigurationRequest configRequest, IProgressMonitor monitor) throws CoreException {
        
        // at this point the JDT project is already created by the tycho plugin
        // we just need to setup the appropriate facets
        IProject project = configRequest.getProject();
        trace("AbstractBundleProjectConfigurator called for POM {0} and project {1}",
                configRequest.getPom().getFullPath(),
                project.getName());
        // delete all previous markers on this pom.xml set by any project configurator
        deleteMarkers(configRequest.getPom());
        
        if (!getPreferences().isBundleProjectConfiguratorEnabled()) {
            trace("m2e project configurator for bundles was disabled through preference.");
            return;
        }

        // check for maven-sling-plugin as well (to make sure this is a Sling project)
        MavenProject mavenProject = configRequest.getMavenProject();
        if (mavenProject.getPlugin(MAVEN_SLING_PLUGIN_KEY) != null) {
            trace("Found maven-sling-plugin in build plugins for project {0}, therefore adding sling bundle facets!", project.getName());
            ConfigurationHelper.convertToBundleProject(project);
        } else {
            trace("Couldn't find maven-sling-plugin in build plugins for project {0}, therefore not adding the sling bundle facets!", project.getName());
        }
        if (!isSupportingM2eIncrementalBuild(mavenProject, getLogger())) {
            markerManager.addMarker(configRequest.getPom(), MARKER_TYPE_BUNDLE_NOT_SUPPORTING_M2E,
                    "Missing m2e incremental build support for generating the bundle manifest, component descriptions and metatype resources. Please use the provided Quick Fixes on this issue to resolve this.",
                    -1,
                    IMarker.SEVERITY_ERROR);
        }
    }
    
    /**
     * @param mavenProject
     * @param logger
     * @return {@code true} in case the pom.xml is correctly configured to support incremental build on the bundle's manifest and the service definitions, otherwise {@code false}
     */
    protected abstract boolean isSupportingM2eIncrementalBuild(MavenProject mavenProject, Logger logger);
}
