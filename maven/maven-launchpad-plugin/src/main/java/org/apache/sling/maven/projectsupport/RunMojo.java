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

import static org.apache.felix.framework.util.FelixConstants.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.framework.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.sling.launchpad.base.impl.ResourceProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.osgi.framework.BundleException;

/**
 * Run a Launchpad application.
 *
 * @goal run
 * @requiresDependencyResolution test
 *
 */
public class RunMojo extends AbstractBundleListMojo implements Notifiable {

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
     *
     * @parameter
     */
    private ArtifactDefinition jarWebSupport;

    /**
     * @parameter expression="${felix.log.level}"
     */
    private String logLevel;

    private ResourceProvider resourceProvider = new ResourceProvider() {

        @Override
        public Iterator<String> getChildren(String path) {
            if (path.equals("resources/bundles")) {
                List<String> levels = new ArrayList<String>();
                for (StartLevel level : getBundleList().getStartLevels()) {
                    levels.add(String.valueOf(level.getLevel()) + "/");
                }
                return levels.iterator();
            } else if (path.equals("resources/corebundles")) {
                List<String> empty = Collections.emptyList();
                return empty.iterator();
            } else {
                try {
                    int i = Integer.parseInt(path);
                    List<String> bundles = new ArrayList<String>();
                    for (StartLevel level : getBundleList().getStartLevels()) {
                        if (level.getLevel() == i) {
                            for (Bundle bundle : level.getBundles()) {
                                ArtifactDefinition d = new ArtifactDefinition(bundle, i);
                                try {
                                    Artifact artifact = getArtifact(d);
                                    bundles.add(artifact.getFile().toURI().toURL().toExternalForm());
                                } catch (Exception e) {
                                    getLog().error("Unable to resolve artifact ", e);
                                }
                            }

                            break;
                        }
                    }
                    return bundles.iterator();

                } catch (NumberFormatException e) {
                    getLog().warn("un-handlable path " + path);
                    return null;

                }
            }
        }

        @Override
        public URL getResource(String path) {
            if (path.endsWith(".properties")) {
                return getClass().getResource("/" + path);
            } else {
                try {
                    return new URL(path);
                } catch (MalformedURLException e) {
                    getLog().error("Expecting a real URL", e);
                    return null;
                }
            }
        }
    };

    private Thread shutdown = new Thread() {
        /**
         * Called when the Java VM is being terminiated, for example because the
         * KILL signal has been sent to the process. This method calls stop on
         * the launched Sling instance to terminate the framework before
         * returning.
         */
        @Override
        public void run() {
            getLog().info("Java VM is shutting down", null);
            shutdown();
        }

        // ---------- Shutdown support for control listener and shutdown hook

        void shutdown() {
            // remove the shutdown hook, will fail if called from the
            // shutdown hook itself. Otherwise this prevents shutdown
            // from being called again
            try {
                Runtime.getRuntime().removeShutdownHook(this);
            } catch (Throwable t) {
                // don't care for problems removing the hook
            }

            // now really shutdown sling
            if (sling != null) {
                getLog().info("Stopping Sling", null);
                sling.destroy();
            }
        }
    };

    private Sling sling;

    /**
     * @parameter expression="${sling.home}" default-value="sling"
     */
    private String slingHome;

    /**
     * @parameter default-value="true"
     */
    private boolean forceBundleLoad;

    public void stopped() {
        sling = null;
    }

    public void updated(File tmpFile) {
        // TODO - should anything happen here?
        getLog().info("File updated " + tmpFile.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        Runtime.getRuntime().addShutdownHook(shutdown);

        try {
            final Map<String, String> props = new HashMap<String, String>();

            props.put(SharedConstants.SLING_HOME, slingHome);

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

            // creating the instance launches the framework and we are done here
            sling = new Sling(this, logger, resourceProvider, props);

            // TODO this seems hacky!
            while (sling != null) {
                Thread.sleep(100);
            }

        } catch (BundleException be) {
            getLog().error("Failed to Start OSGi framework", be);
        } catch (InterruptedException e) {
        }

    }

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
        bundleList.add(jarWebSupport.toBundle());
    }

}
