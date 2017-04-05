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
import org.apache.sling.ide.log.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;


public class MavenBundlePluginProjectConfigurator extends AbstractBundleProjectConfigurator {

    /**
     *  the plugin ID consists of <code>groupId:artifactId</code>, see {@link Plugin#constructKey(String, String)}
     */
    private static final String MAVEN_BUNDLE_PLUGIN_KEY ="org.apache.felix:maven-bundle-plugin";
    
    /**
     * The configurator id used in <a href="https://github.com/tesla/m2eclipse-tycho/blob/master/org.sonatype.tycho.m2e/lifecycle-mapping-metadata.xml">m2e-tycho</a>.
     * @see <a href="https://github.com/tesla/m2eclipse-tycho">m2eclipse-tycho Github</a>
     */
    private static final String M2E_TYCHO_EXTENSION_PROJECT_CONFIGURATOR_ID = "maven-bundle-plugin";
    
    public MavenBundlePluginProjectConfigurator() {
        super(false); // this configurator is only bound to goal "bundle" which is not supposed to be executed in
                      // incremental builds
    }

    @Override
    protected boolean isSupportingM2eIncrementalBuild(MavenProject mavenProject, Logger logger) {
        Plugin bundlePlugin = mavenProject.getPlugin(MAVEN_BUNDLE_PLUGIN_KEY);
        if (bundlePlugin == null) {
            logger.warn("maven-bundle-plugin not configured!");
            return false;
        } else {
            // check if m2elipse-tycho is already installed (which supports incremental builds for "bundle" packagings
            if (LifecycleMappingFactory.createProjectConfigurator(M2E_TYCHO_EXTENSION_PROJECT_CONFIGURATOR_ID) != null) {
                logger.trace("Project configurator with id '" + M2E_TYCHO_EXTENSION_PROJECT_CONFIGURATOR_ID + "' found -> m2e-tycho installed.");
                return true;
            }
            
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
                        logger.warn("Although using maven-bundle-plugin in a version >= 3.2.0 with incremental build support enabled, component descriptors are not exported (exportScr=false) .");
                    } else {
                        logger.trace("Using maven-bundle-plugin in a version >= 3.2.0 with the incremental build support correctly enabled.");
                        return true;
                    }
                }
            } else {
                logger.warn("maven-bundle-plugin in a version < 3.2.0 does not natively support incremental builds.");
            }
        }
        return false;
    }
}
