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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.apache.sling.launchpad.base.shared.Util;

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
@SuppressWarnings("serial")
public class SlingServlet extends GenericServlet implements Notifiable {

    /**
     * The number times Sling will be tried to be started before giving up
     * (value is 20). This number is chosen deliberately as generally Sling
     * should start up smoothly. Whether any bundles within Sling start or not
     * is not counted here.
     */
    private static final int MAX_START_FAILURES = 20;

    /**
     * The name of the system property which may be set to define the default
     * prefix for the sling.home value generated from the Sling servlet context
     * path.
     *
     * @see #toSlingHome(String)
     */
    private static final String SLING_HOME_PREFIX = "sling.home.prefix";

    /**
     * The default value to be used as the prefix for the sling.home value
     * generated from the Sling servlet context path if the
     * {@link #SLING_HOME_PREFIX sling.home.prefix} system property is not set.
     *
     * @see #toSlingHome(String)
     */
    private static final String SLING_HOME_PREFIX_DEFAULT = "sling/";

    private Map<String, String> properties;

    private String slingHome;

    private Loader loader;

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
        this.properties = collectInitParameters();

        this.slingHome = getSlingHome(null);
        if (this.slingHome != null) {
            startSling();
        } else {
            log("Apache Sling cannot be started yet, because sling.home is not defined yet");
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

            // check for problematic application servers like WebSphere
            // where path info and servlet path is set wrong SLING-2410
            final HttpServletRequest request = (HttpServletRequest) req;
            if ( request.getPathInfo() == null && request.getServletPath() != null
                    && request.getServletPath().endsWith(".jsp") ) {
                req = new HttpServletRequestWrapper(request) {

                    @Override
                    public String getPathInfo() {
                        return request.getServletPath();
                    }

                    @Override
                    public String getServletPath() {
                        return "";
                    }

                };
            }
            delegatee.service(req, res);

        } else if (startFailureCounter > MAX_START_FAILURES) {

            // too many startup retries, fail for ever
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);

        } else {

            startSling(req);

            ((HttpServletResponse) res).sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Apache Sling is currently starting up, please try again");
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

        // clear fields
        slingHome = null;
        loader = null;
        sling = null;
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
    @Override
    public void stopped() {
        /**
         * This method is called if the framework is stopped from within by
         * calling stop on the system bundle or if the framework is stopped
         * because the VM is going down and the shutdown hook has initated the
         * shutdown In any case we ensure the reference to the framework is
         * removed and remove the shutdown hook (but don't care if that fails).
         */

        log("Apache Sling has been stopped");

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
    @Override
    public void updated(File updateFile) {

        // drop the sling reference to be able to restart
        synchronized (this) {
            if (startingSling == null) {
                sling = null;
            }
        }

        // ensure we have a VM as clean as possible
        loader.cleanupVM();

        if (updateFile == null) {

            log("Restarting Framework and Apache Sling");
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
            Thread starter = new Thread(new Runnable() {
                @Override
                public void run() {
                    startSling();
                }
            }, "SlingStarter_" + System.currentTimeMillis());

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
            File launchpadHome = getLaunchpadHome(slingHome);
            this.loader = new Loader(launchpadHome) {
                @Override
                protected void info(String msg) {
                    log(msg);
                }
            };
        } catch (IllegalArgumentException iae) {
            startupFailure(null, iae);
            return;
        }

        try {
            URL launcherJar = getServletContext().getResource(
                SharedConstants.DEFAULT_SLING_LAUNCHER_JAR);
            if (launcherJar == null) {
                launcherJar = getServletContext().getResource(
                    "/WEB-INF" + SharedConstants.DEFAULT_SLING_LAUNCHER_JAR);
            }

            startSling(launcherJar);
        } catch (MalformedURLException mue) {
            log("Cannot load Apache Sling Launcher JAR "
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
                log("Apache Sling already started, nothing to do");
                return;
            } else if (startingSling != null) {
                log("Apache Sling being started by Thread " + startingSling);
                return;
            }

            startingSling = Thread.currentThread();
        }

        if (launcherJar != null) {
            try {
                log("Checking launcher JAR in " + slingHome);
                loader.installLauncherJar(launcherJar);
            } catch (IOException ioe) {
                startupFailure("Failed installing " + launcherJar, ioe);
                return;
            }
        } else {
            log("No Launcher JAR to install");
        }

        Object object = null;
        try {
            object = loader.loadLauncher(SharedConstants.DEFAULT_SLING_SERVLET);
        } catch (IllegalArgumentException iae) {
            startupFailure("Cannot load Launcher Servlet "
                + SharedConstants.DEFAULT_SLING_SERVLET, iae);
            return;
        }

        if (object instanceof Servlet) {
            Servlet sling = (Servlet) object;

            if (sling instanceof Launcher) {
                Launcher slingLauncher = (Launcher) sling;
                slingLauncher.setNotifiable(this);
                slingLauncher.setCommandLine(properties);
                slingLauncher.setSlingHome(slingHome);
            }

            try {
                log("Starting launcher ...");
                sling.init(getServletConfig());
                this.sling = sling;
                this.startFailureCounter = 0;
                log("Startup completed");
            } catch (ServletException se) {
                startupFailure(null, se);
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
     * <p>
     * If <code>sling.home</code> can be retrieved, it is returned as an
     * absolute path.
     *
     * @param args The command line arguments
     * @return The value to use for sling.home or <code>null</code> if the value
     *         cannot be retrieved.
     */
    private String getSlingHome(HttpServletRequest request) {

        String source = null;

        // access config and context to be able to log the sling.home source

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

        // substitute any ${...} references and make absolute
        slingHome = Util.substVars(slingHome, SharedConstants.SLING_HOME, null, properties);
        slingHome = new File(slingHome).getAbsolutePath();

        log("Setting sling.home=" + slingHome + " (" + source + ")");
        return slingHome;
    }

    /**
     * Define the sling.launchpad parameter implementing the algorithme defined
     * on the wiki page to find the setting according to this algorithm:
     * <ol>
     * <li>Servlet init parameter <code>sling.launchpad</code>. This path is
     * resolved against the <code>slingHome</code> folder if relative.</li>
     * <li>Servlet context init parameter <code>sling.launchpad</code>. This
     * path is resolved against the <code>slingHome</code> folder if relative.</li>
     * <li>Default to same as <code>sling.home</code></li>
     * </ol>
     * <p>
     * The absolute path of the returned file is stored as the
     * <code>sling.launchpad</code> property in the {@link #properties} map.
     *
     * @param slingHome The absolute path to the Sling Home folder (aka the
     *            <code>sling.home</code>.
     * @return The absolute <code>File</code> indicating the launchpad folder.
     */
    private File getLaunchpadHome(final String slingHome) {
        String launchpadHomeParam = properties.get(SharedConstants.SLING_LAUNCHPAD);
        if (launchpadHomeParam == null || launchpadHomeParam.length() == 0) {
            properties.put(SharedConstants.SLING_LAUNCHPAD, slingHome);
            return new File(slingHome);
        }

        File launchpadHome = new File(launchpadHomeParam);
        if (!launchpadHome.isAbsolute()) {
            launchpadHome = new File(slingHome, launchpadHomeParam);
        }

        properties.put(SharedConstants.SLING_LAUNCHPAD,
            launchpadHome.getAbsolutePath());
        return launchpadHome;
    }

    /**
     * Converts the servlet context path to a path used for the sling.home
     * property. The servlet context path is converted to a simple name by
     * replacing all slash characters in the path with underscores (or a single
     * underscore for the root context path being empty or null). Next the
     * result is prefixed with either value of the
     * <code>sling.home.prefix</code> system property or the (default prefix)
     * <code>sling/</code>.
     *
     * @param contextPath
     * @return
     */
    private String toSlingHome(String contextPath) {
        String prefix = System.getProperty(SLING_HOME_PREFIX,
            SLING_HOME_PREFIX_DEFAULT);

        if (!prefix.endsWith("/")) {
            prefix = prefix.concat("/");
        }

        if (contextPath == null || contextPath.length() == 0) {
            return prefix + "_";
        }

        return prefix + contextPath.replace('/', '_');
    }

    private void startupFailure(String message, Throwable cause) {

        // ensure message
        if (message == null) {
            message = "Failed to start Apache Sling in " + slingHome;
        }

        // unwrap to get the real cause
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        // log it now and increase the failure counter
        log(message, cause);
        startFailureCounter++;

        // ensure the startingSling fields is not set
        synchronized (this) {
            startingSling = null;
        }
    }

    // ---------- Property file variable substition support --------------------

    private Map<String, String> collectInitParameters() {
        HashMap<String, String> props = new HashMap<String, String>();
        for (Enumeration<String> keys = getServletContext().getInitParameterNames(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            props.put(key, getServletContext().getInitParameter(key));
        }
        for (Enumeration<String> keys = getServletConfig().getInitParameterNames(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            props.put(key, getServletConfig().getInitParameter(key));
        }
        return props;
    }

}
