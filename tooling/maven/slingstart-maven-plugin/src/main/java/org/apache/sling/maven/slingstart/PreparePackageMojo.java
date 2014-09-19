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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.apache.sling.slingstart.model.SSMRunMode;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.apache.sling.slingstart.model.SSMSubsystem;
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
public class PreparePackageMojo extends AbstractSubsystemMojo {

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
        final SSMSubsystem model = this.readModel();

        this.prepareGlobal(model);
        this.prepareStandaloneApp(model);
        this.prepareWebapp(model);
    }

    /**
     * Prepare the global map for the artifacts.
     */
    private void prepareGlobal(final SSMSubsystem model) throws MojoExecutionException {
        final Map<String, File> globalContentsMap = new HashMap<String, File>();
        this.buildContentsMap(model, (String)null, globalContentsMap);

        this.project.setContextValue(BuildConstants.CONTEXT_GLOBAL, globalContentsMap);
    }

    /**
     * Prepare the standalone application.
     */
    private void prepareStandaloneApp(final SSMSubsystem model) throws MojoExecutionException {
        final Map<String, File> contentsMap = new HashMap<String, File>();
        this.project.setContextValue(BuildConstants.CONTEXT_STANDALONE, contentsMap);

        // unpack base artifact and create settings
        final File outputDir = new File(this.project.getBuild().getOutputDirectory());
        unpackBaseArtifact(model, outputDir, SSMRunMode.RUN_MODE_STANDALONE);
        this.buildSettings(model, SSMRunMode.RUN_MODE_STANDALONE, outputDir);
        this.buildBootstrapFile(model, SSMRunMode.RUN_MODE_STANDALONE, outputDir);

        this.buildContentsMap(model, SSMRunMode.RUN_MODE_STANDALONE, contentsMap);
    }

    /**
     * Prepare the web application.
     */
    private void prepareWebapp(final SSMSubsystem model) throws MojoExecutionException {
        if ( this.createWebapp ) {
            final Map<String, File> contentsMap = new HashMap<String, File>();
            this.project.setContextValue(BuildConstants.CONTEXT_WEBAPP, contentsMap);

            // unpack base artifact and create settings
            final File outputDir = new File(this.project.getBuild().getDirectory(), BuildConstants.WEBAPP_OUTDIR);
            final File webappDir = new File(outputDir, "WEB-INF");
            unpackBaseArtifact(model, outputDir, SSMRunMode.RUN_MODE_WEBAPP);

            // check for web.xml
            final SSMRunMode webappRM = model.getRunMode(SSMRunMode.RUN_MODE_WEBAPP);
            if ( webappRM != null ) {
                final SSMConfiguration webConfig = webappRM.getConfiguration(SSMConstants.CFG_WEB_XML);
                if ( webConfig != null ) {
                    final File webXML = new File(webappDir, "web.xml");
                    try {
                        FileUtils.fileWrite(webXML, webConfig.properties);
                    } catch (final IOException e) {
                        throw new MojoExecutionException("Unable to write configuration to " + webXML, e);
                    }
                }
            }
            this.buildSettings(model, SSMRunMode.RUN_MODE_WEBAPP, webappDir);
            this.buildBootstrapFile(model, SSMRunMode.RUN_MODE_WEBAPP, outputDir);

            this.buildContentsMap(model, SSMRunMode.RUN_MODE_WEBAPP, contentsMap);
        }
    }

    /**
     * Build a list of all artifacts.
     */
    private void buildContentsMap(final SSMSubsystem model, final String packageRunMode, final Map<String, File> contentsMap)
    throws MojoExecutionException {
        if ( packageRunMode == null ) {
            // add base jar
            final Artifact artifact = getBaseArtifact(model, null, BuildConstants.TYPE_JAR);
            contentsMap.put(BASE_DESTINATION + "/"+ artifact.getArtifactId() + "." + artifact.getArtifactHandler().getExtension(), artifact.getFile());
        }
        for(final SSMRunMode runMode : model.runModes) {
            if ( packageRunMode == null ) {
                if ( runMode.isSpecial()
                     && !runMode.isRunMode(SSMRunMode.RUN_MODE_BOOT)) {
                    continue;
                }
                this.buildContentsMap(model, runMode, contentsMap);
            } else {
                if ( runMode.isRunMode(packageRunMode) ) {
                    this.buildContentsMap(model, runMode, contentsMap);
                }
            }
        }
    }

    /**
     * Build a list of all artifacts from this run mode
     */
    private void buildContentsMap(final SSMSubsystem model, final SSMRunMode runMode, final Map<String, File> contentsMap)
    throws MojoExecutionException{
        for(final SSMStartLevel sl : runMode.startLevels) {
            for(final SSMArtifact a : sl.artifacts) {
                final Artifact artifact = SubsystemUtils.getArtifact(this.project, a.groupId, a.artifactId, model.getValue(a.version), a.type, a.classifier);
                final File artifactFile = artifact.getFile();
                contentsMap.put(getPathForArtifact(sl.level, artifactFile.getName(), runMode), artifactFile);
            }
        }

        final File rootConfDir = new File(this.getTmpDir(), "global-config");
        boolean hasConfig = false;
        for(final SSMConfiguration config : runMode.configurations) {
            // skip special configurations
            if ( config.isSpecial() ) {
                continue;
            }
            final String configPath = getPathForConfiguration(config, runMode);
            final File configFile = new File(rootConfDir, configPath);
            getLog().debug(String.format("Creating configuration at %s", configFile.getPath()));
            configFile.getParentFile().mkdirs();
            try {
                FileUtils.fileWrite(configFile, config.properties);
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
    private void buildSettings(final SSMSubsystem model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        String settings = null;
        final SSMRunMode baseRM = model.getRunMode(SSMRunMode.RUN_MODE_BASE);
        if ( baseRM != null && baseRM.settings != null ) {
            settings = baseRM.settings.properties + "\n";
        } else {
            settings = "";
        }
        final SSMRunMode bootRM = model.getRunMode(SSMRunMode.RUN_MODE_BOOT);
        if ( bootRM != null && bootRM.settings != null ) {
            settings = settings + bootRM.settings.properties + "\n";
        }
        final SSMRunMode packageRM = model.getRunMode(packageRunMode);
        if ( packageRM != null && packageRM.settings != null ) {
            settings = settings + packageRM.settings.properties;
        }

        if ( settings != null ) {
            final File settingsFile = new File(outputDir, PROPERTIES_FILE);
            getLog().debug(String.format("Creating settings at %s", settingsFile.getPath()));
            try {
                FileUtils.fileWrite(settingsFile, settings);
            } catch ( final IOException ioe ) {
                throw new MojoExecutionException("Unable to write properties file.", ioe);
            }
        }
    }

    /**
     * Build the bootstrap file for the given packaging run mode
     */
    private void buildBootstrapFile(final SSMSubsystem model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        String bootstrapTxt = "";
        final SSMRunMode baseRM = model.getRunMode(SSMRunMode.RUN_MODE_BASE);
        if ( baseRM != null ) {
            final SSMConfiguration c = baseRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                bootstrapTxt = c.properties + "\n";
            }
        }
        final SSMRunMode bootRM = model.getRunMode(SSMRunMode.RUN_MODE_BOOT);
        if ( bootRM != null ) {
            final SSMConfiguration c = bootRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                bootstrapTxt = bootstrapTxt + c.properties;
            }
        }
        final SSMRunMode packageRM = model.getRunMode(packageRunMode);
        if ( packageRM != null ) {
            final SSMConfiguration c = packageRM.getConfiguration(SSMConstants.CFG_BOOTSTRAP);
            if ( c != null ) {
                bootstrapTxt = bootstrapTxt + c.properties;
            }
        }

        if ( bootstrapTxt != null ) {
            final File file = new File(outputDir, BOOTSTRAP_FILE);
            getLog().debug(String.format("Creating bootstrap file at %s", file.getPath()));
            try {
                FileUtils.fileWrite(file, bootstrapTxt);
            } catch ( final IOException ioe ) {
                throw new MojoExecutionException("Unable to write bootstrap file.", ioe);
            }
        }
    }

    /**
     * Return the base artifact
     */
    private Artifact getBaseArtifact(final SSMSubsystem model, final String classifier, final String type) throws MojoExecutionException {
        final SSMArtifact baseArtifact = SubsystemUtils.getBaseArtifact(model);

        final Artifact a = SubsystemUtils.getArtifact(this.project, baseArtifact.groupId,
                baseArtifact.artifactId,
                model.getValue(baseArtifact.version),
                type,
                classifier);
        if (a == null) {
            throw new MojoExecutionException(
                    String.format("Project doesn't have a base dependency of groupId %s and artifactId %s",
                            baseArtifact.groupId, baseArtifact.artifactId));
        }
        return a;
    }

    /**
     * Unpack the base artifact
     */
    private void unpackBaseArtifact(final SSMSubsystem model, final File outputDirectory, final String packageRunMode)
     throws MojoExecutionException {
        final String classifier;
        final String type;
        if ( SSMRunMode.RUN_MODE_STANDALONE.equals(packageRunMode) ) {
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
    private String getPathForArtifact(final int startLevel, final String artifactName, final SSMRunMode rm) {
        final Set<String> runModesList = new TreeSet<String>();
        if (rm.runModes != null ) {
            for(final String mode : rm.runModes) {
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

        if ( rm.isRunMode(SSMRunMode.RUN_MODE_BOOT) ) {
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
    private String getPathForConfiguration(final SSMConfiguration config, final SSMRunMode rm) {
        final Set<String> runModesList = new TreeSet<String>();
        if (rm.runModes != null ) {
            for(final String mode : rm.runModes) {
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

        final String mainName = (config.factoryPid != null ? config.factoryPid : config.pid);
        final String alias = (config.factoryPid != null ? "-" + config.pid : "");
        return String.format("%s/%s%s/%s%s.cfg", BASE_DESTINATION, CONFIG_DIRECTORY,
                runModeExt,
                mainName,
                alias);
    }
}
