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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * Task creator for configurations.
 */
public class ConfigTaskCreator
    implements InstallTaskFactory, ConfigurationListener, ResourceTransformer {

    public static final String ALIAS_KEY = "org.apache.sling.installer.osgi.factoryaliaspid";
    public static final String CONFIG_PATH_KEY = "org.apache.sling.installer.osgi.path";

    /** Configuration admin. */
    private ConfigurationAdmin configAdmin;

    /** Resource change listener. */
    private ResourceChangeListener changeListener;

    public ConfigTaskCreator(final ResourceChangeListener listener, final ConfigurationAdmin configAdmin) {
        this.changeListener = listener;
        this.configAdmin = configAdmin;
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

        final InstallTask result;
		if (toActivate.getState() == ResourceState.UNINSTALL) {
		    result = new ConfigRemoveTask(group, this.configAdmin);
		} else {
	        result = new ConfigInstallTask(group, this.configAdmin);
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

    /**
     * @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource)
     */
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_PROPERTIES) ) {
            return checkConfiguration(resource);
        }
        return null;
    }

    /**
     * Check if the registered resource is a configuration
     * @param resource The resource
     */
    private TransformationResult[] checkConfiguration(final RegisteredResource resource) {
        final String url = resource.getURL();
        String lastIdPart = url;
        final int pos = lastIdPart.lastIndexOf('/');
        if ( pos != -1 ) {
            lastIdPart = lastIdPart.substring(pos + 1);
        }

        final String pid;
        // remove extension if known
        if ( isConfigExtension(getExtension(lastIdPart)) ) {
            final int lastDot = lastIdPart.lastIndexOf('.');
            pid = lastIdPart.substring(0, lastDot);
        } else {
            pid = lastIdPart;
        }

        // split pid and factory pid alias
        final String factoryPid;
        final String configPid;
        int n = pid.indexOf('-');
        if (n > 0) {
            configPid = pid.substring(n + 1);
            factoryPid = pid.substring(0, n);
        } else {
            factoryPid = null;
            configPid = pid;
        }

        final Map<String, Object> attr = new HashMap<String, Object>();

        attr.put(Constants.SERVICE_PID, configPid);
        // Factory?
        if (factoryPid != null) {
            attr.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }

        final TransformationResult tr = new TransformationResult();
        final String id = (factoryPid == null ? "" : factoryPid + ".") + configPid;
        tr.setId(id);
        tr.setResourceType(InstallableResource.TYPE_CONFIG);
        tr.setAttributes(attr);

        return new TransformationResult[] {tr};
    }

    /**
     * Compute the extension
     */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }

    private static boolean isConfigExtension(String extension) {
        if ( extension.equals("cfg")
                || extension.equals("config")
                || extension.equals("xml")
                || extension.equals("properties")) {
            return true;
        }
        return false;
    }
}
