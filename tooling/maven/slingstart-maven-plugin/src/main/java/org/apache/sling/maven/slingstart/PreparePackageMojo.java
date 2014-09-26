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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMConstants;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Prepare the sling start applications.
 */
@Mojo(
        name = "prepare-package",
        defaultPhase = LifecyclePhase.PROCESS_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class PreparePackageMojo extends AbstractSlingStartMojo {

    private static final String BASE_DESTINATION = "resources";

    private static final String BOOT_DIRECTORY = "bundles";

    private static final String ARTIFACTS_DIRECTORY = "install";

    private static final String CONFIG_DIRECTORY = "config";

    private static final String BOOTSTRAP_FILE = "sling_bootstrap.txt";

    private static final String PROPERTIES_FILE = "sling_install.properties";

    /**
     * To look up Archiver/UnArchiver implementations
     */
    @Component
    private ArchiverManager archiverManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final SSMDeliverable model = this.readModel();

        this.prepareGlobal(model);
        this.prepareStandaloneApp(model);
        this.prepareWebapp(model);
    }

    /**
     * Prepare the global map for the artifacts.
     */
    private void prepareGlobal(final SSMDeliverable model) throws MojoExecutionException {
        final Map<String, File> globalContentsMap = new HashMap<String, File>();
        this.buildContentsMap(model, (String)null, globalContentsMap);

        this.project.setContextValue(BuildConstants.CONTEXT_GLOBAL, globalContentsMap);
    }

    /**
     * Prepare the standalone application.
     */
    private void prepareStandaloneApp(final SSMDeliverable model) throws MojoExecutionException {
        final Map<String, File> contentsMap = new HashMap<String, File>();
        this.project.setContextValue(BuildConstants.CONTEXT_STANDALONE, contentsMap);

        // unpack base artifact and create settings
        final File outputDir = new File(this.project.getBuild().getOutputDirectory());
        unpackBaseArtifact(model, outputDir, SSMConstants.RUN_MODE_STANDALONE);
        this.buildSettings(model, SSMConstants.RUN_MODE_STANDALONE, outputDir);
        this.buildBootstrapFile(model, SSMConstants.RUN_MODE_STANDALONE, outputDir);

        this.buildContentsMap(model, SSMConstants.RUN_MODE_STANDALONE, contentsMap);
    }

    /**
     * Prepare the web application.
     */
    private void prepareWebapp(final SSMDeliverable model) throws MojoExecutionException {
        if ( this.createWebapp ) {
            final Map<String, File> contentsMap = new HashMap<String, File>();
            this.project.setContextValue(BuildConstants.CONTEXT_WEBAPP, contentsMap);

            // unpack base artifact and create settings
            final File outputDir = new File(this.project.getBuild().getDirectory(), BuildConstants.WEBAPP_OUTDIR);
            final File webappDir = new File(outputDir, "WEB-INF");
            unpackBaseArtifact(model, outputDir, SSMConstants.RUN_MODE_WEBAPP);

            // check for web.xml
            final SSMFeature webappRM = model.getRunMode(SSMConstants.RUN_MODE_WEBAPP);
            if ( webappRM != null ) {
                final SSMConfiguration webConfig = webappRM.getConfiguration(SSMConstants.CFG_WEB_XML);
                if ( webConfig != null ) {
                    final File webXML = new File(webappDir, "web.xml");
                    try {
                        FileUtils.fileWrite(webXML, webConfig.getProperties().get(SSMConstants.CFG_WEB_XML).toString());
                    } catch (final IOException e) {
                        throw new MojoExecutionException("Unable to write configuration to " + webXML, e);
                    }
                }
            }
            this.buildSettings(model, SSMConstants.RUN_MODE_WEBAPP, webappDir);
            this.buildBootstrapFile(model, SSMConstants.RUN_MODE_WEBAPP, outputDir);

            this.buildContentsMap(model, SSMConstants.RUN_MODE_WEBAPP, contentsMap);
        }
    }

    /**
     * Build a list of all artifacts.
     */
    private void buildContentsMap(final SSMDeliverable model, final String packageRunMode, final Map<String, File> contentsMap)
    throws MojoExecutionException {
        if ( packageRunMode == null ) {
            // add base jar
            final Artifact artifact = getBaseArtifact(model, null, BuildConstants.TYPE_JAR);
            contentsMap.put(BASE_DESTINATION + "/"+ artifact.getArtifactId() + "." + artifact.getArtifactHandler().getExtension(), artifact.getFile());
        }
        for(final SSMFeature feature : model.getFeatures()) {
            if ( packageRunMode == null ) {
                if ( feature.isSpecial()
                     && !feature.isRunMode(SSMConstants.RUN_MODE_BOOT)) {
                    continue;
                }
                this.buildContentsMap(model, feature, contentsMap);
            } else {
                if ( feature.isRunMode(packageRunMode) ) {
                    this.buildContentsMap(model, feature, contentsMap);
                }
            }
        }
    }

    /**
     * Build a list of all artifacts from this run mode
     */
    private void buildContentsMap(final SSMDeliverable model, final SSMFeature runMode, final Map<String, File> contentsMap)
    throws MojoExecutionException{
        for(final SSMStartLevel sl : runMode.getStartLevels()) {
            for(final SSMArtifact a : sl.getArtifacts()) {
                final Artifact artifact = ModelUtils.getArtifact(this.project, a.getGroupId(), a.getArtifactId(), model.getValue(a.getVersion()), a.getType(), a.getClassifier());
                final File artifactFile = artifact.getFile();
                contentsMap.put(getPathForArtifact(sl.getLevel(), artifactFile.getName(), runMode), artifactFile);
            }
        }

        final File rootConfDir = new File(this.getTmpDir(), "global-config");
        boolean hasConfig = false;
        for(final SSMConfiguration config : runMode.getConfigurations()) {
            // skip special configurations
            if ( config.isSpecial() ) {
                continue;
            }
            final String configPath = getPathForConfiguration(config, runMode);
            final File configFile = new File(rootConfDir, configPath);
            getLog().debug(String.format("Creating configuration at %s", configFile.getPath()));
            configFile.getParentFile().mkdirs();
            try {
                final FileOutputStream os = new FileOutputStream(configFile);
                try {
                    ConfigurationHandler.write(os, config.getProperties());
                } finally {
                    os.close();
                }
            } catch (final IOException e) {
                throw new MojoExecutionException("Unable to write configuration to " + configFile, e);
            }
            hasConfig = true;
        }
        if ( hasConfig ) {
            contentsMap.put(BASE_DESTINATION, rootConfDir);
        }
    }

    /**
     * Build the settings for the given packaging run mode
     */
    private void buildSettings(final SSMDeliverable model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        final Properties settings = new Properties();
        final SSMFeature baseRM = model.getRunMode(SSMConstants.RUN_MODE_BASE);
        if ( baseRM != null ) {
            settings.putAll(baseRM.getSettings());
        }
        final SSMFeature bootRM = model.getRunMode(SSMConstants.RUN_MODE_BOOT);
        if ( bootRM != null ) {
            settings.putAll(bootRM.getSettings());
        }
        final SSMFeature packageRM = model.getRunMode(packageRunMode);
        if ( packageRM != null ) {
            settings.putAll(packageRM.getSettings());
        }

        if ( settings.size() > 0 ) {
            final File settingsFile = new File(outputDir, PROPERTIES_FILE);
            getLog().debug(String.format("Creating settings at %s", settingsFile.getPath()));
            FileWriter writer = null;
            try {
                writer = new FileWriter(settingsFile);
                settings.store(writer, null);
            } catch ( final IOException ioe ) {
                throw new MojoExecutionException("Unable to write properties file.", ioe);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }
    }

    /**
     * Build the bootstrap file for the given packaging run mode
     */
    private void buildBootstrapFile(final SSMDeliverable model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        final StringBuilder sb = new StringBuilder();
        final SSMFeature baseRM = model.getRunMode(SSMConstants.RUN_MODE_BASE);
        if ( baseRM != null ) {
            final SSMConfiguration c = baseRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                sb.append(c.getProperties().get(c.getPid()));
                sb.append('\n');
            }
        }
        final SSMFeature bootRM = model.getRunMode(SSMConstants.RUN_MODE_BOOT);
        if ( bootRM != null ) {
            final SSMConfiguration c = bootRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                sb.append(c.getProperties().get(c.getPid()));
                sb.append('\n');
            }
        }
        final SSMFeature packageRM = model.getRunMode(packageRunMode);
        if ( packageRM != null ) {
            final SSMConfiguration c = packageRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                sb.append(c.getProperties().get(c.getPid()));
                sb.append('\n');
            }
        }

        if ( sb.length() > 0 ) {
            final File file = new File(outputDir, BOOTSTRAP_FILE);
            getLog().debug(String.format("Creating bootstrap file at %s", file.getPath()));
            try {
                FileUtils.fileWrite(file, sb.toString());
            } catch ( final IOException ioe ) {
                throw new MojoExecutionException("Unable to write bootstrap file.", ioe);
            }
        }
    }

    /**
     * Return the base artifact
     */
    private Artifact getBaseArtifact(final SSMDeliverable model, final String classifier, final String type) throws MojoExecutionException {
        final SSMArtifact baseArtifact = ModelUtils.getBaseArtifact(model);

        final Artifact a = ModelUtils.getArtifact(this.project, baseArtifact.getGroupId(),
                baseArtifact.getArtifactId(),
                model.getValue(baseArtifact.getVersion()),
                type,
                classifier);
        if (a == null) {
            throw new MojoExecutionException(
                    String.format("Project doesn't have a base dependency of groupId %s and artifactId %s",
                            baseArtifact.getGroupId(), baseArtifact.getArtifactId()));
        }
        return a;
    }

    /**
     * Unpack the base artifact
     */
    private void unpackBaseArtifact(final SSMDeliverable model, final File outputDirectory, final String packageRunMode)
     throws MojoExecutionException {
        final String classifier;
        final String type;
        if ( SSMConstants.RUN_MODE_STANDALONE.equals(packageRunMode) ) {
            classifier = BuildConstants.CLASSIFIER_APP;
            type = BuildConstants.TYPE_JAR;
        } else {
            classifier = BuildConstants.CLASSIFIER_WEBAPP;
            type = BuildConstants.TYPE_WAR;
        }
        final Artifact artifact = this.getBaseArtifact(model, classifier, type);
        unpack(artifact.getFile(), outputDirectory);
    }

    /**
     * Unpack a file
     */
    private void unpack(final File source, final File destination)
    throws MojoExecutionException {
        getLog().debug("Unpacking " + source.getPath() + " to\n  " + destination.getPath());
        try {
            destination.mkdirs();

            final UnArchiver unArchiver = archiverManager.getUnArchiver(source);

            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(destination);

            unArchiver.extract();
        } catch (final NoSuchArchiverException e) {
            throw new MojoExecutionException("Unable to find archiver for " + source.getPath(), e);
        } catch (final ArchiverException e) {
            throw new MojoExecutionException("Unable to unpack " + source.getPath(), e);
        }
    }

    /**
     * Get the relative path for an artifact.
     */
    private String getPathForArtifact(final int startLevel, final String artifactName, final SSMFeature rm) {
        final Set<String> runModesList = new TreeSet<String>();
        if (rm.getRunModes() != null ) {
            for(final String mode : rm.getRunModes()) {
                runModesList.add(mode);
            }
        }
        final String runModeExt;
        if ( runModesList.size() == 0 || rm.isSpecial() ) {
            runModeExt = "";
        } else {
            final StringBuilder sb = new StringBuilder();
            for(final String n : runModesList ) {
                sb.append('.');
                sb.append(n);
            }
            runModeExt = sb.toString();
        }

        if ( rm.isRunMode(SSMConstants.RUN_MODE_BOOT) ) {
            return String.format("%s/%s/1/%s", BASE_DESTINATION, BOOT_DIRECTORY,
                    artifactName);
        }
        return String.format("%s/%s%s/%s/%s", BASE_DESTINATION, ARTIFACTS_DIRECTORY,
                runModeExt,
                (startLevel == -1 ? 1 : startLevel),
                artifactName);
    }

    /**
     * Get the relative path for a configuration
     */
    private String getPathForConfiguration(final SSMConfiguration config, final SSMFeature rm) {
        final Set<String> runModesList = new TreeSet<String>();
        if (rm.getRunModes() != null ) {
            for(final String mode : rm.getRunModes()) {
                runModesList.add(mode);
            }
        }
        final String runModeExt;
        if ( runModesList.size() == 0 || rm.isSpecial() ) {
            runModeExt = "";
        } else {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(final String n : runModesList ) {
                if ( first ) {
                    sb.append('/');
                    first = false;
                } else {
                    sb.append('.');
                }
                sb.append(n);
            }
            runModeExt = sb.toString();
        }

        final String mainName = (config.getFactoryPid() != null ? config.getFactoryPid() : config.getPid());
        final String alias = (config.getFactoryPid() != null ? "-" + config.getPid() : "");
        return String.format("%s/%s%s/%s%s.cfg", BASE_DESTINATION, CONFIG_DIRECTORY,
                runModeExt,
                mainName,
                alias);
    }
}
