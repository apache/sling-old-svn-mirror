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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;

/**
 * Mock implementation of {@link Configuration}.
 */
class MockConfiguration implements Configuration {
    
    private final String pid;
    private Dictionary<String, Object> props;
    
    /**
     * @param pid PID
     */
    public MockConfiguration(String pid) {
        this.pid = pid;
        props = newConfig(pid);
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        // return copy of dictionary
        return new Hashtable<String,Object>(MapUtil.toMap(props));
    }

    @Override
    public void update() {
        // the updating of services already registered in mock-osgi is currently not supported.
        // still allow calling this method to allow usage of {@link update(Dictionary)}, but it works
        // only if applied bevore registering a service in mock-osgi.
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(Dictionary properties) {
        this.props = new Hashtable<String, Object>(MapUtil.toMap(properties));;
        this.props.put(Constants.SERVICE_PID, pid);
        this.update();
    }

    @Override
    public void delete() {
        // just clear the props map
        props = newConfig(pid);
    }
    
    @Override
    public String toString() {
        return props.toString();
    }

    private static Dictionary<String,Object> newConfig(String pid) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put(Constants.SERVICE_PID, pid);
        return config;
    }

    // --- unsupported operations ---
    
    @Override
    public void setBundleLocation(String bundleLocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBundleLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFactoryPid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getChangeCount() {
        throw new UnsupportedOperationException();
    }

}
