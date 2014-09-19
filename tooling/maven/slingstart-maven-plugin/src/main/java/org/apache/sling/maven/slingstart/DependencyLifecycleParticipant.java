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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMRunMode;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.apache.sling.slingstart.model.SSMSubsystem;
import org.apache.sling.slingstart.model.xml.XMLSSMModelWriter;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Maven lifecycle participant which adds the artifacts of the model to the dependencies.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class DependencyLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String PLUGIN_ID = "slingstart-maven-plugin";

    private static final String PROVIDED = "provided";

    @Requirement
    private Logger log;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Requirement
    private ArtifactResolver resolver;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        log.info("Searching for slingstart projects...");
        try {
            final Map<String, MavenProject> projectMap = new HashMap<String, MavenProject>();
            for (final MavenProject project : session.getProjects()) {
                projectMap.put(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion(),
                        project);
            }

            for (final MavenProject project : session.getProjects()) {
                for (Plugin plugin : project.getBuild().getPlugins()) {
                    if (plugin.getArtifactId().equals(PLUGIN_ID)) {
                        addDependencies(artifactHandlerManager, resolver, log,
                                session, project, plugin);
                    }
                }
            }
        } catch (final Exception e) {
            throw new MavenExecutionException("Unable to determine plugin-based dependencies", e);
        }
    }

    public static void addDependencies(final ArtifactHandlerManager artifactHandlerManager,
            final ArtifactResolver resolver,
            final Logger log,
            final MavenSession session, final MavenProject project, final Plugin plugin)
    throws Exception {
        // check dependent projects first
        final List<File> dependencies = new ArrayList<File>();
        for(final Dependency d : project.getDependencies() ) {
            if ( d.getType().equals(BuildConstants.PACKAGING_SLINGSTART)
              || d.getType().equals(BuildConstants.PACKAGING_PARTIAL_SYSTEM)) {
                final File modelXML = getSlingstartArtifact(artifactHandlerManager, resolver, project, session, d);
                dependencies.add(modelXML);
            }
        }

        final String directory = nodeValue((Xpp3Dom) plugin.getConfiguration(),
                "systemsDirectory", new File(project.getBasedir(), "src/main/systems").getAbsolutePath());
        final SSMSubsystem model = SubsystemUtils.readFullModel(new File(directory), dependencies, project, session, log);

        final StringWriter w = new StringWriter();
        XMLSSMModelWriter.write(w, model);
        project.setContextValue(SSMSubsystem.class.getName() + "/text", w.toString());

        // start with base artifact
        final SSMArtifact base = SubsystemUtils.getBaseArtifact(model);
        final String[] classifiers = new String[] {null, BuildConstants.CLASSIFIER_APP, BuildConstants.CLASSIFIER_WEBAPP};
        for(final String c : classifiers) {
            final Dependency dep = new Dependency();
            dep.setGroupId(base.groupId);
            dep.setArtifactId(base.artifactId);
            dep.setVersion(model.getValue(base.version));
            dep.setType(base.type);
            dep.setClassifier(c);
            if ( BuildConstants.CLASSIFIER_WEBAPP.equals(c) ) {
                dep.setType(BuildConstants.TYPE_WAR);
            }
            dep.setScope(PROVIDED);

            log.debug("- adding dependency " + dep);
            project.getDependencies().add(dep);
        }

        addDependencies(model, log, project);
    }

    private static void addDependencies(final SSMSubsystem model, final Logger log, final MavenProject project) {
        for(final SSMRunMode runMode : model.runModes) {
            // skip base
            if ( runMode.isRunMode(SSMRunMode.RUN_MODE_BASE) ) {
                continue;
            }
            for(final SSMStartLevel sl : runMode.startLevels) {
                for(final SSMArtifact a : sl.artifacts) {
                    final Dependency dep = new Dependency();
                    dep.setGroupId(a.groupId);
                    dep.setArtifactId(a.artifactId);
                    dep.setVersion(model.getValue(a.version));
                    dep.setType(a.type);
                    dep.setClassifier(a.classifier);
                    dep.setScope(PROVIDED);

                    log.debug("- adding dependency " + dep);
                    project.getDependencies().add(dep);
                }
            }
        }
    }

    private static File getSlingstartArtifact(final ArtifactHandlerManager artifactHandlerManager,
            final ArtifactResolver resolver,
            final MavenProject project,
            final MavenSession session,
            final Dependency d)
    throws MavenExecutionException {
        final Artifact prjArtifact = new DefaultArtifact(d.getGroupId(),
                d.getArtifactId(),
                VersionRange.createFromVersion(d.getVersion()),
                Artifact.SCOPE_PROVIDED,
                d.getType(),
                d.getClassifier(),
                artifactHandlerManager.getArtifactHandler(d.getType()));
        try {
            resolver.resolve(prjArtifact, project.getRemoteArtifactRepositories(), session.getLocalRepository());
        } catch (final ArtifactResolutionException e) {
            throw new MavenExecutionException("Unable to get artifact for " + d, e);
        } catch (final ArtifactNotFoundException e) {
            throw new MavenExecutionException("Unable to get artifact for " + d, e);
        }
        return prjArtifact.getFile();
    }

    private static String nodeValue(final Xpp3Dom config, final String name, final String defaultValue) {
        final Xpp3Dom node = (config == null ? null : config.getChild(name));
        if (node != null) {
            return node.getValue();
        } else {
            return defaultValue;
        }
    }
}
