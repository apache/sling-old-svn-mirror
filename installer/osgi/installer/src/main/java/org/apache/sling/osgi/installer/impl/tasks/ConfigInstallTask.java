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
package org.apache.sling.osgi.installer.impl.tasks;

import java.util.Dictionary;

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/** Install/remove task for configurations 
 *  TODO split into install / remove tasks and reimplement
 * */
public class ConfigInstallTask extends AbstractConfigTask {

    static final String ALIAS_KEY = "_alias_factory_pid";
    static final String CONFIG_PATH_KEY = "_jcr_config_path";
    public static final String [] CONFIG_EXTENSIONS = { ".cfg", ".properties" };
    
    public ConfigInstallTask(RegisteredResource r) {
        super(r);
    }
    
    @Override
    public String getSortKey() {
        return TaskOrder.CONFIG_INSTALL_ORDER + pid.getCompositePid();
    }
    
    @Override
    public String toString() {
        return getClass().getName() + ": " + resource;
    }

    @Override
    public void execute(OsgiInstallerContext ctx) throws Exception {
        super.execute(ctx);
        
        final ConfigurationAdmin ca = ctx.getConfigurationAdmin();
        if(ca == null) {
            ctx.addTaskToNextCycle(this);
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_DEBUG, 
                        "ConfigurationAdmin not available, task will be retried later: " + this);
            }
            return;
        }
        
        // Convert data to a configuration Dictionary
        Dictionary<String, Object> dict = resource.getDictionary();

        if (dict == null) {
            throw new IllegalArgumentException("Null Dictionary for resource " + resource);
        }

        // Add pseudo-properties
        dict.put(CONFIG_PATH_KEY, resource.getUrl());

        // Factory?
        if(pid.getFactoryPid() != null) {
            dict.put(ALIAS_KEY, pid.getFactoryPid());
        }

        // Get or create configuration
        boolean created = false;
        Configuration config = getConfiguration(ca, pid, false, ctx);
        if(config == null) {
            created = true;
            config = getConfiguration(ca, pid, true, ctx);
        }
        if (config.getBundleLocation() != null) {
            config.setBundleLocation(null);
        }
        config.update(dict);
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_INFO,
                    "Configuration " + config.getPid() 
                    + " " + (created ? "created" : "updated") 
                    + " from " + resource);
        }
    }
}