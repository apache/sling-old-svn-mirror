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
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Create and attach a JAR file containing the resolved artifacts from the
 * bundle list.
 */
@Mojo( name = "create-bundle-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class CreateBundleJarMojo extends AbstractLaunchpadFrameworkMojo {

    /**
     * The list of resources we want to add to the bundle JAR file.
     */
    @Parameter
    private Resource[] resources;

    /**
     * The output directory.
     */
    @Parameter( defaultValue = "${project.build.directory}")
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter( defaultValue = "${project.artifactId}-${project.version}", required = true)
    private String jarName;

    /**
     * The Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    private static final String CLASSIFIER = "bundles";

    public static final String[] DEFAULT_INCLUDES = { "**/**" };

    private void addBundles() throws MojoExecutionException {
        BundleList bundles = getInitializedBundleList();

        for (StartLevel level : bundles.getStartLevels()) {
            for (Bundle bundle : level.getBundles()) {
                Artifact artifact = getArtifact(new ArtifactDefinition(bundle,
                        level.getStartLevel()));
                final String destFileName = getPathForArtifact(level.getStartLevel(), bundle.getRunModes(), artifact.getFile().getName());
                try {
                    jarArchiver.addFile(artifact.getFile(), destFileName);
                } catch (ArchiverException e) {
                    throw new MojoExecutionException(
                            "Unable to add file to bundle jar file: "
                                    + artifact.getFile().getAbsolutePath(), e);
                }
            }
        }
    }

    private void addResources(Resource resource) throws MojoExecutionException {
        getLog().info(
                String.format("Adding resources [%s] to [%s]", resource
                        .getDirectory(), resource.getTargetPath()));
        String[] fileNames = getFilesToCopy(resource);
        for (int i = 0; i < fileNames.length; i++) {
            String targetFileName = fileNames[i];
            if (resource.getTargetPath() != null) {
                targetFileName = resource.getTargetPath() + File.separator
                        + targetFileName;
            }

            try {
                jarArchiver.addFile(new File(resource.getDirectory(),
                        fileNames[i]), targetFileName);
            } catch (ArchiverException e) {
                throw new MojoExecutionException(
                        "Unable to add resources to JAR file", e);
            }

        }
    }

    private File createJARFile() throws MojoExecutionException {
        File jarFile = new File(outputDirectory, jarName + "-" + CLASSIFIER
                + "." + JAR);
        jarArchiver.setDestFile(jarFile);

        addBundles();
        addResources();

        try {
            jarArchiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException(
                    "Unable to create bundle jar file", e);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Unable to create bundle jar file", e);
        }

        return jarFile;
    }

    private void addResources() throws MojoExecutionException {
        if (resources != null) {
            for (Resource resource : resources) {
                if (!(new File(resource.getDirectory())).isAbsolute()) {
                    resource.setDirectory(project.getBasedir() + File.separator
                            + resource.getDirectory());
                }
                addResources(resource);
            }
        }
    }

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException,
            MojoFailureException {
        File jarFile = createJARFile();
        projectHelper.attachArtifact(project, JAR, CLASSIFIER, jarFile);
    }

    /**
     * Returns a list of filenames that should be copied over to the destination
     * directory.
     *
     * @param resource
     *            the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private static String[] getFilesToCopy(Resource resource) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resource.getDirectory());
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            scanner.setIncludes(resource.getIncludes().toArray(
                    new String[resource.getIncludes().size()]));
        } else {
            scanner.setIncludes(DEFAULT_INCLUDES);
        }
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            scanner.setExcludes(resource.getExcludes().toArray(
                    new String[resource.getExcludes().size()]));
        }

        scanner.addDefaultExcludes();

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
