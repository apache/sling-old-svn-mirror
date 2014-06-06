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
package org.apache.sling.crankstart.core.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that creates OSGi configurations */
public class Configure implements CrankstartCommand {
    public static final String I_CONFIGURE = "config";
    public static final String FACTORY_SUFFIX = ".factory";
    public static final String FELIX_FORMAT_SUFFIX = "FORMAT:felix.config";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return commandLine.getVerb().startsWith(I_CONFIGURE);
    }
    
    public String getDescription() {
        return I_CONFIGURE + " or " + I_CONFIGURE + FACTORY_SUFFIX + ": create OSGi configurations";
    }

    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        
        // Configs can be in our plain format or in Felix .config format, which supports various data types 
        String pid = null;
        boolean felixFormat = false;
        if(commandLine.getQualifier().endsWith(FELIX_FORMAT_SUFFIX)) {
            felixFormat = true;
            pid = commandLine.getQualifier().split(" ")[0].trim();
        } else {
            pid = commandLine.getQualifier();
        }
        
        Dictionary<String, Object> properties = commandLine.getProperties();
        if(felixFormat) {
            properties = parseFelixConfig(properties);
        }
        final BundleContext bundleContext = crankstartContext.getOsgiFramework().getBundleContext();
        
        // TODO: wait for configadmin service?
        final String CONFIG_ADMIN_CLASS = "org.osgi.service.cm.ConfigurationAdmin";
        final ServiceReference configAdminRef = bundleContext.getServiceReference(CONFIG_ADMIN_CLASS);
        if(configAdminRef == null) {
            throw new IllegalStateException("Required service is missing:" + CONFIG_ADMIN_CLASS);
        }
        final Object configAdminService = bundleContext.getService(configAdminRef);
        
        // Use reflection to minimize coupling with the OSGi framework that we are talking to
        Object config = null;
        if(commandLine.getVerb().endsWith(FACTORY_SUFFIX)) {
            config = configAdminService.getClass()
                    .getMethod("createFactoryConfiguration", String.class)
                    .invoke(configAdminService, pid);
        } else {
            config = configAdminService.getClass()
                    .getMethod("getConfiguration", String.class)
                    .invoke(configAdminService, pid);
        }
        config.getClass()
            .getMethod("setBundleLocation", String.class)
            .invoke(config, (String)null);
        config.getClass()
            .getMethod("update", Dictionary.class)
            .invoke(config, properties);
        log.info("Updated configuration {}: {}", pid, properties);
    }
    
    @SuppressWarnings("unchecked")
    private Dictionary<String, Object> parseFelixConfig(Dictionary<String, Object> properties) throws IOException {
        // Build a stream in Felix .config format and parse it
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> keys = properties.keys();
        while(keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final Object value = properties.get(key);
            sb.append(key).append("=").append(value).append("\n");
        }
        
        final InputStream is = new ByteArrayInputStream(sb.toString().getBytes("UTF-8")); 
        try {
            return ConfigurationHandler.read(is);
        } finally {
            is.close();
        }
    }
}
