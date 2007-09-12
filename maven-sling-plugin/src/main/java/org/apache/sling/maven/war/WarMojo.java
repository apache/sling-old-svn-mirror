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

package org.apache.sling.maven.war;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Constants;

/**
 * This bundle generates the WEB-INF/sling_install.properties containing
 * the referenced assemblies.
 *
 * @goal install-properties
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @description build the sling_install.properties
 *
 */
public class WarMojo extends org.apache.maven.plugin.AbstractMojo {

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The default start level for bundles not listed in the <code>startLevels</code>
     * property. Default if missing or undefined is <code>30</code>. Valid values
     * are integers in the range [1 .. Integer.MAX_VALUE].
     *
     * @parameter expression="${sling.assemblies.startlevel.default}"
     */
    private String defaultStartLevel;

    /**
     * Startlevel mappings for included artifacts. Indexed by
     * groupId.artifactId, value is numeric startlevel [1 .. Integer.MAX_VALUE]
     *
     * @parameter
     */
    private Map startLevels = new HashMap();

    /**
     * Version mapping for included artifacts. Indexed
     * by groupId.artifactId, value is a policy string, either "strict" (default)
     * or "latest".
     *
     * @parameter
     */
    private Map versionPolicies = new HashMap();

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().debug("Executing sling war mojo");
        // check default start level
        if (this.defaultStartLevel == null || this.defaultStartLevel.length() == 0) {
            this.defaultStartLevel = "30";
        }

        final List assemblies = new ArrayList();

        final Map resolved = this.project.getArtifactMap();
        final Set artifacts = this.project.getDependencyArtifacts();
        final Iterator it = artifacts.iterator();
        while ( it.hasNext() ) {
            final Artifact declared = (Artifact) it.next();
            this.getLog().debug("Checking artifact " + declared);
            if (Artifact.SCOPE_COMPILE.equals(declared.getScope())
                || Artifact.SCOPE_PROVIDED.equals(declared.getScope())
                || Artifact.SCOPE_RUNTIME.equals(declared.getScope())) {
                this.getLog().debug("Resolving artifact " + declared);
                Artifact artifact = (Artifact) resolved.get(ArtifactUtils.versionlessKey(declared));
                if (artifact != null) {
                    this.getLog().debug("Getting manifest from artifact " + artifact);
                    try {
                        Manifest m = this.getManifest(artifact);
                        if (m != null ) {
                            final String category = m.getMainAttributes().getValue(Constants.BUNDLE_CATEGORY);
                            this.getLog().debug("Category of artifact " + artifact + " is " + category);
                            if ( category != null && category.equals("assembly") ) {
                                final String name = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                                this.getLog().debug("Found assembly: " + artifact.getArtifactId() + " with name " + name);
                                final String identifier = artifact.getGroupId() + "." + artifact.getArtifactId();
                                final String startLevel = (String) this.startLevels.get(identifier);
                                final String policy = (String) this.versionPolicies.get(identifier);
                                assemblies.add(new AssemblyInfo(name, declared, (startLevel != null ? startLevel : this.defaultStartLevel), policy));
                            }
                        } else {
                            this.getLog().debug("Unable to get manifest from artifact " + artifact);
                        }
                    } catch (IOException ioe) {
                        throw new MojoExecutionException("Unable to get manifest from artifact " + artifact, ioe);
                    }
                } else {
                    this.getLog().debug("Unable to resolve artifact " + declared);
                }
            } else {
                this.getLog().debug("Artifact " + declared + " has not scope compile, provided or runtime, but: " + declared.getScope());
            }
        }
        final File f = new File(this.outputDirectory, this.finalName + File.separator + "WEB-INF" + File.separator + "sling_install.properties");
        if ( assemblies.size() > 0 ) {
            // create the directory if necessary
            f.getParentFile().mkdirs();
            // let's sort the assemblies based on the start level
            Collections.sort(assemblies, new AssemblyInfoComparator());
            String previousLevel = null;
            FileWriter fw = null;
            try {
                fw = new FileWriter(f);
                fw.write("# Generated installation properties for assemblies\n");
                final Iterator i = assemblies.iterator();
                while ( i.hasNext() ) {
                    final AssemblyInfo info = (AssemblyInfo)i.next();
                    // first entry or new level?
                    if ( previousLevel == null || !previousLevel.equals(info.level) ) {
                        if ( previousLevel != null ) {
                            fw.write('\n');
                        }
                        fw.write("sling.install.");
                        fw.write(info.level);
                        fw.write(" = ");
                    } else {
                        // append entry
                        fw.write(',');
                    }
                    fw.write(info.name);
                    fw.write(':');
                    fw.write(info.version);
                    previousLevel = info.level;
                }
                if ( previousLevel != null ) {
                    fw.write('\n');
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to generate " + f, e);
            } finally {
                if ( fw != null ) {
                    try {
                        fw.close();
                    } catch (IOException ignore) {
                        // ignore this
                    }
                }
            }
        } else {
            // if we don't reference any assembly delete a possible
            // properties file from an earlier build
            if ( f.exists() ) {
                f.delete();
            }
        }
    }

    protected Manifest getManifest(Artifact artifact) throws IOException {
        JarFile file = null;
        try {
            file = new JarFile(artifact.getFile());
            return file.getManifest();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    protected static final class AssemblyInfoComparator implements Comparator {

        public int compare(Object arg0, Object arg1) {
            // we know that we sort just assembly infos, so no need to check this!
            final AssemblyInfo info1 = (AssemblyInfo)arg0;
            final AssemblyInfo info2 = (AssemblyInfo)arg1;
            return info1.level.compareTo(info2.level);
        }

    }

    protected static final class AssemblyInfo {

        public final String name;
        public final String version;
        public final String level;

        public AssemblyInfo(String n, Artifact artifact, String l, String policy) {
            this.name = n;
            ArtifactVersion av;
            String v = null;
            String nextVersion = null;
            try {
                av = artifact.getSelectedVersion();
                final StringBuffer buffer = new StringBuffer();
                buffer.append(av.getMajorVersion());
                buffer.append('.');
                buffer.append(av.getMinorVersion());
                buffer.append('.');
                nextVersion = buffer.toString() + (av.getIncrementalVersion() + 1);
                buffer.append(av.getIncrementalVersion());
                // we don't append build number and qualifier to always get the latest version

                //if ( av.getBuildNumber() != 0 ) {
                //    buffer.append('.');
                //    buffer.append(av.getBuildNumber());
                //}
                //if ( av.getQualifier() != null ) {
                //    buffer.append('.');
                //    buffer.append(av.getQualifier());
                //}
                v = buffer.toString();
            } catch (OverConstrainedVersionException e) {
                v = artifact.getVersion();
            }
            if ( policy == null || policy.trim().length() == 0 || policy.trim().equalsIgnoreCase("strict") && nextVersion != null ) {
                this.version = "\"[" + v + "," + nextVersion + ")\"";
            } else {
                this.version = v;
            }
            this.level = l;
        }
    }
}
