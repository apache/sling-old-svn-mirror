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
package org.apache.sling.crankstart.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrankstartContext {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Framework osgiFramework;
    private final Map<String, String> osgiFrameworkProperties = new HashMap<String, String>();
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    
    private final Map<String, String> defaults = new HashMap<String, String>();
    
    /** Name of the default value used to set bundle start levels */
    public static final String DEFAULT_BUNDLE_START_LEVEL = "crankstart.bundle.start.level";

    public void setOsgiFrameworkProperty(String key, String value) {
        osgiFrameworkProperties.put(key, value);
    }
    
    public Map<String, String> getOsgiFrameworkProperties() {
        return Collections.unmodifiableMap(osgiFrameworkProperties);
    }
    
    public void setOsgiFramework(Framework f) {
        if(osgiFramework != null) {
            throw new IllegalStateException("OSGi framework already set");
        }
        osgiFramework = f;
        
        // Shutdown the framework when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if(osgiFramework != null && osgiFramework.getState() == Bundle.ACTIVE) {
                    try {
                        log.info("Stopping the OSGi framework");
                        osgiFramework.stop();
                        log.info("Waiting for the OSGi framework to exit");
                        osgiFramework.waitForStop(0);
                        log.info("OSGi framework stopped");
                    } catch(Exception e) {
                        log.error("Exception while stopping OSGi framework", e);
                    }
                }
            }
        });
    }
    
    public Framework getOsgiFramework() {
        return osgiFramework;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public Map<String, String> getDefaults() {
        return defaults;
    }
}