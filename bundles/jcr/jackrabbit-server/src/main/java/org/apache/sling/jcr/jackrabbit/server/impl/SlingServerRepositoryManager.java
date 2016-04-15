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
package org.apache.sling.jcr.jackrabbit.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.management.DynamicMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository2;
import org.apache.sling.jcr.base.AbstractSlingRepositoryManager;
import org.apache.sling.jcr.base.util.RepositoryAccessor;
import org.apache.sling.jcr.jackrabbit.server.impl.jmx.StatisticsMBeanImpl;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServerRepository</code> creates and configures <tt>Jackrabbit</tt> repository instances.
 */
@Component(
        label = "%repository.name",
        description = "%repository.description",
        metatype = true,
        name = "org.apache.sling.jcr.jackrabbit.server.SlingServerRepository",
        configurationFactory = true)
@Properties({
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = "service.description", value = "Factory for embedded Jackrabbit Repository Instances")
})
public class SlingServerRepositoryManager extends AbstractSlingRepositoryManager {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The name of the configuration property defining the URL to the repository
     * configuration file (value is "config").
     * <p>
     * If the configuration file is located in the local file system, the
     * "file:" scheme must still be specified.
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     */
    @Property(value = "")
    public static final String REPOSITORY_CONFIG_URL = "config";

    /**
     * The name of the configuration property defining the file system directory
     * where the repository files are located (value is "home").
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     */
    @Property(value = "")
    public static final String REPOSITORY_HOME_DIR = "home";

    @Property(value = "")
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    // For backwards compatibility loginAdministrative is still enabled
    // In future releases, this default may change to false.
    public static final boolean DEFAULT_LOGIN_ADMIN_ENABLED = true;

    @Property
    public static final String PROPERTY_DEFAULT_WORKSPACE = "defaultWorkspace";

    @Property(boolValue = DEFAULT_LOGIN_ADMIN_ENABLED)
    public static final String PROPERTY_LOGIN_ADMIN_ENABLED = "admin.login.enabled";

    public static final String DEFAULT_ADMIN_USER = "admin";

    @Property(value = DEFAULT_ADMIN_USER)
    public static final String PROPERTY_ADMIN_USER = "admin.name";

    @Reference
    private ServiceUserMapper serviceUserMapper;

    private ComponentContext componentContext;

    private String adminUserName;

    private Map<String, ServiceRegistration> statisticsServices = new ConcurrentHashMap<String, ServiceRegistration>();

    // ---------- Repository Management ----------------------------------------

