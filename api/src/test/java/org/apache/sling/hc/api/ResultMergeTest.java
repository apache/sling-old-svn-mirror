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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class ResultMergeTest {

    private Result result;
    
    @Before
    public void setup() {
        result = new Result();
        result.setStatus(Result.Status.WARN);
        result.log(ResultLogEntry.LT_DEBUG, "BM1 debug");
        result.log(ResultLogEntry.LT_INFO, "BM2 info");
    }
    
    private void assertResult(Result.Status status, String ... messages) {
        assertEquals(status, result.getStatus());
        final Iterator<ResultLogEntry> it = result.iterator();
        for(String msg : messages) {
            assertTrue("Expecting " + msg + " to be present", it.hasNext());
            assertEquals(msg, it.next().getMessage());
        }
    }
    
    @Test
    public void testInitialState() {
        assertResult(Result.Status.WARN, "BM1 debug", "BM2 info");
    }
    
    @Test
    public void testMergeNoResults() {
        result.merge();
        assertResult(Result.Status.WARN, "BM1 debug", "BM2 info");
    }
    
    @Test
    public void testMergeSingleResult() {
        final Result r2 = new Result();
        r2.log("FOO", "T1");
        r2.log("FOO", "T2");
        result.merge(r2);
        assertResult(Result.Status.WARN, "BM1 debug", "BM2 info", "T1", "T2");
    }
    
    @Test
    public void testMergeMultipleResults() {
        final Result [] more = new Result[3];
        for(int i=0 ; i < more.length; i++) {
            more[i] = new Result();
            if(i==1) {
                more[i].setStatus(Result.Status.CRITICAL);
            }
            more[i].log("BAR", "X" + i);
        }
        result.merge(more);
        assertResult(Result.Status.CRITICAL, "BM1 debug", "BM2 info", "X0", "X1", "X2");
    }
    
}
