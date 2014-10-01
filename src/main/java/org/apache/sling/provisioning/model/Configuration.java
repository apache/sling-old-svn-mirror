/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model;

import java.util.Dictionary;
import java.util.Hashtable;


/**
 * A configuration has either
 * - a pid
 * - or a factory pid and an alias (pid)
 * and properties.
 */
public class Configuration extends Commentable {

    /** The pid. */
    private final String pid;
    /** The factory pid. */
    private final String factoryPid;
    /** The properties. */
    private final Dictionary<String, Object> properties = new Hashtable<String, Object>();

    /**
     * Create a new configuration
     * @param pid The pid or alias for a factory pid
     * @param factoryPid The factory pid
     */
    public Configuration(final String pid, final String factoryPid) {
        this.pid = (pid != null ? pid.trim() : null);
        this.factoryPid = (factoryPid != null ? factoryPid.trim() : null);
    }

    /**
     * Get the pid.
     * If this is a factory configuration, it returns the alias for the configuration
     * @return The pid.
     */
    public String getPid() {
        return this.pid;
    }

    /**
     * Return the factory pid
     * @return The factory pid or null.
     */
    public String getFactoryPid() {
        return this.factoryPid;
    }

    /**
     * Is this a special configuration?
     * @return Special config
     */
    public boolean isSpecial() {
        if ( pid != null && pid.startsWith(":") ) {
            return true;
        }
        return false;
    }

    /**
     * Get all properties of the configuration.
     * @return The properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public String toString() {
        return "Configuration [pid=" + pid
                + ", factoryPid=" + factoryPid
                + ", properties=" + properties
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
