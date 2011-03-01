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
package org.apache.sling.installer.factories.configuration.impl;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Task to install a configuration
 */
public class ConfigInstallTask extends AbstractConfigTask {

    private static final String CONFIG_INSTALL_ORDER = "20-";

    public ConfigInstallTask(final TaskResourceGroup r, final ConfigurationAdmin configAdmin) {
        super(r, configAdmin);
    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER + getCompositePid();
    }

    @SuppressWarnings("unchecked")
	@Override
    public void execute(final InstallationContext ctx) {
        final ConfigurationAdmin ca = this.getConfigurationAdmin();

        // Get or create configuration, but do not
        // update if the new one has the same values.
        boolean created = false;
        try {
            Configuration config = getConfiguration(ca, false);
            if (config == null) {
                created = true;
                config = getConfiguration(ca, true);
            } else {
    			if (ConfigUtil.isSameData(config.getProperties(), getResource().getDictionary())) {
    			    this.getLogger().debug("Configuration {} already installed with same data, update request ignored: {}",
    	                        config.getPid(), getResource());
    				config = null;
    			}
            }

            if (config != null) {
                if (config.getBundleLocation() != null) {
                    config.setBundleLocation(null);
                }
                config.update(getDictionary());
                ctx.log("Installed configuration {} from resource {}", config.getPid(), getResource());
                if ( this.factoryPid != null ) {
                    this.aliasPid = config.getPid();
                }
                this.setFinishedState(ResourceState.INSTALLED, this.getCompositeAliasPid());
                this.getLogger().debug("Configuration " + config.getPid()
                            + " " + (created ? "created" : "updated")
                            + " from " + getResource());
            } else {
                this.setFinishedState(ResourceState.IGNORED, this.getCompositeAliasPid());
            }
        } catch (Exception e) {
            this.getLogger().debug("Exception during installation of config " + this.getResource() + " : " + e.getMessage() + ". Retrying later.", e);
        }
    }
}