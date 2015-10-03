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

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.google.common.collect.ImmutableMap;

/**
 * Mock {@link Bundle} implementation.
 */
public final class MockBundle implements Bundle {

    private static volatile long bundleCounter;

    private final long bundleId;
    private final BundleContext bundleContext;
    private Map<String, String> headers = ImmutableMap.<String, String>of();
    private String symbolicName = "mock-bundle";
    private long lastModified;

    /**
     * Constructor
     * @param bundleContext Bundle context
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
        
        String nameToQuery = name.startsWith("/") ? name : "/" + name;

        return getClass().getResource(nameToQuery);
    }

    @Override
    public int getState() {
        return Bundle.ACTIVE;
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return MapUtil.toDictionary(headers);
    }

    @Override
    public Dictionary<String, String> getHeaders(final String locale) {
        // localziation not supported, always return default headers
        return getHeaders();
    }
    
    /**
     * Set headers for mock bundle
     * @param value Header map
     */
    public void setHeaders(Map<String, String> value) {
        this.headers = value;
    }

    @Override
    public String getSymbolicName() {
        return this.symbolicName;
    }

    /**
     * Set symbolic name for mock bundle
     * @param value Symbolic name
     */
    public void setSymbolicName(String value) {
        this.symbolicName = value;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * Set the last modified value for the mock bundle 
     * @param lastModified last modified
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    @Override
    public Enumeration<String> getEntryPaths(final String path) {
        
        String queryPath = path.startsWith("/") ? path : "/" + path;
        
        URL res = getClass().getResource(queryPath);
        if ( res == null ) {
            return null;
        }
        
        Vector<String> matching = new Vector<String>();
        
        try {
            File file = new File(res.toURI());
            if ( file.isDirectory()) {
                for ( File entry : file.listFiles() ) {
                    String name = entry.isDirectory() ? entry.getName() + "/" : entry.getName();
                    matching.add(relativeWithTrailingSlash(queryPath.substring(1, queryPath.length())) + name);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed opening file from " + res , e);
        } catch ( RuntimeException e) {
            throw new RuntimeException("Failed opening file from " + res , e);
        }
        
        if ( matching.isEmpty() ) {
            return null;
        }
        
        return matching.elements();
    }

    private String relativeWithTrailingSlash(String queryPath) {
        
        // make relative
        if ( queryPath.startsWith("/")) {
            queryPath = queryPath.substring(1, queryPath.length());
        }
        
        // remove trailing slash
        if ( !queryPath.isEmpty() && !queryPath.endsWith("/") ) {
            queryPath = queryPath +"/";
        }
        
        return queryPath;
    }
    
    // --- unsupported operations ---
    @Override
    public Enumeration<URL> findEntries(final String path, final String filePattern, final boolean recurse) {
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
    public Enumeration<URL> getResources(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference[] getServicesInUse() {
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
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(final int signersType) {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi 4.2.0
    public Version getVersion() {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi.core 6.0.0
    public int compareTo(Bundle o) {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi.core 6.0.0
    public <A> A adapt(Class<A> type) {
        throw new UnsupportedOperationException();
    }

    // this is part of org.osgi.core 6.0.0
    public File getDataFile(String filename) {
        throw new UnsupportedOperationException();
    }

}
