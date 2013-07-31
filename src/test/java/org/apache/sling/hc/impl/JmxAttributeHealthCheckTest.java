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
package org.apache.sling.hc.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

public class JmxAttributeHealthCheckTest {
    
    private JmxAttributeHealthCheck hc;
    private ResultLog log;
    private Dictionary<String, String> props;
    private ComponentContext ctx;

    @Before
    public void setup() {
        hc = new JmxAttributeHealthCheck();
        log = new ResultLog(LoggerFactory.getLogger(getClass()));
        
        ctx = Mockito.mock(ComponentContext.class);
        props = new Hashtable<String, String>();
        props.put(JmxAttributeHealthCheck.PROP_OBJECT_NAME, "java.lang:type=ClassLoading");
        props.put(JmxAttributeHealthCheck.PROP_ATTRIBUTE_NAME, "LoadedClassCount");
        Mockito.when(ctx.getProperties()).thenReturn(props);
    }
    
    @Test
    public void testJmxAttributeMatch() {
        props.put(JmxAttributeHealthCheck.PROP_CONSTRAINT, "> 10");
        hc.activate(ctx);
        final Result r = hc.execute(log);
        assertTrue(r.isOk());
        assertFalse(r.getLogEntries().isEmpty());
    }
    
    @Test
    public void testJmxAttributeNoMatch() {
        props.put(JmxAttributeHealthCheck.PROP_CONSTRAINT, "< 10");
        hc.activate(ctx);
        final Result r = hc.execute(log);
        assertFalse(r.isOk());
        assertFalse(r.getLogEntries().isEmpty());
    }
}
