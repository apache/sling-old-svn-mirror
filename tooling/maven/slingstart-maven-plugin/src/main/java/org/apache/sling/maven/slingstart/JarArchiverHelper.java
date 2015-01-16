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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.archiver.jar.ManifestException;

public class JarArchiverHelper {

    private final JarArchiver archiver;

    private final MavenProject project;

    public JarArchiverHelper(final JarArchiver archiver,
                    final MavenProject project,
                    final File destFile) throws MojoExecutionException {
        this(archiver, project, destFile, null);
    }

    public JarArchiverHelper(final JarArchiver archiver,
                    final MavenProject project,
                    final File destFile,
                    final java.util.jar.Manifest manifest) throws MojoExecutionException {
        this.project = project;
        this.archiver = archiver;
        this.archiver.reset();
        this.archiver.setDestFile(destFile);

        this.createManifest(manifest);
    }

    /**
     * Create a manifest
     */
    private void createManifest(final java.util.jar.Manifest manifest) throws MojoExecutionException {
        // create a new manifest
        final Manifest outManifest = new Manifest();

        try {
            boolean hasMain = false;

            // copy entries from existing manifest
            if ( manifest != null ) {
                final Map<Object, Object> attrs = manifest.getMainAttributes();
                for(final Map.Entry<Object, Object> entry : attrs.entrySet()) {
                    final String key = entry.getKey().toString();
                    if ( !BuildConstants.ATTRS_EXCLUDES.contains(key)) {
                        final Attribute a = new Attribute(key, entry.getValue().toString());
                        outManifest.addConfiguredAttribute(a);
                    }
                    if ( key.equals(BuildConstants.ATTR_MAIN_CLASS) ) {
                        hasMain = true;
                    }
                }
            }
            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_IMPLEMENTATION_BUILD,
                            project.getVersion()));
            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_IMPLEMENTATION_VERSION,
                            project.getVersion()));

            String organizationName = project.getOrganization() != null ? project.getOrganization().getName() : null;
            if ( organizationName != null ) {
                outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_IMPLEMENTATION_VENDOR,
                            organizationName));
                outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_CREATED_BY,
                            organizationName));
                outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_BUILT_BY,
                            organizationName));
                outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_SPECIFICATION_VENDOR,
                        organizationName));
            }

            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_IMPLEMENTATION_VENDOR_ID,
                            project.getGroupId()));
            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_IMPLEMENTATION_TITLE,
                            project.getName()));
            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_SPECIFICATION_TITLE,
                            project.getName()));
            outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_SPECIFICATION_VERSION,
                            project.getVersion()));

            if ( archiver.getDestFile().getName().endsWith(".jar") && !hasMain) {
                outManifest.addConfiguredAttribute(new Attribute(BuildConstants.ATTR_MAIN_CLASS,
                                BuildConstants.ATTR_VALUE_MAIN_CLASS));
            }

            archiver.addConfiguredManifest(outManifest);
        } catch (final ManifestException e) {
            throw new MojoExecutionException("Unable to create manifest for " + this.archiver.getDestFile(), e);
        }
    }

    public void addDirectory(File directory, String prefix, String[] includes, String[] excludes)
                    throws MojoExecutionException {
        try {
            archiver.addDirectory(directory, prefix, includes, excludes);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void addDirectory(File directory, String prefix) throws MojoExecutionException {
        try {
            archiver.addDirectory(directory, prefix);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void addDirectory(File directory, String[] includes, String[] excludes) throws MojoExecutionException {
        try {
            archiver.addDirectory(directory, includes, excludes);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void addDirectory(File directory) throws MojoExecutionException {
        try {
            archiver.addDirectory(directory);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void addFile(File arg0, String arg1, int arg2) throws MojoExecutionException {
        try {
            archiver.addFile(arg0, arg1, arg2);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void addFile(File inputFile, String destFileName) throws MojoExecutionException {
        try {
            archiver.addFile(inputFile, destFileName);
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        }
    }

    public void createArchive() throws MojoExecutionException {
        try {
            archiver.createArchive();
        } catch (final ArchiverException ae) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), ae);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create archive for " + this.archiver.getDestFile(), e);
        }
    }

    public void addArtifacts(final Map<String, File> globalContentsMap, final String prefix)
    throws MojoExecutionException {
        for(final Map.Entry<String, File> entry : globalContentsMap.entrySet()) {
            if ( entry.getValue().isFile() ) {
                this.addFile(entry.getValue(), prefix + entry.getKey());
            } else {
                this.addDirectory(entry.getValue(), prefix);
            }
        }
    }
}
