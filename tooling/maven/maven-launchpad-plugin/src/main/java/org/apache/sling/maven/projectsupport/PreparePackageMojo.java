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
package org.apache.sling.maven.projectsupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Initialize a Sling application project by extracting bundles into the correct
 * locations.
 */
@Mojo(name = "prepare-package", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class PreparePackageMojo extends AbstractLaunchpadFrameworkMojo {

    /**
     * The output directory for the default bundles in a WAR-packaged project,
     * the base JAR (in the subdirectory named in the baseDestination
     * parameter), and any additional bundles.
     */
    @Parameter(defaultValue = "${project.build.directory}/launchpad-bundles")
    private File warOutputDirectory;

    /**
     * The project's packaging type.
     */
    @Parameter(property = "project.packaging")
    private String packaging;

    /**
     * The definition of the base JAR.
     */
    @Parameter
    private ArtifactDefinition base;

    /**
     * The definition of the package to be included to provide web support for
     * JAR-packaged projects (i.e. pax-web).
     */
    @Parameter
    private ArtifactDefinition jarWebSupport;

    /**
     * The project's build output directory (i.e. target/classes).
     */
    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private File buildOutputDirectory;

    /**
     * The temp directory (i.e. target/maven-launchpad-plugintmp).
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/maven-launchpad-plugintmp", readonly = true)
    private File tempDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * The Jar archiver.
     */
    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    @Override
    public void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        copyBaseArtifact();
        copyBundles(getInitializedBundleList(), getOutputDirectory());
        copyConfigurationFiles();
        if (JAR.equals(packaging)) {
            unpackBaseArtifact();
        }
    }

    @Override
    protected void initArtifactDefinitions(Properties dependencies) {
        if (base == null) {
            base = new ArtifactDefinition();
        }
        base.initDefaults(dependencies.getProperty("base"));

        if (jarWebSupport == null) {
            jarWebSupport = new ArtifactDefinition();
        }
        jarWebSupport.initDefaults(dependencies.getProperty("jarWebSupport"));
    }

    /**
     * Add the JAR Web Support bundle to the bundle list.
     */
    @Override
    protected void initBundleList(BundleList bundleList) {
        if (packaging.equals(JAR)) {
            bundleList.add(jarWebSupport.toBundleList());
        }
    }

    /**
     * Patch the sling properties
     */
    private void patchSlingProperties(final File dest, final Properties additionalProps)
    throws MojoExecutionException {
        final File origSlingProps = new File(dest, "sling.properties");
        if ( !origSlingProps.exists() ) {
            throw new MojoExecutionException("sling.properties not found at " + origSlingProps);
        }

        // read original properties
        final Properties orig = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(origSlingProps);
            orig.load(fis);
        } catch (final IOException ioe) {
            throw new MojoExecutionException("Unable to read " + origSlingProps, ioe);
        } finally {
            if ( fis != null ) {
                try { fis.close(); } catch (final IOException ignore) {}
            }
        }

        // patch
        final Enumeration<Object> keys = additionalProps.keys();
        if ( keys.hasMoreElements() ) {
            getLog().info("Patching sling.properties");
        }
        while ( keys.hasMoreElements() ) {
            final Object key = keys.nextElement();
            orig.put(key, additionalProps.get(key));
        }

        /// and save
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(origSlingProps);
            orig.store(fos, null);
        } catch (final IOException ioe) {
            throw new MojoExecutionException("Unable to save " + origSlingProps, ioe);
        } finally {
            if ( fis != null ) {
                try { fis.close(); } catch (final IOException ignore) {}
            }
        }
    }

    /**
     * Patch the sling bootstrap command file
     */
    private void patchSlingBootstrap(final File dest, final String additionalCmd)
    throws MojoExecutionException {
        getLog().info("Patching sling_bootstrap.txt");
        final File origSlingCmd = new File(dest, "sling_bootstrap.txt");
        FileWriter writer = null;

        /// and write
        try {
            writer = new FileWriter(origSlingCmd);

            writer.write(additionalCmd);
        } catch (final IOException ioe) {
            throw new MojoExecutionException("Unable to save " + origSlingCmd, ioe);
        } finally {
            if ( writer != null ) {
                try { writer.close(); } catch (final IOException ignore) {}
            }
        }
    }

    private void copyBaseArtifact() throws MojoExecutionException {
        Artifact artifact = getBaseArtifact();
        if (artifact == null) {
            throw new MojoExecutionException(
                    String.format("Project doesn't have a base dependency of groupId %s and artifactId %s",
                                    base.getGroupId(), base.getArtifactId()));
        }
        File destinationDir = new File(getOutputDirectory(), baseDestination);
        File destinationFile = new File(destinationDir, artifact
                .getArtifactId()
                + "." + artifact.getArtifactHandler().getExtension());

        // check if custom sling.properties file or bootstrap command exists
        final Properties additionalProps = this.getSlingProperties(JAR.equals(this.packaging));
        final String bootstrapCmd = this.getSlingBootstrap(JAR.equals(this.packaging));
        if ( additionalProps != null || bootstrapCmd != null ) {
            // unpack to a temp destination
            final File dest = new File(this.tempDirectory, "basejar");
            try {
                unpack(artifact.getFile(), dest);

                // patch sling properties
                if ( additionalProps != null ) {
                    this.patchSlingProperties(dest, additionalProps);
                }

                // patch bootstrap command
                if  ( bootstrapCmd != null ) {
                    this.patchSlingBootstrap(dest, bootstrapCmd);
                }

                // and repack again
                pack(dest, destinationFile);
            } finally {
                this.tempDirectory.delete();
            }
        } else {
            // we can just copy
            if (shouldCopy(artifact.getFile(), destinationFile)) {
                try {
                    getLog().info(
                            String.format("Copying base artifact from %s to %s.",
                                    artifact.getFile(), destinationFile));
                    FileUtils.copyFile(artifact.getFile(), destinationFile);
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Unable to copy base artifact.", e);
                }
            } else {
                getLog().debug(
                        String.format("Skipping copy of base artifact from %s.",
                                artifact.getFile()));
            }
        }
    }

    private Artifact getBaseArtifact() throws MojoExecutionException {
        Artifact baseDependency = getBaseDependency();
        if (baseDependency == null) {
            return null;
        }

        return getArtifact(base.getGroupId(), base.getArtifactId(),
                baseDependency.getVersion(), base.getType(), base
                        .getClassifier());

    }

    private Artifact getBaseDependency() {
        return project.getArtifactMap().get(
                base.getGroupId() + ":" + base.getArtifactId());
    }

    protected File getOutputDirectory() {
        if (WAR.equals(packaging)) {
            return warOutputDirectory;
        }
        return buildOutputDirectory;
    }

    protected void unpackBaseArtifact() throws MojoExecutionException {
        Artifact artifact = getBaseDependency();
        if (artifact == null) {
            throw new MojoExecutionException(
                    String
                            .format(
                                    "Project doesn't have a base dependency of groupId %s and artifactId %s",
                                    base.getGroupId(), base.getArtifactId()));
        }
        unpack(artifact.getFile(), buildOutputDirectory);
    }

    private void copyConfigurationFiles() throws MojoExecutionException {
        final File configDir = this.getConfigDirectory();
        if (configDir.exists() ) {
            try {
                copyDirectory(configDir, new File(getOutputDirectory(), CONFIG_PATH_PREFIX), null, FileUtils.getDefaultExcludes());
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy configuration files", e);
            }
        }
    }

    private void unpack(File source, File destination)
            throws MojoExecutionException {
        getLog().info("Unpacking " + source.getPath() + " to\n  " + destination.getPath());
        try {
            destination.mkdirs();

            UnArchiver unArchiver = archiverManager.getUnArchiver(source);

            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(destination);

            unArchiver.extract();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unable to find archiver for " + source.getPath(), e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Unable to unpack " + source.getPath(), e);
        }
    }

    private void pack(File sourceDir, File destination)
    throws MojoExecutionException {
        getLog().info("Packing " + sourceDir.getPath() + " to\n  " + destination.getPath());
        try {
            destination.getParentFile().mkdirs();

            jarArchiver.setDestFile(destination);
            jarArchiver.addDirectory(sourceDir);
            jarArchiver.setManifest(new File(sourceDir, "META-INF/MANIFEST.MF".replace('/', File.separatorChar)));
            jarArchiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to pack " + sourceDir.getPath(), e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Unable to pack " + sourceDir.getPath(), e);
        }
    }
}
