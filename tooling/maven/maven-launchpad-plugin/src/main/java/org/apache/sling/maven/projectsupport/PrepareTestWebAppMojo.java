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

import java.io.File;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Initialize a Sling integration test webapp by extracting bundles into the
 * correct locations, including the current artifact.
 */
@Mojo( name = "prepare-test-webapp", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.PACKAGE)
public class PrepareTestWebAppMojo extends PreparePackageMojo {

    /**
     * The project's build directory (i.e. target).
     */
    @Parameter( property = "project.build.directory", readonly = true)
    private File buildDirectory;

    /**
     * The start level for the current artifact.
     */
    @Parameter( defaultValue = "16")
    private int startLevel;

    /**
     * The output directory for bundles.
     */
    @Parameter( defaultValue = "${project.build.directory}/launchpad-bundles")
    private File outputDirectory;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Override
    public void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        super.executeWithArtifacts();
        copy(getPrimaryArtifact(), startLevel, null, getOutputDirectory());
    }

    @Override
    protected File getOutputDirectory() {
        return outputDirectory;
    }


    @Override
    protected void unpackBaseArtifact() throws MojoExecutionException {
        // No-op. This is JAR-specific.
    }

    private File getPrimaryArtifact() throws MojoExecutionException {
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(project.getPackaging());

        String artifactName = project.getBuild().getFinalName() + "." + handler.getExtension();

        File file = new File(buildDirectory, artifactName);
        if (!file.exists()) {
            throw new MojoExecutionException("Project's primary artifact (" + file.getPath() + ") doesn't exist.");
        }
        return file;
    }

}
