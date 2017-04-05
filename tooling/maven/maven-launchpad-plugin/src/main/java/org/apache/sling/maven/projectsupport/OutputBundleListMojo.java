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

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Writer;

/**
 * Output the bundle list back to the console.
 */
@Mojo( name = "output-bundle-list", requiresDependencyResolution = ResolutionScope.TEST)
public class OutputBundleListMojo extends AbstractUsingBundleListMojo {

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        BundleListXpp3Writer writer = new BundleListXpp3Writer();
        try {
            writer.write(new OutputStreamWriter(System.out), getInitializedBundleList());
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write bundle list", e);
        }
    }

}
