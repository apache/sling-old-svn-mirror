/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.maven.bundlesupport;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deploy a JAR representing an OSGi Bundle. This method posts the bundle built
 * by maven to an OSGi Bundle Repository accepting the bundle. The plugin uses
 * a </em>multipart/format-data</em> POST request to just post the file to
 * the URL configured in the <code>obr</code> property. 
 */
@Mojo(name="deploy-file", requiresProject= false)
public class BundleDeployFileMojo extends AbstractBundleDeployMojo {

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property="sling.file")
    private String bundleFileName;
    
    @Override
    protected String getJarFileName() throws MojoExecutionException {
        if (bundleFileName == null) {
            throw new MojoExecutionException("Missing sling.file parameter");
        }
        
        return bundleFileName;
    }
    
    @Override
    protected File fixBundleVersion(File jarFile) {
        // we just upload the file as is (the obr might fix the version, too)
        return jarFile;
    }


}
