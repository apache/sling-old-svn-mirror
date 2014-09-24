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
package org.apache.sling.slingstart.model;

import java.util.Dictionary;
import java.util.Hashtable;


/**
 * Configuration
 */
public class SSMConfiguration {

    private final String pid;

    private final String factoryPid;

    private final Dictionary<String, Object> properties = new Hashtable<String, Object>();

    public SSMConfiguration(final String pid, final String factoryPid) {
        this.pid = (pid != null ? pid.trim() : null);
        this.factoryPid = (factoryPid != null ? factoryPid.trim() : null);
    }

    public String getPid() {
        return this.pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    /**
     * validates the object and throws an IllegalStateException
     * This object needs:
     * - pid
     * - properties
     * - factoryPid is optional
     *
     * @throws IllegalStateException
     */
    public void validate() {
        // check/correct values
        if ( pid == null || pid.isEmpty() ) {
            throw new IllegalStateException("pid");
        }
        if ( properties == null || properties.isEmpty() ) {
            throw new IllegalStateException("properties");
        }
    }

    public boolean isSpecial() {
        if ( pid != null && pid.startsWith(":") ) {
            return true;
        }
        return false;
    }

    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    public void addProperty(final String key, final Object value) {
        this.properties.put(key, value);
    }

    @Override
    public String toString() {
        return "SSMConfiguration [pid=" + pid + ", factoryPid=" + factoryPid
                + ", properties=" + properties + "]";
    }
}
