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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.PropertyUtils;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;

public abstract class AbstractUsingBundleListMojo extends AbstractBundleListMojo {

    /**
     * JAR Packaging type.
     */
    protected static final String JAR = "jar";

    /**
     * WAR Packaging type.
     */
    protected static final String WAR = "war";

    protected static final String CONFIG_PATH_PREFIX = "resources/config";

    protected static final String BUNDLE_PATH_PREFIX = "resources/bundles";

    protected static boolean shouldCopy(File source, File dest) {
        if (!dest.exists()) {
            return true;
        }
        return source.lastModified() > dest.lastModified();
    }

    /**
     * The definition of the defaultBundleList artifact.
     *
     * @parameter
     */
    protected ArtifactDefinition defaultBundleList;

    /**
     * Any additional bundles to include in the project's bundles directory.
     *
     * @parameter
     */
    private ArtifactDefinition[] additionalBundles;

    private BundleList bundleList;

    /**
     * Bundles which should be removed from the project's bundles directory.
     *
     * @parameter
     */
    private ArtifactDefinition[] bundleExclusions;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component hint="maven"
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * If true, include the default bundles.
     *
     * @parameter default-value="true"
     */
    private boolean includeDefaultBundles;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List<?> remoteRepos;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @parameter
     */
    private File[] rewriteRuleFiles;

    /**
     * @parameter expression="${session}
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * @component
     */
    protected MavenFileFilter mavenFileFilter;

    /**
     * The zip unarchiver.
     *
     * @component role="org.codehaus.plexus.archiver.UnArchiver" roleHint="zip"
     */
    private ZipUnArchiver zipUnarchiver;

    private Properties slingProperties;

    private Properties slingWebappProperties;

    private Properties slingStandaloneProperties;

    private String slingBootstrapCommand;

    private String slingWebappBootstrapCommand;

    private String slingStandaloneBootstrapCommand;

    /**
     * @parameter default-value="${project.build.directory}/tmpBundleListconfig"
     */
    private File tmpOutputDir;

    /**
     * @parameter default-value="${project.build.directory}/tmpConfigDir"
     */
    private File tempConfigDir;

    private File overlayConfigDir;

    public final void execute() throws MojoFailureException, MojoExecutionException {
        try {
            initBundleList();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to load dependency information from properties file.", e);
        }
        executeWithArtifacts();

    }

    @Override
    protected File getConfigDirectory() {
        if ( this.overlayConfigDir != null ) {
            return this.overlayConfigDir;
        }
        return super.getConfigDirectory();
    }

    /**
     * Execute the logic of the plugin after the default artifacts have been
     * initialized.
     */
    protected abstract void executeWithArtifacts() throws MojoExecutionException, MojoFailureException;

