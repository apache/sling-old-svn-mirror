/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Dictionary;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.junit.Test;
import org.mockito.Mockito;
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

public class ResultRegistryTest {

    
    
    
    @Test
    public void testSingleSubmissionNoExpire() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), null, "tag1");
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
    }

    @Test
    public void testSingleSubmissionExpireInFuture() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1);
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical", null), c, "tag1");
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
        assertContains("tag1", pbc.getTags());
    }

    @Test
    public void testSingleSubmissionExpireInPast() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        c.add(Calendar.HOUR, -2); //make the calendar reference become expired.
        Result rv = impl.execute();
        assertEquals(Result.Status.OK, rv.getStatus());
        assertNotContains("tag1", pbc.getTags());
    }

    @Test
    public void testSingleSubmissionChangeWithExpired() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, -1);
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag2"); 
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("tag1", pbc.getTags());
        assertNotContains("tag2", pbc.getTags());
    }
    
    
    @Test
    public void testSingleSubmissionChangeFromDateToNull() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        impl.put("identifier", new Result(Result.Status.WARN, "mycritical"), null, "tag2"); 
        c.add(Calendar.HOUR, -100);
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("tag1", pbc.getTags());
        assertNotContains("tag2", pbc.getTags());
    }
    
    @Test
    public void testSingleSubmissionChangeFromNullToDate() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), null, "tag1");
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c, "tag2"); 
        c.add(Calendar.HOUR, -100);
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("tag1", pbc.getTags());
        assertNotContains("tag2", pbc.getTags());
    }
    
    
    @Test
    public void testSingleSubmissionReplaceLessCritial() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.OK, "myok"), c, "tag2"); //this new one should NOT remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("mycritical", rv.toString());
        assertContains("identifier", rv.toString());
        assertContains("tag1", pbc.getTags());
        assertNotContains("tag2", pbc.getTags());
    }
    
    @Test
    public void testSingleSubmissionReplaceExpiredCriticalWithLessCritical() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        c.set(Calendar.HOUR, -100); //exipred critical
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c, "tag2"); //this new one should remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.WARN, rv.getStatus());
        assertContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
        assertNotContains("tag1", pbc.getTags());
        assertContains("tag2", pbc.getTags());
    }


    @Test
    public void testSingleSubmissionReplace() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c, "tag1");
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag2"); //this new one should remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertNotContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
        assertNotContains("tag1", pbc.getTags());
        assertContains("tag2", pbc.getTags());
    }

    @Test
    public void testSingleSubmissionReplaceCalendar() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c1, "tag1");
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.HOUR, 2); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c2, "tag2"); //this new one should not replace the current one, but replace it's calendar.
        
        c1.add(Calendar.HOUR, -10); //making the first calendar expired.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertNotContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
        assertContains("tag1", pbc.getTags());
        assertNotContains("tag2", pbc.getTags());
    }
    
    @Test
    public void testSingleSubmissionAdd() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));
        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c1, "tag1");
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("newidentifier", new Result(Result.Status.OK, "myok"), c2, "tag2"); //this new one should add
        
        Result rv = impl.execute();
        assertContains("identifier", rv.toString());
        assertContains("newidentifier", rv.toString());
        assertContains("mycritical", rv.toString());
        assertContains("myok", rv.toString());
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("tag1", pbc.getTags());
        assertContains("tag2", pbc.getTags());
    }

    @Test
    public void testSingleSubmissionRemove() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        PhonyServiceRegistration pbc = new PhonyServiceRegistration();
        impl.activate(getPhonyBundleContext(pbc));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c, "tag1");
        impl.remove("identifier");
        
        Result rv = impl.execute();
        assertEquals(Result.Status.OK, rv.getStatus());
        assertNotContains("tag1", pbc.getTags());
    }
    
    void assertContains(String expected, String value) {
        assertNotNull(expected);
        assertNotNull(value);
        assertTrue(value + " does not contain " + expected, value.contains(expected));
    }

    void assertContains(String expected, String[] values) {
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for(String value : values) {
            if(expected.equals(value)) {
                found = true;
                break;
            }
            else {
                sb.append(value + ",");
            }
        }
        assertTrue("Unable to find " + expected + " in " + sb.toString(), found);
    }
    
    void assertNotContains(String expected, String[] values) {
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for(String value : values) {
            if(expected.equals(value)) {
                found = true;
            }
            else {
                sb.append(value + ",");
            }
        }
        assertTrue("Found " + expected + " in " + sb.toString(), !found);
    }

    
    void assertNotContains(String expected, String value) {
        assertNotNull(expected);
        assertNotNull(value);
        assertTrue(value + " contains " + expected, !value.contains(expected));
    }
    
    @SuppressWarnings("rawtypes")
    private class PhonyServiceRegistration implements ServiceRegistration {
        @SuppressWarnings("rawtypes")
        Dictionary properties;

        public String[] getTags() {
            if(properties == null) {
                return new String[0];
            }
            String[] rv = (String[]) properties.get(HealthCheck.TAGS);
            if(rv == null) {
                return new String[0];
            }
            return rv;
        }
        
        @Override
        public ServiceReference getReference() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setProperties(Dictionary properties) {
            this.properties = properties;
        }

        @Override
        public void unregister() {
            // TODO Auto-generated method stub
        }
        
    }
    
    BundleContext getPhonyBundleContext(PhonyServiceRegistration psr) {
        BundleContext phony = Mockito.mock(BundleContext.class);
        Mockito.when(phony.registerService((Class<HealthCheck>)Mockito.any(Class.class), Mockito.any(ResultRegistryImpl.class), (Dictionary)Mockito.any())).thenReturn(psr);
        return phony;
    }
       
}