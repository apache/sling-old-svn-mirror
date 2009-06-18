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
package org.apache.sling.commons.testing.osgi;

import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class MockBundle implements Bundle {

    private long bundleId;

    public MockBundle(long bundleId) {
        this.bundleId = bundleId;
    }

    public long getBundleId() {
        return bundleId;
    }

    public Enumeration<?> findEntries(String path, String filePattern,
            boolean recurse) {
        return null;
    }

    public URL getEntry(String name) {
        return null;
    }

    public Enumeration<?> getEntryPaths(String path) {
        return null;
    }

    public Dictionary<?, ?> getHeaders() {
        return null;
    }

    public Dictionary<?, ?> getHeaders(String locale) {
        return null;
    }

    public long getLastModified() {
        return 0;
    }

    public String getLocation() {
        return null;
    }

    public ServiceReference[] getRegisteredServices() {
        return null;
    }

    public URL getResource(String name) {
        return null;
    }

    public Enumeration<?> getResources(String name) {
        return null;
    }

    public ServiceReference[] getServicesInUse() {
        return null;
    }

    public int getState() {
        return 0;
    }

    public String getSymbolicName() {
        return null;
    }

    public boolean hasPermission(Object permission) {
        return false;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public void start() {

    }

    public void stop() {

    }

    public void uninstall() {

    }

    public void update() {

    }

    public void update(InputStream in) {

    }

    public BundleContext getBundleContext() {
        return null;
    }

}