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

import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.framework.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.PropertyUtils;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.osgi.framework.BundleException;

public abstract class AbstractLaunchpadStartingMojo extends AbstractUsingBundleListMojo implements Notifiable {

    /** Default log level setting if no set on command line (value is "INFO"). */
    private static final int DEFAULT_LOG_LEVEL = Logger.LOG_INFO;

    /** Mapping between log level numbers and names */
    private static final String[] logLevels = { "FATAL", "ERROR", "WARN", "INFO", "DEBUG" };

    /**
     * The configuration property setting the port on which the HTTP service
     * listens
     */
    private static final String PROP_PORT = "org.osgi.service.http.port";

    /** Return the log level code for the string */
    private static int toLogLevelInt(String level, int defaultLevel) {
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equalsIgnoreCase(level)) {
                return i;
            }
        }

        return defaultLevel;
    }

    /**
     * @parameter expression="${http.port}" default-value="8080"
     */
    private int httpPort;

    /**
     * The definition of the package to be included to provide web support for
     * JAR-packaged projects (i.e. pax-web).
     */
    @Parameter
    private ArtifactDefinition jarWebSupport;

    @Parameter(property = "felix.log.level")
    private String logLevel;

    @Parameter(property = "propertiesFile", defaultValue = "src/test/config/sling.properties")
    private File propertiesFile;

    @Parameter(property = "resourceProviderRoot", defaultValue = "src/test/resources")
    private File resourceProviderRoot;

    private LaunchpadContentProvider resourceProvider = new BundleListContentProvider(resourceProviderRoot) {

        @Override
        BundleList getInitializedBundleList() {
            return AbstractLaunchpadStartingMojo.this.getInitializedBundleList();
        }

        @Override
        File getConfigDirectory() {
            return AbstractLaunchpadStartingMojo.this.getConfigDirectory();
        }
        
        @Override
        Artifact getArtifact(ArtifactDefinition def) throws MojoExecutionException {
            return AbstractLaunchpadStartingMojo.this.getArtifact(def);
        }
        
        @Override
        Log getLog() {
            return AbstractLaunchpadStartingMojo.this.getLog();
        }
    };

    private Sling sling;

    /**
     * @parameter expression="${sling.home}" default-value="${basedir}/sling"
     */
    private String slingHome;

    /**
     * @parameter default-value="true"
     */
    private boolean forceBundleLoad;

    public void stopped() {
        sling = null;
    }

    public void updated(File updateFile) {
        // clear the reference to the framework
        sling = null;

        if (updateFile != null) {
            getLog().warn("Maven Launchpad Plugin doesn't support updating the framework bundle.");
        }

        getLog().info("Restarting Framework and Sling");

        try {
            executeWithArtifacts();
        } catch (MojoExecutionException e) {
            getLog().error("Unable to restart Framework and Sling", e);
            System.exit(1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeWithArtifacts() throws MojoExecutionException {
        try {
            final Map<String, String> props = new HashMap<String, String>();

            props.put(SharedConstants.SLING_HOME, slingHome);

            // ensure launchpad is set
            props.put(SharedConstants.SLING_LAUNCHPAD, slingHome);

            if (forceBundleLoad) {
                props.put(SharedConstants.FORCE_PACKAGE_BUNDLE_LOADING, "true");
            }

            // set up and configure Felix Logger
            int logLevelNum;
            if (logLevel == null) {
                logLevelNum = DEFAULT_LOG_LEVEL;
            } else {
                logLevelNum = toLogLevelInt(logLevel, DEFAULT_LOG_LEVEL);
            }
            props.put(LOG_LEVEL_PROP, String.valueOf(logLevelNum));
            // Display port number on console, in case HttpService doesn't
            getLog().info("HTTP server port: " + httpPort);
            props.put(PROP_PORT, String.valueOf(httpPort));

            // prevent tons of needless WARN from the framework
            Logger logger = new Logger();
            logger.setLogLevel(Logger.LOG_ERROR);

            if (propertiesFile.exists()) {
                File tmp = null;
                try {
                    tmp = File.createTempFile("sling", "props");
                    mavenFileFilter.copyFile(propertiesFile, tmp, true, project, Collections.EMPTY_LIST, true,
                            System.getProperty("file.encoding"), mavenSession);
                    Properties loadedProps = PropertyUtils.loadPropertyFile(tmp, null);
                    for (Object key : loadedProps.keySet()) {
                        props.put((String) key, (String) loadedProps.get(key));
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to create filtered properties file", e);
                } catch (MavenFilteringException e) {
                    throw new MojoExecutionException("Unable to create filtered properties file", e);
                } finally {
                    if (tmp != null) {
                        tmp.delete();
                    }
                }
            }

            sling = startSling(resourceProvider, props, logger);

        } catch (BundleException be) {
            getLog().error("Failed to Start OSGi framework", be);
        }

    }

    protected abstract Sling startSling(LaunchpadContentProvider resourceProvider, Map<String, String> props,
            Logger logger) throws BundleException;

    protected void stopSling() {
        if (sling != null) {
            sling.destroy();
        }
    }

    @Override
    protected void initArtifactDefinitions(Properties dependencies) {
        if (jarWebSupport == null) {
            jarWebSupport = new ArtifactDefinition();
        }
        jarWebSupport.initDefaults(dependencies.getProperty("jarWebSupport"));
    }

    /**
     * Add the JAR Web Support bundle to the bundle list.
     */
    @Override
    protected void initBundleList(BundleList bundleList) {
        bundleList.add(jarWebSupport.toBundleList());
    }
}
