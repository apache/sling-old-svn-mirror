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

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Utilities for configuration handling
 */
abstract class ConfigUtil {

    /**
     * This property has been used in older versions to keep track where the
     * configuration has been installed from.
     */
    private static final String CONFIG_PATH_KEY = "org.apache.sling.installer.osgi.path";

    /**
     * This property has been used in older versions to keep track of factory
     * configurations.
     */
    private static final String ALIAS_KEY = "org.apache.sling.installer.osgi.factoryaliaspid";

    /** Configuration properties to ignore when comparing configs */
    private static final Set<String> IGNORED_PROPERTIES = new HashSet<String>();
    static {
        IGNORED_PROPERTIES.add(Constants.SERVICE_PID);
        IGNORED_PROPERTIES.add(CONFIG_PATH_KEY);
        IGNORED_PROPERTIES.add(ALIAS_KEY);
        IGNORED_PROPERTIES.add(ConfigurationAdmin.SERVICE_FACTORYPID);
    }

    private static Set<String> collectKeys(final Dictionary<String, Object>a) {
        final Set<String> keys = new HashSet<String>();
        final Enumeration<String> aI = a.keys();
        while (aI.hasMoreElements() ) {
            final String key = aI.nextElement();
            if ( !IGNORED_PROPERTIES.contains(key) ) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** True if a and b represent the same config data, ignoring "non-configuration" keys in the dictionaries */
    public static boolean isSameData(Dictionary<String, Object>a, Dictionary<String, Object>b) {
        boolean result = false;
        if (a != null && b != null) {
            final Set<String> keysA = collectKeys(a);
            final Set<String> keysB = collectKeys(b);
            if ( keysA.size() == keysB.size() && keysA.containsAll(keysB) ) {
                for(final String key : keysA ) {
                    final Object valA = a.get(key);
                    final Object valB = b.get(key);
                    if ( valA.getClass().isArray() ) {
                        if ( !Arrays.equals((Object[])valA, (Object[])valB) ) {
                            return result;
                        }
                    } else if ( valA instanceof Number ) {
                        // JCR only supports Long but not Integer
                        // therefore we have to add a special case here!
                        if ( ! (valB instanceof Number) ) {
                            return result;
                        }
                        if ( !(String.valueOf(valA).equals(String.valueOf(valB))) ) {
                            return result;
                        }
                    } else {
                        if ( !a.get(key).equals(b.get(key)) ) {
                            return result;
                        }
                    }
                }
                result = true;
            }
        }
        return result;
    }

    /**
     * Remove all ignored properties
     */
    public static Dictionary<String, Object> cleanConfiguration(final Dictionary<String, Object> config) {
        final Dictionary<String, Object> cleanedConfig = new Hashtable<String, Object>();
        final Enumeration<String> e = config.keys();
        while(e.hasMoreElements()) {
            final String key = e.nextElement();
            if ( !IGNORED_PROPERTIES.contains(key) ) {
                cleanedConfig.put(key, config.get(key));
            }
        }

        return cleanedConfig;
    }

    public static Configuration getConfiguration(final ConfigurationAdmin ca,
            final String factoryPid,
            final String configPid,
            final boolean createIfNeeded)
    throws IOException, InvalidSyntaxException {
        Configuration result = null;

        if (factoryPid == null) {
            if (createIfNeeded) {
                result = ca.getConfiguration(configPid, null);
            } else {
                String filter = "(" + Constants.SERVICE_PID + "=" + configPid
                        + ")";
                Configuration[] configs = ca.listConfigurations(filter);
                if (configs != null && configs.length > 0) {
                    result = configs[0];
                }
            }
        } else {
            Configuration configs[] = null;
            if ( configPid != null ) {
                configs = ca.listConfigurations("(&("
                        + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + factoryPid
                        + ")(" + Constants.SERVICE_PID + "=" + configPid
                        + "))");
            }
            if (configs == null || configs.length == 0) {
                if (createIfNeeded) {
                    result = ca.createFactoryConfiguration(factoryPid, null);
                }
            } else {
                result = configs[0];
            }
        }

        return result;
    }
}
