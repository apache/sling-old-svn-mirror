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
package org.apache.sling.launcher.webapp;

import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.framework.Logger;
import org.apache.sling.launcher.app.ClassLoaderResourceProvider;
import org.apache.sling.launcher.app.ResourceProvider;
import org.apache.sling.launcher.app.Sling;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * The <code>SlingServlet</code> serves as a basic servlet for Project Sling.
 * The tasks of this servlet are as follows:
 * <ul>
 * <li>The {@link #init()} method launches Apache <code>Felix</code> as the
 * OSGi framework implementation we use.
 * <li>Registers as a service listener interested for services of type
 * <code>javax.servlet.Servlet</code>.
 * <li>Handles requests by delegating to a servlet which is expected to be
 * registered with the framework as a service of type
 * <code>javax.servlet.Servlet</code>. If no delegatee servlet has been
 * registered request handlings results in a temporary unavailability of the
 * servlet.
 * </ul>
 * <p>
 * <b>Request Handling</b>
 * <p>
 * This servlet handles request by forwarding to a delegatee servlet. The
 * delegatee servlet is automatically retrieved from the service registry by the
 * {@link #getDelegatee()}. This method also makes sure, the such a servlet
 * actually exits by throwing an <code>UnvailableException</code> if not and
 * also makes sure the servlet is initialized.
 * <p>
 * <b>Launch Configuration</b>
 * <p>
 * The Apache <code>Felix</code> framework requires configuration parameters
 * to be specified for startup. This servlet builds the list of parameters from
 * three locations:
 * <ol>
 * <li>The <code>com/day/osgi/servlet/SlingServlet.properties</code> is read
 * from the servlet class path. This properties file contains default settings.</li>
 * <li>Extensions of this servlet may provide additional properties to be
 * loaded overwriting the {@link #loadConfigProperties()} method.
 * <li>Finally, web application init parameters are added to the properties and
 * may overwrite existing properties of the same name(s).
 * </ol>
 * <p>
 * After loading all properties, variable substitution takes place on the
 * property values. A variable is indicated as <code>${&lt;prop-name&gt;}</code>
 * where <code>&lt;prop-name&gt;</code> is the name of a system or
 * configuration property (configuration properties override system properties).
 * Variables may be nested and are resolved from inner-most to outer-most. For
 * example, the property value <code>${outer-${inner}}</code> is resolved by
 * first resolving <code>${inner}</code> and then resolving the property whose
 * name is the catenation of <code>outer-</code> and the result of resolving
 * <code>${inner}</code>.
 * <p>
 * <b>Logging</b>
 * <p>
 * This servlet logs through the servlet container logging mechanism by calling
 * the <code>GenericServlet.log</code> methods. Bundles launched within the
 * framework provided by this servlet may use whatever logging mechanism they
 * choose to use. The Day Commons OSGI Log Bundle provides an OSGi Log Service
 * implementation, which also provides access to Apache Commons Logging, SLF4J
 * and Log4J logging. It is recommended that this bundle is used to setup and
 * configure logging for systems based on this servlet.
 */
public class SlingServlet extends GenericServlet {

    /** Pseduo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /** Mapping between log level numbers and names */
    private static final String[] logLevels = { "FATAL", "ERROR", "WARN",
        "INFO", "DEBUG" };

    /**
     * The Sling configuration property name setting the initial log level
     * (corresponds to LogbackManager.LOG_LEVEL constant)
     */
    private static final String PROP_LOG_LEVEL = "org.apache.sling.commons.log.level";

    /**
     * The name of the configuration property defining the obr repository.
     */
    private static final String OBR_REPOSITORY_URL = "obr.repository.url";

    /**
     * The <code>Felix</code> instance loaded on {@link #init()} and stopped
     * on {@link #destroy()}.
     */
    private SlingBridge sling;

    /**
     * The map of delegatee servlets to which requests are delegated. This map
     * is managed through the
     * {@link #serviceChanged(ServiceEvent) service listener} based on servlets
     * registered.
     *
     * @see #getDelegatee()
     * @see #ungetDelegatee(Object)
     */
    private Servlet delegatee;

    /**
     * Initializes this servlet by loading the framework configuration
     * properties, starting the OSGi framework (Apache Felix) and exposing the
     * system bundle context and the <code>Felix</code> instance as servlet
     * context attributes.
     *
     * @throws ServletException if the framework cannot be initialized.
     */
    public final void init() throws ServletException {
        super.init();

        // read the default parameters
        Map<String, String> props = loadConfigProperties();

        try {
            Logger logger = new ServletContextLogger(getServletContext());
            ResourceProvider rp = new ServletContextResourceProvider(
                getServletContext());
            sling = new SlingBridge(logger, rp, props);
        } catch (Exception ex) {
            log("Cannot start the OSGi framework", ex);
            throw new UnavailableException("Cannot start the OSGi Framework: "
                + ex);
        }

        // set up the OSGi HttpService proxy servlet
        delegatee = new HttpServiceServlet();
        delegatee.init(getServletConfig());

        log("Servlet " + getServletName() + " initialized");
    }

    /**
     * Services the request by delegating to the delegatee servlet. If no
     * delegatee servlet is available, a <code>UnavailableException</code> is
     * thrown.
     *
     * @param req the <code>ServletRequest</code> object that contains the
     *            client's request
     * @param res the <code>ServletResponse</code> object that will contain
     *            the servlet's response
     * @throws UnavailableException if the no delegatee servlet is currently
     *             available
     * @throws ServletException if an exception occurs that interferes with the
     *             servlet's normal operation occurred
     * @throws IOException if an input or output exception occurs
     */
    public final void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        // delegate the request to the registered delegatee servlet
        Servlet delegatee = getDelegatee();
        if (delegatee == null) {
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            delegatee.service(req, res);
        }
    }

    /**
     * Destroys this servlet by shutting down the OSGi framework and hence the
     * delegatee servlet if one is set at all.
     */
    public final void destroy() {

        // destroy the delegatee
        if (delegatee != null) {
            delegatee.destroy();
            delegatee = null;
        }

        // shutdown the Felix container
        if (sling != null) {
            sling.destroy();
            sling = null;
        }

        // finally call the base class destroy method
        super.destroy();
    }

    Servlet getDelegatee() {
        return delegatee;
    }

    // ---------- Configuration Loading ----------------------------------------

    /**
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties are
     * accessible to the framework and to bundles and are intended for
     * configuration purposes. By default, the configuration property file is
     * located in the <tt>conf/</tt> directory of the Felix installation
     * directory and is called "<tt>config.properties</tt>". The
     * installation directory of Felix is assumed to be the parent directory of
     * the <tt>felix.jar</tt> file as found on the system class path property.
     * The precise file from which to load configuration properties can be set
     * by initializing the "<tt>felix.config.properties</tt>" system
     * property to an arbitrary URL.
     *
     * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was
     *         an error.
     */
    private Map<String, String> loadConfigProperties() {
        // The config properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.
        Map<String, String> props = new HashMap<String, String>();

        // The following property must start with a comma!
        final String servletVersion = getServletContext().getMajorVersion() + "." +
                                      getServletContext().getMinorVersion();
        props.put(
            Sling.PROP_SYSTEM_PACKAGES,
            ",javax.servlet;javax.servlet.http;javax.servlet.resources; version=" + servletVersion);

        // prevent system properties from being considered
        props.put(Sling.SLING_IGNORE_SYSTEM_PROPERTIES, "true");

        // add optional boot delegation for JCR and Jackrabbit API
        props.put("sling.include.jcr-client", "jcr-client.properties");

        // copy context init parameters
        @SuppressWarnings("unchecked")
        Enumeration<String> pe = getServletContext().getInitParameterNames();
        while (pe.hasMoreElements()) {
            String name = pe.nextElement();
            props.put(name, getServletContext().getInitParameter(name));
        }

        // copy servlet init parameters
        pe = getInitParameterNames();
        while (pe.hasMoreElements()) {
            String name = pe.nextElement();
            props.put(name, getInitParameter(name));
        }

        // ensure the Felix Logger loglevel matches the Sling log level
        checkLogSettings(props);

        // if the specified obr location is not a url and starts with a '/', we
        // assume that this location is inside the webapp and create the correct
        // full url
        final String repoLocation = props.get(OBR_REPOSITORY_URL);
        if (repoLocation != null && repoLocation.indexOf(":/") < 1
            && repoLocation.startsWith("/")) {
            try {
                final URL url = getServletContext().getResource(repoLocation);
                // only if we get back a resource url, we update it
                if (url != null) {
                    props.put(OBR_REPOSITORY_URL, url.toExternalForm());
                }
            } catch (MalformedURLException e) {
                // if an exception occurs, we ignore it
            }
        }
        return props;
    }

    private void checkLogSettings(Map<String, String> props) {
        String logLevelString = props.get(PROP_LOG_LEVEL);
        if (logLevelString != null) {
            int logLevel = 1;
            try {
                logLevel = Integer.parseInt(logLevelString);
            } catch (NumberFormatException nfe) {
                // might be a loglevel name
                for (int i=0; i < logLevels.length; i++) {
                    if (logLevels[i].equalsIgnoreCase(logLevelString)) {
                        logLevel = i;
                        break;
                    }
                }
            }
            props.put(LOG_LEVEL_PROP, String.valueOf(logLevel));
        }
    }

    private static class ServletContextLogger extends Logger {
        private ServletContext servletContext;

        private ServletContextLogger(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        protected void doLog(ServiceReference sr, int level, String msg,
                Throwable throwable) {

            // unwind throwable if it is a BundleException
            if ((throwable instanceof BundleException)
                && (((BundleException) throwable).getNestedException() != null)) {
                throwable = ((BundleException) throwable).getNestedException();
            }

            String s = (sr == null) ? null : "SvcRef " + sr;
            s = (s == null) ? msg : s + " " + msg;
            s = (throwable == null) ? s : s + " (" + throwable + ")";

            switch (level) {
                case LOG_DEBUG:
                    servletContext.log("DEBUG: " + s);
                    break;
                case LOG_ERROR:
                    servletContext.log("ERROR: " + s, throwable);
                    break;
                case LOG_INFO:
                    servletContext.log("INFO: " + s);
                    break;
                case LOG_WARNING:
                    servletContext.log("WARNING: " + s);
                    break;
                default:
                    servletContext.log("UNKNOWN[" + level + "]: " + s);
            }
        }
    }

    private static class ServletContextResourceProvider extends
            ClassLoaderResourceProvider {

        /**
         * The root folder for internal web application files (value is
         * "/WEB-INF/").
         */
        private static final String WEB_INF = "/WEB-INF";

        private ServletContext servletContext;
        
        private ServletContextResourceProvider(ServletContext servletContext) {
            super(SlingServlet.class.getClassLoader());
            this.servletContext = servletContext;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<String> getChildren(String path) {
            // ensure leading slash
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }

            Set resources = servletContext.getResourcePaths(path); // unchecked
            if (resources == null || resources.isEmpty()) {
                resources = servletContext.getResourcePaths(WEB_INF + path); // unchecked
            }

            Iterator resourceIterator;
            if ( resources == null || resources.isEmpty() ) {
                // fall back to the class path
                resourceIterator = super.getChildren(path);
                
                if(resourceIterator.hasNext()) {
                    return resourceIterator;
                }

                // fall back to WEB-INF within the class path
                resourceIterator = super.getChildren(WEB_INF + path);

                if(resourceIterator.hasNext()) {
                    return resourceIterator;
                }
            }
            
            if ( resources == null ) {
                return Collections.EMPTY_LIST.iterator();
            }
            return resources.iterator(); // unchecked
        }

        public URL getResource(String path) {
            // nothing for empty or null path
            if (path == null || path.length() == 0) {
                return null;
            }

            // ensure leading slash
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }

            try {
                // try direct path
                URL resource = servletContext.getResource(path);
                if (resource != null) {
                    return resource;
                }

                // otherwise try WEB-INF location
                resource = servletContext.getResource(WEB_INF + path);
                if(resource != null) {
                    return resource;
                }
                
                // try classpath
                resource = super.getResource(path);
                if(resource != null) {
                    return resource;
                }

                // try WEB-INF within the classpath
                resource = super.getResource(WEB_INF + path);
                if(resource != null) {
                    return resource;
                }

            } catch (MalformedURLException mue) {
                servletContext.log("Failure to get resource " + path, mue);
            }

            // fall back to no resource found
            return null;
        }

    }
}
