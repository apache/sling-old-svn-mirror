/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.webapp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;

/**
 * The <code>SlingServlet</code> is the externally visible Web Application
 * launcher for Sling. Please refer to the full description <i>The Sling
 * Launchpad</i> on the Sling Wiki for a full description of this class.
 * <p>
 * Logging goes to ServletContext.log methods.
 * <p>
 * This class goes into the secondary artifact with the classifier <i>webapp</i>
 * to be used as the main servlet to be registered in the servlet container.
 * 
 * @see <a href="http://cwiki.apache.org/SLING/the-sling-launchpad.html">The
 *      Sling Launchpad</a>
 */
public class SlingServlet extends GenericServlet implements Notifiable {

    /**
     * The number times Sling will be tried to be started before giving up
     * (value is 20). This number is chosen deliberately as generally Sling
     * should start up smoothly. Whether any bundles within Sling start or not
     * is not counted here.
     */
    private static final int MAX_START_FAILURES = 20;

    private String slingHome;

    private Servlet sling;

    /**
     * Field managed by the {@link #startSling(String)} method to indicate
     * whether Sling is in the process of being started.
     */
    private Thread startingSling;

    /**
     * Counter to count the number of failed startups. After this number
     * expires, the SlingServlet will not try to start Sling any more.
     */
    private int startFailureCounter = 0;

    // ---------- GenericServlet

    /**
     * Launches the SLing framework if the sling.home setting can be derived
     * from the configuration or the SerlvetContext. Otherwise Sling is not
     * started yet and will be started when the first request comes in.
     */
    @Override
    public void init() {

        slingHome = getSlingHome(null);
        if (slingHome != null) {
            startSling();
        } else {
            log("Sling cannot be started yet, because sling.home is not defined yet");
        }

        log("Servlet " + getServletName() + " initialized");
    }

    @Override
    public String getServletInfo() {
        if (sling != null) {
            return sling.getServletInfo();
        }

        return "Sling Launchpad Proxy";
    }

    /**
     * If Sling has already been started, the request is forwarded to the
     * started Sling framework. Otherwise the Sling framework is started unless
     * there were too many startup failures.
     * <p>
     * If the request is not forwarded to Sling, this method returns a 404/NOT
     * FOUND if the startup failure counter has exceeded or 503/SERVICE
     * UNAVAILABLE if the Sling framework is starting up.
     * <p>
     * If a request causes the framework to start, it is immediately terminated
     * with said response status and framework is started in a separate thread.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        // delegate the request to the registered delegatee servlet
        Servlet delegatee = sling;
        if (delegatee != null) {

            delegatee.service(req, res);

        } else if (startFailureCounter > MAX_START_FAILURES) {

            // too many startup retries, fail for ever
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);

        } else {

            startSling(req);

            ((HttpServletResponse) res).sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Sling is currently starting up, please try again");
        }
    }

    /**
     * Stop the Sling framework when the web application is being stopped
     */
    @Override
    public void destroy() {
        if (sling != null) {
            sling.destroy();
        }
    }

    // ---------- Notifiable interface

    /**
     * The framework has been stopped by calling the <code>Bundle.stop()</code>
     * on the system bundle. This actually terminates the Sling Standalone
     * application.
     * <p>
     * Note, that a new request coming in while the web application is still
     * running, will actually cause Sling to restart !
     */
    public void stopped() {
        /**
         * This method is called if the framework is stopped from within by
         * calling stop on the system bundle or if the framework is stopped
         * because the VM is going down and the shutdown hook has initated the
         * shutdown In any case we ensure the reference to the framework is
         * removed and remove the shutdown hook (but don't care if that fails).
         */

        log("Sling has been stopped");

        // clear the reference to the framework
        sling = null;
    }

    /**
     * The framework has been stopped with the intent to be restarted by calling
     * either of the <code>Bundle.update</code> methods on the system bundle.
     * <p>
     * If an <code>InputStream</code> was provided, this has been copied to a
     * temporary file, which will be used in place of the existing launcher jar
     * file.
     * 
     * @param updateFile The temporary file to replace the existing launcher jar
     *            file. If <code>null</code> the existing launcher jar will be
     *            used again.
     */
    public void updated(File updateFile) {

        // drop the sling reference to be able to restart
        synchronized (this) {
            if (startingSling == null) {
                sling = null;
            }
        }

        if (updateFile == null) {

            log("Restarting Framework and Sling");
            startSling((URL) null);

        } else {

            log("Restarting Framework with update from " + updateFile);
            try {
                startSling(updateFile.toURI().toURL());
            } catch (MalformedURLException mue) {
                log("Cannot get URL for file " + updateFile, mue);
            } finally {
                updateFile.delete();
            }

        }
    }

    // --------- internal

    /**
     * If Sling is not currently starting up, a thread is started to start Sling
     * in the background.
     */
    private void startSling(final ServletRequest request) {
        if (startingSling == null) {
            slingHome = getSlingHome((HttpServletRequest) request);
            Thread starter = new Thread("SlingStarter_"
                + System.currentTimeMillis()) {
                @Override
                public void run() {
                    startSling();
                }
            };

            starter.setDaemon(true);
            starter.start();
        }
    }

