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
package org.apache.sling.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.sling.event.impl.jobs.jcr.JCRHelper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;

/**
 * Tests for the EventUtil utility methods.
 */
@RunWith(JMock.class)
public class EventUtilTest {

    protected Mockery context;

    public EventUtilTest() {
        this.context = new JUnit4Mockery();
    }

    @Test public void testDistributeFlag() {
        final Event distributableEvent = EventUtil.createDistributableEvent("some/topic", null);
        assertTrue(EventUtil.shouldDistribute(distributableEvent));
        final Event nonDistributableEvent = new Event("another/topic", (Dictionary<String, Object>)null);
        assertFalse(EventUtil.shouldDistribute(nonDistributableEvent));
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a", "a");
        props.put("b", "b");
        final Event distributableEvent2 = EventUtil.createDistributableEvent("some/topic", props);
        assertTrue(EventUtil.shouldDistribute(distributableEvent2));
        // we should have four properties: 2 custom, one for the dist flag and the fourth for the topic
        assertEquals(4, distributableEvent2.getPropertyNames().length);
        assertEquals("a", distributableEvent2.getProperty("a"));
        assertEquals("b", distributableEvent2.getProperty("b"));
    }

    @Test public void testLocalFlag() {
        final Event localEvent = new Event("local/event", (Dictionary<String, Object>)null);
        assertTrue(EventUtil.isLocal(localEvent));
        final Properties props = new Properties();
        props.put(EventUtil.PROPERTY_APPLICATION, "application1");
        final Event remoteEvent = new Event("remote/event", (Dictionary<Object, Object>)props);
        assertFalse(EventUtil.isLocal(remoteEvent));
    }

    protected Value getValueOfType(final int type, String name) {
        final Value v = this.context.mock(Value.class, name);
        this.context.checking(new Expectations() {{
            allowing(v).getType();will(returnValue(type));
        }});
        return v;
    }

    @Test public void testGetNodePropertyValue() {
        final ValueFactory factory = this.context.mock(ValueFactory.class);
        this.context.checking(new Expectations() {{
            allowing(factory).createValue(true);
            will(returnValue(getValueOfType(PropertyType.BOOLEAN, "booleanValue1")));
            allowing(factory).createValue(false);
            will(returnValue(getValueOfType(PropertyType.BOOLEAN, "booleanValue2")));
            allowing(factory).createValue(with(any(Long.class)));
            will(returnValue(getValueOfType(PropertyType.LONG, "longValue")));
            allowing(factory).createValue(with(any(String.class)));
            will(returnValue(getValueOfType(PropertyType.STRING, "stringValue")));
            allowing(factory).createValue(with(any(Calendar.class)));
            will(returnValue(getValueOfType(PropertyType.DATE, "dateValue")));
        }});
        // boolean
        assertEquals(PropertyType.BOOLEAN, JCRHelper.getNodePropertyValue(factory, true).getType());
        assertEquals(PropertyType.BOOLEAN, JCRHelper.getNodePropertyValue(factory, false).getType());
        assertEquals(PropertyType.BOOLEAN, JCRHelper.getNodePropertyValue(factory, Boolean.TRUE).getType());
        assertEquals(PropertyType.BOOLEAN, JCRHelper.getNodePropertyValue(factory, Boolean.FALSE).getType());
        // long
        assertEquals(PropertyType.LONG, JCRHelper.getNodePropertyValue(factory, (long)5).getType());
        // int = not possible
        assertEquals(null, JCRHelper.getNodePropertyValue(factory, 5));
        // string
        assertEquals(PropertyType.STRING, JCRHelper.getNodePropertyValue(factory, "something").getType());
        // calendar
        assertEquals(PropertyType.DATE, JCRHelper.getNodePropertyValue(factory, Calendar.getInstance()).getType());
    }
}
