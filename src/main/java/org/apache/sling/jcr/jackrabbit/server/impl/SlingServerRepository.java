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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.BeanFactory;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AnonCredentials;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.xml.sax.InputSource;

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

    @Reference
    private BeanFactory beanFactory;

    //---------- Repository Management ----------------------------------------

    @Override
    protected Repository acquireRepository() {
        Repository repository = super.acquireRepository();
        if (repository != null) {
            return repository;
        }

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> environment = this.getComponentContext().getProperties();
        String configURLObj = (String) environment.get(REPOSITORY_CONFIG_URL);
        String home = getAbsoluteHomePath(environment,getComponentContext().getBundleContext());
        File homeFile = new File(home);
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
                crc = create(new InputSource(ins), homeFile);
            } else {
                crc = create(getRepositoryConfigSource(homeFile), homeFile);
            }

            return RepositoryImpl.create(crc);

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


    @Override
    protected void disposeRepository(Repository repository) {
        super.disposeRepository(repository);

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
     * Overrides the registerService method of <code>AbstractSlingRepository</code>, in order to register
     * <code>org.apache.jackrabbit.api.management.RepositoryManager</code> Service using the
     * component properties as service registration properties.
     *
     * @return The OSGi <code>ServiceRegistration</code> object representing
     *         the registered service.
     *
     * @see org.apache.sling.jcr.base.AbstractSlingRepository#registerService()
     */
    @Override
    protected ServiceRegistration registerService() {

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = getComponentContext().getProperties();

        String[] interfaces = new String[] {
            SlingRepository.class.getName(), Repository.class.getName(), RepositoryManager.class.getName()
        };

        return getComponentContext().getBundleContext().registerService(interfaces, this, props);
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

    public static String getAbsoluteHomePath(Dictionary<String, Object> p,BundleContext context){
        String home = (String) p.get(REPOSITORY_HOME_DIR);

        // ensure absolute home (path)
        File homeFile = new File(home);
        if (!homeFile.isAbsolute()) {
            String slingHomePath = context.getProperty("sling.home");
            if (slingHomePath != null) {
                homeFile = new File(slingHomePath, home);
            } else {
                homeFile = homeFile.getAbsoluteFile();
            }
            home = homeFile.getAbsolutePath();
        }
        return home;
    }

    public static InputSource getRepositoryConfigSource(File homeFile) throws FileNotFoundException {
        return new InputSource(new FileInputStream(new File(homeFile, "repository.xml")));
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

    private RepositoryConfig create(InputSource xml, File dir) throws ConfigurationException {
        java.util.Properties variables = new java.util.Properties(System.getProperties());
        variables.setProperty(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE, dir.getPath());
        RepositoryConfigurationParser parser =
                new RepositoryConfigurationParser(variables);
        parser.setBeanFactory(beanFactory);
        RepositoryConfig config = parser.parseRepositoryConfig(xml);
        config.init();
        return config;
    }

}
