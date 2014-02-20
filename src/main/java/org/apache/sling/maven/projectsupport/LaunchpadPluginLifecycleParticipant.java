/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.projectsupport;

import static org.apache.sling.maven.projectsupport.BundleListUtils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Maven lifecycle participant which adds the default bundle list, the
 * jar web support bundle, and the contents of any local bundle list.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class LaunchpadPluginLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String PLUGIN_ID = "maven-launchpad-plugin";

    private static final String PROVIDED = "provided";

    @Requirement
    private Logger log;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        try {
            Map<String, MavenProject> projectMap = new HashMap<String, MavenProject>();
            for (MavenProject project : session.getProjects()) {
                projectMap.put(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion(),
                        project);
            }

            for (MavenProject project : session.getProjects()) {
                for (Plugin plugin : project.getBuild().getPlugins()) {
                    if (plugin.getArtifactId().equals(PLUGIN_ID)) {
                        BundleListDependencyAdder performer = new BundleListDependencyAdder(session, project, plugin);
                        performer.addDependencies();
                    }
                }
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Unable to determine launchpad plugin-based dependencies", e);
        }
        super.afterProjectsRead(session);
    }

    private class BundleListDependencyAdder {

        private final MavenSession session;
        private final MavenProject project;
        private final Plugin plugin;
        private final List<ArtifactDefinition> additionalBundles;

        private ArtifactDefinition defaultBundleList;
        private boolean includeDefaultBundles;
        private ArtifactDefinition jarWebSupport;
        private File bundleListFile;

        public BundleListDependencyAdder(MavenSession session, MavenProject project, Plugin plugin) {
            this.session = session;
            this.project = project;
            this.plugin = plugin;
            this.additionalBundles = new ArrayList<ArtifactDefinition>();
        }

        void addDependencies() throws Exception {
            readConfiguration();

            addBundleListDependencies();

            if (hasPreparePackageExecution()) {
                if (includeDefaultBundles && !isCurrentArtifact(project, defaultBundleList)) {
                    log.debug(String.format("adding default bundle list (%s) to dependencies of project %s", defaultBundleList, project));
                    project.getDependencies().addAll(defaultBundleList.toDependencyList(PROVIDED));
                }

                if (hasJarPackagingExecution()) {
                    log.debug(String.format("adding jar web support (%s) to dependencies of project %s", jarWebSupport, project));
                    project.getDependencies().addAll(jarWebSupport.toDependencyList(PROVIDED));
                }
            }
        }

        private void addBundleListDependencies() throws IOException, XmlPullParserException, MojoExecutionException {
            BundleList bundleList;

            if (bundleListFile.exists()) {
                bundleList = readBundleList(bundleListFile);
            } else {
                bundleList = new BundleList();
            }

            if (additionalBundles != null) {
                for (ArtifactDefinition def : additionalBundles) {
                    bundleList.add(def.toBundleList());
                }
            }

            interpolateProperties(bundleList, project, session);

            for (StartLevel startLevel : bundleList.getStartLevels()) {
                for (Bundle bundle : startLevel.getBundles()) {
                    log.debug(String.format("adding bundle (%s) from bundle list to dependencies of project %s", bundle, project));
                    project.getDependencies().addAll(ArtifactDefinition.toDependencyList(bundle, PROVIDED));
                }
            }
        }

        private void readConfiguration() throws IOException {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            defaultBundleList = null;
            jarWebSupport = null;
            includeDefaultBundles = true;
            bundleListFile = new File(project.getBasedir(), "src/main/bundles/list.xml");
            if (configuration != null) {
                includeDefaultBundles = nodeValue(configuration, "includeDefaultBundles", true);
                Xpp3Dom defaultBundleListConfig = configuration.getChild("defaultBundleList");
                if (defaultBundleListConfig != null) {
                    defaultBundleList = new ArtifactDefinition(defaultBundleListConfig);
                }
                Xpp3Dom jarWebSupportConfig = configuration.getChild("jarWebSupport");
                if (jarWebSupportConfig != null) {
                    jarWebSupport = new ArtifactDefinition(jarWebSupportConfig);
                }
                Xpp3Dom bundleListFileConfig = configuration.getChild("bundleListFile");
                if (bundleListFileConfig != null) {
                    bundleListFile = new File(project.getBasedir(), bundleListFileConfig.getValue());
                }

                configureAdditionalBundles(configuration);
            }

            for (PluginExecution execution : plugin.getExecutions()) {
                Xpp3Dom executionConfiguration = (Xpp3Dom) execution.getConfiguration();
                if (executionConfiguration != null) {
                    configureAdditionalBundles(executionConfiguration);
                }
            }

            initArtifactDefinitions(getClass().getClassLoader(), new ArtifactDefinitionsCallback() {

                public void initArtifactDefinitions(Properties dependencies) {
                    if (defaultBundleList == null) {
                        defaultBundleList = new ArtifactDefinition();
                    }
                    defaultBundleList.initDefaults(dependencies.getProperty("defaultBundleList"));

                    if (jarWebSupport == null) {
                        jarWebSupport = new ArtifactDefinition();
                    }
                    jarWebSupport.initDefaults(dependencies.getProperty("jarWebSupport"));
                }
            });
        }

        private void configureAdditionalBundles(Xpp3Dom configuration) {
            Xpp3Dom additionalBundlesConfig = configuration.getChild("additionalBundles");
            if (additionalBundlesConfig != null) {
                Xpp3Dom[] bundleConfigs = additionalBundlesConfig.getChildren("bundle");
                if (bundleConfigs != null) {
                    for (Xpp3Dom bundleConfig : bundleConfigs) {
                        additionalBundles.add(new ArtifactDefinition(bundleConfig));
                    }
                }
            }
        }

        private boolean hasJarPackagingExecution() {
            if (AbstractUsingBundleListMojo.JAR.equals(project.getPackaging())) {
                return true;
            } else {
                for (PluginExecution execution : plugin.getExecutions()) {
                    if (execution.getGoals().contains("prepare-package")) {
                        Xpp3Dom executionConfig = (Xpp3Dom) execution.getConfiguration();
                        if (executionConfig != null) {
                            Xpp3Dom packagingConfig = executionConfig.getChild("packaging");
                            if (packagingConfig != null
                                    && AbstractUsingBundleListMojo.JAR.equals(packagingConfig.getValue())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        }

        private boolean hasPreparePackageExecution() {
            for (PluginExecution execution : plugin.getExecutions()) {
                if (execution.getGoals().contains("prepare-package")) {
                    return true;
                }
            }
            return false;
        }
    }
}
