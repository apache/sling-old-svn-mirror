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
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Writer;

/**
 * Attaches the bundle list as a project artifact.
 *
 * @goal attach-bundle-list
 * @phase package
 * @requiresDependencyResolution test
 * @description attach the bundle list as a project artifact
 */
public class AttachBundleListMojo extends AbstractUsingBundleListMojo {

    private static final String CLASSIFIER = "bundlelist";

    private static final String TYPE = "xml";

    /**
     * @parameter default-value="${project.build.directory}/bundleList.xml"
     */
    private File outputFile;

    private BundleListXpp3Writer writer = new BundleListXpp3Writer();

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(outputFile);
            writer.write(fw, getInitializedBundleList());
            projectHelper.attachArtifact(project, TYPE, CLASSIFIER, outputFile);
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
    }

}
