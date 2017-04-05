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

import static org.apache.jackrabbit.vault.util.Constants.FILTER_XML;
import static org.apache.jackrabbit.vault.util.Constants.META_DIR;
import static org.apache.jackrabbit.vault.util.Constants.META_INF;
import static org.apache.jackrabbit.vault.util.Constants.ROOT_DIR;
import static org.apache.jackrabbit.vault.util.Constants.VAULT_DIR;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Manages OSGi configurations for File System Resource provider.
 */
abstract class AbstractFsMountMojo extends AbstractBundlePostMojo {

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property = "sling.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
    private String bundleFileName;

    /**
     * FileVault filesystem layout content root folder.
     */
    @Parameter(property = "sling.filevault.jcr_root.file")
    private File fileVaultJcrRootFile;

    /**
     * Path to META-INF/vault/filter.xml when using FileVault XML filesystem
     * layout.
     */
    @Parameter(property = "sling.filevault.filterxml.file")
    private File fileVaultFilterXmlFile;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String targetUrl = getConsoleTargetURL();
        
        // check for Sling-Initial-Content
        File bundleFile = new File(bundleFileName);
        if (bundleFile.exists()) {
            configureSlingInitialContent(targetUrl, bundleFile);
            return;
        }
        
        // try to detect filevault layout
        File jcrRootFile;
        File filterXmlFile;
        if (fileVaultJcrRootFile != null) {
            jcrRootFile = fileVaultJcrRootFile;
        }
        else {
            jcrRootFile = detectJcrRootFile();
        }
        if (fileVaultFilterXmlFile != null) {
            filterXmlFile = fileVaultFilterXmlFile;
        }
        else {
            filterXmlFile = detectFilterXmlFile();
        }
        if (jcrRootFile != null && jcrRootFile.exists() && filterXmlFile != null && filterXmlFile.exists()) {
            configureFileVaultXml(targetUrl, jcrRootFile, filterXmlFile);
            return;
        }
        
        getLog().info("No Bundle with initial content or FileVault content package found - skipping.");
    }

    @SuppressWarnings("unchecked")
    private File detectJcrRootFile() {
        List<Resource> resources = project.getResources();
        if (resources != null) {
            for (Resource resource : resources) {
                File dir = new File(resource.getDirectory());
                // look for dir ending with /jcr_root
                if (dir.exists() && dir.isDirectory() && StringUtils.equals(dir.getName(), ROOT_DIR)) {
                    return dir;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private File detectFilterXmlFile() {
        List<Resource> resources = project.getResources();
        if (resources != null) {
            for (Resource resource : resources) {
                File dir = new File(resource.getDirectory());
                if (dir.exists() && dir.isDirectory() ) {
                    // look for META-INF -> vault/filter.xml
                    if (StringUtils.equals(dir.getName(), META_INF)) {
                        File filterXml = new File(dir, VAULT_DIR + "/" + FILTER_XML);
                        if (filterXml.exists()) {
                            return filterXml;
                        }
                    }
                    // look for META-INF/vault -> filter.xml
                    else if (StringUtils.equals(dir.getName(), VAULT_DIR)) {
                        File filterXml = new File(dir, FILTER_XML);
                        if (filterXml.exists()) {
                            return filterXml;
                        }
                    }
                    // look for jcr_root -> ../META-INF/vault/filter.xml
                    else if (StringUtils.equals(dir.getName(), ROOT_DIR)) {
                        File filterXml = new File(dir.getParentFile(), META_DIR + "/" + FILTER_XML);
                        if (filterXml.exists()) {
                            return filterXml;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected abstract void configureSlingInitialContent(final String targetUrl, final File bundleFile)
            throws MojoExecutionException;

    protected abstract void configureFileVaultXml(final String targetUrl, final File jcrRootFile, final File filterXmlFile)
            throws MojoExecutionException;

}