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
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.util.FileUtils;

/**
 * This class contains the Launchpad-framework specific utility methods.
 *
 */
public abstract class AbstractLaunchpadFrameworkMojo extends AbstractUsingBundleListMojo {

    /**
     * The name of the directory within the output directory into which the base
     * JAR should be installed.
     */
    @Parameter(defaultValue = "resources")
    protected String baseDestination;

    /**
     * The directory which contains the start-level bundle directories.
     */
    @Parameter(defaultValue = "bundles")
    private String bundlesDirectory;

    /**
     * The directory which contains the bootstrap bundle directories.
     */
    @Parameter
    private String bootDirectory;

    protected String getPathForArtifact(final int startLevel, final String artifactName, final String runModes) {
        final Set<String> runModesList = new TreeSet<String>();
        if (runModes != null ) {
            for(final String mode : runModes.split(",")) {
                if ( mode.trim().length() > 0 ) {
                    runModesList.add(mode);
                }
            }
        }
        final String runModeExt;
        if ( runModesList.size() == 0 ) {
            runModeExt = "";
        } else {
            final StringBuilder sb = new StringBuilder();
            for(final String n : runModesList ) {
                sb.append('.');
                sb.append(n);
            }
            runModeExt = sb.toString();
        }
        if ( startLevel == -1 && bootDirectory != null ) {
            return String.format("%s/%s%s/1/%s", baseDestination, bootDirectory,
                    runModeExt,
                    artifactName);
        }
        return String.format("%s/%s%s/%s/%s", baseDestination, bundlesDirectory,
                runModeExt,
                (startLevel == -1 ? 1 : startLevel),
                artifactName, runModeExt);
    }

    protected void copyBundles(BundleList bundles, File outputDirectory) throws MojoExecutionException {
        for (StartLevel startLevel : bundles.getStartLevels()) {
            for (Bundle bundle : startLevel.getBundles()) {
                copy(new ArtifactDefinition(bundle, startLevel.getStartLevel()), outputDirectory);
            }
        }
    }

    protected void copy(ArtifactDefinition additionalBundle, File outputDirectory) throws MojoExecutionException {
        Artifact artifact = getArtifact(additionalBundle);
        copy(artifact.getFile(), additionalBundle.getStartLevel(), additionalBundle.getRunModes(), outputDirectory);
    }

    protected void copy(File file, int startLevel, String runModes, File outputDirectory) throws MojoExecutionException {
        File destination = new File(outputDirectory, getPathForArtifact(startLevel, file.getName().replace('/', File.separatorChar), runModes));
        if (shouldCopy(file, destination)) {
            getLog().info(String.format("Copying bundle from %s to %s", file.getPath(), destination.getPath()));
            try {
                FileUtils.copyFile(file, destination);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy bundle from " + file.getPath(), e);
            }
        }
    }

}
