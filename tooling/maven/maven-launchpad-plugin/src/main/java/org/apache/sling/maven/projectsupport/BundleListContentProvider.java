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

import static org.apache.sling.maven.projectsupport.AbstractUsingBundleListMojo.BUNDLE_PATH_PREFIX;
import static org.apache.sling.maven.projectsupport.AbstractUsingBundleListMojo.CONFIG_PATH_PREFIX;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;

/** LaunchpadContentProvider that provides resources based on a BundleList
 *  and other resources specific to this module.
 */
abstract class BundleListContentProvider implements LaunchpadContentProvider {
    
    private final File resourceProviderRoot;
    private final static List<String> EMPTY_STRING_LIST = Collections.emptyList();
    
    BundleListContentProvider(File resourceProviderRoot) {
        this.resourceProviderRoot = resourceProviderRoot;
    }
    
    private Iterator<String> handleBundlePathRoot(String path) {
        final Set<String> levels = new HashSet<String>();
        for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
            // we treat the boot level as level 1
            if ( level.getStartLevel() == -1 ) {
                levels.add(BUNDLE_PATH_PREFIX + "/1/");
            } else {
                levels.add(BUNDLE_PATH_PREFIX + "/" + level.getLevel() + "/");
            }
        }
        return levels.iterator();
    }
    
    private Iterator<String> handleConfigPath() {
        if (getConfigDirectory().exists() && getConfigDirectory().isDirectory()) {
            File[] configFiles = getConfigDirectory().listFiles(new FileFilter() {

                public boolean accept(File file) {
                    return file.isFile();
                }
            });

            List<String> fileNames = new ArrayList<String>();
            for (File cfgFile : configFiles) {
                if (cfgFile.isFile()) {
                    fileNames.add(CONFIG_PATH_PREFIX + "/" + cfgFile.getName());
                }
            }

            return fileNames.iterator();

        } else {
            return EMPTY_STRING_LIST.iterator();
        }
    }
    
    private Iterator<String> handleBundlePathFolder(String path) {
        final String startLevelInfo = path.substring(BUNDLE_PATH_PREFIX.length() + 1);
        try {
            final int startLevel = Integer.parseInt(startLevelInfo);

            final List<String> bundles = new ArrayList<String>();
            for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
                if (level.getStartLevel() == startLevel || (startLevel == 1 && level.getStartLevel() == -1)) {
                    for (final Bundle bundle : level.getBundles()) {
                        final ArtifactDefinition d = new ArtifactDefinition(bundle, startLevel);
                        try {
                            final Artifact artifact = getArtifact(d);
                            bundles.add(artifact.getFile().toURI().toURL().toExternalForm());
                        } catch (Exception e) {
                            getLog().error("Unable to resolve artifact ", e);
                        }
                    }
                }
            }
            return bundles.iterator();

        } catch (NumberFormatException e) {
            // we ignore this
        }
        return null;
    }
    
    private Iterator<String> handleResourcesRoot() {
        final Set<String> subDirs = new HashSet<String>();
        subDirs.add(BUNDLE_PATH_PREFIX);
        subDirs.add(CONFIG_PATH_PREFIX);
        subDirs.add("resources/corebundles");
        return subDirs.iterator();
    }

    public Iterator<String> getChildren(String path) {
        Iterator<String> result = null;
        if (path.equals(BUNDLE_PATH_PREFIX)) {
            result = handleBundlePathRoot(path);
        } else if (path.equals("resources/corebundles")) {
            result = EMPTY_STRING_LIST.iterator();
        } else if (path.equals(CONFIG_PATH_PREFIX)) {
            result = handleConfigPath();
        } else if (path.startsWith(BUNDLE_PATH_PREFIX)) {
            result = handleBundlePathFolder(path);
        } else if (path.equals("resources") ) {
            result = handleResourcesRoot();
        } else {
            getLog().warn("un-handlable " + getClass().getSimpleName() + " path: " + path);
        }

        return result;
    }

    public URL getResource(String path) {
        if (path.startsWith(CONFIG_PATH_PREFIX)) {
            File configFile = new File(getConfigDirectory(), path.substring(CONFIG_PATH_PREFIX.length() + 1));
            if (configFile.exists()) {
                try {
                    return configFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    // ignore this one
                }
            }
        }

        File resourceFile = new File(resourceProviderRoot, path);
        if (resourceFile.exists()) {
            try {
                return resourceFile.toURI().toURL();
            } catch (MalformedURLException e) {
                getLog().error("Unable to create URL for file", e);
                return null;
            }
        } else {
            URL fromClasspath = getClass().getResource("/" + path);
            if (fromClasspath != null) {
                return fromClasspath;
            }

            try {
                return new URL(path);
            } catch (MalformedURLException e) {
                return null;
            }
        }

    }

    public InputStream getResourceAsStream(String path) {
        URL res = this.getResource(path);
        if (res != null) {
            try {
                return res.openStream();
            } catch (IOException ioe) {
                // ignore this one
            }
        }

        // no resource
        return null;
    }
    
    abstract BundleList getInitializedBundleList();
    
    abstract File getConfigDirectory();
    
    abstract Artifact getArtifact(ArtifactDefinition def) throws MojoExecutionException;
    
    abstract Log getLog();
}