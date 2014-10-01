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
import org.apache.maven.execution.MavenSession;
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

    /**
     * Read all model files from the directory in alphabetical order
     * @param logger
     */
    private static Model readLocalModel(final File systemsDirectory, final MavenProject project, final MavenSession session, final Logger logger)
    throws MojoExecutionException {
        final Model result = new Model();
        final List<String> candidates = new ArrayList<String>();
        if ( systemsDirectory != null && systemsDirectory.exists() ) {
            for(final File f : systemsDirectory.listFiles() ) {
                if ( f.isFile() && f.getName().endsWith(".txt") && !f.getName().startsWith(".") ) {
                    candidates.add(f.getName());
                }
            }
            Collections.sort(candidates);
        }
        if ( candidates.size() == 0 ) {
            throw new MojoExecutionException("No model files found in " + systemsDirectory);
        }
        for(final String name : candidates) {
            logger.debug("Reading model " + name + " in project " + project.getId());
            try {
                final File f = new File(systemsDirectory, name);
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
                throw new MojoExecutionException("Unable to read " + name, io);
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
    public static Model readFullModel(final File systemsDirectory,
            final List<File> dependentModels,
            final MavenProject project,
            final MavenSession session,
            final Logger logger)
    throws MojoExecutionException {
        try {
            final Model localModel = readLocalModel(systemsDirectory, project, session, logger);

            // check dependent models
            Model depModel = null;
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
            final Model result;
            if ( depModel != null ) {
                Map<Traceable, String> errors = ModelUtility.validate(depModel);
                if (errors != null ) {
                    throw new MojoExecutionException("Invalid model : " + errors);
                }
                ModelUtility.merge(depModel, localModel);
                errors = ModelUtility.validate(depModel);
                if (errors != null ) {
                    throw new MojoExecutionException("Invalid model : " + errors);
                }

                result = depModel;
            } else {
                result = localModel;
            }
            return result;
        } catch ( final IOException ioe) {
            throw new MojoExecutionException("Unable to cache model", ioe);
        }
    }

    public static org.apache.sling.provisioning.model.Artifact getBaseArtifact(final Model model) throws MojoExecutionException {
        final Feature base = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
        if ( base == null ) {
            throw new MojoExecutionException("No launchpad feature found.");
        }
        // get global run mode
        final RunMode runMode = base.getRunMode(null);
        if ( runMode == null ) {
            throw new MojoExecutionException("No global run mode found in launchpad feature.");
        }
        if ( runMode.getArtifactGroups().isEmpty() ) {
            throw new MojoExecutionException("No base artifacts defined.");
        }
        if ( runMode.getArtifactGroups().size() > 1 ) {
            throw new MojoExecutionException("Base run mode should only have a single start level.");
        }
        org.apache.sling.provisioning.model.Artifact firstArtifact = null;
        for(final org.apache.sling.provisioning.model.Artifact a : runMode.getArtifactGroups().get(0)) {
            if ( firstArtifact == null ) {
                firstArtifact = a;
            } else {
                throw new MojoExecutionException("Base run mode should contain exactly one artifact.");
            }
        }
        return firstArtifact;
    }

    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    public static Artifact getArtifact(final MavenProject project,
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
        return null;
    }

    private static final String RAW_MODEL_TXT = Model.class.getName() + "/raw";
    private static final String EFFECTIVE_MODEL_TXT = Model.class.getName() + "/effective";

    private static final String RAW_MODEL = Model.class.getName() + "/raw";
    private static final String EFFECTIVE_MODEL = Model.class.getName() + "/effective";

    /**
     * Store the raw model in the project.
     * @param project The maven project
     * @param model The model
     * @throws IOException If writing fails
     */
    public static void storeRawModel(final MavenProject project, final Model model)
    throws IOException {
        final StringWriter w = new StringWriter();
        ModelWriter.write(w, model);
        project.setContextValue(RAW_MODEL_TXT, w.toString());
    }

    /**
     * Get the raw model from the project
     * @param project The maven projet
     * @return The raw model
     * @throws MojoExecutionException If reading fails
     */
    public static Model getRawModel(final MavenProject project) throws MojoExecutionException {
        Model result = (Model)project.getContextValue(RAW_MODEL);
        if ( result == null ) {
            final String contents = (String)project.getContextValue(RAW_MODEL_TXT);
            try {
                result = ModelReader.read(new StringReader(contents), null);
                project.setContextValue(RAW_MODEL, result);
            } catch ( final IOException ioe) {
                throw new MojoExecutionException("Unable to read cached model.", ioe);
            }
        }
        return result;
    }

    /**
     * Store the effective model in the project.
     * @param project The maven project
     * @param model The model
     * @throws IOException If writing fails
     */
    public static void storeEffectiveModel(final MavenProject project, final Model model)
    throws IOException {
        final StringWriter w = new StringWriter();
        ModelWriter.write(w, model);
        project.setContextValue(EFFECTIVE_MODEL_TXT, w.toString());
    }

    /**
     * Get the effective model from the project
     * @param project The maven projet
     * @return The raw model
     * @throws MojoExecutionException If reading fails
     */
    public static Model getEffectiveModel(final MavenProject project) throws MojoExecutionException {
        Model result = (Model)project.getContextValue(EFFECTIVE_MODEL);
        if ( result == null ) {
            final String contents = (String)project.getContextValue(EFFECTIVE_MODEL_TXT);
            try {
                result = ModelReader.read(new StringReader(contents), null);
                project.setContextValue(EFFECTIVE_MODEL, result);
            } catch ( final IOException ioe) {
                throw new MojoExecutionException("Unable to read cached model.", ioe);
            }
        }
        return result;
    }
}
