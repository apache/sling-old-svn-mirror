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

/** Remove a Configuration */
public class ConfigRemoveTask extends AbstractConfigTask {

    private static final String CONFIG_REMOVE_ORDER = "10-";

    public ConfigRemoveTask(final TaskResourceGroup r,
            final ConfigurationAdmin configAdmin) {
        super(r, configAdmin);
    }

    @Override
    public String getSortKey() {
        return CONFIG_REMOVE_ORDER + getCompositePid();
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void execute(final InstallationContext ctx) {
        synchronized ( Coordinator.SHARED ) {
            try {
                final Configuration cfg = getConfiguration();
                if (cfg == null) {
                    this.getLogger().debug("Cannot delete config , pid={} not found, ignored ({})", getCompositePid(), getResource());
                } else {
                    if ( !ConfigUtil.isSameData(cfg.getProperties(), this.getResource().getDictionary()) ) {
                        this.getLogger().debug("Configuration has changed after it has been installed!");
                    } else {
                        final Coordinator.Operation op = new Coordinator.Operation(cfg.getPid(), cfg.getFactoryPid(), true);

                        this.getLogger().debug("Deleting config {} ({})", getCompositePid(), getResource());
                        cfg.delete();
                        ctx.log("Deleted configuration {} from resource {}", getCompositePid(), getResource());

                        Coordinator.SHARED.add(op);
                    }
                }
            } catch (final Exception e) {
                this.getLogger().debug("Exception during removal of config " + this.getResource() + " : " + e.getMessage() + ". Retrying later.", e);
            }
            // we always set to uninstalled as the resource really has been deleted
            this.setFinishedState(ResourceState.UNINSTALLED);
        }
    }
}