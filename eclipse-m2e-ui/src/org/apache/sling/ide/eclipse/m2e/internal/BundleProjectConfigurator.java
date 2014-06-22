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
import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.apache.sling.ide.eclipse.core.debug.PluginLogger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class BundleProjectConfigurator extends AbstractProjectConfigurator {

    private static final String MAVEN_SLING_PLUGIN_ARTIFACT_ID = "maven-sling-plugin";
    private static final String MAVEN_SLING_PLUGIN_GROUP_ID = "org.apache.sling";

    @Override
    public void configure(ProjectConfigurationRequest configRequest, IProgressMonitor monitor) throws CoreException {
        // at this point the JDT project is already created by the tycho plugin
        // we just need to setup the appropriate facets
        PluginLogger logger = Activator.getDefault().getPluginLogger();
        IProject project = configRequest.getProject();
        logger.trace("BundleProjectActivator called for POM {0} and project {1}", configRequest.getPom().getFullPath(),
                project.getName());

        // check for maven-sling-plugin as well (to make sure this is a Sling project)
        for (Plugin plugin : configRequest.getMavenProject().getBuildPlugins()) {
            if (plugin.getArtifactId().equals(MAVEN_SLING_PLUGIN_ARTIFACT_ID)
                    && plugin.getGroupId().equals(MAVEN_SLING_PLUGIN_GROUP_ID)) {
                logger.trace(
                        "Found maven-sling-plugin in build plugins for project {0}, therefore adding sling bundle facets!",
                        project.getName());
                ConfigurationHelper.convertToBundleProject(project);
                return;
            }
        }
        logger.trace("Couldn't find maven-sling-plugin in build plugins for project {0}", project.getName());
    }
}
