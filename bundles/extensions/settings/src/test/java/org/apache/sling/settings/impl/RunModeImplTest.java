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
package org.apache.sling.settings.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Set;

import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class RunModeImplTest {

    private void assertParse(String str, String [] expected) {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock(str));
        final Set<String> modes = rm.getRunModes();
        final String[] actual = modes.toArray(new String[modes.size()]);
        assertArrayEquals("Parsed runModes match for '" + str + "'", expected, actual);
    }

    @org.junit.Test public void testParseRunModes() {
        assertParse(null, new String[0]);
        assertParse("", new String[0]);
        assertParse(" foo \t", new String[] { "foo" });
        assertParse(" foo \t,  bar\n", new String[] { "foo", "bar" });
    }

    @org.junit.Test public void testMatchesNotEmpty() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar"));

        assertTrue("single foo should be active", rm.getRunModes().contains("foo"));

        assertFalse("wiz should be not active", rm.getRunModes().contains("wiz"));
        assertFalse("bah should be not active", rm.getRunModes().contains("bah"));
        assertFalse("empty should be not active", rm.getRunModes().contains(""));
    }

    private static final class BundleContextMock implements BundleContext {

        private final String str;

        public BundleContextMock(String str) {
            this.str = str;
        }

        public void addBundleListener(BundleListener listener) {
            // TODO Auto-generated method stub

        }

        public void addFrameworkListener(FrameworkListener listener) {
            // TODO Auto-generated method stub

        }

        public void addServiceListener(ServiceListener listener, String filter)
                throws InvalidSyntaxException {
            // TODO Auto-generated method stub

        }

        public void addServiceListener(ServiceListener listener) {
            // TODO Auto-generated method stub

        }

        public Filter createFilter(String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceReference[] getAllServiceReferences(String clazz,
                String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle getBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle getBundle(long id) {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle[] getBundles() {
            return new Bundle[0];
        }

        public File getDataFile(String filename) {
            try {
                final File f = File.createTempFile("sling", "id");
                f.deleteOnExit();
                return f;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        public String getProperty(String key) {
            return str;
        }

        public Object getService(ServiceReference reference) {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceReference getServiceReference(String clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceReference[] getServiceReferences(String clazz,
                String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle installBundle(String location, InputStream input)
                throws BundleException {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle installBundle(String location) throws BundleException {
            // TODO Auto-generated method stub
            return null;
        }

        @SuppressWarnings("unchecked")
        public ServiceRegistration registerService(String clazz,
                Object service, Dictionary properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @SuppressWarnings("unchecked")
        public ServiceRegistration registerService(String[] clazzes,
                Object service, Dictionary properties) {
            // TODO Auto-generated method stub
            return null;
        }

        public void removeBundleListener(BundleListener listener) {
            // TODO Auto-generated method stub

        }

        public void removeFrameworkListener(FrameworkListener listener) {
            // TODO Auto-generated method stub

        }

        public void removeServiceListener(ServiceListener listener) {
            // TODO Auto-generated method stub

        }

        public boolean ungetService(ServiceReference reference) {
            // TODO Auto-generated method stub
            return false;
        }
    }
}