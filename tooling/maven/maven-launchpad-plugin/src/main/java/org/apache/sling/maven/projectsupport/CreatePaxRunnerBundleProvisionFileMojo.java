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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;

/**
 * Create and attach a Pax Runner bundle provision file.
 */
@Mojo(name = "create-paxrunner-provision-file", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class CreatePaxRunnerBundleProvisionFileMojo extends AbstractUsingBundleListMojo {

    private static final String CLASSIFIER = "bundles";

    private static final String TYPE = "pax";

    /**
     * The output directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/bundles-pax")
    private File outputFile;

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        FileWriter out = null;
        try {
            out = new FileWriter(outputFile);

            BundleList bundleList = getInitializedBundleList();
            for (StartLevel level : bundleList.getStartLevels()) {
                for (Bundle bundle : level.getBundles()) {
                    String line = String.format("mvn:%s/%s/%s@%d\n", bundle.getGroupId(), bundle.getArtifactId(),
                            bundle.getVersion(), level.getStartLevel());
                    out.write(line);
                }
            }

            projectHelper.attachArtifact(project, TYPE, CLASSIFIER, outputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write " + outputFile.getName(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
