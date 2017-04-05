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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
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
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class RunModeImplTest {

    private void assertParse(String str, String [] expected) {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock(str, null, null));
        final Set<String> modes = rm.getRunModes();

        Set<String> expectedSet = new HashSet<String>(expected.length);
        for (String expectedEntry : expected) {
            expectedSet.add(expectedEntry);
        }

        assertThat("Parsed runModes match for '" + str + "'", modes, equalTo(expectedSet));
    }

    @org.junit.Test public void testParseRunModes() {
        assertParse(null, new String[0]);
        assertParse("", new String[0]);
        assertParse(" foo \t", new String[] { "foo" });
        assertParse(" foo \t,  bar\n", new String[] { "foo", "bar" });
    }

    private void assertActive(SlingSettingsService s, boolean active, String ...modes) {
        for(String mode : modes) {
            if(active) {
                assertTrue(mode + " should be active", s.getRunModes().contains(mode));
            } else {
                assertFalse(mode + " should NOT be active", s.getRunModes().contains(mode));
            }
        }
    }

    @org.junit.Test public void testMatchesNotEmpty() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", null, null));
        assertActive(rm, true, "foo", "bar");
        assertActive(rm, false, "wiz", "bah", "");
    }

    @org.junit.Test public void testOptions() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", "a,b,c|d,e,f", null));
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test public void testEmptyRunModesWithOptions() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("", "a,b,c|d,e,f", null));
        assertActive(rm, true, "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test public void testOptionsSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e", "a,b,c|d,e,f", null));
        assertActive(rm, true, "foo", "bar", "c", "e");
        assertActive(rm, false, "a", "b", "d", "f");
    }

    @org.junit.Test public void testOptionsMultipleSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e,f,a", "a,b,c|d,e,f", null));
        assertActive(rm, true, "foo", "bar", "a", "e");
        assertActive(rm, false, "b", "c", "d", "f");
    }

    @org.junit.Test public void testOptionsMultipleSelected2() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,f,a,d", "a,b,c|d,e,f", null));
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test public void testInstallOptions() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar", null, "a,b,c|d,e,f"));
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test public void testInstallOptionsSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e", null , "a,b,c|d,e,f"));
        assertActive(rm, true, "foo", "bar", "c", "e");
        assertActive(rm, false, "a", "b", "d", "f");
    }

    @org.junit.Test public void testInstallOptionsMultipleSelected() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,e,f,a", null, "a,b,c|d,e,f"));
        assertActive(rm, true, "foo", "bar", "a", "e");
        assertActive(rm, false, "b", "c", "d", "f");
    }

    @org.junit.Test public void testInstallOptionsMultipleSelected2() {
        final SlingSettingsService rm = new SlingSettingsServiceImpl(new BundleContextMock("foo,bar,c,d,f,a", null, "a,b,c|d,e,f"));
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test public void testInstallOptionsRestart() {
        final BundleContextMock bc = new BundleContextMock("foo,bar,c,e,f,a", null, "a,b,c|d,e,f");

        {
            // create first context to simulate install
            final SlingSettingsService rm = new SlingSettingsServiceImpl(bc);
            assertActive(rm, true, "foo", "bar", "a", "e");
            assertActive(rm, false, "b", "c", "d", "f");
        }

        {
            final SlingSettingsService rm = new SlingSettingsServiceImpl(bc);
            assertActive(rm, true, "foo", "bar", "a", "e");
            assertActive(rm, false, "b", "c", "d", "f");
        }

        // simulate restart with different run modes: new ones that are
        // mentioned in the .options properties are ignored
        bc.update("foo,doo,a,b,c,d,e,f,waa");
        {
            final SlingSettingsService rm = new SlingSettingsServiceImpl(bc);
            assertActive(rm, true, "foo", "doo", "a", "e", "waa");
            assertActive(rm, false, "bar", "b", "c", "d", "f");
        }
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

        @Override
        public void addBundleListener(BundleListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addFrameworkListener(FrameworkListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addServiceListener(ServiceListener listener, String filter)
                throws InvalidSyntaxException {
            // TODO Auto-generated method stub

        }

        @Override
        public void addServiceListener(ServiceListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public Filter createFilter(String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference[] getAllServiceReferences(String clazz,
                String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getBundle(long id) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle[] getBundles() {
            return new Bundle[0];
        }

        @Override
        public File getDataFile(String filename) {
            File f = files.get(filename);
            if ( f == null ) {
                try {
                    f = File.createTempFile(filename, "id");
                    f.delete();
                    files.put(filename, f);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
            return f;
        }

        @Override
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

        @Override
        public Object getService(ServiceReference reference) {
            return null;
        }

        @Override
        public ServiceReference getServiceReference(String clazz) {
            return null;
        }

        @Override
        public ServiceReference[] getServiceReferences(String clazz,
                String filter) throws InvalidSyntaxException {
            return null;
        }

        @Override
        public Bundle installBundle(String location, InputStream input)
                throws BundleException {
            return null;
        }

        @Override
        public Bundle installBundle(String location) throws BundleException {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServiceRegistration registerService(String clazz,
                Object service, Dictionary properties) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServiceRegistration registerService(String[] clazzes,
                Object service, Dictionary properties) {
            return null;
        }

        @Override
        public void removeBundleListener(BundleListener listener) {
        }

        @Override
        public void removeFrameworkListener(FrameworkListener listener) {
        }

        @Override
        public void removeServiceListener(ServiceListener listener) {
        }

        @Override
        public boolean ungetService(ServiceReference reference) {
            return false;
        }

        @Override
        public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory,
                Dictionary<String, ?> properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
                throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getBundle(String location) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}