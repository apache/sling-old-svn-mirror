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
import java.util.HashMap;
import java.util.Map;
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
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock(str, null, null));
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
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", null, null));

        assertTrue("single foo should be active", rm.getRunModes().contains("foo"));

        assertFalse("wiz should be not active", rm.getRunModes().contains("wiz"));
        assertFalse("bah should be not active", rm.getRunModes().contains("bah"));
        assertFalse("empty should be not active", rm.getRunModes().contains(""));
    }

    @org.junit.Test public void testOptions() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", "a,b,c|d,e,f", null));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("a should be active", rm.getRunModes().contains("a"));
        assertTrue("d should be active", rm.getRunModes().contains("d"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("c should not be active", rm.getRunModes().contains("c"));
        assertFalse("e should not be active", rm.getRunModes().contains("e"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testOptionsSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e", "a,b,c|d,e,f", null));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("c should be active", rm.getRunModes().contains("c"));
        assertTrue("e should be active", rm.getRunModes().contains("e"));
        assertFalse("a should not be active", rm.getRunModes().contains("a"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("d should not be active", rm.getRunModes().contains("d"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testOptionsMultipleSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e,f,a", "a,b,c|d,e,f", null));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("a should be active", rm.getRunModes().contains("a"));
        assertTrue("e should be active", rm.getRunModes().contains("e"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("c should not be active", rm.getRunModes().contains("c"));
        assertFalse("d should not be active", rm.getRunModes().contains("d"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testInstallOptions() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", null, "a,b,c|d,e,f"));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("a should be active", rm.getRunModes().contains("a"));
        assertTrue("d should be active", rm.getRunModes().contains("d"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("c should not be active", rm.getRunModes().contains("c"));
        assertFalse("e should not be active", rm.getRunModes().contains("e"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testInstallOptionsSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e", null , "a,b,c|d,e,f"));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("c should be active", rm.getRunModes().contains("c"));
        assertTrue("e should be active", rm.getRunModes().contains("e"));
        assertFalse("a should not be active", rm.getRunModes().contains("a"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("d should not be active", rm.getRunModes().contains("d"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testInstallOptionsMultipleSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e,f,a", null, "a,b,c|d,e,f"));
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("a should be active", rm.getRunModes().contains("a"));
        assertTrue("e should be active", rm.getRunModes().contains("e"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("c should not be active", rm.getRunModes().contains("c"));
        assertFalse("d should not be active", rm.getRunModes().contains("d"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));
    }

    @org.junit.Test public void testInstallOptionsRestart() {
        final BundleContextMock bc = new BundleContextMock("foo,bar,c,e,f,a", null, "a,b,c|d,e,f");
        new SlingSettingsServiceImpl(bc); // first context to simulate install
        final SlingSettingsService rm = new SlingSettingsServiceImpl(bc);
        assertTrue("foo should be active", rm.getRunModes().contains("foo"));
        assertTrue("bar should be active", rm.getRunModes().contains("bar"));
        assertTrue("a should be active", rm.getRunModes().contains("a"));
        assertTrue("e should be active", rm.getRunModes().contains("e"));
        assertFalse("b should not be active", rm.getRunModes().contains("b"));
        assertFalse("c should not be active", rm.getRunModes().contains("c"));
        assertFalse("d should not be active", rm.getRunModes().contains("d"));
        assertFalse("f should not be active", rm.getRunModes().contains("f"));

        // and another restart with different run modes
        bc.update("foo,doo,a,b,c,d,e,f");
        final SlingSettingsService rm2 = new SlingSettingsServiceImpl(bc);
        assertTrue("foo should be active", rm2.getRunModes().contains("foo"));
        assertTrue("doo should be active", rm2.getRunModes().contains("doo"));
        assertTrue("a should be active", rm2.getRunModes().contains("a"));
        assertTrue("e should be active", rm2.getRunModes().contains("e"));
        assertFalse("bar should not be active", rm2.getRunModes().contains("bar"));
        assertFalse("b should not be active", rm2.getRunModes().contains("b"));
        assertFalse("c should not be active", rm2.getRunModes().contains("c"));
        assertFalse("d should not be active", rm2.getRunModes().contains("d"));
        assertFalse("f should not be active", rm2.getRunModes().contains("f"));
    }

    private static final class BundleContextMock implements BundleContext {

        private String runModes;
        private final String options;
        private final String installOptions;

        private final Map<String, File> files = new HashMap<String, File>();

        public BundleContextMock(String runModes, String options, String installOptions) {
            this.runModes = runModes;
            this.options = options;
            this.installOptions = installOptions;
        }

        public void update(final String rm) {
            this.runModes = rm;
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
            File f = files.get(filename);
            if ( f == null ) {
                try {
                    f = File.createTempFile(filename, "id");
                    f.deleteOnExit();
                    files.put(filename, f);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
            return f;
        }

        public String getProperty(String key) {
            if ( key.equals(SlingSettingsService.RUN_MODES_PROPERTY) ) {
                return runModes;
            } else if ( key.equals(SlingSettingsService.RUN_MODE_OPTIONS) ) {
                return options;
            } else if ( key.equals(SlingSettingsService.RUN_MODE_INSTALL_OPTIONS) ) {
                return installOptions;
            }
            return null;
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