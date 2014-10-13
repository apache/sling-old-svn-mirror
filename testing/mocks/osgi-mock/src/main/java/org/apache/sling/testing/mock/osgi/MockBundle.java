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

import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Mock {@link Bundle} implementation.
 */
class MockBundle implements Bundle {

    private static volatile long bundleCounter;

    private final long bundleId;
    private final BundleContext bundleContext;

    /**
     * Constructor
     */
    public MockBundle(BundleContext bundleContext) {
        this.bundleId = ++bundleCounter;
        this.bundleContext = bundleContext;
    }

    @Override
    public long getBundleId() {
        return this.bundleId;
    }

    @Override
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    @Override
    public URL getEntry(final String name) {
        // try to load resource from classpath
        return getClass().getResource(name);
    }

    @Override
    public int getState() {
        return Bundle.ACTIVE;
    }

    // --- unsupported operations ---
    @Override
    public Enumeration<?> findEntries(final String path, final String filePattern, final boolean recurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<?> getEntryPaths(final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<?, ?> getHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<?, ?> getHeaders(final String locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getResource(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<?> getResources(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSymbolicName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(final Object permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> loadClass(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(final InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start(final int options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(final int options) {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi 4.2.0
    public Map getSignerCertificates(final int signersType) {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi 4.2.0
    public Version getVersion() {
        throw new UnsupportedOperationException();
    }

}
