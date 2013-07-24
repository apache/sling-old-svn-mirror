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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Install an OSGi bundle to a running Sling instance.
 *
 * @goal install
 * @phase install
 * @description install an OSGi bundle jar to a running Sling instance
 */
public class BundleInstallMojo extends AbstractBundleInstallMojo {

    /**
     * Whether to skip this step even though it has been configured in the
     * project to be executed. This property may be set by the
     * <code>sling.install.skip</code> comparable to the <code>maven.test.skip</code>
     * property to prevent running the unit tests.
     * 
     * @parameter expression="${sling.install.skip}" default-value="false"
     * @required
     */
    private boolean skip;
    
    /**
     * The name of the generated JAR file.
     *
     * @parameter expression="${sling.file}" default-value="${project.build.directory}/${project.build.finalName}.jar"
     * @required
     */
    private String bundleFileName;

    @Override
    public void execute() throws MojoExecutionException {
        // don't do anything, if this step is to be skipped
        if (skip) {
            getLog().debug("Skipping bundle installation as instructed");
            return;
        }

        super.execute();
    }
    
    @Override
    protected String getBundleFileName() {
        return bundleFileName;
    }
}