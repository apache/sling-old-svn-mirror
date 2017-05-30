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
import org.apache.sling.maven.bundlesupport.fsresource.FileVaultXmlMounter;
import org.apache.sling.maven.bundlesupport.fsresource.SlingInitialContentMounter;

/**
 * Removes OSGi configurations for the 
 * <a href="https://sling.apache.org/documentation/bundles/accessing-filesystem-resources-extensions-fsresource.html">Apache Sling File System Resource Provider</a>.
 */
@Mojo(name = "fsunmount", requiresProject = true)
public class FsUnMountMojo extends AbstractFsMountMojo {

    @Override
    protected void configureSlingInitialContent(final String targetUrl, final File bundleFile) throws MojoExecutionException {
        new SlingInitialContentMounter(getLog(), getHttpClient(), project).unmount(targetUrl, bundleFile);
    }

    @Override
    protected void configureFileVaultXml(String targetUrl, File jcrRootFile, File filterXmlFile) throws MojoExecutionException {
        new FileVaultXmlMounter(getLog(), getHttpClient(), project).unmount(targetUrl, jcrRootFile, filterXmlFile);
    }

    @Override
    protected void ensureBundlesInstalled(String targetUrl) throws MojoExecutionException {
        // nothing to do on uninstall
    }

}