    /**
     * Get a resolved Artifact from the coordinates found in the artifact
     * definition.
     *
     * @param def the artifact definition
     * @return the artifact, which has been resolved
     * @throws MojoExecutionException
     */
    protected Artifact getArtifact(ArtifactDefinition def) throws MojoExecutionException {
        return getArtifact(def.getGroupId(), def.getArtifactId(), def.getVersion(), def.getType(), def.getClassifier());
    }

    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    protected Artifact getArtifact(String groupId, String artifactId, String version, String type, String classifier)
            throws MojoExecutionException {
        Artifact artifact;
        VersionRange vr;

        try {
            vr = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException e) {
            vr = VersionRange.createFromVersion(version);
        }

        if (StringUtils.isEmpty(classifier)) {
            artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type, null, Artifact.SCOPE_COMPILE);
        } else {
            artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type, classifier,
                    Artifact.SCOPE_COMPILE);
        }

        // This code kicks in when the version specifier is a range.
        if (vr.getRecommendedVersion() == null) {
            try {
                List<?> availVersions = metadataSource.retrieveAvailableVersions(artifact, local, remoteRepos);
                ArtifactVersion resolvedVersion = vr.matchVersion(availVersions);
                artifact.setVersion(resolvedVersion.toString());
            } catch (ArtifactMetadataRetrievalException e) {
                throw new MojoExecutionException("Unable to find version for artifact", e);
            }

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

    protected BundleList getBundleList() {
        return bundleList;
    }

    /**
     * Hook methods for subclasses to initialize any additional artifact
     * definitions.
     *
     * @param dependencies the dependency properties loaded from the JAR file
     */
    protected void initArtifactDefinitions(Properties dependencies) {
    }

    /**
     * Hook methods for subclasses to initialize the bundle list.
     */
    protected void initBundleList(BundleList bundleList) {
    }

    protected boolean isCurrentArtifact(ArtifactDefinition def) {
        return (def.getGroupId().equals(project.getGroupId()) && def.getArtifactId().equals(project.getArtifactId()));
    }

    /**
     * Initialize the artifact definitions using defaults inside the plugin JAR.
     *
     * @throws IOException if the default properties can't be read
     * @throws XmlPullParserException
     * @throws MojoExecutionException
     */
    private final void initArtifactDefinitions() throws IOException {
        Properties dependencies = new Properties();
        dependencies.load(getClass().getResourceAsStream(
                "/org/apache/sling/maven/projectsupport/dependencies.properties"));

        if (defaultBundleList == null) {
            defaultBundleList = new ArtifactDefinition();
        }
        defaultBundleList.initDefaults(dependencies.getProperty("defaultBundleList"));

        initArtifactDefinitions(dependencies);
    }

    @SuppressWarnings("unchecked")
    private final void initBundleList() throws IOException, XmlPullParserException, MojoExecutionException {
        initArtifactDefinitions();
        if (isCurrentArtifact(defaultBundleList)) {
            bundleList = readBundleList(bundleListFile);
        } else {
            bundleList = new BundleList();
            if (includeDefaultBundles) {
                Artifact defBndListArtifact = getArtifact(defaultBundleList.getGroupId(),
                        defaultBundleList.getArtifactId(), defaultBundleList.getVersion(), defaultBundleList.getType(),
                        defaultBundleList.getClassifier());
                getLog().info("Using bundle list file from " + defBndListArtifact.getFile().getAbsolutePath());
                bundleList = readBundleList(defBndListArtifact.getFile());
            }

            if (bundleListFile.exists()) {
                bundleList.merge(readBundleList(bundleListFile));
            }
        }
        if (additionalBundles != null) {
            for (ArtifactDefinition def : additionalBundles) {
                bundleList.add(def.toBundle());
            }
        }
        addDependencies(bundleList);
        if (bundleExclusions != null) {
            for (ArtifactDefinition def : bundleExclusions) {
                bundleList.remove(def.toBundle(), false);
            }
        }

        final Set<Artifact> dependencies = project.getDependencyArtifacts();
        for (Artifact artifact : dependencies) {
            if (PARTIAL.equals(artifact.getType())) {
                getLog().info(
                        String.format("merging partial bundle list for %s:%s:%s", artifact.getGroupId(),
                                artifact.getArtifactId(), artifact.getVersion()));
                bundleList.merge(readBundleList(artifact.getFile()));

                // check for configuration artifact
                Artifact cfgArtifact = null;
                try {
                    cfgArtifact = getArtifact(artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion(),
                            AttachPartialBundleListMojo.CONFIG_TYPE,
                            AttachPartialBundleListMojo.CONFIG_CLASSIFIER);
                } catch (final MojoExecutionException ignore) {
                    // we just ignore this
                }
                if ( cfgArtifact != null ) {
                    getLog().info(
                            String.format("merging partial bundle list configuration for %s:%s:%s", cfgArtifact.getGroupId(),
                                    cfgArtifact.getArtifactId(), cfgArtifact.getVersion()));

                    // extract
                    zipUnarchiver.setSourceFile(cfgArtifact.getFile());
                    try {
                        this.tmpOutputDir.mkdirs();
                        zipUnarchiver.setDestDirectory(this.tmpOutputDir);
                        zipUnarchiver.extract();

                        final File slingDir = new File(this.tmpOutputDir, "sling");
                        this.readSlingProperties(new File(slingDir, AttachPartialBundleListMojo.SLING_COMMON_PROPS), 0);
                        this.readSlingProperties(new File(slingDir, AttachPartialBundleListMojo.SLING_WEBAPP_PROPS), 1);
                        this.readSlingProperties(new File(slingDir, AttachPartialBundleListMojo.SLING_STANDALONE_PROPS), 2);
                        this.readSlingBootstrap(new File(slingDir, AttachPartialBundleListMojo.SLING_COMMON_BOOTSTRAP), 0);
                        this.readSlingBootstrap(new File(slingDir, AttachPartialBundleListMojo.SLING_WEBAPP_BOOTSTRAP), 1);
                        this.readSlingBootstrap(new File(slingDir, AttachPartialBundleListMojo.SLING_STANDALONE_BOOTSTRAP), 2);

                        // and now configurations
                        if ( this.overlayConfigDir == null ) {
                            this.tempConfigDir.mkdirs();
                            if ( this.getConfigDirectory().exists() ) {
                                FileUtils.copyDirectory(this.getConfigDirectory(), this.tempConfigDir,
                                        null, FileUtils.getDefaultExcludesAsString());
                            }
                            this.overlayConfigDir = this.tempConfigDir;
                        }
                        final File configDir = new File(this.tmpOutputDir, "config");
                        if ( configDir.exists() ) {
                            FileUtils.copyDirectory(configDir, this.tempConfigDir,
                                    null, FileUtils.getDefaultExcludesAsString());
                        }
                    } catch (final ArchiverException ae) {
                        throw new MojoExecutionException("Unable to extract configuration archive.",ae);
                    } finally {
                        // and delete at the end
                        FileUtils.deleteDirectory(this.tmpOutputDir);
                    }
                }
            }
        }


        initBundleList(bundleList);

        interpolateProperties(bundleList);

        rewriteBundleList(bundleList);
    }

    private void interpolateProperties(BundleList bundleList) throws MojoExecutionException {
        Interpolator interpolator = createInterpolator();
        for (final StartLevel sl : bundleList.getStartLevels()) {
            for (final Bundle bndl : sl.getBundles()) {
                try {
                    bndl.setArtifactId(interpolator.interpolate(bndl.getArtifactId()));
                    bndl.setGroupId(interpolator.interpolate(bndl.getGroupId()));
                    bndl.setVersion(interpolator.interpolate(bndl.getVersion()));
                    bndl.setClassifier(interpolator.interpolate(bndl.getClassifier()));
                    bndl.setType(interpolator.interpolate(bndl.getType()));
                } catch (InterpolationException e) {
                    throw new MojoExecutionException("Unable to interpolate properties for bundle " + bndl.toString(), e);
                }
            }
        }

    }

    private Interpolator createInterpolator() {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();

        final Properties props = new Properties();
        props.putAll(project.getProperties());
        props.putAll(mavenSession.getExecutionProperties());

        interpolator.addValueSource(new PropertiesBasedValueSource(props));

        // add ${project.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource(Arrays.asList("project", "pom"), project, true));

        // add ${session.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource("session", mavenSession));

        // add ${settings.foo}
        final Settings settings = mavenSession.getSettings();
        if (settings != null) {
            interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        }

        return interpolator;
    }

    private void rewriteBundleList(BundleList bundleList) throws MojoExecutionException {
        if (rewriteRuleFiles != null) {
            KnowledgeBase knowledgeBase = createKnowledgeBase(rewriteRuleFiles);
            StatefulKnowledgeSession session = knowledgeBase.newStatefulKnowledgeSession();
            try {
                session.setGlobal("mavenSession", mavenSession);
                session.setGlobal("mavenProject", project);
                session.insert(bundleList);
                session.fireAllRules();
            } finally {
                session.dispose();
            }
        }
    }

    private KnowledgeBase createKnowledgeBase(File[] files) throws MojoExecutionException {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        builder.add(ResourceFactory.newClassPathResource("drools-globals.drl", getClass()), ResourceType.DRL);
        for (File file : files) {
            getLog().info("Parsing rule file " + file.getAbsolutePath());
            builder.add(ResourceFactory.newFileResource(file), ResourceType.DRL);
        }
        if (builder.hasErrors()) {
            getLog().error("Rule errors:");
            for (KnowledgeBuilderError error : builder.getErrors()) {
                getLog().error(error.toString());
            }
            throw new MojoExecutionException("Unable to create rules. See log for details.");
        }

        KnowledgeBase base = KnowledgeBaseFactory.newKnowledgeBase();
        base.addKnowledgePackages(builder.getKnowledgePackages());
        return base;
    }

    private void copyProperties(final Properties source, final Properties dest) {
        final Enumeration<Object> keys = source.keys();
        while ( keys.hasMoreElements() ) {
            final Object key = keys.nextElement();
            dest.put(key, source.get(key));
        }
    }

    private void readSlingProperties(final File propsFile, final int mode) throws MojoExecutionException {
        if (propsFile.exists()) {
            File tmp = null;
            try {
                tmp = File.createTempFile("sling", "props");
                mavenFileFilter.copyFile(propsFile, tmp, true, project, null, true,
                        System.getProperty("file.encoding"), mavenSession);
                final Properties loadedProps = PropertyUtils.loadPropertyFile(tmp, null);
                if ( mode == 0 ) {
                    if ( this.slingProperties == null ) {
                        this.slingProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingProperties);
                    }
                } else if ( mode == 1 ) {
                    if ( this.slingWebappProperties == null ) {
                        this.slingWebappProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingWebappProperties);
                    }
                } else {
                    if ( this.slingStandaloneProperties == null ) {
                        this.slingStandaloneProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingStandaloneProperties);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to create filtered properties file", e);
            } catch (MavenFilteringException e) {
                throw new MojoExecutionException("Unable to create filtered properties file", e);
            } finally {
                if (tmp != null) {
                    tmp.delete();
                }
            }
        }
    }

    protected Properties getSlingProperties(final boolean standalone) throws MojoExecutionException {
        readSlingProperties(this.commonSlingProps, 0);
        final Properties additionalProps = (standalone ? this.slingStandaloneProperties : this.slingWebappProperties);
        if ( this.slingProperties == null) {
            return additionalProps;
        }
        if ( additionalProps != null ) {
            final Properties combinedProps = new Properties();
            this.copyProperties(this.slingProperties, combinedProps);
            this.copyProperties(additionalProps, combinedProps);
            return combinedProps;
        }
        return this.slingProperties;
    }

    /**
     * Try to read the bootstrap command file
     * The filter is copied to a tmp location to apply filtering.
     * @throws MojoExecutionException
     */
    private void readSlingBootstrap(final File bootstrapFile, final int mode) throws MojoExecutionException {
        if (bootstrapFile.exists()) {
            File tmp = null;
            Reader reader = null;
            try {
                tmp = File.createTempFile("sling", "bootstrap");
                mavenFileFilter.copyFile(bootstrapFile, tmp, true, project, null, true,
                        System.getProperty("file.encoding"), mavenSession);
                reader = new FileReader(tmp);
                final StringBuilder sb = new StringBuilder();
                if ( mode == 0 ) {
                    if ( this.slingBootstrapCommand != null ) {
                        sb.append(this.slingBootstrapCommand);
                    }
                } else if ( mode == 1 ) {
                    if ( this.slingWebappBootstrapCommand != null ) {
                        sb.append(this.slingWebappBootstrapCommand);
                    }
                } else {
                    if ( this.slingStandaloneBootstrapCommand != null ) {
                        sb.append(this.slingStandaloneBootstrapCommand);
                    }
                }
                final char[] buffer = new char[2048];
                int l;
                while ( (l = reader.read(buffer, 0, buffer.length) ) != -1 ) {
                    sb.append(buffer, 0, l);
                }
                sb.append('\n');
                if ( mode == 0 ) {
                    this.slingBootstrapCommand = sb.toString();
                } else if ( mode == 1 ) {
                    this.slingWebappBootstrapCommand = sb.toString();
                } else {
                    this.slingStandaloneBootstrapCommand = sb.toString();
                }
            } catch (final IOException e) {
                throw new MojoExecutionException("Unable to create filtered bootstrap file", e);
            } catch (final MavenFilteringException e) {
                throw new MojoExecutionException("Unable to create filtered bootstrap file", e);
            } finally {
                if (tmp != null) {
                    tmp.delete();
                }
                if ( reader != null ) {
                    try {
                        reader.close();
                    } catch (final IOException ignore) {}
                }
            }
        }
    }

    /**
     * Try to read the bootstrap command file and return its content
     * The filter is copied to a tmp location to apply filtering.
     * @return The contents are <code>null</code>
     * @throws MojoExecutionException
     */
    protected String getSlingBootstrap(final boolean standalone) throws MojoExecutionException {
        this.readSlingBootstrap(this.commonSlingBootstrap, 0);
        final String addCmds = (standalone ? this.slingStandaloneBootstrapCommand : this.slingWebappBootstrapCommand);
        if ( this.slingBootstrapCommand == null ) {
            return addCmds;
        }
        if ( addCmds != null ) {
            final StringBuilder builder = new StringBuilder(this.slingBootstrapCommand);
            builder.append('\n');
            builder.append(addCmds);
            return builder.toString();
        }
        return this.slingBootstrapCommand;
    }
}
