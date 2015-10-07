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

import static org.apache.sling.maven.projectsupport.BundleListUtils.interpolateProperties;
import static org.apache.sling.maven.projectsupport.BundleListUtils.readBundleList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Writer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Attaches the bundle list as a project artifact.
 */
@Mojo( name = "attach-partial-bundle-list", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class AttachPartialBundleListMojo extends AbstractBundleListMojo {

    public static final String CONFIG_CLASSIFIER = "bundlelistconfig";

    public static final String CONFIG_TYPE = "zip";

    public static final String CLASSIFIER = "bundlelist";

    public static final String TYPE = "xml";

    public static final String SLING_COMMON_PROPS = "common.properties";

    public static final String SLING_COMMON_BOOTSTRAP = "common.bootstrap.txt";

    public static final String SLING_WEBAPP_PROPS = "webapp.properties";

    public static final String SLING_WEBAPP_BOOTSTRAP = "webapp.bootstrap.txt";

    public static final String SLING_STANDALONE_PROPS = "standalone.properties";

    public static final String SLING_STANDALONE_BOOTSTRAP = "standalone.bootstrap.txt";

    @Parameter( defaultValue = "${project.build.directory}/bundleListconfig")
    private File configOutputDir;

    @Parameter( defaultValue = "${project.build.directory}/list.xml")
    private File bundleListOutput;

    /**
     * The zip archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="zip"
     */
    @Component(role = UnArchiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final BundleList initializedBundleList;
        if (bundleListFile.exists()) {
            try {
                initializedBundleList = readBundleList(bundleListFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to read bundle list file", e);
            } catch (XmlPullParserException e) {
                throw new MojoExecutionException("Unable to read bundle list file", e);
            }
        } else {
            throw new MojoFailureException(String.format("Bundle list file %s does not exist.", bundleListFile.getAbsolutePath()));
        }

        interpolateProperties(initializedBundleList, this.project, this.mavenSession);

        final BundleListXpp3Writer writer = new BundleListXpp3Writer();
        try {
            this.bundleListOutput.getParentFile().mkdirs();
            writer.write(new FileWriter(bundleListOutput), initializedBundleList);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write bundle list", e);
        }

        // if this project is a partial bundle list, it's the main artifact
        if ( project.getPackaging().equals(PARTIAL) ) {
            project.getArtifact().setFile(bundleListOutput);
        } else {
            // otherwise attach it as an additional artifact
            projectHelper.attachArtifact(project, TYPE, CLASSIFIER, bundleListOutput);
        }

        this.getLog().info("Attaching bundle list configuration");
        try {
            this.attachConfigurations();
        } catch (final IOException ioe) {
            throw new MojoExecutionException("Unable to attach configuration.", ioe);
        } catch (final ArchiverException ioe) {
            throw new MojoExecutionException("Unable to attach configuration.", ioe);
        }
    }

    private boolean checkFile(final File f) {
        return f != null && f.exists();
    }

    private void attachConfigurations() throws MojoExecutionException, IOException, ArchiverException {
        if ( this.ignoreBundleListConfig ) {
            this.getLog().debug("ignoreBundleListConfig is set to true, therefore not attaching configurations.");
            return;
        }
        // check if we have configurations
        boolean hasConfigs = this.checkFile(this.getConfigDirectory());
        hasConfigs |= this.checkFile(this.commonSlingBootstrap);
        hasConfigs |= this.checkFile(this.commonSlingProps);
        hasConfigs |= this.checkFile(this.webappSlingBootstrap);
        hasConfigs |= this.checkFile(this.webappSlingProps);
        hasConfigs |= this.checkFile(this.standaloneSlingBootstrap);
        hasConfigs |= this.checkFile(this.standaloneSlingProps);

        if ( !hasConfigs ) {
            this.getLog().debug("No configurations to attach.");
            return;
        }
        // copy configuration, as this project might use different names we have to copy everything!
        this.configOutputDir.mkdirs();
        if ( this.checkFile(this.commonSlingBootstrap) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.commonSlingBootstrap, new File(slingDir, SLING_COMMON_BOOTSTRAP));
        }
        if ( this.checkFile(this.commonSlingProps) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.commonSlingProps, new File(slingDir, SLING_COMMON_PROPS));
        }
        if ( this.checkFile(this.webappSlingBootstrap) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.webappSlingBootstrap, new File(slingDir, SLING_WEBAPP_BOOTSTRAP));
        }
        if ( this.checkFile(this.webappSlingProps) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.webappSlingProps, new File(slingDir, SLING_WEBAPP_PROPS));
        }
        if ( this.checkFile(this.standaloneSlingBootstrap) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.standaloneSlingBootstrap, new File(slingDir, SLING_STANDALONE_BOOTSTRAP));
        }
        if ( this.checkFile(this.standaloneSlingProps) ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.copyFile(this.standaloneSlingProps, new File(slingDir, SLING_STANDALONE_PROPS));
        }
        if ( this.checkFile(this.getConfigDirectory()) ) {
            final File configDir = new File(this.configOutputDir, "config");
            configDir.mkdirs();
            copyDirectory(this.getConfigDirectory(), configDir,
                    null, FileUtils.getDefaultExcludes());
        }
        final File destFile = new File(this.configOutputDir.getParent(), this.configOutputDir.getName() + ".zip");
        zipArchiver.setDestFile(destFile);
        zipArchiver.addDirectory(this.configOutputDir);
        zipArchiver.createArchive();

        projectHelper.attachArtifact(project, CONFIG_TYPE, CONFIG_CLASSIFIER, destFile);
    }
}
