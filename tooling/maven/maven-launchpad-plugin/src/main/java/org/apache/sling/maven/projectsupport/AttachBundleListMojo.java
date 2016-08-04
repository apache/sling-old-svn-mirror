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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Writer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;

/**
 * Attaches the bundle list as a project artifact.
 */
@Mojo(name = "attach-bundle-list", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class AttachBundleListMojo extends AbstractUsingBundleListMojo {

    @Parameter(defaultValue = "${project.build.directory}/bundleList.xml")
    private File outputFile;

    @Parameter(defaultValue = "${project.build.directory}/bundleListconfig")
    private File configOutputDir;

    /**
     * The zip archiver.
     */
    @Component(role = UnArchiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    private final BundleListXpp3Writer writer = new BundleListXpp3Writer();

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        FileWriter fw = null;
        try {
            this.outputFile.getParentFile().mkdirs();
            fw = new FileWriter(outputFile);
            writer.write(fw, getInitializedBundleList());
            projectHelper.attachArtifact(project, AttachPartialBundleListMojo.TYPE, AttachPartialBundleListMojo.CLASSIFIER, outputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to output effective bundle list", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                }
            }
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
        hasConfigs |= this.getSlingBootstrap(true) != null;
        hasConfigs |= this.getSlingBootstrap(false) != null;
        hasConfigs |= this.getSlingProperties(true) != null;
        hasConfigs |= this.getSlingProperties(false) != null;

        if ( !hasConfigs ) {
            this.getLog().debug("No configurations to attach.");
            return;
        }
        // copy configuration, as this project might use different names we have to copy everything!
        this.configOutputDir.mkdirs();
        if ( this.getSlingBootstrap(false) != null ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.fileWrite(new File(slingDir, AttachPartialBundleListMojo.SLING_WEBAPP_BOOTSTRAP).getAbsolutePath(),
                                "UTF-8", this.getSlingBootstrap(false));
        }
        if ( this.getSlingProperties(false) != null ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            final FileOutputStream fos = new FileOutputStream(new File(slingDir, AttachPartialBundleListMojo.SLING_WEBAPP_PROPS));
            try {
                this.getSlingProperties(false).store(fos, null);
            } finally {
                try { fos.close(); } catch (final IOException ioe) {}
            }
        }
        if ( this.getSlingBootstrap(true) != null ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            FileUtils.fileWrite(new File(slingDir, AttachPartialBundleListMojo.SLING_STANDALONE_BOOTSTRAP).getAbsolutePath(),
                    "UTF-8", this.getSlingBootstrap(true));
        }
        if ( this.getSlingProperties(true) != null ) {
            final File slingDir = new File(this.configOutputDir, "sling");
            slingDir.mkdirs();
            final FileOutputStream fos = new FileOutputStream(new File(slingDir, AttachPartialBundleListMojo.SLING_STANDALONE_PROPS));
            try {
                this.getSlingProperties(true).store(fos, null);
            } finally {
                try { fos.close(); } catch (final IOException ioe) {}
            }
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

        projectHelper.attachArtifact(project, AttachPartialBundleListMojo.CONFIG_TYPE,
                AttachPartialBundleListMojo.CONFIG_CLASSIFIER, destFile);
    }
}
