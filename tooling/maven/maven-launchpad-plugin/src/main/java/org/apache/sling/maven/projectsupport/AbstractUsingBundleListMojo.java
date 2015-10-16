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

import static org.apache.sling.maven.projectsupport.BundleListUtils.interpolateProperties;
import static org.apache.sling.maven.projectsupport.BundleListUtils.readBundleList;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.PropertyUtils;
import org.apache.sling.maven.projectsupport.BundleListUtils.ArtifactDefinitionsCallback;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.FileUtils;
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
     */
    @Parameter
    protected ArtifactDefinition defaultBundleList;

    /**
     * Any additional bundles to include in the project's bundles directory.
     */
    @Parameter
    private ArtifactDefinition[] additionalBundles;

    private BundleList initializedBundleList;

    /**
     * Bundles which should be removed from the project's bundles directory.
     */
    @Parameter
    private ArtifactDefinition[] bundleExclusions;

    /**
     * If true, include the default bundles.
     */
    @Parameter( property = "includeDefaultBundles", defaultValue = "true")
    private boolean includeDefaultBundles;

    @Parameter
    private File[] rewriteRuleFiles;

    /**
     * The list of tokens to include when copying configs
     * from partial bundle lists.
     */
    @Parameter( defaultValue = "**")
    private String[] configIncludes;

    /**
     * The list of tokens to exclude when copying the configs
     * from partial bundle lists.
     */
    @Parameter
    private String[] configExcludes;

    /**
     * The list of names to exclude when copying properties
     * from partial bundle lists.
     */
    @Parameter
    private String[] propertiesExcludes;

    @Component
    protected MavenFileFilter mavenFileFilter;

    /**
     * The zip unarchiver.
     */
    @Component(role = UnArchiver.class, hint = "zip")
    private ZipUnArchiver zipUnarchiver;

    private Properties slingProperties;

    private Properties slingWebappProperties;

    private Properties slingStandaloneProperties;

    private String slingBootstrapCommand;

    private String slingWebappBootstrapCommand;

    private String slingStandaloneBootstrapCommand;

    @Parameter(defaultValue = "${project.build.directory}/tmpBundleListconfig")
    private File tmpOutputDir;

    @Parameter(defaultValue = "${project.build.directory}/tmpConfigDir")
    private File tempConfigDir;

    private File overlayConfigDir;

    public final void execute() throws MojoFailureException, MojoExecutionException {
        try {
            initBundleList();
            extractConfigurations();
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

    protected BundleList getInitializedBundleList() {
        return initializedBundleList;
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

    /**
     * Initialize the artifact definitions using defaults inside the plugin JAR.
     *
     * @throws IOException if the default properties can't be read
     * @throws XmlPullParserException
     * @throws MojoExecutionException
     */
    private final void initArtifactDefinitions() throws IOException {
        BundleListUtils.initArtifactDefinitions(getClass().getClassLoader(), new ArtifactDefinitionsCallback() {

            public void initArtifactDefinitions(Properties dependencies) {
                if (defaultBundleList == null) {
                    defaultBundleList = new ArtifactDefinition();
                }
                defaultBundleList.initDefaults(dependencies.getProperty("defaultBundleList"));

                AbstractUsingBundleListMojo.this.initArtifactDefinitions(dependencies);
            }
        });
    }

    private final void initBundleList() throws IOException, XmlPullParserException, MojoExecutionException {
        initArtifactDefinitions();
        if (BundleListUtils.isCurrentArtifact(project, defaultBundleList)) {
            initializedBundleList = readBundleList(bundleListFile);
        } else {
            initializedBundleList = new BundleList();
            if (includeDefaultBundles) {
                Artifact defBndListArtifact = getArtifact(defaultBundleList.getGroupId(),
                        defaultBundleList.getArtifactId(), defaultBundleList.getVersion(), defaultBundleList.getType(),
                        defaultBundleList.getClassifier());
                getLog().info("Using bundle list file from " + defBndListArtifact.getFile().getAbsolutePath());
                initializedBundleList = readBundleList(defBndListArtifact.getFile());
            }

            if (bundleListFile.exists()) {
                initializedBundleList.merge(readBundleList(bundleListFile));
            }
        }
        // add additional bundles
        if (additionalBundles != null) {
            for (ArtifactDefinition def : additionalBundles) {
                initializedBundleList.add(def.toBundleList());
            }
        }

        interpolateProperties(initializedBundleList, project, mavenSession);

        // check for partial bundle lists
        final Set<Artifact> dependencies = project.getDependencyArtifacts();
        for (Artifact artifact : dependencies) {
            if (PARTIAL.equals(artifact.getType())) {
                getLog().info(
                        String.format("Merging partial bundle list %s:%s:%s", artifact.getGroupId(),
                                artifact.getArtifactId(), artifact.getVersion()));
                initializedBundleList.merge(readBundleList(artifact.getFile()));
            }
        }

        // handle exclusions
        if (bundleExclusions != null) {
            for (ArtifactDefinition def : bundleExclusions) {
                initializedBundleList.remove(def.toBundleList(), false);
            }
        }

        initBundleList(initializedBundleList);

        interpolateProperties(initializedBundleList, project, mavenSession);

        rewriteBundleList(initializedBundleList);
    }

    private final void extractConfigurations() throws MojoExecutionException, IOException {
        final Set<Artifact> dependencies = project.getDependencyArtifacts();
        for (Artifact artifact : dependencies) {
            if (PARTIAL.equals(artifact.getType())) {
                extractConfiguration(artifact);
            }
        }
        // copy own config files
        if ( this.overlayConfigDir != null && super.getConfigDirectory().exists() ) {
            copyDirectory(super.getConfigDirectory(), this.overlayConfigDir, null, FileUtils.getDefaultExcludes());
        }
    }

    private void extractConfiguration(final Artifact artifact) throws MojoExecutionException, IOException {
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
                    String.format("Merging settings from partial bundle list %s:%s:%s", cfgArtifact.getGroupId(),
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
                final File configDir = new File(this.tmpOutputDir, "config");
                if ( configDir.exists() ) {
                    if ( this.overlayConfigDir == null ) {
                        this.tempConfigDir.mkdirs();
                        this.overlayConfigDir = this.tempConfigDir;
                    }
                    final String[] defaultExcludes = FileUtils.getDefaultExcludes();
                    String[] excludes;
                    if ( this.configExcludes != null ) {
                        excludes = new String[defaultExcludes.length + this.configExcludes.length];
                        System.arraycopy(defaultExcludes, 0, excludes, 0, defaultExcludes.length);
                        System.arraycopy(this.configExcludes, 0, excludes, defaultExcludes.length, this.configExcludes.length);
                    } else {
                        excludes = defaultExcludes;
                    }
                    String[] includes = null;
                    if ( this.configIncludes != null ) {
                        includes = this.configIncludes;
                    }
                    copyDirectory(configDir, this.overlayConfigDir,
                                    includes, excludes);
                }
            } catch (final ArchiverException ae) {
                throw new MojoExecutionException("Unable to extract configuration archive.",ae);
            } finally {
                // and delete at the end
                FileUtils.deleteDirectory(this.tmpOutputDir);
            }
        }
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
                mavenFileFilter.copyFile(propsFile, tmp, true, project, Collections.EMPTY_LIST, true,
                        System.getProperty("file.encoding"), mavenSession);
                final Properties loadedProps = PropertyUtils.loadPropertyFile(tmp, null);
                if ( mode == 0 ) {
                    if ( this.slingProperties == null ) {
                        this.slingProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingProperties);
                    }
                    filterProperties(this.slingProperties);
                } else if ( mode == 1 ) {
                    if ( this.slingWebappProperties == null ) {
                        this.slingWebappProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingWebappProperties);
                    }
                    filterProperties(this.slingWebappProperties);
                } else {
                    if ( this.slingStandaloneProperties == null ) {
                        this.slingStandaloneProperties = loadedProps;
                    } else {
                        this.copyProperties(loadedProps, this.slingStandaloneProperties);
                    }
                    filterProperties(this.slingStandaloneProperties);
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

    /**
     * Filter properties by removing excluded properties
     */
    private void filterProperties(final Properties props) {
        if ( this.propertiesExcludes != null ) {
            for(final String name : this.propertiesExcludes) {
                props.remove(name.trim());
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
                mavenFileFilter.copyFile(bootstrapFile, tmp, true, project, Collections.EMPTY_LIST, true,
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
