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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class MockBundle implements Bundle {

    private long bundleId;

    public MockBundle(long bundleId) {
        this.bundleId = bundleId;
    }

    @Override
    public long getBundleId() {
        return bundleId;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        return null;
    }

    @Override
    public URL getEntry(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        return null;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) {
        return null;
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        return null;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public String getSymbolicName() {
        return null;
    }

    @Override
    public boolean hasPermission(Object permission) {
        return false;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void uninstall() {

    }

    @Override
    public void update() {

    }

    @Override
    public void update(InputStream in) {

    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public void start(int options) throws BundleException {
    }

    @Override
    public void stop(int options) throws BundleException {
    }

    @Override
    public Map getSignerCertificates(int signersType) {
        return null;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public int compareTo(Bundle o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <A> A adapt(Class<A> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getDataFile(String filename) {
        // TODO Auto-generated method stub
        return null;
    }
}