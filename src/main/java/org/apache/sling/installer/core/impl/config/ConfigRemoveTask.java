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
package org.apache.sling.installer.core.impl.config;

import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.OsgiInstallerContext;
import org.apache.sling.installer.core.impl.RegisteredResource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Remove a Configuration */
public class ConfigRemoveTask extends AbstractConfigTask {

    private static final String CONFIG_REMOVE_ORDER = "10-";

    public ConfigRemoveTask(final EntityResourceList r,
            final ServiceTracker configAdminServiceTracker) {
        super(r, configAdminServiceTracker);
    }

    @Override
    public String getSortKey() {
        return CONFIG_REMOVE_ORDER + getCompositePid();
    }

    /**
     * @see org.apache.sling.installer.core.impl.OsgiInstallerTask#execute(org.apache.sling.installer.core.impl.OsgiInstallerContext)
     */
    @SuppressWarnings("unchecked")
    public void execute(final OsgiInstallerContext ctx) {
        final ConfigurationAdmin ca = this.getConfigurationAdmin();
        if (ca == null) {
            this.getLogger().debug("ConfigurationAdmin not available, task will be retried later: {}", this);
            return;
        }

        try {
            final Configuration cfg = getConfiguration(ca, false);
            if (cfg == null) {
                this.getLogger().debug("Cannot delete config , pid={} not found, ignored ({})", getCompositePid(), getResource());
                this.setFinishedState(RegisteredResource.State.IGNORED);
            } else {
                if ( cfg.getProperties().get(ConfigTaskCreator.CONFIG_PATH_KEY) == null ) {
                    this.getLogger().debug("Configuration has not been installed by this resource. Not removing!");
                    this.setFinishedState(RegisteredResource.State.IGNORED);
                } else if ( !isSameData(cfg.getProperties(), this.getResource().getDictionary()) ) {
                    this.getLogger().debug("Configuration has changed after is has been installed. Not removing!");
                    this.setFinishedState(RegisteredResource.State.IGNORED);
                } else {
                    this.getLogger().debug("Deleting config {} ({})", getCompositePid(), getResource());
                    cfg.delete();
                    ctx.log("Deleted configuration {} from resource {}", getCompositePid(), getResource());
                    this.setFinishedState(RegisteredResource.State.UNINSTALLED);
                }
            }
        } catch (Exception e) {
            this.getLogger().debug("Exception during removal of config " + this.getResource() + " : " + e.getMessage() + ". Retrying later.", e);
        }
    }
}