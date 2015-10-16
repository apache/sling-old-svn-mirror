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

package org.apache.sling.maven.bundlesupport;

import java.io.File;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Deploy a JAR representing an OSGi Bundle. This method posts the bundle built
 * by maven to an OSGi Bundle Repository accepting the bundle. The plugin uses
 * a </em>multipart/format-data</em> POST request to just post the file to
 * the URL configured in the <code>obr</code> property.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class BundleDeployMojo extends AbstractBundleDeployMojo {

    /**
     * Whether to skip this step even though it has been configured in the
     * project to be executed. This property may be set by the
     * <code>sling.deploy.skip</code> comparable to the <code>maven.test.skip</code>
     * property to prevent running the unit tests.
     */
    @Parameter(property = "sling.deploy.skip", defaultValue = "false", required = true)
    private boolean skip;

	/**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property = "project.build.finalName", alias = "jarName", required = true)
    private String jarName;

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        // don't do anything, if this step is to be skipped
        if (skip) {
            getLog().debug("Skipping bundle deployment as instructed");
            return;
        }

        super.execute();
    }

    @Override
    protected String getJarFileName() {
        return buildDirectory + "/" + jarName;
    }

    @Override
    protected File fixBundleVersion(File jarFile) throws MojoExecutionException {
        // if this is a snapshot, replace "SNAPSHOT" with the date generated
        // by the maven deploy plugin
        if ( this.project.getVersion().indexOf("SNAPSHOT") > 0 ) {
            // create new version string by replacing all '-' with '.'
            String newVersion = this.project.getArtifact().getVersion();
            int firstPos = newVersion.indexOf('-') + 1;
            int pos = 0;
            while (pos != -1) {
                pos = newVersion.indexOf('-');
                if ( pos != -1 ) {
                    newVersion = newVersion.substring(0, pos ) + '.' + newVersion.substring(pos+1);
                }
            }
            // now remove all dots after the third one
            pos = newVersion.indexOf('.', firstPos);
            while ( pos != -1 ) {
                newVersion = newVersion.substring(0, pos) + newVersion.substring(pos+1);
                pos = newVersion.indexOf('.', pos+1);
            }
            return changeVersion(jarFile, project.getVersion(), newVersion);
        }

        // if this is a final release append "final"
        try {
            final ArtifactVersion v = this.project.getArtifact().getSelectedVersion();
            if ( v.getBuildNumber() == 0 && v.getQualifier() == null ) {
                final String newVersion = this.project.getArtifact().getVersion() + ".FINAL";
                return changeVersion(jarFile, project.getVersion(), newVersion);
            }
        } catch (OverConstrainedVersionException ocve) {
            // we ignore this and don't append "final"!
        }

        // just return the file in case of some issues
        return jarFile;
    }
}