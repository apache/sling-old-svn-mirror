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

import java.util.Dictionary;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.InternalService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Task creator for configurations.
 */
public class ConfigTaskCreator
    implements InternalService, InstallTaskFactory, ConfigurationListener {

    public static final String ALIAS_KEY = "org.apache.sling.installer.osgi.factoryaliaspid";
    public static final String CONFIG_PATH_KEY = "org.apache.sling.installer.osgi.path";


    /** Interface of the config admin */
    private static String CONFIG_ADMIN_SERVICE_NAME = ConfigurationAdmin.class.getName();

    /** Service tracker for the configuration admin. */
    private ServiceTracker configAdminServiceTracker;

    /** Resource change listener. */
    private ResourceChangeListener changeListener;

    /** Service registration. */
    private ServiceRegistration listenerReg;

    private BundleContext bundleContext;

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext, org.apache.sling.installer.api.ResourceChangeListener)
     */
    public void init(final BundleContext bc, final ResourceChangeListener listener) {
        this.changeListener = listener;
        this.configAdminServiceTracker = new ServiceTracker(bc, CONFIG_ADMIN_SERVICE_NAME, null);
        this.configAdminServiceTracker.open();
        listenerReg = bc.registerService(ConfigurationListener.class.getName(), this, null);
        this.bundleContext = bc;
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#deactivate()
     */
    public void deactivate() {
        if ( this.configAdminServiceTracker != null ) {
            this.configAdminServiceTracker.close();
            this.configAdminServiceTracker = null;
        }
        if ( this.listenerReg != null ) {
            this.listenerReg.unregister();
            this.listenerReg = null;
        }
        this.changeListener = null;
        this.bundleContext = null;
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#getDescription()
     */
    public String getDescription() {
        return "Apache Sling Configuration Install Task Factory";
    }

    /**
     * Create a task to install or uninstall a configuration.
     *
	 * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
	 */
	public InstallTask createTask(final TaskResourceGroup group) {
        final TaskResource toActivate = group.getActiveResource();
        if ( !toActivate.getType().equals(InstallableResource.TYPE_CONFIG) ) {
            return null;
        }
	    // if there is no config admin, just return
	    if ( this.configAdminServiceTracker.getService() == null ) {
            return null;
	    }
	    final InstallTask result;
		if (toActivate.getState() == ResourceState.UNINSTALL) {
		    result = new ConfigRemoveTask(group, this.configAdminServiceTracker);
		} else {
	        result = new ConfigInstallTask(group, this.configAdminServiceTracker);
		}
		return result;
	}

    /**
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @SuppressWarnings("unchecked")
    public void configurationEvent(final ConfigurationEvent event) {
        final String id = (event.getFactoryPid() == null ? "" : event.getFactoryPid() + ".") + event.getPid();
        if ( event.getType() == ConfigurationEvent.CM_DELETED ) {
            this.changeListener.resourceRemoved(InstallableResource.TYPE_CONFIG, id);
        } else {
            final ConfigurationAdmin configAdmin = (ConfigurationAdmin) this.bundleContext.getService(event.getReference());
            if ( configAdmin != null ) {
                try {
                    final Configuration config = ConfigUtil.getConfiguration(configAdmin,
                            event.getFactoryPid(),
                            event.getPid(),
                            false, false);
                    if ( config != null ) {
                        final Dictionary<String, Object> dict = ConfigUtil.cleanConfiguration(config.getProperties());
                        this.changeListener.resourceAddedOrUpdated(InstallableResource.TYPE_CONFIG, id, null, dict);
                    }
                } catch ( final Exception ignore) {
                    // ignore for now (TODO)
                }
            }
        }
    }
}
