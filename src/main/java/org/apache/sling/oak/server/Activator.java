/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.oak.server;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void start(BundleContext context) throws Exception {
        ensureNodeStoreConfig(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
    
    /** Create a config for the Oak SegmentNodeStore unless we already have one */
    private void ensureNodeStoreConfig(BundleContext context) throws Exception {
        final ServiceReference ref = context.getServiceReference(ConfigurationAdmin.class.getName());
        if(ref == null) {
            log.error("verifyConfiguration: Failed to get ConfigurationAdmin ServiceReference");
        }
        final ConfigurationAdmin ca = (ConfigurationAdmin)context.getService(ref);
        if (ca == null) {
            log.error("verifyConfiguration: Failed to get ConfigurationAdmin");
            return;
        }

        final String nodeStoreServicePid = "org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService";
        
        // Nothing to do if we already have a config 
        final Configuration[] cfgs = ca.listConfigurations("("
                + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + nodeStoreServicePid+ ")");
        if (cfgs != null && cfgs.length > 0) {
            log.info("verifyConfiguration: {} Configurations available for {}, nothing to do",
                    cfgs.length, nodeStoreServicePid);
            return;
        }
        
        // Else create a default NodeStore config
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("name", "Default NodeStore config from the oak-server bundle");
        props.put("repository.home", "oak-server-default-NodeStore");
        Configuration config = ca.getConfiguration(nodeStoreServicePid);
        config.setBundleLocation(null);
        config.update(props);
        log.info("Created default NodeStore config {}, properties={}", config.getPid(), props);
    }
    
}