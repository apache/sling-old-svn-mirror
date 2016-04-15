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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Initialize a Sling application project by extracting bundles into the correct
 * locations.
 */
@Mojo(
        name = "package",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class PackageMojo extends AbstractSlingStartMojo {

    private static final String[] EXCLUDES_MANIFEST = new String[] {"META-INF/MANIFEST.MF"};

    /**
     * The Jar archiver.
     */
    @Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        @SuppressWarnings("unchecked")
        final Map<String, File> globalContentsMap = (Map<String, File>) this.project.getContextValue(BuildConstants.CONTEXT_GLOBAL);

        this.packageStandaloneApp(globalContentsMap);
        this.packageWebapp(globalContentsMap);
    }

    private void packageStandaloneApp(final Map<String, File> globalContentsMap) throws MojoExecutionException {
        this.getLog().info("Packaging standalone jar...");

        final File buildDirectory = new File(this.project.getBuild().getDirectory());
        @SuppressWarnings("unchecked")
        final Map<String, File> contentsMap = (Map<String, File>) this.project.getContextValue(BuildConstants.CONTEXT_STANDALONE);

        final File buildOutputDirectory = this.getStandaloneOutputDirectory();
        final File manifestFile = new File(buildOutputDirectory, "META-INF/MANIFEST.MF");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(manifestFile);
            final Manifest mf = new Manifest(fis);

            final File outputFile = new File(buildDirectory, this.project.getArtifactId() + "-" + this.project.getVersion() + ".jar");

            final JarArchiverHelper helper = new JarArchiverHelper(jarArchiver, this.project, outputFile, mf);
            helper.addDirectory(buildOutputDirectory, null, EXCLUDES_MANIFEST);

            helper.addArtifacts(globalContentsMap, "");
            helper.addArtifacts(contentsMap, "");

            helper.createArchive();
            if ( BuildConstants.PACKAGING_SLINGSTART.equals(project.getPackaging()) ) {
                project.getArtifact().setFile(outputFile);
            } else {
                projectHelper.attachArtifact(project, BuildConstants.TYPE_JAR, BuildConstants.CLASSIFIER_APP, outputFile);
            }
        } catch ( final IOException ioe) {
            throw new MojoExecutionException("Unable to create standalone jar", ioe);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private void packageWebapp(final Map<String, File> globalContentsMap) throws MojoExecutionException {
        if ( this.createWebapp ) {
            this.getLog().info("Packaging webapp...");

            final File buildDirectory = new File(this.project.getBuild().getDirectory());
            @SuppressWarnings("unchecked")
            final Map<String, File> contentsMap = (Map<String, File>) this.project.getContextValue(BuildConstants.CONTEXT_WEBAPP);

            final File buildOutputDirectory = new File(buildDirectory, BuildConstants.WEBAPP_OUTDIR);
            final File outputFile = new File(buildDirectory, this.project.getArtifactId() + "-" + this.project.getVersion() + ".war");

            final JarArchiverHelper helper = new JarArchiverHelper(this.jarArchiver, this.project, outputFile);
            helper.addDirectory(buildOutputDirectory, null, EXCLUDES_MANIFEST);

            helper.addArtifacts(globalContentsMap, "WEB-INF/");
            helper.addArtifacts(contentsMap, "WEB-INF/");

            helper.createArchive();

            projectHelper.attachArtifact(project, BuildConstants.TYPE_WAR, BuildConstants.CLASSIFIER_WEBAPP, outputFile);
        }
    }
}
