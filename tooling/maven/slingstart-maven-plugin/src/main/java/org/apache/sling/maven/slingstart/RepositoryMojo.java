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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.io.ModelWriter;

/**
 * Create a mvn repository structure from the artifacts
 */
@Mojo(
        name = "repository",
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class RepositoryMojo extends AbstractSlingStartMojo {

    private static final String DIR_NAME = "artifacts";

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Component
    private ArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().info("Creating repository...");
        final File artifactDir = new File(this.project.getBuild().getDirectory(), DIR_NAME);

        // artifacts
        final Model model = ProjectHelper.getEffectiveModel(this.project, getResolverOptions());

        for(final Feature feature : model.getFeatures()) {
            for(final RunMode runMode : feature.getRunModes()) {
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    for(final org.apache.sling.provisioning.model.Artifact artifact : group ) {
                        copyArtifactToRepository(artifact, artifactDir);
                    }
                }
            }
        }
        // base artifact
        try {
            final org.apache.sling.provisioning.model.Artifact baseArtifact = ModelUtils.findBaseArtifact(model);
            final org.apache.sling.provisioning.model.Artifact appArtifact =
                    new org.apache.sling.provisioning.model.Artifact(baseArtifact.getGroupId(),
                    baseArtifact.getArtifactId(),
                    baseArtifact.getVersion(),
                    BuildConstants.CLASSIFIER_APP,
                    BuildConstants.TYPE_JAR);
            copyArtifactToRepository(appArtifact, artifactDir);
        } catch ( final MavenExecutionException mee) {
            throw new MojoExecutionException(mee.getMessage(), mee.getCause());
        }
        // models
        Model rawModel = ProjectHelper.getRawModel(this.project);
        if (usePomVariables) {
            rawModel = ModelUtility.applyVariables(rawModel, new PomVariableResolver(project));
        }
        if (usePomDependencies) {
            rawModel = ModelUtility.applyArtifactVersions(rawModel, new PomArtifactVersionResolver(project, allowUnresolvedPomDependencies));
        }

        final String classifier = (project.getPackaging().equals(BuildConstants.PACKAGING_PARTIAL_SYSTEM) ? null : BuildConstants.PACKAGING_PARTIAL_SYSTEM);
        final org.apache.sling.provisioning.model.Artifact rawModelArtifact =
                new org.apache.sling.provisioning.model.Artifact(
                        this.project.getGroupId(),
                        this.project.getArtifactId(),
                        this.project.getVersion(),
                        classifier,
                        BuildConstants.TYPE_TXT);
        final File rawModelFile = getRepositoryFile(artifactDir, rawModelArtifact);

        Writer writer = null;
        try {
            writer = new FileWriter(rawModelFile);
            ModelWriter.write(writer, rawModel);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write model to " + rawModelFile, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        for(final Map.Entry<String, String> entry : ProjectHelper.getDependencyModel(this.project).entrySet()) {
            final org.apache.sling.provisioning.model.Artifact modelDepArtifact = org.apache.sling.provisioning.model.Artifact.fromMvnUrl(entry.getKey());
            final String modelClassifier = (modelDepArtifact.getType().equals(BuildConstants.PACKAGING_SLINGSTART) ? BuildConstants.PACKAGING_PARTIAL_SYSTEM : modelDepArtifact.getClassifier());
            final org.apache.sling.provisioning.model.Artifact modelArtifact = new org.apache.sling.provisioning.model.Artifact(
                    modelDepArtifact.getGroupId(),
                    modelDepArtifact.getArtifactId(),
                    modelDepArtifact.getVersion(),
                    modelClassifier,
                    BuildConstants.TYPE_TXT);
            final File modelFile = getRepositoryFile(artifactDir, modelArtifact);
            Writer modelWriter = null;
            try {
                modelWriter = new FileWriter(modelFile);
                modelWriter.write(entry.getValue());
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to write model to " + modelFile, e);
            } finally {
                IOUtils.closeQuietly(modelWriter);
            }
        }
    }

    /**
     * Get the file in the repository directory
     * @param artifactDir The base artifact directory
     * @param artifact The artifact
     * @return The file
     */
    private File getRepositoryFile(final File artifactDir, final org.apache.sling.provisioning.model.Artifact artifact) {
        final StringBuilder artifactNameBuilder = new StringBuilder();
        artifactNameBuilder.append(artifact.getArtifactId());
        artifactNameBuilder.append('-');
        artifactNameBuilder.append(artifact.getVersion());
        if ( artifact.getClassifier() != null && artifact.getClassifier().length() > 0 ) {
            artifactNameBuilder.append('-');
            artifactNameBuilder.append(artifact.getClassifier());
        }
        artifactNameBuilder.append('.');
        artifactNameBuilder.append(artifact.getType());
        final String artifactName = artifactNameBuilder.toString();

        final StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId().replace('.', File.separatorChar));
        sb.append(File.separatorChar);
        sb.append(artifact.getArtifactId());
        sb.append(File.separatorChar);
        sb.append(artifact.getVersion());
        sb.append(File.separatorChar);
        sb.append(artifactName);
        final String destPath = sb.toString();

        final File artifactFile = new File(artifactDir, destPath);
        artifactFile.getParentFile().mkdirs();

        return artifactFile;
    }

    /**
     * Copy a single artifact to the repository
     * @throws MojoExecutionException
     */
    private void copyArtifactToRepository(final org.apache.sling.provisioning.model.Artifact artifact,
            final File artifactDir)
    throws MojoExecutionException {
        final File artifactFile = getRepositoryFile(artifactDir, artifact);

        final Artifact source = ModelUtils.getArtifact(this.project, this.mavenSession, this.artifactHandlerManager, this.resolver,
                artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier());

        try {
            FileUtils.copyFile(source.getFile(), artifactFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy artifact from " + source.getFile(), e);
        }
    }
}
