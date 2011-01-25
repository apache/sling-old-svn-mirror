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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base class for configuration-related tasks
 */
abstract class AbstractConfigTask extends AbstractInstallTask {

    /** Configuration properties to ignore when comparing configs */
    protected static final Set<String> ignoredProperties = new HashSet<String>();
    static {
        ignoredProperties.add(Constants.SERVICE_PID);
        ignoredProperties.add(ConfigTaskCreator.CONFIG_PATH_KEY);
        ignoredProperties.add(ConfigTaskCreator.ALIAS_KEY);
    }

    /** Configuration PID */
    protected final String configPid;

    /** Factory PID or null */
    protected final String factoryPid;

    /** Tracker for the configuration admin. */
    private final ServiceTracker configAdminServiceTracker;

    AbstractConfigTask(final TaskResourceGroup r, final ServiceTracker configAdminServiceTracker) {
        super(r);
        this.configAdminServiceTracker = configAdminServiceTracker;
        this.configPid = (String)getResource().getAttribute(Constants.SERVICE_PID);
        this.factoryPid = (String)getResource().getAttribute(ConfigurationAdmin.SERVICE_FACTORYPID);
    }

    /**
     * Get the configuration admin - if available
     */
    protected ConfigurationAdmin getConfigurationAdmin() {
        return (ConfigurationAdmin)this.configAdminServiceTracker.getService();
    }

    protected String getCompositePid() {
        return (factoryPid == null ? "" : factoryPid + ".") + configPid;
    }

    protected Dictionary<String, Object> getDictionary() {
        // Copy dictionary and add pseudo-properties
        final Dictionary<String, Object> d = this.getResource().getDictionary();
        if ( d == null ) {
            return null;
        }

        final Dictionary<String, Object> result = new Hashtable<String, Object>();
        final Enumeration<String> e = d.keys();
        while(e.hasMoreElements()) {
            final String key = e.nextElement();
            result.put(key, d.get(key));
        }

        result.put(ConfigTaskCreator.CONFIG_PATH_KEY, getResource().getURL());
        if ( this.factoryPid != null ) {
            result.put(ConfigTaskCreator.ALIAS_KEY, configPid);
        }

        return result;
    }

    protected Configuration getConfiguration(final ConfigurationAdmin ca,
                                             final boolean createIfNeeded)
    throws IOException, InvalidSyntaxException {
        Configuration result = null;

        if (this.factoryPid == null) {
            if ( createIfNeeded ) {
                result = ca.getConfiguration(this.configPid, null);
            } else {
                String filter = "(" + Constants.SERVICE_PID + "=" + this.configPid + ")";
                Configuration[] configs = ca.listConfigurations( filter );
                if ( configs != null && configs.length > 0 ) {
                    result = configs[0];
                }
            }
        } else {
            Configuration configs[] = ca.listConfigurations(
                "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID
                + "=" + this.factoryPid + ")(" + ConfigTaskCreator.ALIAS_KEY + "=" + configPid
                + "))");

            if (configs == null || configs.length == 0) {
                if (createIfNeeded) {
                    result = ca.createFactoryConfiguration(this.factoryPid, null);
                }
            } else {
                result = configs[0];
            }
        }

        return result;
    }

    private Set<String> collectKeys(final Dictionary<String, Object>a) {
        final Set<String> keys = new HashSet<String>();
        final Enumeration<String> aI = a.keys();
        while (aI.hasMoreElements() ) {
            final String key = aI.nextElement();
            if ( !ignoredProperties.contains(key) ) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** True if a and b represent the same config data, ignoring "non-configuration" keys in the dictionaries */
    protected boolean isSameData(Dictionary<String, Object>a, Dictionary<String, Object>b) {
        boolean result = false;
        if (a != null && b != null) {
            final Set<String> keysA = collectKeys(a);
            final Set<String> keysB = collectKeys(b);
            if ( keysA.size() == keysB.size() && keysA.containsAll(keysB) ) {
                for(final String key : keysA ) {
                    if ( !a.get(key).equals(b.get(key)) ) {
                        return result;
                    }
                }
                result = true;
            }
        }
        return result;
    }
}
