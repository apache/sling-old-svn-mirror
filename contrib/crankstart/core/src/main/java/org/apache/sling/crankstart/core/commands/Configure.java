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

import java.util.Dictionary;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that logs a message */
public class Configure implements CrankstartCommand {
    public static final String I_CONFIGURE = "config";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_CONFIGURE.equals(commandLine.getVerb());
    }

    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final String pid = commandLine.getQualifier();
        final Dictionary<String, Object> properties = commandLine.getProperties();
        final BundleContext bundleContext = crankstartContext.getOsgiFramework().getBundleContext();
        
        // TODO: wait for configadmin service?
        final String CONFIG_ADMIN_CLASS = "org.osgi.service.cm.ConfigurationAdmin";
        final ServiceReference configAdminRef = bundleContext.getServiceReference(CONFIG_ADMIN_CLASS);
        if(configAdminRef == null) {
            throw new IllegalStateException("Required service is missing:" + CONFIG_ADMIN_CLASS);
        }
        final Object configAdminService = bundleContext.getService(configAdminRef);
        
        // TODO: handle configuration factories
        // For now, use reflection to minimize coupling with the OSGi framework that we are talking to
        final Object config = configAdminService.getClass()
            .getMethod("getConfiguration", String.class)
            .invoke(configAdminService, pid);
        config.getClass()
            .getMethod("setBundleLocation", String.class)
            .invoke(config, (String)null);
        config.getClass()
            .getMethod("update", Dictionary.class)
            .invoke(config, properties);
        log.info("Updated configuration {}: {}", pid, properties);
    }
}
