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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.INSTALLED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.UPDATED;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process OSGi Configuration resources */
public class ConfigResourceProcessor implements OsgiResourceProcessor {

    private static final String ALIAS_KEY = "_alias_factory_pid";
    public static final String CONFIG_EXTENSION = ".cfg";
    private final ConfigurationAdmin configurationAdmin;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    ConfigResourceProcessor(ConfigurationAdmin ca) {
        configurationAdmin = ca;
    }
    
    public boolean canProcess(String uri) {
        return uri.endsWith(CONFIG_EXTENSION);
    }

    @SuppressWarnings("unchecked")
    public int installOrUpdate(String uri, Map<String, Object> attributes, InputStream data) throws Exception {
        
        // Load configuration properties
        final Dictionary dict = loadDictionary(data);
        
        // Get pids from node name
        final ConfigurationPid pid = new ConfigurationPid(uri);
        log.debug("{} created for uri {}", pid, uri);

        if(pid.getFactoryPid() != null) {
            dict.put(ALIAS_KEY, pid.getFactoryPid());
        }

        // get or create configuration
        int result = UPDATED;
        Configuration config = getConfiguration(pid, false);
        if(config == null) {
            result = INSTALLED;
            config = getConfiguration(pid, true);
        }
        if (config.getBundleLocation() != null) {
            config.setBundleLocation(null);
        }
        config.update(dict);
        log.info("Configuration {} {}", config.getPid(), (result == UPDATED ? "updated" : "created"));
        return result;
    }
    
    Dictionary<?,?> loadDictionary(InputStream data) throws IOException {
        Dictionary<?,?> dict = null;
        try {
            dict = ConfigurationHandler.read(data);
        } finally {
            data.close();
        }
        return dict;
    }

    public void processResourceQueue() throws Exception {
        // TODO might need to retry installing configs, as
        // we do for bundles
    }

    public void uninstall(String uri, Map<String, Object> attributes) throws Exception {
        final ConfigurationPid pid = new ConfigurationPid(uri);
        final Configuration cfg = getConfiguration(pid, false);
        if(cfg == null) {
            log.debug("Config {} deleted but {} not found, ignoring", uri, pid);
        } else {
            log.info("Deleting config {} (uri = {})", pid, uri);
            cfg.delete();
        }
    }

    /** Get or create configuration */
    Configuration getConfiguration(ConfigurationPid cp, boolean createIfNeeded) throws IOException, InvalidSyntaxException {
        Configuration result = null;
        
        if (cp.getFactoryPid() == null) {
            result = configurationAdmin.getConfiguration(cp.getConfigPid(), null);
        } else {
            Configuration configs[] = configurationAdmin.listConfigurations(
                "(|(" + ALIAS_KEY
                + "=" + cp.getFactoryPid() + ")(.alias_factory_pid=" + cp.getFactoryPid()
                + "))");
            
            if (configs == null || configs.length == 0) {
                if(createIfNeeded) {
                    result = configurationAdmin.createFactoryConfiguration(cp.getConfigPid(), null);
                }
            } else {
                result = configs[0];
            }
        }
        
        return result;
    }

}
