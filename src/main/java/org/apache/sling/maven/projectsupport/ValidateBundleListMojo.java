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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Validate that the artifacts listed in a bundle list are valid
 *
 * @goal validate-bundle-list
 * @phase validate
 * @requiresDependencyResolution test
 * @description validate that the artifacts listed in a bundle list are valid
 */
public class ValidateBundleListMojo extends AbstractBundleListMojo {

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
            initializedBundleList = new BundleList();
        }

        interpolateProperties(initializedBundleList, project, mavenSession);

        for (StartLevel sl : initializedBundleList.getStartLevels()) {
            for (Bundle bundle : sl.getBundles()) {
                getArtifact(new ArtifactDefinition(bundle, -1));
            }
        }
    }
}
