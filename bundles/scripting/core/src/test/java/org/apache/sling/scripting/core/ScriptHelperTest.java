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
package org.apache.sling.scripting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

public class ScriptHelperTest {
    
    @Rule
    public SlingContext sling = new SlingContext();
    
    private ScriptHelper sh;
    private final int [] RANKINGS = { 42, 62, -12, 76, -123, 0, 7432, -21 };
    
    @Before
    public void setup() {
        sh = new ScriptHelper(sling.bundleContext(), null);
        
        for(int rank : RANKINGS) {
            final Integer svc = rank;
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_RANKING, rank);
            sling.bundleContext().registerService(Integer.class.getName(), svc, props);
        }
    }
    
    private void assertHigherRankingComesFirst(Integer ...values) {
        Integer previous = null;
        for(Integer current : values) {
            if(previous != null && current > previous) {
                fail("Ranking " + current + " is higher than previous " + previous);
            }
            previous = current;
        }
    }
    
    @Test
    public void testNullRefs() {
        assertNull("Expecting null if no services found", sh.getService(ScriptHelperTest.class));
    }
    
    @Test
    public void testGetServicesOrdering() {
        final Integer [] svc = sh.getServices(Integer.class, null);
        assertNotNull(svc);
        assertEquals(RANKINGS.length, svc.length);
        assertHigherRankingComesFirst(svc);
    }
}