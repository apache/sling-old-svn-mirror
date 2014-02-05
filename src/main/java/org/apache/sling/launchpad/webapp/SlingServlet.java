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
        SlingSessionListener.stopDelegatee();

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
        SlingSessionListener.stopDelegatee();
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
                SlingSessionListener.stopDelegatee();
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

            SlingSessionListener.startDelegate(sling.getClass().getClassLoader());

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
        slingHome = substVars(slingHome);
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

    /**
     * The starting delimiter of variable names (value is "${").
     */
    private static final String DELIM_START = "${";

    /**
     * The ending delimiter of variable names (value is "}").
     */
    private static final String DELIM_STOP = "}";

    private String substVars(final String val) {
        if (val.contains(DELIM_START)) {
            return substVars(val, null, null, properties);
        }

        return val;
    }

    /**
     * This method performs property variable substitution on the specified
     * value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property, then the
     * corresponding property value is substituted for the variable placeholder.
     * Multiple variable placeholders may exist in the specified value as well
     * as nested variable placeholders, which are substituted from inner most to
     * outer most. Configuration properties override system properties.
     *
     * NOTE - this is a verbatim copy of the same-named method
     * in o.a.s.launchpad.base.impl.Sling. Please keep them in sync.
     *
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to detect
     *            cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property
     *         substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *             property placeholder syntax or a recursive variable
     *             reference.
     */
    private static String substVars(String val, String currentKey,
            Map<String, String> cycleMap, Map<String, String> configProps)
            throws IllegalArgumentException {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null) {
            cycleMap = new HashMap<String, String>();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0) {
            int idx = val.indexOf(DELIM_START, startDelim
                + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim)) {
                break;
            } else if (idx < stopDelim) {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) && (stopDelim < 0)) {
            return val;
        }
        // At this point, we found a stop delimiter without a start,
        // so throw an exception.
        else if (((startDelim < 0) || (startDelim > stopDelim))
            && (stopDelim >= 0)) {
            throw new IllegalArgumentException(
                "stop delimiter with no start delimiter: " + val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable = val.substring(startDelim + DELIM_START.length(),
            stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null) {
            throw new IllegalArgumentException("recursive variable reference: "
                + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
                ? configProps.get(variable)
                : null;
        if (substValue == null) {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim) + substValue
            + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }
}
