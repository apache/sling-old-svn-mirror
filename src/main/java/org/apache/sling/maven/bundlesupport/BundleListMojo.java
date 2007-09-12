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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Lists artifacts scoped as "bundleinstall" in a properties file at a
 * configurable location - WEB-INF/autoinstall.properties by default. The
 * properties file contains three entries for each bundle thus added: The bundle
 * symbolic name, the bundle version and the start level to assign the bundle
 * after installation.
 * <p>
 * Bundles are listed in the file sorted by bundle startlevel, symblic name and
 * version. Bundle start levels may be declared using the
 * <code>defaultStartLevel</code> and <code>startLevels</code> properties.
 *
 * @goal list
 * @phase process-resources
 * @description List bundles to be installed/updated from an OBR on startup
 * @requiresDependencyResolution bundleinstall
 */
public class BundleListMojo extends AbstractMojo {

    /**
     * The prefix to the properties defining the bundle to be installed. This
     * property cannot be modified.
     *
     * @parameter expression="install."
     * @required
     * @readonly
     */
    private String prefix;

    /**
     * The file in which the bundle name and versions are listed.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}/WEB-INF/autoinstall/${project.groupId}.${project.artifactId}.properties"
     * @required
     */
    private String bundleInstallProperties;

    /**
     * The default start level for installed bundles. May be overwritten by
     * specific per-artifact settings
     *
     * @parameter expression="10"
     * @required
     */
    private String defaultStartLevel;

    /**
     * Specific startlevels for artifacts. Indexed by the artifact's groupId
     * plus artifactId, value is the specific startlevel.
     * <p>
     * Sample configuration:
     *
     * <pre>
     *      &lt;startLevels&gt;
     *            &lt;groupId.artifactId&gt;30&lt;/groupId.artifactId&gt;
     *            &lt;org.apache.osgi.day-osgi-webmanager&gt;5&lt;/org.apache.osgi.day-osgi-webmanager&gt;
     *      &lt;/startLevels&gt;
     * </pre>
     *
     * @parameter
     */
    private Map startLevels = new HashMap();

    /**
     * Dependencies which are installed
     *
     * @parameter expression="${project.artifacts}"
     * @readonly
     */
    private Set dependencies;

    /**
     * Execute this Mojo
     *
     * @throws MojoExecutionException
     */
    public void execute() {

        SortedMap byStartLevel = new TreeMap();
        Iterator artifacts = this.dependencies.iterator();
        while (artifacts.hasNext()) {
            Artifact artifact = (Artifact) artifacts.next();
            String artifactScope = artifact.getScope();
            if (artifactScope == null
                || !artifactScope.startsWith("bundleinstall")) {
                this.getLog().debug(
                    "Ignoring non-bundle artifact " + artifact.getArtifactId());
                continue;
            }

            // fix scope to not include the artifact in the final bundle
            artifact.setScope(Artifact.SCOPE_PROVIDED);

            // copy file
            BundleSpec bs = this.getBundleSpec(artifact, artifactScope);
            if (bs != null) {
                Integer sl = new Integer(bs.startLevel);
                SortedSet slSet = (SortedSet) byStartLevel.get(sl);
                if (slSet == null) {
                    slSet = new TreeSet();
                    byStartLevel.put(sl, slSet);
                }
                slSet.add(bs);
                this.getLog().debug(
                    "Added Bundle " + bs.symbolicName + "/" + bs.version);
            }
        }

        // do not write anynothing if there is nothing to write !
        if (byStartLevel.isEmpty()) {
            this.getLog().info("No Bundles added this time, nothing more to do");
            return;
        }

        // convert map of sets into properties
        int id = 0;
        Properties props = new Properties();
        for (Iterator mi = byStartLevel.values().iterator(); mi.hasNext();) {
            SortedSet slSet = (SortedSet) mi.next();
            for (Iterator bi = slSet.iterator(); bi.hasNext();) {
                BundleSpec bs = (BundleSpec) bi.next();
                props.setProperty(this.prefix + id + ".symbolic-name",
                    bs.symbolicName);
                props.setProperty(this.prefix + id + ".version", bs.version);
                props.setProperty(this.prefix + id + ".startlevel", String.valueOf(bs.startLevel));
                id++;

                this.getLog().debug(id + "  ==>  " + bs.symbolicName);
            }
        }

        // write the properties file
        File bundleInstallPropertiesFile = new File(this.bundleInstallProperties);
        bundleInstallPropertiesFile.getParentFile().mkdirs();
        OutputStream out = null;
        try {
            out = new FileOutputStream(bundleInstallPropertiesFile);
            props.store(
                out,
                "Bundles to install from OBR on startup, automatically generated, do not modify");
        } catch (IOException ioe) {
            this.getLog().error(
                "Cannot store auto install properties "
                    + bundleInstallPropertiesFile, ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private BundleSpec getBundleSpec(Artifact artifact, String declaredScope) {
        File bundleFile = artifact.getFile();
        if (!bundleFile.exists()) {
            this.getLog().debug(
                "setBundleProperties: " + bundleFile + " does not exist");
            return null;
        }

        JarFile jaf = null;
        try {
            jaf = new JarFile(bundleFile);
            Manifest manif = jaf.getManifest();
            if (manif == null) {
                this.getLog().debug(
                    "setBundleProperties: Missing manifest in " + bundleFile);
                return null;
            }

            String symbName = manif.getMainAttributes().getValue(
                "Bundle-SymbolicName");
            if (symbName == null) {
                this.getLog().debug(
                    "setBundleProperties: No Bundle-SymbolicName in "
                        + bundleFile);
                return null;
            }
            this.getLog().debug(
                "setBundleProperties: " + bundleFile + " contains Bundle "
                    + symbName);

            String version = manif.getMainAttributes().getValue(
                "Bundle-Version");
            if (version == null) {
                // default version if missing
                version = "0.0.0";
            } else if (version.endsWith(".SNAPSHOT")) {
                // cutoff .SNAPSHOT qualifier
                version = version.substring(0, version.length()
                    - ".SNAPSHOT".length());
            }

            BundleSpec bs = new BundleSpec();
            bs.symbolicName = symbName;
            bs.version = version;
            bs.startLevel = this.getStartLevel(artifact);
            return bs;
        } catch (IOException ioe) {
            this.getLog().warn(
                "setBundleProperties: Problem list bundle " + bundleFile, ioe);
        } finally {
            if (jaf != null) {
                try {
                    jaf.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
        }

        // fall back to not being a bundle
        return null;
    }

    private int getStartLevel(Artifact artifact) {
        String startLevel = null;
        if (this.startLevels != null) {
            String id = artifact.getGroupId() + "." + artifact.getArtifactId();
            startLevel = (String) this.startLevels.get(id);
        }

        // fallback to default start level if none explicitly set
        if (startLevel == null) {
            startLevel = this.defaultStartLevel;
        }

        // convert to int - fall back to -1 if wrong type
        try {
            return Integer.parseInt(startLevel);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private static class BundleSpec implements Comparable {
        String symbolicName;

        String version;

        int startLevel;

        public int compareTo(Object obj) {
            BundleSpec other = (BundleSpec) obj;

            // order by start level
            if (this.startLevel < other.startLevel) {
                return -1;
            } else if (this.startLevel > other.startLevel) {
                return 1;
            }

            // order by symbolic version if symbolic names are equal
            if (this.symbolicName.equals(other.symbolicName)) {
                if (this.version.equals(other.version)) {
                    return 0;
                }

                return this.version.compareTo(other.version);
            }

            // order by symbolic name
            return this.symbolicName.compareTo(other.symbolicName);
        }
    }
}