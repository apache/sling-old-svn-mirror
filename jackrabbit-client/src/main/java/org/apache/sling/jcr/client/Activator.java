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
package org.apache.sling.jcr.client;

import java.util.Hashtable;

import javax.naming.Context;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Activator</code> TODO
 */
public class Activator implements BundleActivator {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    public static final String CLIENT_REPOSITORY_FACTORY_PID = SlingClientRepository.class.getName();

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

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {

        // check the name of the default context, nothing to do if none
        String slingContext = context.getProperty(SLING_CONTEXT_DEFAULT);
        if (slingContext == null) {
            return;
        }

        ServiceReference sr = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (sr == null) {
            log.info("Activator: Need ConfigurationAdmin Service to ensure configuration");
            return;
        }

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(sr);
        if (ca == null) {
            log.info("Activator: Need ConfigurationAdmin Service to ensure configuration (has gone ?)");
            return;
        }

        try {
            // find a configuration for theses properties...
            Configuration[] cfgs = ca.listConfigurations("("
                + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + CLIENT_REPOSITORY_FACTORY_PID + ")");
            if (cfgs != null && cfgs.length > 0) {
                log.info("Activator: {} Configurations available for {}, nothing to do",
                    new Object[] { new Integer(cfgs.length),
                        CLIENT_REPOSITORY_FACTORY_PID });
                return;
            }

            // we have no configuration, create from default settings
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put(SLING_CONTEXT, slingContext);
            props.put(SlingClientRepository.REPOSITORY_NAME, "crx");
            props.put(Context.PROVIDER_URL, "http://jcr.day.com");
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.day.util.jndi.provider.MemoryInitialContextFactory");

            // create the factory and set the properties
            ca.createFactoryConfiguration(CLIENT_REPOSITORY_FACTORY_PID).update(props);

        } catch (Throwable t) {
            log.error("Activator: Cannot check or define configuration", t);
        } finally {
            context.ungetService(sr);
        }
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext arg0) throws Exception {
        // nothing to do
    }
}
