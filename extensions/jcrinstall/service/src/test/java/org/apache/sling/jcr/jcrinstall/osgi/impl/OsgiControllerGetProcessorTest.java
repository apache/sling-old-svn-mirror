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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class OsgiControllerGetProcessorTest {
  
    private final Mockery mockery = new Mockery();

    @org.junit.Test public void testNoProcessors() throws Exception {
        final OsgiControllerImpl c = new OsgiControllerImpl();
        Utilities.setProcessors(c);
        assertNull("Controller must return null processor for null uri", c.getProcessor(null, null));
        assertNull("Controller must return null processor for TEST uri", c.getProcessor("TEST", null));
    }
    
    @org.junit.Test public void testTwoProcessors() throws Exception {
        final OsgiControllerImpl c = new OsgiControllerImpl();
        final OsgiResourceProcessor p1 = mockery.mock(OsgiResourceProcessor.class);
        final OsgiResourceProcessor p2 = mockery.mock(OsgiResourceProcessor.class);
        Utilities.setProcessors(c, p1, p2);
        
        mockery.checking(new Expectations() {{
            allowing(p1).canProcess("foo", null) ; will(returnValue(true));
            allowing(p1).canProcess("bar", null) ; will(returnValue(false));
            allowing(p2).canProcess("foo", null) ; will(returnValue(false));
            allowing(p2).canProcess("bar", null) ; will(returnValue(true));
        }});
        
        assertEquals("foo extension must return processor p1", p1, c.getProcessor("foo", null));
        assertEquals("bar extension must return processor p2", p2, c.getProcessor("bar", null));
    }
}
