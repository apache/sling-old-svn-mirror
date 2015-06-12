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
package org.apache.sling.crankstart.launcher;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Dictionary;

import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Setup the OSGi configurations based on a provisioning model */
public class Configurations implements Closeable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String CONFIG_ADMIN_CLASS = "org.osgi.service.cm.ConfigurationAdmin";
    private final Model model;
    private final RunModeFilter rmFilter;
    private final ServiceTracker tracker;
    private ConfigAdminProxy proxy; 
    
    /** We use reflection to talk to ConfigAdmin, to avoid classloader issues as 
     *  the service comes from inside the OSGi framework and we are outside of that.
     */
    private static class ConfigAdminProxy {
        private final Object svc;
        
        ConfigAdminProxy(Object configAdminService) {
            svc = configAdminService;
        }
        
        Object createFactoryConfiguration(String factoryPid) 
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            return svc.getClass()
                    .getMethod("createFactoryConfiguration", String.class)
                    .invoke(svc, factoryPid);

        }
        
        Object getConfiguration(String pid) 
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            return svc.getClass()
                    .getMethod("getConfiguration", String.class)
                    .invoke(svc, pid);
            
        }
        
        void setConfigBundleLocation(Object config, String location) 
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            config.getClass()
            .getMethod("setBundleLocation", String.class)
            .invoke(config, location);
        }
        
        void updateConfig(Object config, Dictionary<String, Object> properties) 
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            config.getClass()
            .getMethod("update", Dictionary.class)
            .invoke(config, properties);
        }
    }
    
    public Configurations(BundleContext ctx, Model m, RunModeFilter rmFilter) {
        model = m;
        this.rmFilter = rmFilter;
        tracker = new ServiceTracker(ctx, CONFIG_ADMIN_CLASS, null);
        tracker.open();
    }
    
    public void close() {
        tracker.close();
    }
    
    /** Activate our configurations if possible, and if not done already.
     *  Can be called as many times as convenient, to make sure this happens
     *  as early as possible.
     */
    public synchronized void maybeConfigure() {
        if(proxy != null) {
            log.debug("Configurations already activated, doing nothing");
            return;
        }
        final Object service = tracker.getService(); 
        if(service == null) {
            log.debug("ConfigurationAdmin service not yet available, doing nothing");
            return;
        }
        
        proxy = new ConfigAdminProxy(service);
        log.info("Activating configurations from provisioning model");
        for(Feature f : model.getFeatures()) {
            for(RunMode r : f.getRunModes()) {
                if(!rmFilter.runModeActive(r)) {
                    log.info("RunMode is not active, ignored: {}", Arrays.asList(r.getNames()));
                    continue;
                }
                for(Configuration c : r.getConfigurations()) {
                    try {
                        setConfig(c);
                    } catch(Exception e) {
                        log.warn("Failed to activate configuration " + c, e);
                    }
                }
            }
        }
    }
    
    private void setConfig(Configuration c) 
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        final String factoryPid = c.getFactoryPid();
        String configSource = null;
        Object config = null;
        if(factoryPid != null) {
            config = proxy.createFactoryConfiguration(factoryPid);
            configSource = "factory PID " + factoryPid;
        } else {
            config = proxy.getConfiguration(c.getPid());
            configSource = "PID " + c.getPid();
        }

        proxy.setConfigBundleLocation(config, null);
        proxy.updateConfig(config, c.getProperties());
        log.info("Created and updated Configuration using [{}]: [{}]", configSource, config);
    }
}