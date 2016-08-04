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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.commons.osgi.BSNRenamer;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.FeatureTypes;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.io.ModelWriter;
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

    private static final String ALL_RUNMODES_KEY = "_all_";

    private static final String BASE_DESTINATION = "resources";

    private static final String BOOT_DIRECTORY = "bundles";

    private static final String ARTIFACTS_DIRECTORY = "install";

    private static final String CONFIG_DIRECTORY = "config";

    private static final String PROVISIONING_DIRECTORY = "provisioning";

    private static final String EMBEDDED_MODEL_FILENAME = "model.txt";

    private static final String BOOTSTRAP_FILE = "sling_bootstrap.txt";

    private static final String PROPERTIES_FILE = "sling_install.properties";

    /**
     * To look up Archiver/UnArchiver implementations
     */
    @Component
    private ArchiverManager archiverManager;

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
        final Model model = ProjectHelper.getEffectiveModel(this.project, getResolverOptions());

        execute(model);
    }

    void execute(final Model model) throws MojoExecutionException {
        this.prepareGlobal(model);
        this.prepareStandaloneApp(model);
        this.prepareWebapp(model);
    }

    /**
     * Prepare the global map for the artifacts.
     */
    private void prepareGlobal(final Model model) throws MojoExecutionException {
        final Map<String, File> globalContentsMap = new HashMap<String, File>();
        this.buildContentsMap(model, (String)null, globalContentsMap);

        this.project.setContextValue(BuildConstants.CONTEXT_GLOBAL, globalContentsMap);
    }

    /**
     * Prepare the standalone application.
     */
    private void prepareStandaloneApp(final Model model) throws MojoExecutionException {
        final Map<String, File> contentsMap = new HashMap<String, File>();
        this.project.setContextValue(BuildConstants.CONTEXT_STANDALONE, contentsMap);

        // unpack base artifact and create settings
        final File outputDir = getStandaloneOutputDirectory();
        unpackBaseArtifact(model, outputDir, ModelConstants.RUN_MODE_STANDALONE);
        this.buildSettings(model, ModelConstants.RUN_MODE_STANDALONE, outputDir);
        this.buildBootstrapFile(model, ModelConstants.RUN_MODE_STANDALONE, outputDir);
        this.embedModel(model, outputDir);

        this.buildContentsMap(model, ModelConstants.RUN_MODE_STANDALONE, contentsMap);
    }

    /** Embed our model in the created jar file */
    private void embedModel(Model model, File outputDir) throws MojoExecutionException {
        final File modelDir = new File(new File(outputDir, BASE_DESTINATION), PROVISIONING_DIRECTORY);
        modelDir.mkdirs();
        final File modelFile = new File(modelDir, EMBEDDED_MODEL_FILENAME);
        try {
            final FileWriter w = new FileWriter(modelFile);
            try {
                w.write("# Aggregated provisioning model embedded by " + getClass().getName() + "\n");
                ModelWriter.write(w, model);
            } finally {
                w.flush();
                w.close();
            }
        } catch(IOException ioe) {
            throw new MojoExecutionException("Failed to create model file " + modelFile.getAbsolutePath(), ioe);
        }
    }

    /**
     * Prepare the web application.
     */
    private void prepareWebapp(final Model model) throws MojoExecutionException {
        if ( this.createWebapp ) {
            final Map<String, File> contentsMap = new HashMap<String, File>();
            this.project.setContextValue(BuildConstants.CONTEXT_WEBAPP, contentsMap);

            // unpack base artifact and create settings
            final File outputDir = new File(this.project.getBuild().getDirectory(), BuildConstants.WEBAPP_OUTDIR);
            final File webappDir = new File(outputDir, "WEB-INF");
            unpackBaseArtifact(model, outputDir, ModelConstants.RUN_MODE_WEBAPP);

            // check for web.xml
            final Feature webappF = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
            if ( webappF != null ) {
                final RunMode webappRM = webappF.getRunMode(null);
                if ( webappRM != null ) {
                    final Configuration webConfig = webappRM.getConfiguration(ModelConstants.CFG_LAUNCHPAD_WEB_XML);
                    if ( webConfig != null ) {
                        final File webXML = new File(webappDir, "web.xml");
                        try {
                            FileUtils.fileWrite(webXML, webConfig.getProperties().get(ModelConstants.CFG_LAUNCHPAD_WEB_XML).toString());
                        } catch (final IOException e) {
                            throw new MojoExecutionException("Unable to write configuration to " + webXML, e);
                        }
                    }
                }
            }
            this.buildSettings(model, ModelConstants.RUN_MODE_WEBAPP, webappDir);
            this.buildBootstrapFile(model, ModelConstants.RUN_MODE_WEBAPP, webappDir);
            this.embedModel(model, webappDir);

            this.buildContentsMap(model, ModelConstants.RUN_MODE_WEBAPP, contentsMap);
        }
    }

    /**
     * Build a list of all artifacts.
     */
    private void buildContentsMap(final Model model, final String packageRunMode, final Map<String, File> contentsMap)
    throws MojoExecutionException {
        if ( packageRunMode == null ) {
            // add base jar
            final Artifact artifact = getBaseArtifact(model, null, BuildConstants.TYPE_JAR);
            contentsMap.put(BASE_DESTINATION + "/"+ artifact.getArtifactId() + "." + artifact.getArtifactHandler().getExtension(), artifact.getFile());
        }
        for(final Feature feature : model.getFeatures()) {
            if ( feature.isSpecial() && !feature.getName().equals(ModelConstants.FEATURE_BOOT)) {
                continue;
            } else if (FeatureTypes.SUBSYSTEM_APPLICATION.equals(feature.getType()) ||
                    FeatureTypes.SUBSYSTEM_COMPOSITE.equals(feature.getType()) ||
                    FeatureTypes.SUBSYSTEM_FEATURE.equals(feature.getType())) {
                buildSubsystemBase(contentsMap, feature);
            } else {
                for(final RunMode runMode : feature.getRunModes()) {
                    if ( packageRunMode == null ) {
                        if ( runMode.isSpecial() ) {
                            continue;
                        }
                        this.buildContentsMap(model, runMode, contentsMap, feature.getName().equals(ModelConstants.FEATURE_BOOT));
                    } else {
                        if ( runMode.isRunMode(packageRunMode) ) {
                            this.buildContentsMap(model, runMode, contentsMap, feature.getName().equals(ModelConstants.FEATURE_BOOT));
                        }
                    }
                }
            }
        }
    }

    private void buildSubsystemBase(final Map<String, File> contentsMap, final Feature feature) throws MojoExecutionException {
        AtomicInteger startLevelHolder = new AtomicInteger(); // Used as output argument
        File subsystemFile = createSubsystemBaseFile(feature, startLevelHolder);
        if (subsystemFile != null)
            contentsMap.put(getPathForArtifact(startLevelHolder.get(), subsystemFile.getName()), subsystemFile);
    }

    /**
     * Build a list of all artifacts from this run mode
     */
    private void buildContentsMap(final Model model, final RunMode runMode, final Map<String, File> contentsMap, final boolean isBoot)
    throws MojoExecutionException{
        for(final ArtifactGroup group : runMode.getArtifactGroups()) {
            for(final org.apache.sling.provisioning.model.Artifact a : group) {
                Artifact artifact = null;
                if ( a.getGroupId().equals(this.project.getGroupId())
                        && a.getArtifactId().equals(this.project.getArtifactId())
                        && a.getVersion().equals(this.project.getVersion()) ) {
                    for(final Artifact projectArtifact : this.project.getAttachedArtifacts()) {
                        if ( projectArtifact.getClassifier().equals(a.getClassifier()) ) {
                            artifact = projectArtifact;
                            break;
                        }
                    }
                    if ( artifact == null ) {
                        throw new MojoExecutionException("Unable to find artifact from same project: " + a.toMvnUrl());
                    }
                } else {
                    artifact = ModelUtils.getArtifact(this.project, this.mavenSession, this.artifactHandlerManager, this.resolver,
                        a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(), a.getClassifier());
                }
                File artifactFile = artifact.getFile();

                String newBSN = a.getMetadata().get("bundle:rename-bsn");
                if (newBSN != null) {
                    try {
                        getTmpDir().mkdirs();
                        artifactFile = new BSNRenamer(artifactFile, getTmpDir(), newBSN).process();
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable to rename bundle BSN to " + newBSN + " for " + artifactFile, e);
                    }
                }

                contentsMap.put(getPathForArtifact(group.getStartLevel(), artifactFile.getName(), runMode, isBoot), artifactFile);
            }
        }

        final File rootConfDir = new File(this.getTmpDir(), "global-config");
        boolean hasConfig = false;
        for(final Configuration config : runMode.getConfigurations()) {
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

    private File createSubsystemBaseFile(Feature feature, AtomicInteger startLevelHolder) throws MojoExecutionException {
        File subsystemFile = new File(getTmpDir(), feature.getName() + ".subsystem-base");
        if (subsystemFile.exists()) {
            // This subsystem has already been created
            // TODO is there a better way to avoid calling this multiple times?
            return null;
        }

        startLevelHolder.set(-1);

        // The runmodes information has to be the first item in the archive so that we always have it available when the
        // archive is being processed. For this reason a Jar file is used here as it's guaranteed to store the manifest
        // first.
        Manifest runModesManifest = getRunModesManifest(feature);

        getLog().info("Creating subsystem base file: " + subsystemFile.getName());
        subsystemFile.getParentFile().mkdirs();

        try (JarOutputStream os = new JarOutputStream(new FileOutputStream(subsystemFile), runModesManifest)) {
            Map<String, Integer> bsnStartOrderMap = new HashMap<>();

            for (RunMode rm : feature.getRunModes()) {
                for (ArtifactGroup ag : rm.getArtifactGroups()) {
                    int startOrder = ag.getStartLevel(); // For subsystems the start level on the artifact group is used as start order.

                    for (org.apache.sling.provisioning.model.Artifact a : ag) {
                        Artifact artifact = ModelUtils.getArtifact(this.project, this.mavenSession, this.artifactHandlerManager, this.resolver,
                                a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(), a.getClassifier());
                        File artifactFile = artifact.getFile();
                        String entryName = getEntryName(artifactFile, startOrder);

                        ZipEntry ze = new ZipEntry(entryName);
                        try {
                            os.putNextEntry(ze);
                            Files.copy(artifactFile.toPath(), os);
                        } finally {
                            os.closeEntry();
                        }
                    }
                }
            }

            int sl = createSubsystemManifest(feature, bsnStartOrderMap, os);
            if (sl != -1)
                startLevelHolder.set(sl);
            addReadme(os);
        } catch (IOException ioe) {
            throw new MojoExecutionException("Problem creating subsystem .esa file " + subsystemFile, ioe);
        }
        return subsystemFile;
    }

    private Manifest getRunModesManifest(Feature feature) throws MojoExecutionException {
        Map<String, StringBuilder> runModes = new HashMap<>();

        for (RunMode rm : feature.getRunModes()) {
            for (ArtifactGroup ag : rm.getArtifactGroups()) {
                int startOrder = ag.getStartLevel(); // For subsystems the start level on the artifact group is used as start order.

                for (org.apache.sling.provisioning.model.Artifact a : ag) {
                    Artifact artifact = ModelUtils.getArtifact(this.project, this.mavenSession, this.artifactHandlerManager, this.resolver,
                            a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(), a.getClassifier());
                    File artifactFile = artifact.getFile();
                    String entryName = getEntryName(artifactFile, startOrder);

                    String [] runModeNames = rm.getNames();
                    if (runModeNames == null)
                        runModeNames = new String[] {ALL_RUNMODES_KEY};

                    for (String runModeName : runModeNames) {
                        StringBuilder sb = runModes.get(runModeName);
                        if (sb == null) {
                            sb = new StringBuilder();
                            runModes.put(runModeName, sb);
                        } else {
                            sb.append('|');
                        }

                        sb.append(entryName);
                    }
                }
            }
        }

        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0"); // Manifest does not work without this value
        attrs.putValue("About-This-Manifest", "This is not a real manifest, it is used as information when this archive is transformed into a real subsystem .esa file");
        for (Map.Entry<String, StringBuilder> entry : runModes.entrySet()) {
            attrs.putValue(entry.getKey().replace(':', '_'), entry.getValue().toString());
        }
        return mf;
    }

    private String getEntryName(File artifactFile, int startOrder) {
        return "Potential_Bundles/" + startOrder + "/" + artifactFile.getName();
    }

    // This manifest will be used as the basis for the OSGI-INF/SUBSYSTEM.MF file when the real
    // .esa file is generated. However since some contents of that file depend on the actual
    // runmode that is being executed, additional information will be added to the SUBSYSTEM.MF
    // file at startup time before it's finalized (example: Subsystem-Content).
    private int createSubsystemManifest(Feature feature,
            Map<String, Integer> startOrderMap, ZipOutputStream os) throws IOException {
        int subsystemStartLevel = -1;
        ZipEntry ze = new ZipEntry("SUBSYSTEM-MANIFEST-BASE.MF");
        try {
            os.putNextEntry(ze);

            Manifest mf = new Manifest();
            Attributes attributes = mf.getMainAttributes();
            attributes.putValue("Manifest-Version", "1.0"); // Manifest does not work without this value
            attributes.putValue("Subsystem-SymbolicName", feature.getName());
            attributes.putValue("Subsystem-Version", "1"); // Version must be an integer (cannot be a long), TODO better idea?
            attributes.putValue("Subsystem-Type", feature.getType());
            for (Section section : feature.getAdditionalSections("subsystem-manifest")) {
                String sl = section.getAttributes().get("startLevel");
                try {
                    subsystemStartLevel = Integer.parseInt(sl);
                } catch (NumberFormatException nfe) {
                    // Not a valid start level
                }

                BufferedReader br = new BufferedReader(new StringReader(section.getContents()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        String key = line.substring(0, idx);
                        String value;
                        idx++;
                        if (line.length() > idx)
                            value = line.substring(idx);
                        else
                            value = "";
                        attributes.putValue(key.trim(), value.trim());
                    }
                }
            }
            mf.write(os);
        } finally {
            os.closeEntry();
        }

        return subsystemStartLevel;
    }

    private void addReadme(ZipOutputStream os) throws IOException {
        ZipEntry ze = new ZipEntry("readme.txt");
        try (InputStream is = getClass().getResourceAsStream("/subsystem-base/readme.txt")) {
            os.putNextEntry(ze);
            IOUtils.copy(is, os);
        } finally {
            os.closeEntry();
        }
    }

    /**
     * Build the settings for the given packaging run mode
     */
    private void buildSettings(final Model model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        final Properties settings = new Properties();
        final Feature launchpadFeature = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
        if ( launchpadFeature != null ) {
            final RunMode launchpadRunMode = launchpadFeature.getRunMode(null);
            if ( launchpadRunMode != null ) {
                for(final Map.Entry<String, String> entry : launchpadRunMode.getSettings()) {
                    settings.put(entry.getKey(), deescapeVariablePlaceholders(entry.getValue()));
                }
            }
        }
        final Feature bootFeature = model.getFeature(ModelConstants.FEATURE_BOOT);
        if ( bootFeature != null ) {
            final RunMode bootRunMode = bootFeature.getRunMode(null);
            if ( bootRunMode != null ) {
                for(final Map.Entry<String, String> entry : bootRunMode.getSettings()) {
                    settings.put(entry.getKey(), deescapeVariablePlaceholders(entry.getValue()));
                }
            }
        }
        for(final Feature f : model.getFeatures()) {
            final RunMode packageRM = f.getRunMode(new String[] {packageRunMode});
            if ( packageRM != null ) {
                for(final Map.Entry<String, String> entry : packageRM.getSettings()) {
                    settings.put(entry.getKey(), deescapeVariablePlaceholders(entry.getValue()));
                }
            }
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
    private void buildBootstrapFile(final Model model, final String packageRunMode, final File outputDir)
    throws MojoExecutionException {
        final StringBuilder sb = new StringBuilder();

        final Feature launchpadFeature = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
        if ( launchpadFeature != null ) {
            final RunMode launchpadRunMode = launchpadFeature.getRunMode(null);
            if ( launchpadRunMode != null ) {
                final Configuration c = launchpadRunMode.getConfiguration(ModelConstants.CFG_LAUNCHPAD_BOOTSTRAP);
                if ( c != null ) {
                    sb.append(c.getProperties().get(c.getPid()));
                    sb.append('\n');
                }
            }
            final RunMode packageRM = launchpadFeature.getRunMode(new String[] {packageRunMode});
            if ( packageRM != null ) {
                final Configuration c = packageRM.getConfiguration(ModelConstants.CFG_LAUNCHPAD_BOOTSTRAP);
                if ( c != null ) {
                    sb.append(c.getProperties().get(c.getPid()));
                    sb.append('\n');
                }
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
    private Artifact getBaseArtifact(final Model model, final String classifier, final String type) throws MojoExecutionException {
        try {
            final org.apache.sling.provisioning.model.Artifact baseArtifact = ModelUtils.findBaseArtifact(model);

            final Artifact a = ModelUtils.getArtifact(this.project,  this.mavenSession, this.artifactHandlerManager, this.resolver,
                    baseArtifact.getGroupId(),
                    baseArtifact.getArtifactId(),
                    baseArtifact.getVersion(),
                    type,
                    classifier);
            if (a == null) {
                throw new MojoExecutionException(
                        String.format("Project doesn't have a base dependency of groupId %s and artifactId %s",
                                baseArtifact.getGroupId(), baseArtifact.getArtifactId()));
            }
            return a;
        } catch ( final MavenExecutionException mee) {
            throw new MojoExecutionException(mee.getMessage(), mee.getCause());
        }
    }

    /**
     * Unpack the base artifact
     */
    private void unpackBaseArtifact(final Model model, final File outputDirectory, final String packageRunMode)
     throws MojoExecutionException {
        final String classifier;
        final String type;
        if ( ModelConstants.RUN_MODE_STANDALONE.equals(packageRunMode) ) {
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

    private String getPathForArtifact(final int startLevel, final String artifactName) {
        return getPathForArtifact(startLevel, artifactName, null, false);
    }

    /**
     * Get the relative path for an artifact.
     */
    private String getPathForArtifact(final int startLevel, final String artifactName, final RunMode rm, final boolean isBoot) {
        final Set<String> runModesList = new TreeSet<String>();
        if ( rm != null && rm.getNames() != null ) {
            for(final String mode : rm.getNames()) {
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

        if ( isBoot ) {
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
    private String getPathForConfiguration(final Configuration config, final RunMode rm) {
        final Set<String> runModesList = new TreeSet<String>();
        if (rm.getNames() != null ) {
            for(final String mode : rm.getNames()) {
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
        return String.format("%s/%s%s/%s%s.config", BASE_DESTINATION, CONFIG_DIRECTORY,
                runModeExt,
                mainName,
                alias);
    }

    /**
     * Replace \${var} with ${var}
     * @param text String with escaped variables
     * @return String with deescaped variables
     */
    private String deescapeVariablePlaceholders(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\\\\\$", "\\$");
    }

}