    /**
     * Called from the startup thread initiated by a request or from
     * {@link #init()} to install the launcher jar and actually start sling.
     */
    private void startSling() {
        try {
            URL launcherJar = getServletContext().getResource(
                SharedConstants.DEFAULT_SLING_LAUNCHER_JAR);
            if (launcherJar == null) {
                launcherJar = getServletContext().getResource(
                    "/WEB-INF/" + SharedConstants.DEFAULT_SLING_LAUNCHER_JAR);
            }

            startSling(launcherJar);
        } catch (MalformedURLException mue) {
            log("Cannot load Sling Launcher JAR "
                + SharedConstants.DEFAULT_SLING_LAUNCHER_JAR, mue);
        }
    }

    /**
     * Installs the launcher jar from the given URL (if not <code>null</code>)
     * and launches Sling from that launcher.
     */
    private void startSling(URL launcherJar) {
        synchronized (this) {
            if (sling != null) {
                log("Sling already started, nothing to do");
                return;
            } else if (startingSling != null) {
                log("Sling being started by Thread " + startingSling);
                return;
            }

            startingSling = Thread.currentThread();
        }

        if (launcherJar != null) {
            try {
                log("Checking launcher JAR in " + slingHome);
                if (Loader.installLauncherJar(launcherJar, slingHome)) {
                    log("Installed or Updated launcher JAR file from " + launcherJar);
                } else {
                    log("Existing launcher JAR file is already up to date");
                }
            } catch (IOException ioe) {
                log("Failed installing " + launcherJar, ioe);
            }
        } else {
            log("No Launcher JAR to install");
        }

        Object object = Loader.loadLauncher(
            SharedConstants.DEFAULT_SLING_SERVLET, slingHome);
        try {
            log("Loading launcher class "
                + SharedConstants.DEFAULT_SLING_SERVLET);
            object = Loader.loadLauncher(SharedConstants.DEFAULT_SLING_SERVLET,
                slingHome);
        } catch (IllegalArgumentException iae) {
            log("Cannot load Launcher Servlet "
                + SharedConstants.DEFAULT_SLING_SERVLET, iae);
            return;
        }

        if (object instanceof Servlet) {
            Servlet sling = (Servlet) object;

            if (sling instanceof Launcher) {
                Launcher slingLauncher = (Launcher) sling;
                slingLauncher.setNotifiable(this);
                slingLauncher.setSlingHome(slingHome);
            }

            try {
                log("Starting launcher ...");
                sling.init(getServletConfig());
                this.sling = sling;
                this.startFailureCounter = 0;
                log("Startup completed");
            } catch (ServletException se) {
                Throwable cause = se.getCause();
                if (cause == null) {
                    cause = se;
                }

                log("Failed to start Sling in " + slingHome, cause);
                startFailureCounter++;
            }
        }

        // reset the starting flag
        synchronized (this) {
            startingSling = null;
        }
    }

    /**
     * Define the sling.home parameter implementing the algorithme defined on
     * the wiki page to find the setting according to this algorithm:
     * <ol>
     * <li>Servlet parameter <code>sling.home</code></li>
     * <li>Context <code>sling.home</code></li>
     * <li>Derived from ServletContext path</li>
     * </ol>
     * <p>
     * <code>null</code> may be returned by this method if no
     * <code>sling.home</code> parameter is set and if the servlet container
     * does not provide the Servlet API 2.5
     * <code>ServletContext.getContextPath()</code> method and the
     * <code>request</code> parameter is <code>null</code>.
     * 
     * @param args The command line arguments
     * @return The value to use for sling.home or <code>null</code> if the value
     *         cannot be retrieved.
     */
    private String getSlingHome(HttpServletRequest request) {

        String source = null;

        // 1. servlet config parameter
        String slingHome = getServletConfig().getInitParameter(
            SharedConstants.SLING_HOME);
        if (slingHome != null) {

            source = "servlet parameter sling.home";

        } else {

            // 2. servlet context parameter
            slingHome = getServletContext().getInitParameter(
                SharedConstants.SLING_HOME);
            if (slingHome != null) {

                source = "servlet context parameter sling.home";

            } else {

                // 3. servlet context path (Servlet API 2.5 and later)
                try {

                    String contextPath = getServletContext().getContextPath();
                    slingHome = toSlingHome(contextPath);
                    source = "servlet context path";

                } catch (NoSuchMethodError nsme) {

                    // 4.servlet context path (Servlet API 2.4 and earlier)
                    if (request != null) {

                        String contextPath = request.getContextPath();
                        slingHome = toSlingHome(contextPath);
                        source = "servlet context path (from request)";

                    } else {

                        log("ServletContext path not available here, delaying startup until first request");
                        return null;

                    }
                }

            }
        }

        log("Setting sling.home=" + slingHome + " (" + source + ")");
        return slingHome;
    }

    // convert the servlet context path to a directory path for sling.home
    private String toSlingHome(String contextPath) {
        String prefix = "sling/";
        if (contextPath == null || contextPath.length() == 0) {
            return prefix + "_";
        }

        return prefix + contextPath.replace('/', '_');
    }

}
