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
package org.apache.sling.hc.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

public class ResultTest {
    
    @Test
    public void testInitiallyOk() {
        final Result result = new Result();
        assertFalse(result.iterator().hasNext());
        assertTrue(result.isOk());
    }
    
    @Test
    public void testDebugLogNoChange() {
        final Result result = new Result();
        result.log(ResultLogEntry.LT_DEBUG, "Some debug message");
        assertTrue(result.iterator().hasNext());
        assertTrue(result.isOk());
    }
    
    @Test
    public void testInfoLogNoChange() {
        final Result result = new Result();
        result.log(ResultLogEntry.LT_INFO, "Some info message");
        assertTrue(result.iterator().hasNext());
        assertTrue(result.isOk());
    }
    
    @Test
    public void testOthersTypesSetStatusWarn() {
        final String [] entryTypes = new String [] {
            ResultLogEntry.LT_WARN,
            ResultLogEntry.LT_WARN_CONFIG,
            ResultLogEntry.LT_WARN_SECURITY,
            "SomeNewLogEntryType" + System.currentTimeMillis()
        };
        
        for(String et : entryTypes) {
            final Result result = new Result();
            result.log(et, "Some message");
            assertTrue(result.iterator().hasNext());
            assertTrue(result.getStatus().equals(Result.Status.WARN));
        }
    }
    
    @Test
    public void testNoStatusChangeIfAlreadyCritical() {
        final Result result = new Result();
        result.setStatus(Result.Status.CRITICAL);
        assertTrue(result.getStatus().equals(Result.Status.CRITICAL));
        result.log(ResultLogEntry.LT_WARN, "Some message");
        assertTrue(result.iterator().hasNext());
        assertTrue(result.getStatus().equals(Result.Status.CRITICAL));
    }
    
    @Test
    public void testSuppliedLogger() {
        final Logger myLogger = Mockito.mock(Logger.class);
        final Result r = new Result(myLogger);
        r.log("foo", "Some message");
        Mockito.verify(myLogger).warn(Matchers.anyString());
    }
    
    @Test
    public void testLogEntries() {
        final Result r = new Result();
        r.log("ONE", "M1");
        r.log("two", "M2");
        r.log("THREE", "M3");
        
        final Iterator<ResultLogEntry> it = r.iterator();
        assertEquals("ONE", it.next().getEntryType());
        assertEquals("two", it.next().getEntryType());
        assertEquals("THREE", it.next().getEntryType());
        assertFalse(it.hasNext());
    }
    
    @Test
    public void testSetStatus() {
        final Result r = new Result();
        assertEquals("Expecting initial OK status", Result.Status.OK, r.getStatus());
        r.setStatus(Result.Status.CRITICAL);
        assertEquals("Expecting CRITICAL status after setting it", Result.Status.CRITICAL, r.getStatus());
        r.setStatus(Result.Status.WARN);
        assertEquals("Still expecting CRITICAL status after setting it to WARN", Result.Status.CRITICAL, r.getStatus());
    }
    
    @Test
    public void testOkIsConsistent() {
        {
            final Result r = new Result();
            assertTrue(r.isOk());
            r.setStatus(Result.Status.OK);
            assertTrue("Expecting isOk for OK Status", r.isOk());
        }
        
        final Result.Status [] ts = {
            Result.Status.WARN,
            Result.Status.CRITICAL,
            Result.Status.HEALTH_CHECK_ERROR
        };

        for(Result.Status s : ts) {
            final Result r = new Result();
            r.setStatus(s);
            assertFalse("Expecting isOk fales for " + s + " Status", r.isOk());
        }
    }
}
