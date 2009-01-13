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
package org.apache.sling.jcr.jackrabbit.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.util.Hashtable;

import org.apache.sling.jcr.base.util.RepositoryAccessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Activator</code> TODO
 */
public class Activator implements BundleActivator, ServiceListener {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    public static final String SERVER_REPOSITORY_FACTORY_PID = SlingServerRepository.class.getName();

    /**
     * The name of the configuration property naming the Sling Context for which
     * a factory configuration has been created.
     */
    public static final String SLING_CONTEXT = "sling.context";

    /**
     * The name of the framework property containing the default sling context
     * name.
     */
    public static final String SLING_CONTEXT_DEFAULT = "sling.context.default";

    // The name of the Configuration Admin Service
    private static final String CONFIG_ADMIN_NAME = ConfigurationAdmin.class.getName();

    // this bundle's context, used by verifyConfiguration
    private BundleContext bundleContext;

    // the name of the default sling context
    private String slingContext;

    protected String getRepositoryName() {
    	String repoName = bundleContext.getProperty("sling.repository.name");
    	if (repoName != null)
    		return repoName; // the repository name is set
    	else
    		return "jackrabbit";
    }

    public void start(BundleContext context) {

        this.bundleContext = context;

        // check the name of the default context, nothing to do if none
        slingContext = context.getProperty(SLING_CONTEXT_DEFAULT);
        if (slingContext == null) {
            slingContext = "default";
        }

        ServiceReference sr = context.getServiceReference(CONFIG_ADMIN_NAME);
        if (sr != null) {

            // immediately verify the configuration as the service is here
            verifyConfiguration(sr);

        } else {

            // register as service listener for Configuration Admin to verify
            // the configuration when the service is registered
            try {
                bundleContext.addServiceListener(this, "("
                    + Constants.OBJECTCLASS + "=" + CONFIG_ADMIN_NAME + ")");
            } catch (InvalidSyntaxException ise) {
                log.error(
                    "start: Failed to register for Configuration Admin Service, will not verify configuration",
                    ise);
            }
        }
    }

    public void stop(BundleContext arg0) {
        
        /*
         * when stopping Derby (which is used by Jackrabbit by default) a
         * derby.antiGC thread keeps running which prevents this bundle from
         * being garbage collected ... we try to really stop derby here and
         * ignore the exception since according to
         * http://db.apache.org/derby/docs/10.4/devguide/tdevdvlp20349.html this
         * exception will always be thrown.
         */

        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (Throwable t) {
            // exception is always thrown
        }
    }

    // ---------- ServiceListener ----------------------------------------------

    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {

            // verify the configuration with the newly registered service
            verifyConfiguration(event.getServiceReference());

            // don't care for any more service state changes
            bundleContext.removeServiceListener(this);
        }
    }

    // ---------- internal -----------------------------------------------------

    private void verifyConfiguration(ServiceReference ref) {
        ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService(ref);
        if (ca == null) {
            log.error("verifyConfiguration: Failed to get Configuration Admin Service from Service Reference");
            return;
        }

        try {
            // find a configuration for theses properties...
            Configuration[] cfgs = ca.listConfigurations("("
                + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + SERVER_REPOSITORY_FACTORY_PID + ")");
            if (cfgs != null && cfgs.length > 0) {
                log.info(
                    "verifyConfiguration: {} Configurations available for {}, nothing to do",
                    new Object[] { new Integer(cfgs.length),
                        SERVER_REPOSITORY_FACTORY_PID });
                return;
            }

            // No config, create a default one.
            Hashtable<String, String> defaultConfig = new Hashtable<String, String>();
            final String overrideUrl = bundleContext.getProperty(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY);
            if(overrideUrl != null && overrideUrl.length() > 0) {
                // Ignore other parameters if override URL (SLING-254) is set 
                defaultConfig.put(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY, overrideUrl);
                log.info(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY + "=" + overrideUrl + 
                    ", using it to create the default configuration");
                
            } else {
               initDefaultConfig(defaultConfig, bundleContext); 
            }
            
            // create the factory and set the properties
            Configuration config = ca.createFactoryConfiguration(SERVER_REPOSITORY_FACTORY_PID);
            config.update(defaultConfig);

            log.info("verifyConfiguration: Created configuration {} for {}",
                config.getPid(), config.getFactoryPid());

        } catch (Throwable t) {
            log.error(
                "verifyConfiguration: Cannot check or define configuration", t);
        } finally {
            bundleContext.ungetService(ref);
        }
    }
    
    private void initDefaultConfig(Hashtable<String, String> props, BundleContext bundleContext) throws IOException {
        File homeDir = getHomeDir(bundleContext);
        if (homeDir == null)
        	return;
       
        File configFile = getConfigFile(bundleContext, homeDir);

        // default config values
        props.put(SLING_CONTEXT, slingContext);
        props.put(SlingServerRepository.REPOSITORY_CONFIG_URL,
            configFile.getPath());
        props.put(SlingServerRepository.REPOSITORY_HOME_DIR,
            homeDir.getPath());
        props.put(SlingServerRepository.REPOSITORY_REGISTRATION_NAME,
            this.getRepositoryName());
    }
    
    private File getHomeDir(BundleContext bundleContext) throws IOException {
    	File homeDir;
    	
    	String repoHomePath = bundleContext.getProperty("sling.repository.home");
    	String slingHomePath = bundleContext.getProperty("sling.home");
    	
    	if (repoHomePath != null) {     		
         	homeDir = new File(repoHomePath, getRepositoryName());
        } else if (slingHomePath != null) {
    		homeDir = new File(slingHomePath, getRepositoryName());
    	} else {
    		homeDir = new File(getRepositoryName());
    	} 
    	
    	// make sure jackrabbit home exists
        log.info("Creating default config for Jackrabbit in " + homeDir);
        if (!homeDir.isDirectory()) {
            if (!homeDir.mkdirs()) {
                log.info("verifyConfiguration: Cannot create Jackrabbit home "
                    + homeDir + ", failed creating default configuration");
                return null;
            }
        }	
    	
    	return homeDir;
    }
    
    private File getConfigFile(BundleContext bundleContext, File homeDir) throws IOException {
    	File configFile;
    	
    	String repoConfigFileUrl = bundleContext.getProperty("sling.repository.config.file.url");
    	if (repoConfigFileUrl != null) {
    		// the repository config file is set
    		URL configFileUrl = null;
			try {
				configFileUrl = new URL(repoConfigFileUrl);
			} catch (MalformedURLException e) {
				// this not an url, trying with "file:"
				configFileUrl = new URL("file://" + repoConfigFileUrl);
			}
    		
    		// local support only
    		configFile = new File(configFileUrl.getFile());
    		if (configFile.canRead())
    			return configFile;
    	}
    	
        // ensure the configuration file (inside the home Dir !)
        configFile = new File(homeDir, "repository.xml");
        SlingServerRepository.copyFile(bundleContext.getBundle(), "repository.xml", configFile);
    	return configFile;
    }
    
}
