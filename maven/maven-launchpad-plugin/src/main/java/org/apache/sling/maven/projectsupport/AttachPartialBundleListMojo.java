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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;

/**
 * Attaches the bundle list as a project artifact.
 *
 * @goal attach-partial-bundle-list
 * @phase package
 * @description attach the partial bundle list as a project artifact
 */
public class AttachPartialBundleListMojo extends AbstractBundleListMojo {

    public static final String CONFIG_CLASSIFIER = "bundlelistconfig";

    public static final String CONFIG_TYPE = "zip";

    public static final String SLING_COMMON_PROPS = "common.properties";

    public static final String SLING_COMMON_BOOTSTRAP = "bootstrap.txt";

    /**
     * @parameter default-value="${project.build.directory}/bundleListconfig"
     */
    private File configOutputDir;

    /**
     * @parameter expression="${ignoreBundleListConfig}"
     *            default-value="false"
     */
    private boolean ignoreBundleListConfig;

    /**
     * The zip archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="zip"
     */
    private ZipArchiver zipArchiver;

    public void execute() throws MojoExecutionException, MojoFailureException {
        project.getArtifact().setFile(bundleListFile);
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
        if ( this.checkFile(this.getConfigDirectory()) ) {
            final File configDir = new File(this.configOutputDir, "config");
            configDir.mkdirs();
            FileUtils.copyDirectory(this.getConfigDirectory(), configDir,
                    null, FileUtils.getDefaultExcludesAsString());
        }
        final File destFile = new File(this.configOutputDir.getParent(), this.configOutputDir.getName() + ".zip");
        zipArchiver.setDestFile(destFile);
        zipArchiver.addDirectory(this.configOutputDir);
        zipArchiver.createArchive();

        projectHelper.attachArtifact(project, CONFIG_TYPE, CONFIG_CLASSIFIER, destFile);
    }
}
