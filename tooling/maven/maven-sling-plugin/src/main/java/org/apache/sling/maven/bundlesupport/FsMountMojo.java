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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Creates OSGi configurations for File System Resource provider.
 */
@Mojo(name = "fsmount", requiresProject = true)
public class FsMountMojo extends AbstractBundlePostMojo {

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property = "sling.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
    private String bundleFileName;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;
    
    @Override
    public void execute() throws MojoExecutionException {
        File file = new File(bundleFileName);
        if(!file.exists()) {
            getLog().info(file + " does not exist, skipping.");
            return;
        }
        
        FsMountHelper fsMountHelper = new FsMountHelper(getLog(), getHttpClient(), project);
        fsMountHelper.configureInstall(getTargetURL(), file);
    }

}