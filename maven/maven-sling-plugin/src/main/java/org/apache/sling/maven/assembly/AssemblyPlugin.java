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

package org.apache.sling.maven.assembly;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * The <code>AssemblyPlugin</code> TODO
 *
 * @goal assembly
 * @phase package
 * @requiresDependencyResolution compile
 * @description build a Sling Assembly jar
 */
public class AssemblyPlugin extends AbstractMojo {

    /**
     * The name of the bundle manifest header providing the specification(s) of
     * the bundle(s) to be installed along with this Assembly Bundle (value is
     * "Assembly-Bundles").
     */
    public static final String ASSEMBLY_BUNDLES = "Assembly-Bundles";

    /**
     * The name of the bundle manifest header providing the source of the
     * bundles to be installed (value is "Assembly-BundleRepository").
     */
    public static final String ASSEMBLY_BUNDLEREPOSITORY = "Assembly-BundleRepository";

    /**
     * The location in the Assembly Bundle of embedded bundles to install (value
     * is "OSGI-INF/bundles/").
     */
    public static final String EMBEDDED_BUNDLE_LOCATION = "OSGI-INF/bundles/";

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
     * An optional comma-separated list of URLs of OSGi Bundle Repositories -
     * e.g. http://repohost.day.com/repository.xml - to use for the installation
     * of the bundles.
     *
     * @parameter
     */
    private String repositories;

