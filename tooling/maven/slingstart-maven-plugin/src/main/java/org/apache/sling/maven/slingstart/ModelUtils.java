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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;
import org.codehaus.plexus.logging.Logger;

public abstract class ModelUtils {

    private static final String EXT_TXT = ".txt";
    private static final String EXT_MODEL = ".model";

    /**
     * Read all model files from the directory in alphabetical order.
     * Only files ending with .txt or .model are read.
     *
     * @param startingModel The model into which the read models are merged or {@code null}
     * @param modelDirectory The directory to scan for models
     * @param project The current maven project
     * @param session The current maven session
     * @param logger The logger
     */
    private static Model readLocalModel(final Model startingModel,
            final File modelDirectory,
            final MavenProject project,
            final MavenSession session,
            final Logger logger)
    throws MojoExecutionException {
        final Model result = (startingModel != null ? startingModel : new Model());
        final List<String> candidates = new ArrayList<String>();
        if ( modelDirectory != null && modelDirectory.exists() ) {
            for(final File f : modelDirectory.listFiles() ) {
                if ( f.isFile() && !f.getName().startsWith(".") ) {
                    if ( f.getName().endsWith(EXT_TXT) || f.getName().endsWith(EXT_MODEL) ) {
                        candidates.add(f.getName());
                    }
                }
            }
            Collections.sort(candidates);
        }
        if ( candidates.size() == 0 ) {
            throw new MojoExecutionException("No model files found in " + modelDirectory);
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
                        throw new MojoExecutionException("Invalid model at " + name + " : " + errors);
                    }
                    ModelUtility.merge(result, current);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            } catch ( final IOException io) {
                throw new MojoExecutionException("Unable to read model at " + name, io);
            }
        }

        final Map<Traceable, String> errors = ModelUtility.validate(result);
        if (errors != null ) {
            throw new MojoExecutionException("Invalid assembled model : " + errors);
        }

        return result;
    }

    /**
     * Read the full model
     */
    public static Model readFullModel(final File modelDirectory,
            final List<File> dependentModels,
            final MavenProject project,
            final MavenSession session,
            final Logger logger)
    throws MojoExecutionException {
        try {
            // read dependent models
            Model depModel = null;
            if ( dependentModels != null ) {
                for(final File file : dependentModels) {
                    FileReader r = null;
                    try {
                        r = new FileReader(file);
                        if ( depModel == null ) {
                            depModel = new Model();
                        }
                        final Model readModel = ModelReader.read(r, file.getAbsolutePath());
                        final Map<Traceable, String> errors = ModelUtility.validate(readModel);
                        if (errors != null ) {
                            throw new MojoExecutionException("Invalid model at " + file + " : " + errors);
                        }
                        ModelUtility.merge(depModel, readModel);
                    } finally {
                        IOUtils.closeQuietly(r);
                    }
                }
            }
            if ( depModel != null ) {
                final Map<Traceable, String> errors = ModelUtility.validate(depModel);
                if (errors != null ) {
                    throw new MojoExecutionException("Invalid model : " + errors);
                }
            }

            final Model result = readLocalModel(depModel, modelDirectory, project, session, logger);

            return result;
        } catch ( final IOException ioe) {
            throw new MojoExecutionException("Unable to cache model", ioe);
        }
    }

    public static final class SearchResult {
        public org.apache.sling.provisioning.model.Artifact artifact;
        public String errorMessage;
    }

    public static SearchResult findBaseArtifact(final Model model) throws MojoExecutionException {
        final SearchResult result = new SearchResult();
        final Feature base = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
        if ( base == null ) {
            result.errorMessage = "No launchpad feature found.";
        } else {
            // get global run mode
            final RunMode runMode = base.getRunMode();
            if ( runMode == null ) {
                result.errorMessage = "No global run mode found in launchpad feature.";
            } else {
                if ( runMode.getArtifactGroups().isEmpty() ) {
                    result.errorMessage = "No base artifacts defined.";
                } else if ( runMode.getArtifactGroups().size() > 1 ) {
                    result.errorMessage = "Base run mode should only have a single start level.";
                } else {
                    org.apache.sling.provisioning.model.Artifact firstArtifact = null;
                    for(final org.apache.sling.provisioning.model.Artifact a : runMode.getArtifactGroups().get(0)) {
                        if ( firstArtifact == null ) {
                            firstArtifact = a;
                        } else {
                            result.errorMessage = "Base run mode should contain exactly one artifact.";
                            break;
                        }
                    }
                    if ( firstArtifact == null ) {
                        result.errorMessage = "No base artifacts defined.";
                    }
                    if ( result.errorMessage == null ) {
                        result.artifact = firstArtifact;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    public static Artifact getArtifact(final MavenProject project,
            final MavenSession session,
            final ArtifactHandlerManager artifactHandlerManager,
            final ArtifactResolver resolver,
            final String groupId, final String artifactId, final String version, final String type, final String classifier)
    throws MojoExecutionException {
        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for(final Artifact artifact : artifacts) {
            if ( artifact.getGroupId().equals(groupId)
               && artifact.getArtifactId().equals(artifactId)
               && artifact.getVersion().equals(version)
               && artifact.getType().equals(type)
               && ((classifier == null && artifact.getClassifier() == null) || (classifier != null && classifier.equals(artifact.getClassifier()))) ) {
                return artifact;
            }
        }
        final Artifact prjArtifact = new DefaultArtifact(groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                Artifact.SCOPE_PROVIDED,
                type,
                classifier,
                artifactHandlerManager.getArtifactHandler(type));
        try {
            resolver.resolve(prjArtifact, project.getRemoteArtifactRepositories(), session.getLocalRepository());
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to get artifact for " + groupId + ":" + artifactId + ":" + version, e);
        } catch (final ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to get artifact for " + groupId + ":" + artifactId + ":" + version, e);
        }
        return prjArtifact;
    }

    private static final String RAW_MODEL_TXT = Model.class.getName() + "/raw.txt";
    private static final String RAW_MODEL_DEPS = Model.class.getName() + "/raw.deps";

    private static final String EFFECTIVE_MODEL = Model.class.getName() + "/effective";
    private static final String RAW_MODEL = Model.class.getName() + "/raw";

    /**
     * Store the model info from the dependency lifecycle participant
     * @param project The maven project
     * @param model The local model
     * @param dependencies The dependencies (either String or File objects)
     * @throws IOException If writing fails
     */
    public static void storeModelInfo(final MavenProject project, final Model model, final List<Object> dependencies)
    throws IOException {
        // we have to serialize as the dependency lifecycle participant uses a different class loader (!)
        final StringWriter w = new StringWriter();
        ModelWriter.write(w, model);
        project.setContextValue(RAW_MODEL_TXT, w.toString());
        project.setContextValue(RAW_MODEL_DEPS, dependencies);
    }

    public static void prepareModel(final MavenProject project,
            final MavenSession session)
    throws MojoExecutionException {
        final String contents = (String)project.getContextValue(RAW_MODEL_TXT);
        final Model localModel;
        try {
            localModel = ModelReader.read(new StringReader(contents), null);
        } catch ( final IOException ioe) {
            throw new MojoExecutionException("Unable to read cached model.", ioe);
        }
        final List<File> modelDependencies = new ArrayList<File>();
        @SuppressWarnings("unchecked")
        final List<Object> localDeps = (List<Object>)project.getContextValue(RAW_MODEL_DEPS);
        for(final Object o : localDeps) {
            if ( o instanceof String ) {
                final String[] info = ((String)o).split(":");

                final Dependency dep = new Dependency();
                dep.setGroupId(info[0]);
                dep.setArtifactId(info[1]);
                dep.setVersion(info[2]);
                if ( info[3] != null && info[3].length() > 0 ) {
                    dep.setClassifier(info[3]);
                }
                if ( info[4] != null && info[4].length() > 0 ) {
                    dep.setType(info[4]);
                }
                modelDependencies.add(getSlingstartArtifact(project, session, dep));
            } else {
                modelDependencies.add((File)o);
            }
        }
        // read dependent models
        Model depModel = null;
        for(final File file : modelDependencies) {
            FileReader r = null;
            try {
                r = new FileReader(file);
                if ( depModel == null ) {
                    depModel = new Model();
                }
                final Model readModel = ModelReader.read(r, file.getAbsolutePath());
                final Map<Traceable, String> errors = ModelUtility.validate(readModel);
                if (errors != null ) {
                    throw new MojoExecutionException("Invalid model at " + file + " : " + errors);
                }
                ModelUtility.merge(depModel, readModel);
            } catch ( final IOException ioe) {
                throw new MojoExecutionException("Unable to read model from " + file, ioe);
            } finally {
                IOUtils.closeQuietly(r);
            }
        }

        final Model rawModel;
        if ( depModel != null ) {
            ModelUtility.merge(depModel, localModel);
            final Map<Traceable, String> errors = ModelUtility.validate(depModel);
            if (errors != null ) {
                throw new MojoExecutionException("Invalid model : " + errors);
            }
            rawModel = depModel;
        } else {
            rawModel = localModel;
        }

        // store raw model
        project.setContextValue(RAW_MODEL, rawModel);
        // create and store effective model
        final Model effectiveModel = ModelUtility.getEffectiveModel(rawModel, null);
        project.setContextValue(EFFECTIVE_MODEL, effectiveModel);
    }

    private static File getSlingstartArtifact(final MavenProject project,
            final MavenSession session,
            final Dependency dep)
    throws MojoExecutionException {
        for (final MavenProject p : session.getProjects()) {
            // we only need to find the group id / artifact id, version is correct anyway
            if ( p.getGroupId().equals(dep.getGroupId())
                 && p.getArtifactId().equals(dep.getArtifactId()) ) {

                // check main artifact first
                if ( dep.getClassifier() == null && p.getPackaging().equals(dep.getType()) ) {
                    if ( p.getArtifact() != null && p.getArtifact().getFile() != null ) {
                        return p.getArtifact().getFile();
                    }
                }
                // followed by attached artifacts
                for(final Artifact a : p.getAttachedArtifacts()) {
                    if ( equals(a.getType(), dep.getType() ) && equals(a.getClassifier(), dep.getClassifier())) {
                        if ( a.getFile() != null ) {
                            return a.getFile();
                        }
                    }
                }
                break;
            }
        }
        throw new MojoExecutionException("Unable to find dependency build artifact " + dep);
    }

    private final static boolean equals(final String a, final String b) {
        if ( a == null && b == null ) {
            return true;
        }
        if ( a == null ) {
            return false;
        }
        return a.equals(b);
    }

    /**
     * Get the effective model from the project
     * @param project The maven projet
     * @return The effective model
     */
    public static Model getEffectiveModel(final MavenProject project) {
        return (Model)project.getContextValue(EFFECTIVE_MODEL);
    }

    /**
     * Get the raw model from the project
     * @param project The maven projet
     * @return The raw model
     */
    public static Model getRawModel(final MavenProject project) {
        return (Model)project.getContextValue(RAW_MODEL);
    }
}
