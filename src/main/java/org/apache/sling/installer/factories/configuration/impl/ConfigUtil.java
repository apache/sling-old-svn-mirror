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
     * This property marks the configuration as being deleted.
     */
    public static final String PROPERTY_DELETE_MARKER = "org.apache.sling.installer.configuration.deleted";

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
                result = true;
                for(final String key : keysA ) {
                    final Object valA = a.get(key);
                    final Object valB = b.get(key);
                    if ( valA.getClass().isArray() ) {
                        final Object[] arrA = (Object[])valA;
                        final Object[] arrB = (Object[])valB;

                        if ( arrA.length != arrB.length ) {
                            result = false;
                            break;
                        }
                        for(int i=0; i<arrA.length; i++) {
                            if ( !(String.valueOf(arrA[i]).equals(String.valueOf(arrB[i]))) ) {
                                result = false;
                                break;
                            }
                        }
                    } else {
                        // we always do a string comparison
                        if ( !(String.valueOf(valA).equals(String.valueOf(valB))) ) {
                            result = false;
                            break;
                        }
                    }
                }
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

    /**
     * Encode the value for the ldap filter: \, *, (, and ) should be escaped.
     */
    private static String encode(final String value) {
        return value.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    public static Configuration getConfiguration(final ConfigurationAdmin ca,
            final String factoryPid,
            final String configPid)
    throws IOException, InvalidSyntaxException {
        return getOrCreateConfiguration(ca, factoryPid, configPid, null, false);
    }

    public static Configuration createConfiguration(final ConfigurationAdmin ca,
            final String factoryPid,
            final String configPid,
            final String location)
    throws IOException, InvalidSyntaxException {
        return getOrCreateConfiguration(ca, factoryPid, configPid, location, true);
    }

    private static Configuration getOrCreateConfiguration(final ConfigurationAdmin ca,
            final String factoryPid,
            final String configPid,
            final String location,
            final boolean createIfNeeded)
    throws IOException, InvalidSyntaxException {
        Configuration result = null;

        if (factoryPid == null) {
            if (createIfNeeded) {
                result = ca.getConfiguration(configPid, location);
            } else {
                String filter = "(" + Constants.SERVICE_PID + "=" + encode(configPid)
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
                        + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + encode(factoryPid)
                        + ")(" + Constants.SERVICE_PID + "=" + encode(configPid)
                        + "))");
            }
            if (configs == null || configs.length == 0) {
                // check for old style with alias pid
                configs = ca.listConfigurations(
                        "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID
                        + "=" + factoryPid + ")(" + ALIAS_KEY + "=" + encode(configPid)
                        + "))");

                if (configs == null || configs.length == 0) {
                    if (createIfNeeded) {
                        result = ca.createFactoryConfiguration(factoryPid, location);
                    }
                } else {
                    result = configs[0];
                }
            } else {
                result = configs[0];
            }
        }

        return result;
    }

    public static boolean toBoolean(final Object obj, final boolean defaultValue) {
        boolean result = defaultValue;
        if ( obj != null ) {
            if (obj instanceof Boolean) {
                result = ((Boolean) obj).booleanValue();
            } else {
                result = Boolean.valueOf(String.valueOf(obj));
            }
        }
        return result;
    }
}
