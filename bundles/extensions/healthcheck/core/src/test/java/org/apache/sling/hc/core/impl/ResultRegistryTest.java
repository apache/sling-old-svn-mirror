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

import java.util.Calendar;

import org.apache.sling.hc.api.Result;
import org.junit.Test;

public class ResultRegistryTest {

    
    @Test
    public void testSingleSubmissionNoExpire() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), null);
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
    }

    @Test
    public void testSingleSubmissionExpireInFuture() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1);
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
    }

    @Test
    public void testSingleSubmissionExpireInPast() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        c.add(Calendar.HOUR, -2); //make the calendar reference become expired.
        Result rv = impl.execute();
        assertEquals(Result.Status.OK, rv.getStatus());
    }

    @Test
    public void testSingleSubmissionChangeWithExpired() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, -1);
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c); 
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
    }
    
    
    @Test
    public void testSingleSubmissionChangeFromDateToNull() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        impl.put("identifier", new Result(Result.Status.WARN, "mycritical"), null); 
        c.add(Calendar.HOUR, -100);
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
    }
    
    @Test
    public void testSingleSubmissionChangeFromNullToDate() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), null);
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c); 
        c.add(Calendar.HOUR, -100);
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
    }
    
    
    @Test
    public void testSingleSubmissionReplaceLessCritial() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.OK, "myok"), c); //this new one should NOT remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertContains("mycritical", rv.toString());
        assertContains("identifier", rv.toString());
    }
    
    @Test
    public void testSingleSubmissionReplaceExpiredCriticalWithLessCritical() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        c.set(Calendar.HOUR, -100); //exipred critical
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c); //this new one should remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.WARN, rv.getStatus());
        assertContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
    }


    @Test
    public void testSingleSubmissionReplace() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c);
        c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c); //this new one should remove the current one.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertNotContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
    }

    @Test
    public void testSingleSubmissionReplaceCalendar() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c1);
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.HOUR, 2); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.WARN, "mywarn"), c2); //this new one should not replace the current one, but replace it's calendar.
        
        c1.add(Calendar.HOUR, -10); //making the first calendar expired.
        
        Result rv = impl.execute();
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
        assertNotContains("mywarn", rv.toString());
        assertContains("identifier", rv.toString());
        assertContains("mycritical", rv.toString());
    }
    
    
    @Test
    public void testSingleSubmissionAdd() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c1);
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("newidentifier", new Result(Result.Status.OK, "myok"), c2); //this new one should add
        
        Result rv = impl.execute();
        assertContains("identifier", rv.toString());
        assertContains("newidentifier", rv.toString());
        assertContains("mycritical", rv.toString());
        assertContains("myok", rv.toString());
        assertEquals(Result.Status.CRITICAL, rv.getStatus());
    }

    @Test
    public void testSingleSubmissionRemove() {
        ResultRegistryImpl impl = new ResultRegistryImpl();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1); //future lets it be stored
        impl.put("identifier", new Result(Result.Status.CRITICAL, "mycritical"), c);
        impl.remove("identifier");
        
        Result rv = impl.execute();
        assertEquals(Result.Status.OK, rv.getStatus());
    }
    
    void assertContains(String expected, String value) {
        assertNotNull(expected);
        assertNotNull(value);
        assertTrue(value + " does not contain " + expected, value.contains(expected));
    }
    void assertNotContains(String expected, String value) {
        assertNotNull(expected);
        assertNotNull(value);
        assertTrue(value + " contains " + expected, !value.contains(expected));
    }
}