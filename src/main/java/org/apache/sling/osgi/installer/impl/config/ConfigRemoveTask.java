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
package org.apache.sling.osgi.installer.impl.config;

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Remove a Configuration */
public class ConfigRemoveTask extends AbstractConfigTask {

    private static final String CONFIG_REMOVE_ORDER = "10-";

    static final String ALIAS_KEY = "_alias_factory_pid";
    static final String CONFIG_PATH_KEY = "_jcr_config_path";
    public static final String [] CONFIG_EXTENSIONS = { ".cfg", ".properties" };

    public ConfigRemoveTask(final RegisteredResource r,
            final ServiceTracker configAdminServiceTracker) {
        super(r, configAdminServiceTracker);
    }

    @Override
    public String getSortKey() {
        return CONFIG_REMOVE_ORDER + pid.getCompositePid();
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerTask#execute(org.apache.sling.osgi.installer.impl.OsgiInstallerContext)
     */
    public void execute(final OsgiInstallerContext ctx) {
        final ConfigurationAdmin ca = this.getConfigurationAdmin();
        if (ca == null) {
            ctx.addTaskToNextCycle(this);
            this.getLogger().debug("ConfigurationAdmin not available, task will be retried later: {}", this);
            return;
        }

        try {
            final Configuration cfg = getConfiguration(ca, pid, false);
            if (cfg == null) {
                this.getLogger().debug("Cannot delete config , pid={} not found, ignored ({})", pid, getResource());
                this.getResource().setState(RegisteredResource.State.IGNORED);
            } else {
                this.getLogger().debug("Deleting config {} ({})", pid, getResource());
                cfg.delete();
                ctx.log("Deleted configuration {} from resource {}", pid, getResource());
                this.getResource().setState(RegisteredResource.State.UNINSTALLED);
            }
        } catch (Exception e) {
            this.getLogger().debug("Exception during removal of config " + this.getResource() + " : " + e.getMessage() + ". Retrying later.", e);
            ctx.addTaskToNextCycle(this);
        }
    }
}