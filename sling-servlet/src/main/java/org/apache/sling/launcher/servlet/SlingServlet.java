/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launcher.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import org.apache.sling.launcher.Logger;
import org.apache.sling.launcher.ResourceProvider;
import org.apache.sling.launcher.Sling;

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
 * loaded overwriting the {@link #loadPropertiesOverride(Properties)} method.
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

    /**
     * The name of the servlet context attribute containing the
     * <code>Felix</code> instance.
     * <p>
     * This context attribute must be used with utmost care ! It is not intended
     * for this value to be used in generall applications of the framework.
     */
    public static final String CONTEXT_ATTR_SLING_FRAMEWORK = "org.apache.sling.framework";

    /**
     * The name of the configuration property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, JCR
     * repository, etc., is located.
     * <p>
     * The value of this property, if missing from the web application
     * deployment descriptor as an init-param, is "sling".
     * <p>
     * This configuration property is generally set in the web application
     * configuration and may be referenced in all property files (default, user
     * supplied and web application parameters) used to build the framework
     * configuration.
     */
    public static final String SLING_HOME = "sling.home";

    /**
     * The name of the configuration property defining a properties file
     * defining a list of bundles, which are installed into the framework when
     * it has been launched (value is "org.apache.osgi.bundles").
     * <p>
     * This configuration property is generally set in the web application
     * configuration and may be referenced in all property files (default, user
     * supplied and web application parameters) used to build the framework
     * configuration.
     */
    public static final String OSGI_FRAMEWORK_BUNDLES = "org.apache.osgi.bundles";

    /**
     * The singleton instance of this class. This is used by the
     * {@link #registerDelegatee(Servlet)} and
     * {@link #unregisterDelegatee(Servlet)} methods to register the servlet
     * delegate which implements the OSGi HttpService.
     * <p>
     * This field is managed through the {@link #setInstance(SlingServlet)}
     * method called by the {@link #init()} and {@link #destroy()} methods.
     */
    private static SlingServlet instance;

    /**
     * The <code>Felix</code> instance loaded on {@link #init()} and stopped
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

    /**
     * Reference counter concurrent of delegatee service calls. Only when this
     * counter has dropped to zero, can the delegatee servlet be destroyed.
     */
    private int delegateeRefCtr;

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
        setInstance(this);

        if (getServletContext().getAttribute(CONTEXT_ATTR_SLING_FRAMEWORK) != null) {
            log("Has this framework already been started ???");
            return;
        }

        // read the default parameters
        Map<String, String> props = loadConfigProperties();

        try {
            Logger logger = new ServletContextLogger(getServletContext());
            ResourceProvider rp = new ServletContextResourceProvider(
                getServletContext());
            sling = new Sling(logger, rp, props);
        } catch (Exception ex) {
            log("Cannot start the OSGi framework", ex);
            throw new UnavailableException("Cannot start the OSGi Framework: "
                + ex);
        }

        // set the context attributes only if all setup has been successfull
        getServletContext().setAttribute(CONTEXT_ATTR_SLING_FRAMEWORK, sling);

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
        Servlet delegatee = acquireDelegateeRef();
        if (delegatee == null) {
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                delegatee.service(req, res);
            } finally {
                releaseDelegateeRef();
            }
        }
    }

    /**
     * Destroys this servlet by shutting down the OSGi framework and hence the
     * delegatee servlet if one is set at all.
     */
    public final void destroy() {
        // remove the context attributes immediately
        getServletContext().removeAttribute(CONTEXT_ATTR_SLING_FRAMEWORK);

        // shutdown the Felix container
        if (sling != null) {
            sling.destroy();
            sling = null;
        }

        setInstance(null);

        // finally call the base class destroy method
        super.destroy();
    }

    // ---------- Delegatee Servlet Support ------------------------------------

    private synchronized static void setInstance(SlingServlet launcherServlet) {
        if (launcherServlet != null && instance != null) {
            throw new IllegalStateException("Instance already registered.");
        }

        instance = launcherServlet;
    }

    /**
     * Returns the singleton instance of this class or <code>null</code> if
     * the singleton instance has not been initialized yet or if it has been
     * destroyed.
     * <p>
     * This method is for internal use only and MUST NOT be used by client
     * applications and bundles.
     */
    public static Servlet getInstance() {
        return instance;
    }

    public synchronized static void registerDelegatee(Servlet delegatee) {
        // dont register if the servlet is shutting down
        if (instance == null) {
            return;
        }

        synchronized (instance) {
            if (delegatee == null) {
                throw new NullPointerException("Delegatee must not be null");
            }

            try {
                delegatee.init(instance.getServletConfig());
                instance.delegatee = delegatee;
            } catch (ServletException se) {
                instance.getServletContext().log(
                    "ERROR: Cannot initialize" + " delegatee servlet", se);
            }
        }
    }

    public synchronized static void unregisterDelegatee(Servlet delegatee) {
        // dont unregister if the servlet is shutting down
        if (instance == null) {
            return;
        }

        synchronized (instance) {
            // nothing to do if no servlet is registered
            if (instance.delegatee == null) {
                return;
            }

            if (delegatee != instance.delegatee) {
                throw new IllegalArgumentException(
                    "Servlet to unregister does not match registered delegatee Servlet");
            }

            Servlet oldDelegatee = instance.delegatee;
            instance.delegatee = null;
            while (instance.delegateeRefCtr > 0) {
                try {
                    instance.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            oldDelegatee.destroy();
        }
    }

    private synchronized Servlet acquireDelegateeRef() {
        if (delegatee != null) {
            delegateeRefCtr++;
            return delegatee;
        }

        return null;
    }

    private synchronized void releaseDelegateeRef() {
        // discount delegatee ref, even if delegatee has already been set to
        // null,
        // this is the protocol with the unregisterDelegatee method, which
        // first sets the delegatee to null and then waits for the counter
        // to drop to zero
        delegateeRefCtr--;

        // notify threads potentially waiting for the counter to drop to zero
        notifyAll();
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

        props.put(
            Sling.PROP_SYSTEM_PACKAGES,
            ",javax.servlet;javax.servlet.http;javax.servlet.resources; version=2.3,javax.portlet,org.apache.sling.launcher.servlet");

        // prevent system properties from being considered
        props.put(Sling.SLING_IGNORE_SYSTEM_PROPERTIES, "true");

        // add optional boot delegation for JCR and Jackrabbit API
        props.put("sling.include.jcr-client", "jcr-client.properties");

        // copy context init parameters
        Enumeration<?> pe = getServletContext().getInitParameterNames();
        while (pe.hasMoreElements()) {
            String name = (String) pe.nextElement();
            props.put(name, getServletContext().getInitParameter(name));
        }

        // copy servlet init parameters
        pe = getInitParameterNames();
        while (pe.hasMoreElements()) {
            String name = (String) pe.nextElement();
            props.put(name, getInitParameter(name));
        }

        return props;
    }

    private static class ServletContextLogger extends Logger {
        private ServletContext servletContext;

        private ServletContextLogger(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        public void log(String message, Throwable throwable) {
            if (throwable == null) {
                servletContext.log(message);
            } else {
                servletContext.log(message, throwable);
            }
        }
    }

    private static class ServletContextResourceProvider extends
            ResourceProvider {

        /**
         * The root folder for internal web application files (value is
         * "/WEB-INF/").
         */
        private static final String WEB_INF = "/WEB-INF";

        private ServletContext servletContext;

        private ServletContextResourceProvider(ServletContext servletContext) {
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
            if (resources.isEmpty()) {
                resources = servletContext.getResourcePaths(WEB_INF + path); // unchecked
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
                return servletContext.getResource(WEB_INF + path);

            } catch (MalformedURLException mue) {
                servletContext.log("Failure to get resource " + path, mue);
            }

            // fall back to no resource found
            return null;
        }

    }
}
