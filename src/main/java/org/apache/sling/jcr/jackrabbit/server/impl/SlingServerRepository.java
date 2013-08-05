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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.management.DynamicMBean;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository;
import org.apache.sling.jcr.jackrabbit.server.impl.jmx.StatisticsMBeanImpl;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AnonCredentials;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServerRepository</code> TODO
 *
 */
@Component(label="%repository.name", description="%repository.description", metatype=true,
    name="org.apache.sling.jcr.jackrabbit.server.SlingServerRepository", configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="Factory for embedded Jackrabbit Repository Instances")
})
public class SlingServerRepository extends AbstractSlingRepository
        implements Repository, SlingRepository, RepositoryManager {

    /**
     * The name of the configuration property defining the URL to the
     * repository configuration file (value is
     * "config").
     * <p>
     * If the configuration file is located in the local file system, the
     * "file:" scheme must still be specified.
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     *
     */
    @Property(value="")
    public static final String REPOSITORY_CONFIG_URL = "config";

    /**
     * The name of the configuration property defining the file system
     * directory where the repository files are located (value is
     * "home").
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     *
     */
    @Property(value="")
    public static final String REPOSITORY_HOME_DIR = "home";

    @Property(value="")
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    private static final Logger LOGGER = LoggerFactory.getLogger(SlingServerRepository.class);

    private Map<String, ServiceRegistration> statisticsServices = new ConcurrentHashMap<String, ServiceRegistration>();

    //---------- Repository Management ----------------------------------------

    @Override
    protected Repository acquireRepository() {
        Repository repository = super.acquireRepository();
        if (repository != null) {
            return registerStatistics(repository);
        }

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> environment = this.getComponentContext().getProperties();
        String configURLObj = (String) environment.get(REPOSITORY_CONFIG_URL);
        String home = (String) environment.get(REPOSITORY_HOME_DIR);

        // ensure absolute home (path)
        File homeFile = new File(home);
        if (!homeFile.isAbsolute()) {
            BundleContext context = getComponentContext().getBundleContext();
            String slingHomePath = context.getProperty("sling.home");
            if (slingHomePath != null) {
                homeFile = new File(slingHomePath, home);
            } else {
                homeFile = homeFile.getAbsoluteFile();
            }
            home = homeFile.getAbsolutePath();
        }

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
                    log(LogService.LOG_INFO, "Using configuration file " + configFile.getAbsolutePath());

                } else {

                    try {

                        URL configURL = new URL(configURLObj);
                        ins = configURL.openStream();
                        log(LogService.LOG_INFO, "Using configuration URL " + configURL);

                    } catch (MalformedURLException mue) {

                        log(LogService.LOG_INFO, "Configuration File "
                            + configFile.getAbsolutePath()
                            + " has been lost, trying to recreate");

                        final Bundle bundle = getComponentContext().getBundleContext().getBundle();
                        SlingServerRepository.copyFile(bundle, "repository.xml", configFile);

                        ins = new FileInputStream(configFile);
                        log(LogService.LOG_INFO, "Using configuration file " + configFile.getAbsolutePath());
                    }
                }
                crc = RepositoryConfig.create(ins, home);
            } else {
                crc = RepositoryConfig.create(homeFile);
            }

            return registerStatistics(RepositoryImpl.create(crc));

        } catch (IOException ioe) {

            log(LogService.LOG_ERROR,
                "acquireRepository: IO problem starting repository from "
                    + configURLObj + " in " + home, ioe);

        } catch (RepositoryException re) {

            log(LogService.LOG_ERROR,
                "acquireRepository: Repository problem starting repository from "
                    + configURLObj + " in " + home, re);
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

    private Repository registerStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            try {
                RepositoryImpl repositoryImpl = (RepositoryImpl) repository;
                StatisticsMBeanImpl mbean = new StatisticsMBeanImpl(
                        repositoryImpl);
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                String mbeanName = StatisticsMBeanImpl
                        .getMBeanName(repositoryImpl);
                properties.put("jmx.objectname", mbeanName);
                properties.put(Constants.SERVICE_VENDOR, "Apache");
                statisticsServices.put(
                        mbeanName,
                        getComponentContext().getBundleContext()
                                .registerService(
                                        DynamicMBean.class
                                                .getName(), mbean, properties));
            } catch (Exception e) {
                LOGGER.error("Unable to register statistics ", e);
            }
        }
        return repository;
    }


    @Override
    protected void disposeRepository(Repository repository) {
        super.disposeRepository(repository);
        unregisterStatistics(repository);

        if (repository instanceof RepositoryImpl) {

            try {
                ((RepositoryImpl) repository).shutdown();
            } catch (Throwable t) {
                log(LogService.LOG_ERROR,
                    "deactivate: Unexpected problem shutting down repository",
                    t);
            }

        } else {
            log(LogService.LOG_INFO,
                "Repository is not a RepositoryImpl, nothing to do");
        }
    }

    private void unregisterStatistics(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            String mbeanName = StatisticsMBeanImpl
                    .getMBeanName((RepositoryImpl) repository);
            try {
                ServiceRegistration serviceRegistration = statisticsServices
                        .get(mbeanName);
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister statistics JMX bean {} ",
                        e.getMessage());
            }
            statisticsServices.remove(mbeanName);
        }
    }

    //---------- Repository Manager Interface Methods -------------------------


    /**
     * @throws UnsupportedOperationException This method is not supported
     *      in this context.
     */
    public void stop() {
        throw new UnsupportedOperationException("Not allowed to manually stop the repository");
    }

    public DataStoreGarbageCollector createDataStoreGarbageCollector() throws RepositoryException {
        RepositoryImpl repository = (RepositoryImpl) getRepository();
        if (repository != null) {
            return repository.createDataStoreGarbageCollector();
        }

        throw new RepositoryException("Repository couldn't be acquired");
    }

    /**
     * Returns the Jackrabbit {@code RepositoryManager} interface implemented by
     * the Jackrabbit Repository in addition to the {@code SlingRepository} and
     * {@code Repository} interfaces implemented by the base class.
     *
     * @since bundle version 2.2.0 replacing the previously overwriting of the
     *        now final {@code AbstractSlingRepository.registerService} method.
     */
    protected String[] getServiceRegistrationInterfaces() {
        return new String[] {
                SlingRepository.class.getName(), Repository.class.getName(), RepositoryManager.class.getName()
        };
    }

    //---------- Helper -------------------------------------------------------

    public static void copyFile(Bundle bundle, String entryPath, File destFile) throws FileNotFoundException, IOException {
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

    public static void copyStream(InputStream source, File destFile) throws FileNotFoundException, IOException {
         OutputStream dest = null;

        try {

            // ensure path to parent folder of licFile
            destFile.getParentFile().mkdirs();

            dest = new FileOutputStream(destFile);
            byte[] buf = new byte[2048];
            int rd;
            while ( (rd = source.read(buf)) >= 0) {
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

    /**
     * {@inheritDoc}
     * @see org.apache.sling.jcr.base.AbstractSlingRepository#getAdministrativeCredentials(java.lang.String)
     */
    @Override
    protected Credentials getAdministrativeCredentials(String adminUser) {
        return new AdministrativeCredentials(adminUser);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.sling.jcr.base.AbstractSlingRepository#getAnonCredentials(java.lang.String)
     */
    @Override
    protected Credentials getAnonCredentials(String anonUser) {
        return new AnonCredentials(anonUser);
    }
}
