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
package org.apache.sling.launchpad.base.webapp;

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
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.framework.Logger;
import org.apache.felix.http.proxy.ProxyServlet;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.ClassLoaderResourceProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * The <code>SlingServletDelegate</code> serves as a basic servlet for Project Sling.
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
 * <li>The <code>sling.properties</code> is read
 * from the servlet class path. This properties file contains default settings.</li>
 * <li>Extensions of this servlet may provide additional properties to be
 * loaded overwriting the {@link #loadConfigProperties(String)} method.
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
 * choose to use. The Commons OSGI Log Bundle provides an OSGi Log Service
 * implementation, which also provides access to Apache Commons Logging, SLF4J
 * and Log4J logging. It is recommended that this bundle is used to setup and
 * configure logging for systems based on this servlet.
 */
public class SlingServletDelegate extends GenericServlet implements Launcher {

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
     * Flag set by the {@link #destroy()} method to indicate the servlet has
     * been destroyed. This flag is used by the {@link #startSling(String)}
     * method to check whether the SlingServletDelegate has been destroyed while Sling
     * was starting up.
     */
    private boolean servletDestroyed = false;

    /**
     * The OSGI framework instance loaded on {@link #init()} and stopped
     * on {@link #destroy()}.
     */
    private Sling sling;

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

    private Notifiable notifiable;

    private Map<String, String> properties;

    private String slingHome;

    @Override
    public void setNotifiable(Notifiable notifiable) {
        this.notifiable = notifiable;
    }

    @Override
    public void setCommandLine(Map<String, String> args) {
        this.properties = args;
    }

    @Override
    public void setSlingHome(String slingHome) {
        this.slingHome = slingHome;
    }

    @Override
    public boolean start() {
        // might want to log, why we don't start !
        return false;
    }

    @Override
    public void stop() {
        destroy();
    }

   /**
     * Initializes this servlet by loading the framework configuration
     * properties, starting the OSGi framework (Apache Felix) and exposing the
     * system bundle context and the <code>Felix</code> instance as servlet
     * context attributes.
     *
     * @throws ServletException if the framework cannot be initialized.
     */
    @Override
    public final void init() throws ServletException {
        // temporary holders control final setup and ensure proper
        // disposal in case of setup errors
        Sling tmpSling = null;
        Servlet tmpDelegatee = null;

        try {

            log("Starting Apache Sling in " + slingHome);

            // read the default parameters
            Map<String, String> props = loadConfigProperties(slingHome);

            Logger logger = new ServletContextLogger(getServletContext());
            LaunchpadContentProvider rp = new ServletContextResourceProvider(
                getServletContext());
            tmpSling = SlingBridge.getSlingBridge(notifiable, logger, rp, props, getServletContext());

            // set up the OSGi HttpService proxy servlet
            tmpDelegatee = new ProxyServlet();
            tmpDelegatee.init(getServletConfig());

            // set the fields only if the SlingServletDelegate has no been destroyed
            // while Sling has been starting up. Otherwise we do not set the
            // fields and leave the temporary variables assigned to have
            // them destroyed in the finally clause.
            if (servletDestroyed) {

                log("SlingServletDelegate destroyed while starting Apache Sling, shutting Apache Sling down");

            } else {

                // set the fields now
                sling = tmpSling;
                delegatee = tmpDelegatee;

                // reset temporary holders to prevent destroyal
                tmpSling = null;
                tmpDelegatee = null;

                log("Apache Sling successfully started in " + slingHome);
            }

        } catch (BundleException be) {

            throw new ServletException("Failed to start Apache Sling in " + slingHome, be);

        } catch (ServletException se) {

            throw new ServletException("Failed to start bridge servlet for Apache Sling", se);

        } catch (Throwable t) {

            throw new ServletException("Uncaught Failure starting Apache Sling", t);

        } finally {

            // clean up temporary fields
            if (tmpDelegatee != null) {
                tmpDelegatee.destroy();
            }
            if (tmpSling != null) {
                tmpSling.destroy();
            }
        }
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
     * @throws javax.servlet.UnavailableException if the no delegatee servlet is currently
     *             available
     * @throws ServletException if an exception occurs that interferes with the
     *             servlet's normal operation occurred
     * @throws IOException if an input or output exception occurs
     */
    @Override
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
    @Override
    public final void destroy() {

        // set the destroyed flag to signal to the startSling method
        // that Sling should be terminated immediately
        servletDestroyed = true;

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
     * @param slingHome The value to be used as the "sling.home" property in the
     *            returned map. This parameter is expected to be non-<code>null</code>.
     * @return A <tt>Properties</tt> instance.
     */
    private Map<String, String> loadConfigProperties(String slingHome) {
        // The config properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.
        Map<String, String> props = new HashMap<String, String>();

        // The following property must start with a comma!
        final String servletVersion = getServletContext().getMajorVersion() + "." +
                                      getServletContext().getMinorVersion();
        String packages = ",javax.servlet;javax.servlet.http;javax.servlet.resources";
        if ( getServletContext().getMajorVersion() >= 3 ) {
            // servlet 3.x adds new packages and we should export as 2.6 and 3.x
            packages = packages + "; version=2.6" + packages + ";javax.servlet.annotation;javax.servlet.descriptor";
        }
        props.put(
                 Sling.PROP_SYSTEM_PACKAGES,
                 packages + "; version=" + servletVersion);
        // extra capabilities
        final String servletCaps = "osgi.contract;osgi.contract=JavaServlet;version:Version=\" " + servletVersion + "\";" +
                        "uses:=\"javax.servlet,javax.servlet.http,javax.servlet.descriptor,javax.servlet.annotation\"";
        props.put(Sling.PROP_EXTRA_CAPS, servletCaps);

        // prevent system properties from being considered
        props.put(Sling.SLING_IGNORE_SYSTEM_PROPERTIES, "true");

        if (this.properties != null) {
            props.putAll(this.properties);
        } else {
            // copy context init parameters
            @SuppressWarnings("unchecked")
            Enumeration<String> cpe = getServletContext().getInitParameterNames();
            while (cpe.hasMoreElements()) {
                String name = cpe.nextElement();
                props.put(name, getServletContext().getInitParameter(name));
            }

            // copy servlet init parameters
            @SuppressWarnings("unchecked")
            Enumeration<String> pe = getInitParameterNames();
            while (pe.hasMoreElements()) {
                String name = pe.nextElement();
                props.put(name, getInitParameter(name));
            }
        }

        // ensure the Felix Logger loglevel matches the Sling log level
        checkLogSettings(props);

        // if the specified obr location is not a url and starts with a '/', we
        // assume that this location is inside the webapp and create the correct
        // full url
        final String repoLocation = props.get(OBR_REPOSITORY_URL);
        if (insideWebapp(repoLocation)) {
            final URL url = getUrl(repoLocation);
            // only if we get back a resource url, we update it
            if (url != null)
                props.put(OBR_REPOSITORY_URL, url.toExternalForm());
        }

        // set sling home
        props.put(SharedConstants.SLING_HOME, slingHome);

        // ensure sling.launchpad is set
        if (!props.containsKey(SharedConstants.SLING_LAUNCHPAD)) {
            props.put(SharedConstants.SLING_LAUNCHPAD, slingHome);
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

    private boolean insideWebapp(String path) {
        return path != null && path.indexOf(":/") < 1 && path.startsWith("/");
    }

    private URL getUrl(String path) {
        try {
            return getServletContext().getResource(path);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static class ServletContextLogger extends Logger {
        private ServletContext servletContext;

        private ServletContextLogger(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        protected void doLog(
                Bundle bundle, ServiceReference sr, int level,
                String msg, Throwable throwable) {

            // unwind throwable if it is a BundleException
            if ((throwable instanceof BundleException)
                && (((BundleException) throwable).getNestedException() != null)) {
                throwable = ((BundleException) throwable).getNestedException();
            }

            String s = (sr == null) ? null : "SvcRef " + sr;
            s = (s == null) ? null : s + " Bundle '" + bundle.getBundleId() + "'";
            s = (s == null) ? msg : s + " " + msg;
            s = (throwable == null) ? s : s + " (" + throwable + ")";

            switch (level) {
                case LOG_DEBUG:
                    servletContext.log("DEBUG: " + s);
                    break;
                case LOG_ERROR:
                    if (throwable == null) {
                        servletContext.log("ERROR: " + s);
                    } else {
                        servletContext.log("ERROR: " + s, throwable);
                    }
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
            super(SlingServletDelegate.class.getClassLoader());
            this.servletContext = servletContext;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<String> getChildren(String path) {
            // ensure leading slash
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }

            @SuppressWarnings("rawtypes")
            Set resources = servletContext.getResourcePaths(path); // unchecked
            if (resources == null || resources.isEmpty()) {
                resources = servletContext.getResourcePaths(WEB_INF + path); // unchecked
            }

            @SuppressWarnings("rawtypes")
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

        @Override
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
