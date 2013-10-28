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
import java.util.Collection;
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
    
    public static final String INSTALL_PATH_PREFIX = "resources/install";
    public static final int BOOTSTRAP_DEF_START_LEVEL = -1;
    public static final int ACTUAL_BOOTSTRAP_START_LEVEL = 1;

    private final File resourceProviderRoot;
    private final static List<String> EMPTY_STRING_LIST = Collections.emptyList();
    
    BundleListContentProvider(File resourceProviderRoot) {
        this.resourceProviderRoot = resourceProviderRoot;
    }
    
    private Iterator<String> handleBundlePathRoot(String path) {
        final Set<String> levels = new HashSet<String>();
        for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
            // Include only bootstrap bundles here, with start level 1.
            // Other bundles go under the install folder, to support run modes
            if( level.getStartLevel() == BOOTSTRAP_DEF_START_LEVEL) {
                levels.add(BUNDLE_PATH_PREFIX + "/" + ACTUAL_BOOTSTRAP_START_LEVEL + "/");
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
    
    private Iterator<String> handleBundlesSubfolder(String path) {
        Iterator<String> result = null;
        final String startLevelInfo = path.substring(BUNDLE_PATH_PREFIX.length() + 1);
        try {
            final int startLevel = Integer.parseInt(startLevelInfo);
            
            // To be consistent with handleBundlePathRoot, consider only level 1 which
            // is assigned to bootstrap bundles
            if(startLevel == ACTUAL_BOOTSTRAP_START_LEVEL) {
                final List<String> bundles = new ArrayList<String>();
                addBundles(bundles, ACTUAL_BOOTSTRAP_START_LEVEL, null);
                addBundles(bundles, BOOTSTRAP_DEF_START_LEVEL, null);
                result = bundles.iterator();
            }

        } catch (NumberFormatException e) {
            getLog().warn("Invalid start level " + startLevelInfo + " in path " + path);
        }
        
        return result;
    }
    
    private void addBundles(Collection<String> bundles, int startLevel, String runMode) {
        for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
            if(level.getStartLevel() == startLevel) {
                for (final Bundle bundle : level.getBundles()) {
                    if(!runModeMatches(bundle, runMode)) {
                        continue;
                    }
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
    }
    
    private boolean runModeMatches(Bundle b, String runMode) {
        if(runMode == null || runMode.length() == 0) {
            return b.getRunModes() == null || b.getRunModes().length() == 0;
        } else {
            return b.getRunModes() != null && b.getRunModes().contains(runMode);
        }
    }
    
    private Iterator<String> handleResourcesRoot() {
        final Set<String> subDirs = new HashSet<String>();
        subDirs.add(BUNDLE_PATH_PREFIX);
        subDirs.add(CONFIG_PATH_PREFIX);
        subDirs.add("resources/corebundles");
        subDirs.add(INSTALL_PATH_PREFIX);
        
        // Compute the set of run modes in our bundles
        final Set<String> runModes = new HashSet<String>();
        for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
            for(Bundle bundle : level.getBundles()) {
                final String modes = bundle.getRunModes();
                if(modes != null && modes.length() > 0) {
                    for(String m : modes.split(",")) {
                        runModes.add("." + m);
                    }
                }
            }
        }
        
        // Add one install subdir per run mode
        for(String m : runModes) {
            subDirs.add(INSTALL_PATH_PREFIX + m);
        }
        return subDirs.iterator();
    }
    
    /** Add one folder per child, using given path as prefix, for start
     *  levels which actually provide bundles for the given run mode.
     */
    private void addStartLevelSubdirs(Collection<String> children, String path, String runMode) {
        for (final StartLevel level : getInitializedBundleList().getStartLevels()) {
            final List<String> bundles = new ArrayList<String>();
            addBundles(bundles, level.getStartLevel(), runMode);
            if(!bundles.isEmpty()) {
                int folderLevel = level.getStartLevel();
                if(folderLevel== BOOTSTRAP_DEF_START_LEVEL) {
                    folderLevel = ACTUAL_BOOTSTRAP_START_LEVEL;
                }
                children.add(path + "/" + folderLevel);
            }
        }
    }

    private Iterator<String> handleInstallPath(String path) {
        // Path is like
        // bundles/install.runMode/12
        // or a subset of that.
        // Extract optional run mode and start level from that
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final String [] parts = path.substring(INSTALL_PATH_PREFIX.length()).split("/");
        if (parts.length > 2){
            throw new IllegalStateException("Cannot parse path " + path);
        }
        final String runMode = parts[0].length() == 0 ? null : parts[0].substring(1);
        final String startLevelInfo = parts.length > 1 ? parts[1] : null; 
        Set<String> result = new HashSet<String>();
        
        if(runMode == null && startLevelInfo == null) {
            // Root folder: add one subdir per start level that provides bundles
            addStartLevelSubdirs(result, INSTALL_PATH_PREFIX, null);
            
        } else if(startLevelInfo == null) {
            // The root of a run mode folder - one subdir per start
            // level which actually provides bundles
            addStartLevelSubdirs(result, path, runMode);
            
        } else {
            // A folder that contains bundles
            try {
                addBundles(result, Integer.parseInt(startLevelInfo), runMode);
            } catch (NumberFormatException e) {
                getLog().warn("Invalid start level info " + startLevelInfo + " in path " + path);
            }
        }
        
        return result.iterator();
    }
    
    private Iterator<String> handleConfigSubpath(String path) {
        // We don't handle config subpaths for now, but do not 
        // warn if we're asked for the children of a file, just
        // return empty in that case
        final File f = getConfigFile(path);
        if(!f.exists()) {
            getLog().warn("BundleListContentProvider cannot get children of config path: " + path);
        }
        return EMPTY_STRING_LIST.iterator();
    }
    
    private File getConfigFile(String path) {
        return new File(getConfigDirectory(), path.substring(CONFIG_PATH_PREFIX.length() + 1));
    }

    public Iterator<String> getChildren(String path) {
        Iterator<String> result = null;
        if (path.equals(BUNDLE_PATH_PREFIX)) {
            result = handleBundlePathRoot(path);
        } else if (path.equals("resources/corebundles")) {
            result = EMPTY_STRING_LIST.iterator();
        } else if (path.equals(CONFIG_PATH_PREFIX)) {
            result = handleConfigPath();
        } else if (path.startsWith(CONFIG_PATH_PREFIX)) {
            result = handleConfigSubpath(path);
        } else if (path.startsWith(BUNDLE_PATH_PREFIX)) {
            result = handleBundlesSubfolder(path);
        } else if (path.startsWith(INSTALL_PATH_PREFIX)) {
            result = handleInstallPath(path);
        } else if (path.equals("resources") ) {
            result = handleResourcesRoot();
        } else if (path.startsWith("file:") ) {
            // Client looks for files under a file - we have none,
            // as our file URLs point to Maven artifacts
            result = EMPTY_STRING_LIST.iterator();
        } else {
            getLog().warn("BundleListContentProvider cannot get children of path: " + path);
        }

        return result;
    }

    public URL getResource(String path) {
        if (path.startsWith(CONFIG_PATH_PREFIX)) {
            final File configFile = getConfigFile(path);
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