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
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConstants;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMTraceable;
import org.apache.sling.slingstart.model.SSMValidator;
import org.apache.sling.slingstart.model.txt.TXTSSMModelReader;
import org.codehaus.plexus.logging.Logger;

public abstract class ModelUtils {

    /**
     * Read all model files from the directory in alphabetical order
     * @param logger
     */
    private static SSMDeliverable readLocalModel(final File systemsDirectory, final MavenProject project, final MavenSession session, final Logger logger)
    throws MojoExecutionException {
        final SSMDeliverable result = new SSMDeliverable();
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
                    final SSMDeliverable current = TXTSSMModelReader.read(reader, f.getAbsolutePath());
                    final Map<SSMTraceable, String> errors = new SSMValidator().validate(current);
                    if (errors != null ) {
                        throw new MojoExecutionException("Invalid model at " + name + " : " + errors);
                    }
                    result.merge(current);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            } catch ( final IOException io) {
                throw new MojoExecutionException("Unable to read " + name, io);
            }
        }

        final Map<SSMTraceable, String> errors = new SSMValidator().validate(result);
        if (errors != null ) {
            throw new MojoExecutionException("Invalid assembled model : " + errors);
        }

        return result;
    }

    /**
     * Read the full model
     */
    public static SSMDeliverable readFullModel(final File systemsDirectory,
            final List<File> dependentModels,
            final MavenProject project,
            final MavenSession session,
            final Logger logger)
    throws MojoExecutionException {
        try {
            final SSMDeliverable localModel = readLocalModel(systemsDirectory, project, session, logger);

            // check dependent models
            SSMDeliverable depModel = null;
            for(final File file : dependentModels) {
                FileReader r = null;
                try {
                    r = new FileReader(file);
                    if ( depModel == null ) {
                        depModel = new SSMDeliverable();
                    }
                    final SSMDeliverable readModel = TXTSSMModelReader.read(r, file.getAbsolutePath());
                    final Map<SSMTraceable, String> errors = new SSMValidator().validate(readModel);
                    if (errors != null ) {
                        throw new MojoExecutionException("Invalid model at " + file + " : " + errors);
                    }
                    depModel.merge(readModel);
                } finally {
                    IOUtils.closeQuietly(r);
                }
            }
            final SSMDeliverable result;
            if ( depModel != null ) {
                Map<SSMTraceable, String> errors = new SSMValidator().validate(depModel);
                if (errors != null ) {
                    throw new MojoExecutionException("Invalid model : " + errors);
                }
                depModel.merge(localModel);
                errors = new SSMValidator().validate(depModel);
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

    public static SSMArtifact getBaseArtifact(final SSMDeliverable model) throws MojoExecutionException {
        // get base run mode
        final SSMFeature base = model.getRunMode(SSMConstants.RUN_MODE_BASE);
        if ( base == null ) {
            throw new MojoExecutionException("No base run mode found.");
        }
        if ( base.getStartLevels().size() == 0 ) {
            throw new MojoExecutionException("No base artifacts defined.");
        }
        if ( base.getStartLevels().size() > 1 ) {
            throw new MojoExecutionException("Base run mode should only have a single start level.");
        }
        if ( base.getStartLevels().get(0).getArtifacts().size() != 1 ) {
            throw new MojoExecutionException("Base run mode should contain exactly one artifact.");
        }

        return base.getStartLevels().get(0).getArtifacts().get(0);
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
}
