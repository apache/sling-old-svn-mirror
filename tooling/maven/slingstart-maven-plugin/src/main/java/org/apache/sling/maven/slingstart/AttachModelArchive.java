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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.io.ModelArchiveWriter;

/**
 * Attach the model archive as a project artifact.
 */
@Mojo(
        name = "attach-modelarchive",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class AttachModelArchive extends AbstractSlingStartMojo {

    /**
     * The filename to be used for the generated model archive file.
     */
    @Parameter(defaultValue = "${project.build.finalName}")
    private String modelArchiveName;

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
        Model model = ProjectHelper.getRawModel(this.project);
        if (usePomVariables) {
            model = ModelUtility.applyVariables(model, new PomVariableResolver(project));
        }
        if (usePomDependencies) {
            model = ModelUtility.applyArtifactVersions(model, new PomArtifactVersionResolver(project, allowUnresolvedPomDependencies));
        }

        // write the model archive
        final File outputFile = new File(this.project.getBuild().getDirectory() + File.separatorChar + modelArchiveName + "." + ModelArchiveWriter.DEFAULT_EXTENSION);
        outputFile.getParentFile().mkdirs();

        try ( final FileOutputStream fos = new FileOutputStream(outputFile)) {
            // TODO provide base manifest
            final JarOutputStream jos = ModelArchiveWriter.write(fos, model, null, new ModelArchiveWriter.ArtifactProvider() {

                @Override
                public InputStream getInputStream(final Artifact artifact) throws IOException {
                    try {
                        final org.apache.maven.artifact.Artifact a = ModelUtils.getArtifact(project, mavenSession,
                                artifactHandlerManager, resolver,
                                artifact.getGroupId(),
                                artifact.getArtifactId(),
                                artifact.getVersion(),
                                artifact.getType(),
                                artifact.getClassifier());
                        return new FileInputStream(a.getFile());
                    } catch (MojoExecutionException e) {
                        throw (IOException)new IOException("Unable to get artifact: " + artifact.toMvnUrl()).initCause(e);
                    }
                }
            });

            // handle license etc.
            final File classesDir = new File(this.project.getBuild().getOutputDirectory());
            if ( classesDir.exists() ) {
                final File metaInfDir = new File(classesDir, "META-INF");
                for(final String name : new String[] {"LICENSE", "NOTICE", "DEPENDENCIES"}) {
                    final File f = new File(metaInfDir, name);
                    if ( f.exists() ) {
                        final JarEntry artifactEntry = new JarEntry("META-INF/" + name);
                        jos.putNextEntry(artifactEntry);

                        final byte[] buffer = new byte[8192];
                        try (final InputStream is = new FileInputStream(f)) {
                            int l = 0;
                            while ( (l = is.read(buffer)) > 0 ) {
                                jos.write(buffer, 0, l);
                            }
                        }
                        jos.closeEntry();

                    }
                }
            }
            jos.finish();
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to write model archive to " + outputFile + " : " + e.getMessage(), e);
        }

        // attach it as an additional artifact
        projectHelper.attachArtifact(project, ModelArchiveWriter.DEFAULT_EXTENSION,
                    BuildConstants.CLASSIFIER_MAR, outputFile);
    }
}
