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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;

/**
 * Validate that the bundle list file (if it exists) does not contain references
 * to SNAPSHOT versions.
 */
@Mojo(name = "check-bundle-list-for-snapshots", requiresDependencyResolution = ResolutionScope.TEST)
public class CheckBundleListForSnapshotsMojo extends AbstractUsingBundleListMojo {

    /**
     * True if the build should be failed if a snapshot is found.
     */
    @Parameter( defaultValue = "true")
    private boolean failOnSnapshot;

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        List<Bundle> snapshots = new ArrayList<Bundle>();
        BundleList bundleList = getInitializedBundleList();
        for (StartLevel level : bundleList.getStartLevels()) {
            for (Bundle bundle : level.getBundles()) {
                if (isSnapshot(bundle)) {
                    snapshots.add(bundle);
                }
            }
        }
        if (!snapshots.isEmpty()) {
            getLog().error("The following entries in the bundle list file are SNAPSHOTs:");
            for (Bundle bundle : snapshots) {
                getLog().error(
                        String
                                .format("     %s:%s:%s", bundle.getGroupId(), bundle.getArtifactId(), bundle
                                        .getVersion()));
            }
            if (failOnSnapshot) {
                throw new MojoFailureException("SNAPSHOTs were found in the bundle list. See log.");
            }
        }
    }

    private boolean isSnapshot(Bundle bundle) {
        return bundle.getVersion().endsWith("SNAPSHOT");
    }

}
