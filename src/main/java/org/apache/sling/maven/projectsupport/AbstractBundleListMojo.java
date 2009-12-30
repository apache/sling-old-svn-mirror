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
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class AbstractBundleListMojo extends AbstractMojo {

    /**
     * The name of the directory within the output directory into which the base
     * JAR should be installed.
     * 
     * @parameter default-value="resources"
     */
    protected String baseDestination;

    /**
     * The directory which contains the start-level bundle directories.
     * 
     * @parameter default-value="bundles"
     */
    protected String bundlesDirectory;

    /**
     * The definition of the defaultBundleList artifact.
     * 
     * @parameter
     */
    private ArtifactDefinition defaultBundleList;

    /**
     * The definition of the defaultBundles package.
     * 
     * @parameter
     */
    private ArtifactDefinition defaultBundles;

    /**
     * @parameter default-value="${basedir}/src/main/bundles/list.xml"
     */
    protected File bundleListFile;

    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @component
     */
    private ArtifactFactory factory;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * JAR Packaging type.
     */
    protected static final String JAR = "jar";

    /**
     * WAR Packaging type.
     */
    protected static final String WAR = "war";

    /**
     * List of Remote Repositories used by the resolver.
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List remoteRepos;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @component
     */
    private ArtifactResolver resolver;

    public final void execute() throws MojoFailureException,
            MojoExecutionException {
        try {
            initArtifactDefinitions();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Unable to load dependency information from properties file.",
                    e);
        }
        executeWithArtifacts();

    }

    protected abstract void executeWithArtifacts()
            throws MojoExecutionException, MojoFailureException;

    protected Artifact getArtifact(ArtifactDefinition bundle)
            throws MojoExecutionException {
        return getArtifact(bundle.getGroupId(), bundle.getArtifactId(), bundle
                .getVersion(), bundle.getType() != null ? bundle.getType()
                : JAR, bundle.getClassifier());
    }

    protected Artifact getArtifact(String groupId, String artifactId,
            String version, String type, String classifier)
            throws MojoExecutionException {
        Artifact artifact;
        VersionRange vr;

        try {
            vr = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException e) {
            vr = VersionRange.createFromVersion(version);
        }

        if (StringUtils.isEmpty(classifier)) {
            artifact = factory.createDependencyArtifact(groupId, artifactId,
                    vr, type, null, Artifact.SCOPE_COMPILE);
        } else {
            artifact = factory.createDependencyArtifact(groupId, artifactId,
                    vr, type, classifier, Artifact.SCOPE_COMPILE);
        }
        try {
            resolver.resolve(artifact, remoteRepos, local);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to resolve artifact.", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to find artifact.", e);
        }
        return artifact;
    }

    protected final void initArtifactDefinitions() throws IOException {
        Properties dependencies = new Properties();
        dependencies
                .load(getClass()
                        .getResourceAsStream(
                                "/org/apache/sling/maven/projectsupport/dependencies.properties"));

        if (defaultBundles == null) {
            defaultBundles = new ArtifactDefinition();
        }
        defaultBundles.initDefaults(dependencies.getProperty("defaultBundles"));

        if (defaultBundleList == null) {
            defaultBundleList = new ArtifactDefinition();
        }
        defaultBundleList.initDefaults(dependencies.getProperty("defaultBundleList"));

        initArtifactDefinitions(dependencies);
    }

    protected void initArtifactDefinitions(Properties dependencies) {
    }

    protected void copy(ArtifactDefinition additionalBundle,
            File outputDirectory) throws MojoExecutionException {
        Artifact artifact = getArtifact(additionalBundle);
        copy(artifact.getFile(), additionalBundle.getStartLevel(),
                outputDirectory);
    }

    protected void copy(File file, int startLevel, File outputDirectory)
            throws MojoExecutionException {
        File destination = new File(outputDirectory, String.format(
                "%s/%s/%s/%s", baseDestination, bundlesDirectory, startLevel,
                file.getName()));
        if (shouldCopy(file, destination)) {
            getLog().info(
                    String.format("Copying bundle from %s to %s", file
                            .getPath(), destination.getPath()));
            try {
                FileUtils.copyFile(file, destination);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy bundle from "
                        + file.getPath(), e);
            }
        }
    }


    protected BundleList readBundleList() throws IOException, XmlPullParserException {
        return readBundleList(bundleListFile);
    }

    
    protected void outputBundleList(File outputDirectory)
            throws MojoExecutionException {
        try {
            if (bundleListFile != null && bundleListFile.exists()) {
                getLog().info(
                        "Using bundle list file from "
                                + bundleListFile.getAbsolutePath());
                BundleList bundles = readBundleList(bundleListFile);
                copyBundles(bundles, outputDirectory);
                return;
            }
        } catch (Exception e) {
            getLog()
                    .warn(
                            String
                                    .format(
                                            "Unable to use bundle list from %s. Falling back to bundles artifact.",
                                            bundleListFile), e);
        }

        try {
            Artifact artifact = getArtifact(defaultBundleList.getGroupId(),
                    defaultBundleList.getArtifactId(), defaultBundleList
                            .getVersion(), defaultBundleList.getType(),
                    defaultBundleList.getClassifier());
            getLog().info(
                    "Using bundle list file from "
                            + artifact.getFile().getAbsolutePath());
            BundleList bundles = readBundleList(artifact.getFile());
            copyBundles(bundles, outputDirectory);
            return;
        } catch (Exception e) {
            getLog()
                    .warn(
                            "Unable to load bundle list from artifact. Falling back to bundle jar",
                            e);
        }

        Artifact defaultBundlesArtifact = getArtifact(defaultBundles
                .getGroupId(), defaultBundles.getArtifactId(), defaultBundles
                .getVersion(), defaultBundles.getType(), defaultBundles
                .getClassifier());
        unpack(defaultBundlesArtifact.getFile(), outputDirectory, null,
                "META-INF/**");
    }

    private void copyBundles(BundleList bundles, File outputDirectory)
            throws MojoExecutionException {
        for (StartLevel startLevel : bundles.getStartLevels()) {
            for (Bundle bundle : startLevel.getBundles()) {
                copy(new ArtifactDefinition(bundle, startLevel.getLevel()),
                        outputDirectory);
            }
        }
    }

    protected BundleList readBundleList(File file) throws IOException,
            XmlPullParserException {
        BundleListXpp3Reader reader = new BundleListXpp3Reader();
        FileInputStream fis = new FileInputStream(file);
        try {
            return reader.read(fis);
        } finally {
            fis.close();
        }
    }

    protected boolean shouldCopy(File source, File dest) {
        if (!dest.exists()) {
            return true;
        } else {
            return source.lastModified() > dest.lastModified();
        }
    }

    protected void unpack(File source, File destination, String includes,
            String excludes) throws MojoExecutionException {
        getLog().info(
                "Unpacking " + source.getPath() + " to\n  "
                        + destination.getPath());
        try {
            destination.mkdirs();

            UnArchiver unArchiver = archiverManager.getUnArchiver(source);

            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(destination);

            if (StringUtils.isNotEmpty(excludes)
                    || StringUtils.isNotEmpty(includes)) {
                IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[] { new IncludeExcludeFileSelector() };

                if (StringUtils.isNotEmpty(excludes)) {
                    selectors[0].setExcludes(excludes.split(","));
                }

                if (StringUtils.isNotEmpty(includes)) {
                    selectors[0].setIncludes(includes.split(","));
                }

                unArchiver.setFileSelectors(selectors);
            }

            unArchiver.extract();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unable to find archiver for "
                    + source.getPath(), e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Unable to unpack "
                    + source.getPath(), e);
        }
    }

}