    /**
     * Whether the bundles are embedded in the created JAR file or not. Default
     * is to not embed the bundles.
     *
     * @parameter expression="${sling.assemblies.embedded}"
     */
    private boolean embedded;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException {
        try {
            String bsn = this.project.getGroupId() + "." + this.project.getArtifactId();
            String version = this.project.getVersion();
            Pattern P_VERSION = Pattern.compile("([0-9]+(\\.[0-9])*)-(.*)");
            Matcher m = P_VERSION.matcher(version);
            if (m.matches()) {
                version = m.group(1) + "." + m.group(3);
            }

            this.getLog().info("Building Assembly " + bsn + " " + version);

            File jarFile = new File(this.outputDirectory, this.finalName + ".jar");

            // create the initial Manifest first
            this.header(Constants.BUNDLE_MANIFESTVERSION, "2");
            this.header(Constants.BUNDLE_SYMBOLICNAME, bsn);
            this.header(Constants.BUNDLE_VERSION, version);
            this.header(Constants.BUNDLE_DESCRIPTION, this.project.getDescription());
            this.header("Bundle-License", this.printLicenses(this.project.getLicenses()));
            this.header(Constants.BUNDLE_NAME, this.project.getName());

            if (this.project.getOrganization() != null) {
                this.header(Constants.BUNDLE_VENDOR,
                    this.project.getOrganization().getName());
                if (this.project.getOrganization().getUrl() != null) {
                    this.header(Constants.BUNDLE_DOCURL,
                        this.project.getOrganization().getUrl());
                }
            }

            // next extract the bundle dependencies and create the Assembly
            // headers
            JarArchiver jar = new JarArchiver();
            this.getBundles(jar);

            // finally we are going to write this whole stuff
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jar);
            archiver.setOutputFile(jarFile);

            archiver.createArchive(this.project, this.archive);

            // set the newly generated file as the primary artifact
            this.project.getArtifact().setFile(jarFile);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unknown error occurred", e);
        }
    }

    private StringBuffer printLicenses(List licenses) {
        if (licenses == null || licenses.size() == 0) return null;
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator i = licenses.iterator(); i.hasNext();) {
            License l = (License) i.next();
            String url = l.getUrl();
            sb.append(del);
            sb.append(url);
            del = ", ";
        }
        return sb;
    }

    private void getBundles(JarArchiver jar) throws MojoExecutionException,
            ArchiverException {
        // set the assembly headers
        if (this.repositories != null && this.repositories.length() > 0) {
            this.getLog().debug("Adding Bundle Repositories [" + this.repositories + "]");
            this.archive.addManifestEntry(ASSEMBLY_BUNDLEREPOSITORY, this.repositories);
        }

        // check default start level
        if (this.defaultStartLevel == null || this.defaultStartLevel.length() == 0) {
            this.defaultStartLevel = "30";
        }

        StringBuffer bundleList = new StringBuffer();

        Map resolved = this.project.getArtifactMap();
        Set artifacts = this.project.getDependencyArtifacts();
        for (Iterator it = artifacts.iterator(); it.hasNext();) {
            Artifact declared = (Artifact) it.next();
            this.getLog().debug("Checking artifact " + declared);
            if (Artifact.SCOPE_COMPILE.equals(declared.getScope())
                    || Artifact.SCOPE_PROVIDED.equals(declared.getScope())
                    || Artifact.SCOPE_RUNTIME.equals(declared.getScope())) {
                this.getLog().debug("Resolving artifact " + declared);
                Artifact artifact = (Artifact) resolved.get(ArtifactUtils.versionlessKey(declared));
                if (artifact != null) {
                    this.getLog().debug("Getting manifest from artifact " + artifact);
                    Manifest m = this.getManifest(artifact);
                    if (m != null
                        && m.getMainAttributes().getValue(
                            Constants.BUNDLE_SYMBOLICNAME) != null) {

                        String name = m.getMainAttributes().getValue(
                            Constants.BUNDLE_SYMBOLICNAME);
                        String version = m.getMainAttributes().getValue(
                            Constants.BUNDLE_VERSION);

                        if (bundleList.length() > 0) bundleList.append(",");

                        bundleList.append(name);
                        if (version != null) {
                            final Version v = new Version(version);
                            final String nextVersion = v.getMajor() + "." + v.getMinor() + "." + (v.getMicro() + 1);
                            bundleList.append(";version=\"");

                            // ensure no SNAPSHOT in qualifier
                            if (artifact.isSnapshot()) {
                                version = v.getMajor() + "." + v.getMinor()
                                    + "." + v.getMicro();
                            } else {

                                if (v.getQualifier() != null
                                    && v.getQualifier().indexOf("SNAPSHOT") >= 0) {
                                    version = v.getMajor() + "." + v.getMinor()
                                        + "." + v.getMicro();
                                }
                            }
                            // if the policy is strict, we exactly want the specified version
                            final String policy = (String)this.versionPolicies.get(artifact.getGroupId() + "." + artifact.getArtifactId());
                            if ( policy == null || policy.trim().length() == 0 || policy.trim().equalsIgnoreCase("strict") ) {
                                bundleList.append("[").append(version).append(",").append(nextVersion).append(")");
                            } else {
                                bundleList.append(version);
                            }

                            bundleList.append('"');
                        }

                        String startLevel = (String) this.startLevels.get(artifact.getGroupId()
                            + "." + artifact.getArtifactId());
                        if (startLevel == null) {
                            startLevel = this.defaultStartLevel;
                        }
                        if (startLevel != null) {
                            bundleList.append(";startlevel=").append(startLevel);
                        }

                        if (this.embedded) {
                            String path = EMBEDDED_BUNDLE_LOCATION
                                + artifact.getGroupId() + "."
                                + artifact.getArtifactId() + "-"
                                + artifact.getVersion() + "."
                                + artifact.getArtifactHandler().getExtension();
                            jar.addFile(artifact.getFile(), path);
                            bundleList.append(";entry=");
                            bundleList.append('"').append(path).append('"');
                            this.getLog().debug("Embedding Bundle Artifact " + artifact + " as [" + path + "]");
                        } else {
                            this.getLog().debug("Referring to Bundle Artifact " + artifact);
                        }

                        // TODO: get linked somehow ...
                    } else {
                        this.getLog().warn("Ignoring " + artifact + ": Missing Manifest");
                    }
                } else {
                    this.getLog().warn("Ignoring " + declared + ": Not resolved");
                }
            } else {
                this.getLog().warn("Ignoring " + declared + ": Wrong scope");
            }
        }

        this.getLog().debug("Adding Bundles:" + bundleList);
        this.archive.addManifestEntry(ASSEMBLY_BUNDLES, bundleList.toString());
    }

    private void header(String key, Object value) {
        if (this.archive.getManifestEntries().containsKey(key) || value == null) {
            return;
        }

        if (value instanceof Collection && ((Collection) value).isEmpty()) {
            return;
        }

        this.archive.addManifestEntry(key, value.toString());
    }

    private Manifest getManifest(Artifact artifact) throws MojoExecutionException {
        JarFile file = null;
        try {
            file = new JarFile(artifact.getFile());
            return file.getManifest();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Unable to read manifest from artifact " + artifact, ioe);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}