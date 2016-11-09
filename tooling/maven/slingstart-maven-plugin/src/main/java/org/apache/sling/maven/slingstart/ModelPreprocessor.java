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
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
import org.apache.sling.provisioning.model.MergeUtility;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ModelPreprocessor {

    public static final class ProjectInfo {

        public MavenProject project;
        public Plugin       plugin;
        public Model        localModel;
        public boolean      done = false;
        public Model        model;
        public final Map<org.apache.sling.provisioning.model.Artifact, Model> includedModels = new HashMap<org.apache.sling.provisioning.model.Artifact, Model>();

    }

    public static final class Environment {
        public ArtifactHandlerManager artifactHandlerManager;
        public ArtifactResolver resolver;
        public MavenSession session;
        public Logger logger;
        public final Map<String, ProjectInfo> modelProjects = new HashMap<String, ProjectInfo>();
    }

    /**
     * Add dependencies for all projects.
     * @param env The environment with all maven settings and projects
     * @throws MavenExecutionException If anything goes wrong.
     */
    public void addDependencies(final Environment env) throws MavenExecutionException {
        for(final ProjectInfo info : env.modelProjects.values()) {
            addDependencies(env, info);
        }
    }

    /**
     * Add dependencies for a single project.
     * @param env The environment with all maven settings and projects
     * @param info The project to process.
     * @throws MavenExecutionException If anything goes wrong.
     */
    private Model addDependencies(final Environment env, final ProjectInfo info)
    throws MavenExecutionException {
        if ( info.done == true ) {
            env.logger.debug("Return prepared model for " + info.project);
            return info.model;
        }
        // prevent recursion and multiple processing
        info.done = true;
        env.logger.debug("Processing project " + info.project);

        // read local model
        final String pattern = nodeValue(info.plugin,
                "modelPattern", AbstractSlingStartMojo.DEFAULT_MODEL_PATTERN);

        final String inlinedModel = nodeValue(info.plugin,
                "model", null);

        String scope = Artifact.SCOPE_PROVIDED;
        try {
            if (hasNodeValue(info.plugin, "modelDirectory")) {
                final String directory = nodeValue(info.plugin,
                        "modelDirectory", null);
                info.localModel = readLocalModel(info.project, inlinedModel, new File(directory), pattern, env.logger);
            } else {
                // use multiple fallbacks here only in case the default model directory is not explicitly set
                File defaultModelDirectory = new File(info.project.getBasedir(), "src/main/provisioning");
                if (defaultModelDirectory.exists()) {
                    env.logger.debug("Try to extract model from default provisioning directory " + defaultModelDirectory.getAbsolutePath());
                    info.localModel = readLocalModel(info.project, inlinedModel, defaultModelDirectory, pattern, env.logger);
                } else {
                    File defaultModelDirectoryInTest = new File(info.project.getBasedir(), "src/test/provisioning");
                    env.logger.debug("Try to extract model from default test provisioning directory " + defaultModelDirectoryInTest.getAbsolutePath());
                    info.localModel = readLocalModel(info.project, inlinedModel, defaultModelDirectoryInTest, pattern, env.logger);
                    scope = Artifact.SCOPE_TEST;
                }
            }
        } catch ( final IOException ioe) {
            throw new MavenExecutionException(ioe.getMessage(), ioe);
        }

        // process attachments
        processAttachments(env, info);

        // check for setting version
        if ( nodeBooleanValue(info.plugin, "setFeatureVersions", false) ) {
            for(final Feature f : info.localModel.getFeatures() ) {
                if ( f.getVersion() == null ) {
                    f.setVersion(cleanupVersion(info.project.getVersion()));
                }
            }
        }

        // prepare resolver options
        ResolverOptions resolverOptions = new ResolverOptions();
        if (nodeBooleanValue(info.plugin, "usePomVariables", false)) {
            resolverOptions.variableResolver(new PomVariableResolver(info.project));
        }
        if (nodeBooleanValue(info.plugin, "usePomDependencies", false)) {
            resolverOptions.artifactVersionResolver(new PomArtifactVersionResolver(info.project,
                    nodeBooleanValue(info.plugin, "allowUnresolvedPomDependencies", false)));
        }

        // we have to create an effective model to add the dependencies
        final Model effectiveModel = ModelUtility.getEffectiveModel(info.localModel, resolverOptions);

        final List<Model> dependencies = searchSlingstartDependencies(env, info, info.localModel, effectiveModel);
        info.model = new Model();
        for(final Model d : dependencies) {
            this.mergeModels(info.model, d);
        }
        this.mergeModels(info.model, info.localModel);
        info.localModel = info.model;
        info.model = ModelUtility.getEffectiveModel(info.model, resolverOptions);

        final Map<Traceable, String> errors = ModelUtility.validate(info.model);
        if ( errors != null ) {
            throw new MavenExecutionException("Unable to create model file for " + info.project + " : " + errors, (File)null);
        }

        addDependenciesFromModel(env, info, scope);

        try {
           ProjectHelper.storeProjectInfo(info);
        } catch ( final IOException ioe) {
            throw new MavenExecutionException(ioe.getMessage(), ioe);
        }
        return info.model;
    }

    /**
     * Add all dependencies from the model
     * @param env The environment
     * @param info The project info
     * @param scope The scope which the new dependencies should have
     * @throws MavenExecutionException
     */
    private void addDependenciesFromModel(
            final Environment env,
            final ProjectInfo info,
            final String scope)
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
                dep.setScope(scope);

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
                        if ( a.getGroupId().equals(info.project.getGroupId())
                             && a.getArtifactId().equals(info.project.getArtifactId())
                             && a.getVersion().equals(info.project.getVersion()) ) {
                            // skip artifact from the same project
                            env.logger.debug("- skipping dependency " + a.toMvnUrl());
                            continue;
                        }
                        final Dependency dep = new Dependency();
                        dep.setGroupId(a.getGroupId());
                        dep.setArtifactId(a.getArtifactId());
                        dep.setVersion(a.getVersion());
                        dep.setType(a.getType());
                        dep.setClassifier(a.getClassifier());

                        dep.setScope(scope);

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
    private List<Model> searchSlingstartDependencies(
            final Environment env,
            final ProjectInfo info,
            final Model rawModel,
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
                            final ProjectInfo depInfo = env.modelProjects.get(key);
                            if ( depInfo != null ) {
                                env.logger.debug("Found reactor " + a.getType() + " dependency : " + a);
                                final Model model = addDependencies(env, depInfo);
                                if ( model == null ) {
                                    throw new MavenExecutionException("Recursive model dependency list including project " + info.project, (File)null);
                                }
                                dependencies.add(model);
                                info.includedModels.put(a, depInfo.localModel);

                            } else {
                                env.logger.debug("Found external " + a.getType() + " dependency: " + a);

                                // "external" dependency, we can already resolve it
                                final File modelFile = resolveSlingstartArtifact(env, info.project, dep);
                                FileReader r = null;
                                try {
                                    r = new FileReader(modelFile);
                                    final Model model = ModelReader.read(r, modelFile.getAbsolutePath());

                                    info.includedModels.put(a, model);

                                    final Map<Traceable, String> errors = ModelUtility.validate(model);
                                    if ( errors != null ) {
                                        throw new MavenExecutionException("Unable to read model file from " + modelFile + " : " + errors, modelFile);
                                    }
                                    final Model fullModel = processSlingstartDependencies(env, info, dep,  model);

                                    dependencies.add(fullModel);
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

                            removeList.add(a);
                        }
                    }
                    for(final org.apache.sling.provisioning.model.Artifact r : removeList) {
                        group.remove(r);
                        final Feature localModelFeature = rawModel.getFeature(feature.getName());
                        if ( localModelFeature != null ) {
                            final RunMode localRunMode = localModelFeature.getRunMode(runMode.getNames());
                            if ( localRunMode != null ) {
                                final ArtifactGroup localAG = localRunMode.getArtifactGroup(group.getStartLevel());
                                if ( localAG != null ) {
                                    localAG.remove(r);
                                }
                            }
                        }
                    }
                }
            }
        }

        return dependencies;
    }

    private Model processSlingstartDependencies(final Environment env, final ProjectInfo info, final Dependency dep, final Model rawModel)
    throws MavenExecutionException {
        env.logger.debug("Processing dependency " + dep);

        // we have to create an effective model to add the dependencies
        final Model effectiveModel = ModelUtility.getEffectiveModel(rawModel, new ResolverOptions());

        final List<Model> dependencies = searchSlingstartDependencies(env, info, rawModel, effectiveModel);
        Model mergingModel = new Model();
        for(final Model d : dependencies) {
            this.mergeModels(mergingModel, d);
        }
        this.mergeModels(mergingModel, rawModel);

        final Map<Traceable, String> errors = ModelUtility.validate(ModelUtility.getEffectiveModel(mergingModel, new ResolverOptions()));
        if ( errors != null ) {
            throw new MavenExecutionException("Unable to create model file for " + dep + " : " + errors, (File)null);
        }

        return mergingModel;
    }

    /**
     * Gets plugins configuration from POM (string parameter).
     * @param plugin Plugin
     * @param name Configuration parameter.
     * @param defaultValue Default value that is returned if parameter is not set
     * @return Parameter value or default value.
     */
    private String nodeValue(final Plugin plugin, final String name, final String defaultValue) {
        final Xpp3Dom config = plugin == null ? null : (Xpp3Dom)plugin.getConfiguration();
        return nodeValue(config, name, defaultValue);
    }

    private String nodeValue(final Xpp3Dom config, final String name, final String defaultValue) {
        final Xpp3Dom node = (config == null ? null : config.getChild(name));
        if (node != null) {
            return node.getValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if plugin configuration value is set in POM for a specific configuration parameter.
     * @param plugin Plugin
     * @param name Configuration parameter.
     * @return {@code true} in case the configuration has been set in the pom, otherwise {@code false}
     */
    private boolean hasNodeValue(final Plugin plugin, final String name) {
        final Xpp3Dom config = plugin == null ? null : (Xpp3Dom)plugin.getConfiguration();
        final Xpp3Dom node = (config == null ? null : config.getChild(name));
        return (node != null);
    }

    private void processAttachments(final Environment env, final ProjectInfo info)
    throws MavenExecutionException {
        final Xpp3Dom config = info.plugin == null ? null : (Xpp3Dom)info.plugin.getConfiguration();
        final Xpp3Dom[] nodes = (config == null ? null : config.getChildren("attach"));
        if ( nodes != null ) {
            for(final Xpp3Dom node : nodes) {
                final String type = nodeValue(node, "type", null);
                if ( type == null ) {
                    throw new MavenExecutionException("Attachment for provisioning model has no type.", (File)null);
                }
                final String classifier = nodeValue(node, "classifier", null);
                final String featureName = nodeValue(node, "feature", null);
                int startLevel = 0;
                final String level = nodeValue(node, "startLevel", null);
                if ( level != null ) {
                    startLevel = Integer.valueOf(level);
                }

                final Feature f;
                if ( featureName != null ) {
                    f = info.localModel.getFeature(featureName);
                } else if ( info.localModel.getFeatures().isEmpty() ) {
                    f = null;
                } else {
                    f = info.localModel.getFeatures().get(0);
                }
                if ( f == null ) {
                    if ( featureName == null ) {
                        throw new MavenExecutionException("No feature found in provisioning model for attachment.", (File)null);
                    }
                    throw new MavenExecutionException("Feature with name '" + featureName + "' not found in provisioning model for attachment.", (File)null);
                }
                final RunMode runMode = f.getOrCreateRunMode(null);
                final ArtifactGroup group = runMode.getOrCreateArtifactGroup(startLevel);

                final org.apache.sling.provisioning.model.Artifact artifact = new org.apache.sling.provisioning.model.Artifact(
                        info.project.getGroupId(),
                        info.project.getArtifactId(),
                        info.project.getVersion(),
                        classifier,
                        type);

                env.logger.debug("Attaching " + artifact + " to feature " + f.getName());
                group.add(artifact);
            }
        }
    }

    /**
     * Gets plugins configuration from POM (boolean parameter).
     * @param plugin Plugin
     * @param name Configuration parameter.
     * @param defaultValue Default value that is returned if parameter is not set
     * @return Parameter value or default value.
     */
    private boolean nodeBooleanValue(final Plugin plugin, final String name, final boolean defaultValue) {
        String booleanValue = nodeValue(plugin, name, Boolean.toString(defaultValue));
        return "true".equals(booleanValue.toLowerCase());
    }

    private File resolveSlingstartArtifact(final Environment env,
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
     * @param inlinedModel the inlined model to be merged with the models in modelDirectory (may be null)
     * @param modelDirectory The directory to scan for models
     * @param pattern Pattern used to find the textual models within the modelDirectory
     * @param logger The logger
     */
    protected Model readLocalModel(
            final MavenProject project,
            final String inlinedModel,
            final File modelDirectory,
            final String pattern,
            final Logger logger)
    throws MavenExecutionException, IOException {
        final Pattern p = Pattern.compile(pattern);
        final List<String> candidates = new ArrayList<String>();
        if ( modelDirectory != null && modelDirectory.exists() ) {
            for(final File f : modelDirectory.listFiles() ) {
                if ( f.isFile() && !f.getName().startsWith(".") ) {
                    if ( p.matcher(f.getName()).matches() ) {
                        candidates.add(f.getName());
                    }
                }
            }
            Collections.sort(candidates);
        }
        if ( candidates.size() == 0 && (inlinedModel == null || inlinedModel.trim().length() == 0) ) {
            throw new MavenExecutionException("No model files found in " + modelDirectory + ", and no model inlined in POM.", (File)null);
        }
        final Model result = new Model();
        if ( inlinedModel != null ) {
            logger.debug("Reading inlined model from project " + project.getId());
            try {
                final Reader reader = new StringReader(inlinedModel);
                try {
                    final Model current = ModelReader.read(reader, "pom");
                    final Map<Traceable, String> errors = ModelUtility.validate(current);
                    if (errors != null ) {
                        throw new MavenExecutionException("Invalid inlined model : " + errors, (File)null);
                    }
                    MergeUtility.merge(result, current, new MergeUtility.MergeOptions().setHandleRemoveRunMode(false));
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            } catch ( final IOException io) {
                throw new MavenExecutionException("Unable to read inlined model", io);
            }
        }
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
                    MergeUtility.merge(result, current, new MergeUtility.MergeOptions().setHandleRemoveRunMode(false));
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

        return postProcessReadModel(result);
    }

    /**
     * Hook to post process the local model
     * @param result The read model
     * @return The post processed model
     */
    protected Model postProcessReadModel(final Model result)  throws MavenExecutionException {
        return result;
    }

    /**
     * Hook to change the merge behavior
     * @param base The base model
     * @param additional The additional model
     */
    protected void mergeModels(final Model base, final Model additional) throws MavenExecutionException {
        MergeUtility.merge(base, additional);
    }

    /**
     * Pattern for converting Maven to OSGi version
     * Based on the DefaultMaven2OsgiConverter from the Apache Maven Project.
     */
    private static final Pattern FUZZY_VERSION = Pattern.compile( "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
        Pattern.DOTALL );


    private String cleanupVersion( final String version ) {
        final StringBuilder result = new StringBuilder();
        final Matcher m = FUZZY_VERSION.matcher( version );
        if ( m.matches() ) {
            final String major = m.group( 1 );
            final String minor = m.group( 3 );
            final String micro = m.group( 5 );
            final String qualifier = m.group( 7 );

            if ( major != null ) {
                result.append( major );
                if ( minor != null ) {
                    result.append( "." );
                    result.append( minor );
                    if ( micro != null ) {
                        result.append( "." );
                        result.append( micro );
                        if ( qualifier != null )
                        {
                            result.append( "." );
                            cleanupModifier( result, qualifier );
                        }
                    } else if ( qualifier != null ) {
                        result.append( ".0." );
                        cleanupModifier( result, qualifier );
                    } else {
                        result.append( ".0" );
                    }
                } else if ( qualifier != null ) {
                    result.append( ".0.0." );
                    cleanupModifier( result, qualifier );
                } else {
                    result.append( ".0.0" );
                }
            }
        } else {
            result.append( "0.0.0." );
            cleanupModifier( result, version );
        }
        return result.toString();
    }

    private static void cleanupModifier( final StringBuilder result, final String modifier )  {
        for ( int i = 0; i < modifier.length(); i++ ) {
            final char c = modifier.charAt( i );
            if ( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || c == '_'
                || c == '-' ) {
                result.append( c );
            } else {
                result.append( '_' );
            }
        }
    }
}
