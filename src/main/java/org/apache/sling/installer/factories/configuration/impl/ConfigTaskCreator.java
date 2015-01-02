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
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.installer.factories.configuration.ConfigurationConstants;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task creator for configurations.
 */
public class ConfigTaskCreator
    implements InstallTaskFactory, ConfigurationListener, ResourceTransformer {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Configuration admin. */
    private final ConfigurationAdmin configAdmin;

    /** Resource change listener. */
    private final ResourceChangeListener changeListener;

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
            // if this is an uninstall, check if we have to install an older version
            // in this case we should do an update instead of uninstall/install (!)
            final TaskResource second = group.getNextActiveResource();
            if ( second != null
                && ( second.getState() == ResourceState.IGNORED || second.getState() == ResourceState.INSTALLED || second.getState() == ResourceState.INSTALL )
                && ( second.getDictionary() == null || second.getDictionary().get(InstallableResource.RESOURCE_IS_TEMPLATE) == null)) {
                result = new ChangeStateTask(group, ResourceState.UNINSTALLED);
            } else {
                result = new ConfigRemoveTask(group, this.configAdmin);
            }
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
        synchronized ( Coordinator.SHARED ) {
            final String id;
            final String pid;
            if (event.getFactoryPid() == null ) {
                id = event.getPid();
                pid = id;
            } else {
                pid = (event.getPid().startsWith(event.getFactoryPid() + '.') ?
                        event.getPid().substring(event.getFactoryPid().length() + 1) : event.getPid());
                id = event.getFactoryPid() + '.' + event.getPid();
            }
            if ( event.getType() == ConfigurationEvent.CM_DELETED ) {
                final Coordinator.Operation op = Coordinator.SHARED.get(event.getPid(), event.getFactoryPid(), true);
                if ( op == null ) {
                    this.changeListener.resourceRemoved(InstallableResource.TYPE_CONFIG, id);
                } else {
                    this.logger.debug("Ignoring configuration event for {}:{}", event.getPid(), event.getFactoryPid());
                }
            } else if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
                try {
                    final Configuration config = ConfigUtil.getConfiguration(configAdmin,
                            event.getFactoryPid(),
                            event.getPid());
                    final Coordinator.Operation op = Coordinator.SHARED.get(event.getPid(), event.getFactoryPid(), false);
                    if ( config != null && op == null ) {
                        final boolean persist = ConfigUtil.toBoolean(config.getProperties().get(ConfigurationConstants.PROPERTY_PERSISTENCE), true);
                        if ( persist ) {
                            final Dictionary<String, Object> dict = ConfigUtil.cleanConfiguration(config.getProperties());
                            final Map<String, Object> attrs = new HashMap<String, Object>();
                            attrs.put(Constants.SERVICE_PID, event.getPid());
                            if ( event.getFactoryPid() == null ) {
                                attrs.put(InstallableResource.RESOURCE_URI_HINT, pid);
                            } else {
                                attrs.put(InstallableResource.RESOURCE_URI_HINT, event.getFactoryPid() + '-' + pid);
                            }
                            if ( config.getBundleLocation() != null ) {
                                attrs.put(InstallableResource.INSTALLATION_HINT, config.getBundleLocation());
                            }
                            // Factory?
                            if (event.getFactoryPid() != null) {
                                attrs.put(ConfigurationAdmin.SERVICE_FACTORYPID, event.getFactoryPid());
                            }
                            this.changeListener.resourceAddedOrUpdated(InstallableResource.TYPE_CONFIG, id, null, dict, attrs);
                        }
                    } else {
                        this.logger.debug("Ignoring configuration event for {}:{}", event.getPid(), event.getFactoryPid());
                    }
                } catch ( final Exception ignore) {
                    // ignore for now
                }
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
        final String url = separatorsToUnix(resource.getURL());
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
            // quick check if this is an existing configuration
            final String fString = pid.substring(0, n);
            final String cString = pid.substring(n + 1);
            boolean useExtendedPid = false;
            try {
                if ( ConfigUtil.getConfiguration(this.configAdmin, fString, fString + '.' + cString) != null ) {
                    useExtendedPid = true;
                }
            } catch ( final Exception ignore) {
                // ignore this
            }
            if ( useExtendedPid ) {
                configPid = fString + '.' + cString;
            } else {
                configPid = pid.substring(n + 1);
            }
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

    /**
     * Converts all separators to the Unix separator of forward slash.
     *
     * @param path  the path to be changed, null ignored
     * @return the updated path
     */
    private static String separatorsToUnix(String path) {
        if (path == null || path.indexOf('\\') == -1) {
            return path;
        }
        return path.replace('\\', '/');
    }
}
