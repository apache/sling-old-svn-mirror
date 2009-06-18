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
package org.apache.sling.jcr.jackrabbit.client;

import java.util.Hashtable;

import javax.naming.Context;

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
    private final Logger log = LoggerFactory.getLogger(this.getClass());

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
    protected static final String CONFIG_ADMIN_NAME = ConfigurationAdmin.class.getName();

    // this bundle's context, used by verifyConfiguration
    protected BundleContext bundleContext;

    // the name of the default sling context
    protected String slingContext;

    /**
     * Return the PID for the configuration.
     * @return
     */
    protected String getClientRepositoryFactoryPID() {
        return SlingClientRepository.class.getName();
    }

    protected void initDefaultConfig(Hashtable<String, String> props) {
        props.put(SLING_CONTEXT, slingContext);
        props.put(SlingClientRepository.REPOSITORY_NAME, "jackrabbit");
        props.put(Context.PROVIDER_URL, "http://sling.apache.org");
        props.put(Context.INITIAL_CONTEXT_FACTORY,
            "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
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
        // nothing to do
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
                + this.getClientRepositoryFactoryPID() + ")");
            if (cfgs != null && cfgs.length > 0) {
                log.info(
                    "verifyConfiguration: {} Configurations available for {}, nothing to do",
                    new Object[] { new Integer(cfgs.length),
                        this.getClientRepositoryFactoryPID() });
                return;
            }

            // we have no configuration, create from default settings
            Hashtable<String, String> defaultConfig = new Hashtable<String, String>();
            final String overrideUrl = bundleContext.getProperty(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY);
            if(overrideUrl != null && overrideUrl.length() > 0) {
                // Ignore other parameters if override URL (SLING-260) is set 
                defaultConfig.put(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY, overrideUrl);
                log.debug(RepositoryAccessor.REPOSITORY_URL_OVERRIDE_PROPERTY + "=" + overrideUrl + 
                    ", using it to create the default configuration");
                
            } else {
               initDefaultConfig(defaultConfig); 
            }

            // create the factory and set the properties
            Configuration config = ca.createFactoryConfiguration(this.getClientRepositoryFactoryPID());
            config.update(defaultConfig);

            log.debug("verifyConfiguration: Created configuration {} for {}",
                config.getPid(), config.getFactoryPid());

        } catch (Throwable t) {
            log.error(
                "verifyConfiguration: Cannot check or define configuration", t);
        } finally {
            bundleContext.ungetService(ref);
        }
    }
}
