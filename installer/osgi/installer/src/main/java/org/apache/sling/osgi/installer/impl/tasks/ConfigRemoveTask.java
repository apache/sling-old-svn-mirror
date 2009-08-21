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

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/** Remove a Configuration */
 public class ConfigRemoveTask extends AbstractConfigTask {

    static final String ALIAS_KEY = "_alias_factory_pid";
    static final String CONFIG_PATH_KEY = "_jcr_config_path";
    public static final String [] CONFIG_EXTENSIONS = { ".cfg", ".properties" };
    
    public ConfigRemoveTask(RegisteredResource r) {
        super(r);
    }
    
    @Override
    public String getSortKey() {
        return TaskOrder.CONFIG_REMOVE_ORDER + pid.getCompositePid();
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
        
        final Configuration cfg = getConfiguration(ca, pid, false, ctx);
        if(cfg == null) {
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_DEBUG,
                        "Cannot delete config , pid=" + pid + " not found, ignored (" + resource + ")");
            }
        } else {
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_INFO,
                        "Deleting config " + pid + " (" + resource + ")");
            }
            cfg.delete();
        }
    }
}