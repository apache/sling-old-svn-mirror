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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelReader;
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

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Requirement
    private ArtifactResolver resolver;

    public static final class ProjectInfo {
        public MavenProject project;
        public Plugin       plugin;
        public Model        localModel;
        public boolean      done = false;
        public Model        model;
    }

    public static final class Environment {
        public ArtifactHandlerManager artifactHandlerManager;
        public ArtifactResolver resolver;
        public MavenSession session;
        public Logger logger;
        public final Map<String, ProjectInfo> modelProjects = new HashMap<String, ProjectInfo>();
    }

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        final Environment env = new Environment();
        env.artifactHandlerManager = artifactHandlerManager;
        env.resolver = resolver;
        env.logger = logger;
        env.session = session;

        logger.debug("Searching for " + BuildConstants.PACKAGING_SLINGSTART + "/" + BuildConstants.PACKAGING_PARTIAL_SYSTEM + " projects...");

        for (final MavenProject project : session.getProjects()) {
            if ( project.getPackaging().equals(BuildConstants.PACKAGING_SLINGSTART)
                 || project.getPackaging().equals(BuildConstants.PACKAGING_PARTIAL_SYSTEM)) {
                logger.debug("Found " + project.getPackaging() + " project: " + project);
                // search plugin configuration (optional)
                final ProjectInfo info = new ProjectInfo();
                for (Plugin plugin : project.getBuild().getPlugins()) {
                    if (plugin.getArtifactId().equals(PLUGIN_ID)) {
                        info.plugin = plugin;
                        break;
                    }
                }
                info.project = project;
                env.modelProjects.put(project.getGroupId() + ":" + project.getArtifactId(), info);
            }
        }

        addDependencies(env);
    }

    public static void addDependencies(final Environment env) throws MavenExecutionException {
        for(final ProjectInfo info : env.modelProjects.values()) {
            addDependencies(env, info);
        }
    }

    private static Model addDependencies(final Environment env, final ProjectInfo info)
    throws MavenExecutionException {
        if ( info.done == true ) {
            env.logger.debug("Return prepared model for " + info.project);
            return info.model;
        }
        // prevent recursion and multiple processing
        info.done = true;
        env.logger.debug("Processing project " + info.project);

        // read local model
        final String directory = nodeValue(info.plugin,
                "modelDirectory",
                new File(info.project.getBasedir(), "src/main/provisioning").getAbsolutePath());
        try {
            info.localModel = readLocalModel(info.project, new File(directory), env.logger);
        } catch ( final IOException ioe) {
            throw new MavenExecutionException(ioe.getMessage(), ioe);
        }

        // we have to create an effective model to add the dependencies
        final Model effectiveModel = ModelUtility.getEffectiveModel(info.localModel, null);

        final List<Model> dependencies = searchSlingstartDependencies(env, info, effectiveModel);
        info.model = new Model();
        for(final Model d : dependencies) {
            ModelUtility.merge(info.model, d);
        }
        ModelUtility.merge(info.model, effectiveModel);
        info.model = ModelUtility.getEffectiveModel(info.model, null);

        final Map<Traceable, String> errors = ModelUtility.validate(info.model);
        if ( errors != null ) {
            throw new MavenExecutionException("Unable to create model file for " + info.project + " : " + errors, (File)null);
        }

        addDependenciesFromModel(env, info);

        try {
           ProjectHelper.storeProjectInfo(info);
        } catch ( final IOException ioe) {
            throw new MavenExecutionException(ioe.getMessage(), ioe);
        }
        return info.model;
    }

    /**
     * Add all dependencies from the model
     * @param project The project
     * @param model The model
     * @param log The logger
     * @throws MavenExecutionException
     */
    private static void addDependenciesFromModel(
            final Environment env,
            final ProjectInfo info)
    throws MavenExecutionException {
        if ( info.project.getPackaging().equals(BuildConstants.PACKAGING_SLINGSTART ) ) {
            // add base artifact if defined in current model
            final org.apache.sling.provisioning.model.Artifact baseArtifact = ModelUtils.findBaseArtifact(info.model);

            final String[] classifiers = new String[] {null, BuildConstants.CLASSIFIER_APP, BuildConstants.CLASSIFIER_WEBAPP};
            for(final String c : classifiers) {
                final Dependency dep = new Dependency();
                dep.setGroupId(baseArtifact.getGroupId());
                dep.setArtifactId(baseArtifact.getArtifactId());
                dep.setVersion(baseArtifact.getVersion());
                dep.setType(baseArtifact.getType());
                dep.setClassifier(c);
                if ( BuildConstants.CLASSIFIER_WEBAPP.equals(c) ) {
                    dep.setType(BuildConstants.TYPE_WAR);
                }
                dep.setScope(Artifact.SCOPE_PROVIDED);

                info.project.getDependencies().add(dep);
                env.logger.debug("- adding base dependency " + ModelUtils.toString(dep));
            }
        }

        for(final Feature feature : info.model.getFeatures()) {
            // skip launchpad feature
            if ( feature.getName().equals(ModelConstants.FEATURE_LAUNCHPAD) ) {
                continue;
            }
            for(final RunMode runMode : feature.getRunModes()) {
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    for(final org.apache.sling.provisioning.model.Artifact a : group) {
                        final Dependency dep = new Dependency();
                        dep.setGroupId(a.getGroupId());
                        dep.setArtifactId(a.getArtifactId());
                        dep.setVersion(a.getVersion());
                        dep.setType(a.getType());
                        dep.setClassifier(a.getClassifier());

                        dep.setScope(Artifact.SCOPE_PROVIDED);

                        env.logger.debug("- adding dependency " + ModelUtils.toString(dep));
                        info.project.getDependencies().add(dep);
                    }
                }
            }
        }
    }

    /**
     * Search for dependent slingstart/slingfeature artifacts and remove them from the effective model.
     * @throws MavenExecutionException
     */
    private static List<Model> searchSlingstartDependencies(
            final Environment env,
            final ProjectInfo info,
            final Model effectiveModel)
    throws MavenExecutionException {
        // slingstart or slingfeature
        final List<Model> dependencies = new ArrayList<Model>();

        for(final Feature feature : effectiveModel.getFeatures()) {
            for(final RunMode runMode : feature.getRunModes()) {
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final List<org.apache.sling.provisioning.model.Artifact> removeList = new ArrayList<org.apache.sling.provisioning.model.Artifact>();
                    for(final org.apache.sling.provisioning.model.Artifact a : group) {
                        if ( a.getType().equals(BuildConstants.PACKAGING_SLINGSTART)
                             || a.getType().equals(BuildConstants.PACKAGING_PARTIAL_SYSTEM)) {

                            final Dependency dep = new Dependency();
                            dep.setGroupId(a.getGroupId());
                            dep.setArtifactId(a.getArtifactId());
                            dep.setVersion(a.getVersion());
                            dep.setType(BuildConstants.PACKAGING_PARTIAL_SYSTEM);
                            if ( a.getType().equals(BuildConstants.PACKAGING_SLINGSTART) ) {
                                dep.setClassifier(BuildConstants.PACKAGING_PARTIAL_SYSTEM);
                            } else {
                                dep.setClassifier(a.getClassifier());
                            }
                            dep.setScope(Artifact.SCOPE_PROVIDED);

                            env.logger.debug("- adding dependency " + ModelUtils.toString(dep));
                            info.project.getDependencies().add(dep);

                            // if it's a project from the current reactor build, we can't resolve it right now
                            final String key = a.getGroupId() + ":" + a.getArtifactId();
                            if ( env.modelProjects.containsKey(key) ) {
                                env.logger.debug("Found reactor " + a.getType() + " dependency : " + a);
                                final Model model = addDependencies(env, env.modelProjects.get(key));
                                if ( model == null ) {
                                    throw new MavenExecutionException("Recursive model dependency list including project " + info.project, (File)null);
                                }
                                dependencies.add(model);
                            } else {
                                env.logger.debug("Found external " + a.getType() + " dependency: " + a);
                                // "external" dependency, we can already resolve it
                                final File modelFile = resolveSlingstartArtifact(env, info.project, dep);
                                FileReader r = null;
                                try {
                                    r = new FileReader(modelFile);
                                    final Model m = ModelReader.read(r, modelFile.getAbsolutePath());

                                    final Map<Traceable, String> errors = ModelUtility.validate(m);
                                    if ( errors != null ) {
                                        throw new MavenExecutionException("Unable to read model file from " + modelFile + " : " + errors, modelFile);
                                    }
                                    dependencies.add(m);
                                } catch ( final IOException ioe) {
                                    throw new MavenExecutionException("Unable to read model file from " + modelFile, ioe);
                                } finally {
                                    try {
                                        if ( r != null ) {
                                            r.close();
                                        }
                                    } catch ( final IOException io) {
                                        // ignore
                                    }
                                }
                            }
                            env.logger.debug("- adding dependency " + ModelUtils.toString(dep));
                            info.project.getDependencies().add(dep);

                            removeList.add(a);
                        }
                    }
                    for(final org.apache.sling.provisioning.model.Artifact r : removeList) {
                        group.remove(r);
                    }
                }
            }
        }

        return dependencies;
    }

    private static String nodeValue(final Plugin plugin, final String name, final String defaultValue) {
        final Xpp3Dom config = plugin == null ? null : (Xpp3Dom)plugin.getConfiguration();
        final Xpp3Dom node = (config == null ? null : config.getChild(name));
        if (node != null) {
            return node.getValue();
        } else {
            return defaultValue;
        }
    }

    private static File resolveSlingstartArtifact(final Environment env,
            final MavenProject project,
            final Dependency d)
    throws MavenExecutionException {
        final Artifact prjArtifact = new DefaultArtifact(d.getGroupId(),
                d.getArtifactId(),
                VersionRange.createFromVersion(d.getVersion()),
                Artifact.SCOPE_PROVIDED,
                d.getType(),
                d.getClassifier(),
                env.artifactHandlerManager.getArtifactHandler(d.getType()));
        try {
            env.resolver.resolve(prjArtifact, project.getRemoteArtifactRepositories(), env.session.getLocalRepository());
        } catch (final ArtifactResolutionException e) {
            throw new MavenExecutionException("Unable to get artifact for " + d, e);
        } catch (final ArtifactNotFoundException e) {
            throw new MavenExecutionException("Unable to get artifact for " + d, e);
        }
        return prjArtifact.getFile();
    }

    /**
     * Read all model files from the directory in alphabetical order.
     * Only files ending with .txt or .model are read.
     *
     * @param project The current maven project
     * @param modelDirectory The directory to scan for models
     * @param logger The logger
     */
    private static Model readLocalModel(
            final MavenProject project,
            final File modelDirectory,
            final Logger logger)
    throws MavenExecutionException, IOException {
        final List<String> candidates = new ArrayList<String>();
        if ( modelDirectory != null && modelDirectory.exists() ) {
            for(final File f : modelDirectory.listFiles() ) {
                if ( f.isFile() && !f.getName().startsWith(".") ) {
                    if ( f.getName().endsWith(".txt") || f.getName().endsWith(".model") ) {
                        candidates.add(f.getName());
                    }
                }
            }
            Collections.sort(candidates);
        }
        if ( candidates.size() == 0 ) {
            throw new MavenExecutionException("No model files found in " + modelDirectory, (File)null);
        }
        final Model result = new Model();
        for(final String name : candidates) {
            logger.debug("Reading model " + name + " in project " + project.getId());
            try {
                final File f = new File(modelDirectory, name);
                final FileReader reader = new FileReader(f);
                try {
                    final Model current = ModelReader.read(reader, f.getAbsolutePath());
                    final Map<Traceable, String> errors = ModelUtility.validate(current);
                    if (errors != null ) {
                        throw new MavenExecutionException("Invalid model at " + name + " : " + errors, (File)null);
                    }
                    ModelUtility.merge(result, current, false);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            } catch ( final IOException io) {
                throw new MavenExecutionException("Unable to read model at " + name, io);
            }
        }

        final Map<Traceable, String> errors = ModelUtility.validate(result);
        if (errors != null ) {
            throw new MavenExecutionException("Invalid assembled model : " + errors, (File)null);
        }

        return result;
    }
}
