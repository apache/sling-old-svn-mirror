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
package org.apache.sling.launcher.app;

import static org.apache.felix.framework.util.FelixConstants.EMBEDDED_EXECUTION_PROP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * The <code>Sling</code> serves as the starting point for Sling.
 * <ul>
 * <li>The {@link #Sling(Logger, ResourceProvider, Map)} method launches
 * Apache <code>Felix</code> as the OSGi framework implementation we use.
 * </ul>
 * <p>
 * <b>Launch Configuration</b>
 * <p>
 * The Apache <code>Felix</code> framework requires configuration parameters
 * to be specified for startup. This servlet builds the list of parameters from
 * three locations:
 * <ol>
 * <li>The <code>com/day/osgi/servlet/Sling.properties</code> is read from
 * the servlet class path. This properties file contains default settings.</li>
 * <li>Extensions of this servlet may provide additional properties to be
 * loaded overwriting the {@link #loadPropertiesOverride(Map)} method.
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
 */
public class Sling implements BundleActivator {

    /** Pseduo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the configuration property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, CRX
     * repository, etc., is located.
     * <p>
     * The value of this property, if not set as a system property defaults to
     * the <i>sling</i> directory in the current working directory.
     *
     * @see #SLING_HOME_URL
     */
    public static final String SLING_HOME = "sling.home";

    /**
     * The name of the configuration property defining the Sling home directory
     * as an URL (value is "sling.home.url").
     * <p>
     * The value of this property is assigned the value of
     * <code>new File(${sling.home}).toURI().toString()</code> before
     * resolving the property variables.
     *
     * @see #SLING_HOME
     */
    public static final String SLING_HOME_URL = "sling.home.url";
    
    /**
     * The name of the configuration property defining the JCR home directory
     * (value is "sling.repository.home").
     * <p>
     * The value of this property could be set as a system property, init-param in
     * web.xml or property in sling.properties.
     * <p>
     * Default value to #SLING_HOME/repository_name
     */
    public static final String JCR_REPO_HOME = "sling.repository.home";
    
    /**
     * The name of the configuration property defining the URL of an existing
     * repository config file (repository.xml).
     * <p>
     * The value of this property could be set as a system property, init-param in
     * web.xml or property in sling.properties.
     * <p>
     * Default value to #SLING_HOME/repository_name/repository.xml
     */
    public static final String JCR_REPO_CONFIG_FILE_URL = "sling.repository.config.file.url";

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
     * The property to be set to ignore the system properties when building the
     * Felix framework properties (value is "sling.ignoreSystemProperties"). If
     * this is property is set to <code>true</code> (case does not matter),
     * the system properties will not be used by
     * {@link #loadConfigProperties(Map)}.
     */
    public static final String SLING_IGNORE_SYSTEM_PROPERTIES = "sling.ignoreSystemProperties";

    /**
     * The name of the default launcher properties file to setup the environment
     * for the <code>Felix</code> framework (value is "sling.properties").
     * <p>
     * Extensions of this class may overwrite some or all properties in this
     * file through Web Application parameters or other properties files.
     */
    public static final String CONFIG_PROPERTIES = "sling.properties";

    public static final String PROP_SYSTEM_PACKAGES = "org.apache.sling.launcher.system.packages";

    /**
     * The simple logger to log messages during startup and shutdown to
     */
    protected final Logger logger;

    private ResourceProvider resourceProvider;

    /**
     * The <code>Felix</code> instance loaded on {@link #init()} and stopped
     * on {@link #destroy()}.
     */
    private Felix felix;

    /**
     * The <code>BundleContext</code> of the OSGi framework system bundle.
     * This is used for service registration and service access to get at the
     * delegatee servlet.
     */
    private BundleContext bundleContext;

    /**
     * Initializes this servlet by loading the framework configuration
     * properties, starting the OSGi framework (Apache Felix) and exposing the
     * system bundle context and the <code>Felix</code> instance as servlet
     * context attributes.
     *
     * @throws BundleException if the framework cannot be initialized.
     */
    public Sling(Logger logger, ResourceProvider resourceProvider,
            Map<String, String> propOverwrite) throws BundleException {
    	
        this.logger = logger;
        this.resourceProvider = resourceProvider;

        this.logger.log(Logger.LOG_INFO, "Starting Sling");

        // read the default parameters
        Map<String, String> props = this.loadConfigProperties(propOverwrite);

        // check for auto-start bundles
        this.setInstallBundles(props);

        // ensure execution environment
        this.setExecutionEnvironment(props);

        // make sure Felix does not exit the VM when terminating ...
        props.put(EMBEDDED_EXECUTION_PROP, "true");

        // the custom activator list just contains this servlet
        List<BundleActivator> activators = new ArrayList<BundleActivator>();
        activators.add(this);
        activators.add(new BootstrapInstaller(logger, resourceProvider));

        // create the framework and start it
        Felix tmpFelix = new Felix(logger, props, activators);
        tmpFelix.start();

        // only assign field if start succeeds
        this.felix = tmpFelix;

        // log sucess message
        this.logger.log(Logger.LOG_INFO, "Sling started");
    }

    /**
     * Destroys this servlet by shutting down the OSGi framework and hence the
     * delegatee servlet if one is set at all.
     */
    public final void destroy() {
        // shutdown the Felix container
        if (felix != null) {
            logger.log(Logger.LOG_INFO, "Shutting down Sling");
            felix.stopAndWait();
            logger.log(Logger.LOG_INFO, "Sling stopped");
            felix = null;
        }
    }

    // ---------- BundleActivator ----------------------------------------------

    /**
     * Called when the OSGi framework is being started. This implementation
     * registers as a service listener for the
     * <code>javax.servlet.Servlet</code> class and calls the
     * {@link #doStartBundle()} method for implementations to execute more
     * startup tasks. Additionally the <code>context</code> URL protocol
     * handler is registered.
     *
     * @param bundleContext The <code>BundleContext</code> of the system
     *            bundle of the OSGi framework.
     * @throws Exception May be thrown if the {@link #doStartBundle()} throws.
     */
    public final void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;

        // register the context URL handler
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { "context" });
        ContextProtocolHandler contextHandler = new ContextProtocolHandler(
            this.resourceProvider);
        bundleContext.registerService(URLStreamHandlerService.class.getName(),
            contextHandler, props);

        // execute optional bundle startup tasks of an extension
        this.doStartBundle();
    }

    /**
     * Called when the OSGi framework is being shut down. This implementation
     * first calls the {@link #doStopBundle()} method method before
     * unregistering as a service listener and ungetting an servlet delegatee if
     * one has been acquired.
     *
     * @param bundleContext The <code>BundleContext</code> of the system
     *            bundle of the OSGi framework.
     */
    public final void stop(BundleContext bundleContext) {
        // execute optional bundle stop tasks of an extension
        try {
            this.doStopBundle();
        } catch (Exception e) {
            this.logger.log(Logger.LOG_ERROR, "Unexpected exception caught", e);
        }

        // drop bundle context reference
        this.bundleContext = null;
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
    private Map<String, String> loadConfigProperties(
            Map<String, String> propOverwrite) throws BundleException {
        // The config properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.
        Map<String, String> props = new HashMap<String, String>();

        // Read the properties file.
        this.load(props, CONFIG_PROPERTIES);

        // resolve inclusions (and remove property)
        this.loadIncludes(props, null);

        // overwrite default properties with initial overwrites
        if (propOverwrite != null) {
            props.putAll(propOverwrite);
        }

        // check whether sling.home is overwritten by system property
        String slingHome = System.getProperty(SLING_HOME);
        if (slingHome == null || slingHome.length() == 0) {

            // no system property, ensure default setting
            slingHome = props.get(SLING_HOME);
            if (slingHome == null || slingHome.length() == 0) {
                slingHome = "sling";
                this.logger.log(Logger.LOG_INFO,
                    "sling.home is not defined. Using '" + slingHome + "'");
            }
        }

        // resolve variables and ensure sling.home is an absolute path
        slingHome = substVars(slingHome, SLING_HOME, null, props);
        File slingHomeFile = new File(slingHome).getAbsoluteFile();
        slingHome = slingHomeFile.getAbsolutePath();

        // overlay with ${sling.home}/sling.properties
        this.logger.log(Logger.LOG_INFO, "Starting sling in " + slingHome);
        File propFile = new File(slingHome, CONFIG_PROPERTIES);
        this.load(props, propFile);

        // create a copy of the properties to perform variable substitution
        Map<String, String> origProps = props;
        props = new HashMap<String, String>();
        props.putAll(origProps);

        // check system properties for any overrides (except sling.home !)
        String ignoreSystemProperties = props.get(SLING_IGNORE_SYSTEM_PROPERTIES);
        if (!"true".equalsIgnoreCase(ignoreSystemProperties)) {
            for (String name : props.keySet()) {
                String sysProp = System.getProperty(name);
                if (sysProp != null) {
                    props.put(name, sysProp);
                }
            }
        }

        // resolve inclusions again
        this.loadIncludes(props, slingHome);

        // overwrite properties, this is not persisted as such
        this.loadPropertiesOverride(props);

        // resolve boot delegation and system packages
        this.resolve(props, "org.osgi.framework.bootdelegation",
            "sling.bootdelegation.");
        this.resolve(props, "org.osgi.framework.system.packages",
            "sling.system.packages.");

        // reset back the sling home property
        // might have been overwritten by system properties, included
        // files or the sling.properties file
        origProps.put(SLING_HOME, slingHome);
        props.put(SLING_HOME, slingHome);
        props.put(SLING_HOME_URL, slingHomeFile.toURI().toString());

        // Perform variable substitution for system properties.
        for (Entry<String, String> entry : props.entrySet()) {
            entry.setValue(substVars(entry.getValue(), entry.getKey(), null,
                props));
        }

        // look for context:/ URLs to substitute
        for (Entry<String, String> entry : props.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (value != null && value.startsWith("context:/")) {
                String path = value.substring("context:/".length() - 1);

                InputStream src = this.resourceProvider.getResourceAsStream(path);
                if (src != null) {
                    File target = new File(slingHome, path);
                    OutputStream dest = null;
                    try {
                        // only copy file if not existing
                        if (!target.exists()) {
                            target.getParentFile().mkdirs();
                            dest = new FileOutputStream(target);
                            byte[] buf = new byte[2048];
                            int rd;
                            while ((rd = src.read(buf)) >= 0) {
                                dest.write(buf, 0, rd);
                            }
                        }

                        // after copying replace property and add url property
                        entry.setValue(target.getAbsolutePath());

                        // also set the new property on the unsubstituted props
                        origProps.put(name, "${sling.home}" + path);

                    } catch (IOException ioe) {
                        this.logger.log(Logger.LOG_ERROR, "Cannot copy file "
                            + value + " to " + target, ioe);
                    } finally {
                        if (dest != null) {
                            try {
                                dest.close();
                            } catch (IOException ignore) {
                            }
                        }
                        try {
                            src.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }
        }

        // write the unsubstituted properties back to the overlay file
        OutputStream os = null;
        try {
            // ensure parent folder(s)
            propFile.getParentFile().mkdirs();

            os = new FileOutputStream(propFile);

            // copy the values into a temporary properties structure to store
            Properties tmp = new Properties();
            tmp.putAll(origProps);
            tmp.store(os, "Overlay properties for configuration");
        } catch (Exception ex) {
            this.logger.log(Logger.LOG_ERROR,
                "Error loading overlay properties from " + propFile, ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex2) {
                    // Nothing we can do.
                }
            }
        }

        return props;
    }

    /**
     * Scans the properties for any properties starting with the given
     * <code>prefix</code> (e.g. <code>sling.bootdelegation.</code>).
     * <ol>
     * <li>Each such property is checked, whether it actually starts with
     * <code>prefix<b>class.</b></code>. If so, the rest of the property
     * name is assumed to be a fully qualified class name which is check,
     * whether it is visible. If so, the value of the property is appended to
     * the value of the <code>osgiProp</code>. If the class cannot be loaded,
     * the property is ignored.
     * <li>Otherwise, if the property does not contain a fully qualified class
     * name, the value of the property is simply appended to the
     * <code>osgiProp</code>.
     * </ol>
     *
     * @param props The <code>Properties</code> to be scanned.
     * @param osgiProp The name of the property in <code>props</code> to which
     *            any matching property values are appended.
     * @param prefix The prefix of properties to handle.
     */
    private void resolve(Map<String, String> props, String osgiProp,
            String prefix) {
        final String propVal = props.get(osgiProp);
        StringBuffer prop = new StringBuffer(propVal == null ? "" : propVal);
        boolean mod = false;
        for (Entry<String, String> pEntry : props.entrySet()) {
            String key = pEntry.getKey();
            if (key.startsWith(prefix)) {
                if (key.indexOf("class.") == prefix.length()) {
                    // prefix is followed by checker class name
                    String className = key.substring(prefix.length()
                        + "class.".length());
                    try {
                        Class.forName(className, true,
                            this.getClass().getClassLoader());
                    } catch (Throwable t) {
                        // don't really care, but class checking failed, so we
                        // do not add
                        this.logger.log(Logger.LOG_DEBUG, "Class " + className
                            + " not found. Ignoring '" + pEntry.getValue()
                            + "' for property " + osgiProp);
                        continue;
                    }
                }

                // get here if class is known or no checker class
                this.logger.log(Logger.LOG_DEBUG, "Adding '"
                    + pEntry.getValue() + "' to property " + osgiProp);
                if (prop.length() > 0) {
                    prop.append(',');
                }
                prop.append(pEntry.getValue());
                mod = true;
            }
        }

        // replace the property with the modified property
        if (mod) {
            this.logger.log(Logger.LOG_DEBUG, "Setting property " + osgiProp
                + " to " + prop.toString());
            props.put(osgiProp, prop.toString());
        }
    }

    private void setInstallBundles(Map<String, String> props) {
        String prefix = "sling.install.";
        Set<String> levels = new TreeSet<String>();
        for (String key : props.keySet()) {
            if (key.startsWith(prefix)) {
                levels.add(key.substring(prefix.length()));
            }
        }

        StringBuffer buf = new StringBuffer();
        for (String level : levels) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append(level);
        }

        props.put(prefix + "bundles", buf.toString());
    }

    /**
     * Ensures sensible Execution Environment setting. If the
     * <code>org.osgi.framework.executionenvironment</code> property is set in
     * the configured properties or the system properties, we ensure that older
     * settings for J2SE-1.2, J2SE-1.3 and J2SE-1.4 are included. If the
     * property is neither set in the configuration properties nor in the system
     * properties, the property is not set.
     *
     * @param props The configuration properties to check and optionally ammend.
     */
    private void setExecutionEnvironment(Map<String, String> props) {
        // get the current Execution Environment setting
        String ee = props.get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
        if (ee == null) {
            ee = System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
        }

        // if there is a setting, ensure J2SE-1.2/3/4/5 is included in the list
        if (ee != null) {
            int javaMinor;
            try {
                String specVString = System.getProperty("java.specification.version");
                javaMinor = Version.parseVersion(specVString).getMinor();
            } catch (IllegalArgumentException iae) {
                // don't care, assume minimal sling version (1.5)
                javaMinor = 5;
            }

            for (int i = 2; i <= javaMinor; i++) {
                String exEnv = "J2SE-1." + i;
                if (ee.indexOf(exEnv) < 0) {
                    ee += "," + exEnv;
                }
            }

            this.logger.log(Logger.LOG_INFO,
                "Using Execution Environment setting: " + ee);
            props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
        } else {
            this.logger.log(Logger.LOG_INFO,
                "Not using Execution Environment setting");
        }
    }

    // ---------- Extension support --------------------------------------------

    /**
     * Loads additional properties into the <code>properties</code> object.
     * <p>
     * This implementation does nothing and may be overwritten by extensions
     * requiring additional properties to be set.
     * <p>
     * This method is called when the servlet is initialized to prepare the
     * configuration for <code>Felix</code>. Implementations may add
     * properties from implementation specific sources. Properties added here
     * overwrite properties loaded from the default properties file and may be
     * overwritten by parameters set in the web application.
     * <p>
     * The <code>properties</code> object has not undergone variable
     * substition and properties added by this method may also contain values
     * refererring to other properties.
     * <p>
     * The properties added in this method will not be persisted in the
     * <code>sling.properties</code> file in the <code>sling.home</code>
     * directory.
     *
     * @param properties The <code>Properties</code> object to which custom
     *            properties may be added.
     */
    protected void loadPropertiesOverride(Map<String, String> properties) {
    }

    /**
     * Returns the <code>BundleContext</code> of the system bundle of the OSGi
     * framework launched by this servlet. This method only returns a non-<code>null</code>
     * object after the system bundle of the framework has been started and
     * before it is being stopped.
     */
    protected final BundleContext getBundleContext() {
        return this.bundleContext;
    }

    /**
     * Executes additional startup tasks and is called by the
     * {@link #start(BundleContext)} method.
     * <p>
     * This implementation does nothing and may be overwritten by extensions
     * requiring additional startup tasks.
     *
     * @throws Exception May be thrown in case of problems.
     */
    protected void doStartBundle() throws Exception {
    }

    /**
     * Executes additional shutdown tasks and is called by the
     * {@link #stop(BundleContext)} method.
     * <p>
     * This implementation does nothing and may be overwritten by extensions
     * requiring additional shutdown tasks.
     * <p>
     * When overwriting this method, it must be made sure, that no exception may
     * be thrown, otherwise unexpected behaviour may result.
     */
    protected void doStopBundle() {
    }

    // ---------- Property file support ----------------------------------------

    /**
     * Looks for <code>sling.include</code> and <code>sling.include.*</code>
     * properties in the <code>props</code> and loads properties form the
     * respective locations.
     * <p>
     * Each <code>sling.include</code> (or <code>sling.include.*</code>)
     * property may contain a comma-separated list of resource and/or file names
     * to be loaded from. The includes are loaded in alphabetical order of the
     * property names.
     * <p>
     * Each reasource path is first tried to be loaded through the
     * {@link #resourceProvider}. If that fails, the resource path is tested as
     * a file. If relative <code>slingHome</code> is used as the parent if not
     * <code>null</code>, otherwise the current working directory is used as
     * the parent.
     * <p>
     * Any non-existing resource is silently ignored.
     * <p>
     * When the method returns, the <code>sling.include</code> and
     * <code>sling.include.*</code> properties are not contained in the
     * <code>props</code> any more.
     *
     * @param props The <code>Properties</code> containing the
     *            <code>sling.include</code> and <code>sling.include.*</code>
     *            properties. This is also the destination for the new
     *            properties loaded.
     * @param slingHome The parent directory used to resolve relative path names
     *            if loading from a file. This may be <code>null</code> in
     *            which case the current working directory is used as the
     *            parent.
     */
    private void loadIncludes(Map<String, String> props, String slingHome) {
        // Build the sort map of include properties first
        // and remove include elements from the properties
        SortedMap<String, String> includes = new TreeMap<String, String>();
        for (Iterator<Entry<String, String>> pi = props.entrySet().iterator(); pi.hasNext();) {
            Entry<String, String> entry = pi.next();
            if (entry.getKey().startsWith("sling.include.")
                || entry.getKey().equals("sling.include")) {
                includes.put(entry.getKey(), entry.getValue());
                pi.remove();
            }
        }

        for (Iterator<Entry<String, String>> ii = includes.entrySet().iterator(); ii.hasNext();) {
            Map.Entry<String, String> entry = ii.next();
            String key = entry.getKey();
            String include = entry.getValue();

            // ensure variable resolution on this property
            include = substVars(include, key, null, props);

            StringTokenizer tokener = new StringTokenizer(include, ",");
            while (tokener.hasMoreTokens()) {
                String file = tokener.nextToken().trim();
                InputStream is = this.resourceProvider.getResourceAsStream(file);
                try {
                    if (is == null && slingHome != null) {
                        File resFile = new File(file);
                        if (!resFile.isAbsolute()) {
                            resFile = new File(slingHome, file);
                        }
                        if (resFile.canRead()) {
                            is = new FileInputStream(resFile);
                            file = resFile.getAbsolutePath(); // for logging
                        }
                    }

                    if (is != null) {
                        this.load(props, is);
                    }
                } catch (IOException ioe) {
                    this.logger.log(Logger.LOG_ERROR,
                        "Error loading config properties from " + file, ioe);
                }
            }
        }
    }

    /**
     * Load properties from the given resource file, which is accessed through
     * the {@link #resourceProvider}. If the resource does not exist, nothing
     * is loaded.
     *
     * @param props The <code>Properties</code> into which the loaded
     *            properties are loaded
     * @param resource The resource from which to load the resources
     */
    private void load(Map<String, String> props, String resource) {
        InputStream is = this.resourceProvider.getResourceAsStream(resource);
        if (is != null) {
            try {
                this.load(props, is);
            } catch (IOException ioe) {
                this.logger.log(Logger.LOG_ERROR,
                    "Error loading config properties from " + resource, ioe);
            }
        }
    }

    /**
     * Load properties from the given file. If the resource cannot be read from
     * (e.g. because it does not exist), nothing is loaded.
     *
     * @param props The <code>Properties</code> into which the loaded
     *            properties are loaded
     * @param file The <code>File</code> to load the properties from
     */
    private void load(Map<String, String> props, File file) {
        if (file != null && file.canRead()) {
            try {
                this.load(props, new FileInputStream(file));
            } catch (IOException ioe) {
                this.logger.log(Logger.LOG_ERROR,
                    "Error loading config properties from "
                        + file.getAbsolutePath(), ioe);
            }
        }
    }

    private void load(Map<String, String> props, InputStream ins)
            throws IOException {
        try {
            Properties tmp = new Properties();
            tmp.load(ins);

            for (Map.Entry<Object, Object> entry : tmp.entrySet()) {
                props.put((String) entry.getKey(), (String) entry.getValue());
            }
        } finally {
            try {
                ins.close();
            } catch (IOException ioe2) {
                // ignore
            }
        }
    }

    // ---------- Property file variable substition support --------------------

    /**
     * The starting delimiter of variable names (value is "${").
     */
    private static final String DELIM_START = "${";

    /**
     * The ending delimiter of variable names (value is "}").
     */
    private static final String DELIM_STOP = "}";

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