    @Override
    protected Repository acquireRepository() {

        BundleContext bundleContext = getComponentContext().getBundleContext();

        final String overrideUrl = bundleContext.getProperty(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY);

        // Do not configure the repository if override URL (SLING-254) is set
        if ( overrideUrl != null && !overrideUrl.isEmpty() ) {
            return null;
        }

        String slingHomePath = bundleContext.getProperty("sling.home");
        File homeFile;
        String configURLObj;
        try {
            homeFile = getOrInitRepositoryHome(bundleContext, slingHomePath);
            configURLObj = getOrInitConfigFileUrl(bundleContext, homeFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(getClass().getName() + " initialisation failed", e);
        }

        // ensure absolute home (path)
        String home = homeFile.getAbsolutePath();

        // somewhat dirty hack to have the derby.log file in a sensible
        // location, but don't overwrite anything already set
        if (System.getProperty("derby.stream.error.file") == null) {
            String derbyLog = home + "/derby.log";
            System.setProperty("derby.stream.error.file", derbyLog);
        }

        InputStream ins = null;
        try {

            RepositoryConfig crc;
            if (configURLObj != null && configURLObj.length() > 0) {
                // check whether the URL is a file path
                File configFile = new File(configURLObj);
                if (configFile.canRead()) {

                    ins = new FileInputStream(configFile);
                    log.info("Using configuration file " + configFile.getAbsolutePath());

                } else {

                    try {

                        URL configURL = new URL(configURLObj);
                        ins = configURL.openStream();
                        log.info("Using configuration URL " + configURL);

                    } catch (MalformedURLException mue) {

                        log.info("Configuration File " + configFile.getAbsolutePath()
                            + " has been lost, trying to recreate");

                        final Bundle bundle = bundleContext.getBundle();
                        SlingServerRepositoryManager.copyFile(bundle, "repository.xml", configFile);

                        ins = new FileInputStream(configFile);
                        log.info("Using configuration file " + configFile.getAbsolutePath());
                    }
                }
                crc = RepositoryConfig.create(ins, home);
            } else {
                crc = RepositoryConfig.create(homeFile);
            }

            return registerStatistics(RepositoryImpl.create(crc));

        } catch (IOException ioe) {

            log.error("acquireRepository: IO problem starting repository from " + configURLObj + " in " + home, ioe);

        } catch (RepositoryException re) {

            log.error("acquireRepository: Repository problem starting repository from " + configURLObj + " in " + home,
                re);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // got no repository ....
        return null;
    }

    private File getOrInitRepositoryHome(BundleContext bundleContext, String slingHomePath) throws IOException {

        String repoHomePath = (String) getComponentContext().getProperties().get(REPOSITORY_HOME_DIR);
        if ( repoHomePath == null || repoHomePath.isEmpty() ) {
            repoHomePath = bundleContext.getProperty("sling.repository.home");
        }

        File homeDir;
        if (repoHomePath != null && !repoHomePath.isEmpty()) {
            homeDir = new File(repoHomePath, getRepositoryName(bundleContext));
        } else if (slingHomePath != null) {
            homeDir = new File(slingHomePath, getRepositoryName(bundleContext));
        } else {
            homeDir = new File(getRepositoryName(bundleContext));
        }

        // make sure jackrabbit home exists
        if (!homeDir.isDirectory()) {
            log.info("Creating default config for Jackrabbit in " + homeDir);
            if (!homeDir.mkdirs()) {
                throw new IOException("verifyConfiguration: Cannot create Jackrabbit home "
                        + homeDir + ", failed creating default configuration");
            }
        }

        return homeDir;
    }

    private String getRepositoryName(BundleContext bundleContext) {
        String repoName = bundleContext.getProperty("sling.repository.name");
        if (repoName != null) {
            return repoName; // the repository name is set
        }
        return "jackrabbit";
    }


    private Repository registerStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            try {
                RepositoryImpl repositoryImpl = (RepositoryImpl) repository;
                StatisticsMBeanImpl mbean = new StatisticsMBeanImpl(repositoryImpl);
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                String mbeanName = StatisticsMBeanImpl.getMBeanName(repositoryImpl);
                properties.put("jmx.objectname", mbeanName);
                properties.put(Constants.SERVICE_VENDOR, "Apache");
                statisticsServices.put(
                    mbeanName,
                    getComponentContext().getBundleContext().registerService(DynamicMBean.class.getName(), mbean,
                        properties));
            } catch (Exception e) {
                log.error("Unable to register statistics ", e);
            }
        }
        return repository;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Dictionary<String, Object> getServiceRegistrationProperties() {
        return this.getComponentContext().getProperties();
    }

    /**
     * Returns the Jackrabbit {@code RepositoryManager} interface implemented by
     * the Jackrabbit Repository in addition to the {@code SlingRepository} and
     * {@code Repository} interfaces implemented by the base class.
     *
     * @since bundle version 2.2.0 replacing the previously overwriting of the
     *        now final {@code AbstractSlingRepository.registerService} method.
     */
    @Override
    protected String[] getServiceRegistrationInterfaces() {
        return new String[] {
            SlingRepository.class.getName(), Repository.class.getName(), RepositoryManager.class.getName()
        };
    }

    @Override
    protected AbstractSlingRepository2 create(Bundle usingBundle) {
        return new SlingServerRepository(this, usingBundle, this.adminUserName);
    }

    @Override
    protected void destroy(AbstractSlingRepository2 repositoryServiceInstance) {
        // nothing to do
    }

    @Override
    protected ServiceUserMapper getServiceUserMapper() {
        return this.serviceUserMapper;
    }

    @Override
    protected void disposeRepository(Repository repository) {
        unregisterStatistics(repository);

        if (repository instanceof RepositoryImpl) {

            try {
                ((RepositoryImpl) repository).shutdown();
            } catch (Throwable t) {
                log.error("deactivate: Unexpected problem shutting down repository", t);
            }

        } else {
            log.error("Repository is not a RepositoryImpl, nothing to do");
        }
    }

    private void unregisterStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            String mbeanName = StatisticsMBeanImpl.getMBeanName((RepositoryImpl) repository);
            try {
                ServiceRegistration serviceRegistration = statisticsServices.get(mbeanName);
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            } catch (Exception e) {
                log.warn("Failed to unregister statistics JMX bean {} ", e.getMessage());
            }
            statisticsServices.remove(mbeanName);
        }
    }

    // --------- SCR integration -----------------------------------------------

    protected ComponentContext getComponentContext() {
        return this.componentContext;
    }

    /**
     * This method must be called if overwritten by implementations !!
     */
    @Activate
    private void activate(final ComponentContext componentContext) throws Exception {
        this.componentContext = componentContext;

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = componentContext.getProperties();
        final String defaultWorkspace = PropertiesUtil.toString(properties.get(PROPERTY_DEFAULT_WORKSPACE), null);
        final boolean disableLoginAdministrative = !PropertiesUtil.toBoolean(
            properties.get(PROPERTY_LOGIN_ADMIN_ENABLED), DEFAULT_LOGIN_ADMIN_ENABLED);

        this.adminUserName = PropertiesUtil.toString(properties.get(PROPERTY_ADMIN_USER), DEFAULT_ADMIN_USER);
        super.start(componentContext.getBundleContext(), defaultWorkspace, disableLoginAdministrative);
    }

    /**
     * This method must be called if overwritten by implementations !!
     *
     * @param componentContext
     */
    @Deactivate
    private void deactivate(final ComponentContext componentContext) {
        super.stop();
        this.adminUserName = null;
        this.componentContext = null;
    }

    // ---------- Helper -------------------------------------------------------

    /**
     * Attempts to retrieve the URL of the repository configuration file, creating a default one is no URL is configured
     *
     * @param bundleContext the bundle context
     * @param home the repository home
     * @return the url, or null if the default location is used
     * @throws IOException error when getting or initialising the config file url
     */
    private String getOrInitConfigFileUrl(BundleContext bundleContext, String home) throws IOException {

        String repoConfigFileUrl = (String) getComponentContext().getProperties().get(REPOSITORY_CONFIG_URL);
        if ( repoConfigFileUrl == null || repoConfigFileUrl.isEmpty() ) {
            repoConfigFileUrl = bundleContext.getProperty("sling.repository.config.file.url");
        }
        if (repoConfigFileUrl != null) {
            // the repository config file is set
            URL configFileUrl = null;
            try {
                // verify it is a good url
                configFileUrl = new URL(repoConfigFileUrl);
                return repoConfigFileUrl;
            } catch (MalformedURLException e) {
                // this not an url, trying with "file:"
                configFileUrl = new URL("file:///" + repoConfigFileUrl);
                File configFile = new File(configFileUrl.getFile());
                if (configFile.canRead()) {
                    return configFileUrl.toString();
                }
            }
        }

        // ensure the configuration file exists (inside the home Dir !)
        File configFile = new File(home, "repository.xml");
        boolean copied = false;

        try {
            URL contextConfigURL = new URL("context:repository.xml");
            InputStream contextConfigStream = contextConfigURL.openStream();
            if (contextConfigStream != null) {
                SlingServerRepositoryManager.copyStream(contextConfigStream, configFile);
                copied = true;
            }
        } catch (Exception e) {}

        if (!copied) {
            SlingServerRepositoryManager.copyFile(bundleContext.getBundle(), "repository.xml", configFile);
        }

        // config file is repository.xml (default) in homeDir
        return null;
    }

    private static void copyFile(Bundle bundle, String entryPath, File destFile) throws FileNotFoundException,
            IOException {
        if (destFile.canRead()) {
            // nothing to do, file exists
            return;
        }

        // copy from property
        URL entryURL = bundle.getEntry(entryPath);
        if (entryURL == null) {
            throw new FileNotFoundException(entryPath);
        }

        // check for a file property
        InputStream source = entryURL.openStream();
        copyStream(source, destFile);
    }

    private static void copyStream(InputStream source, File destFile) throws FileNotFoundException, IOException {
        OutputStream dest = null;

        try {

            // ensure path to parent folder of licFile
            destFile.getParentFile().mkdirs();

            dest = new FileOutputStream(destFile);
            byte[] buf = new byte[2048];
            int rd;
            while ((rd = source.read(buf)) >= 0) {
                dest.write(buf, 0, rd);
            }

        } finally {
            if (dest != null) {
                try {
                    dest.close();
                } catch (IOException ignore) {
                }
            }

            // licSource is not null
            try {
                source.close();
            } catch (IOException ignore) {
            }
        }
    }
}
