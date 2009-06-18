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

import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

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
        final Event nonDistributableEvent = new Event("another/topic", null);
        assertFalse(EventUtil.shouldDistribute(nonDistributableEvent));
    }

    @Test public void testLocalFlag() {
        final Event localEvent = new Event("local/event", null);
        assertTrue(EventUtil.isLocal(localEvent));
        final Properties props = new Properties();
        props.put(EventUtil.PROPERTY_APPLICATION, "application1");
        final Event remoteEvent = new Event("remote/event", props);
        assertFalse(EventUtil.isLocal(remoteEvent));
    }

    @Test public void testGetNodePropertyValue() {
        final ValueFactory factory = this.context.mock(ValueFactory.class);
        this.context.checking(new Expectations() {{
            allowing(factory).createValue(true);
            will(returnValue(new ValueImpl(PropertyType.BOOLEAN)));
            allowing(factory).createValue(false);
            will(returnValue(new ValueImpl(PropertyType.BOOLEAN)));
            allowing(factory).createValue(with(any(Long.class)));
            will(returnValue(new ValueImpl(PropertyType.LONG)));
            allowing(factory).createValue(with(any(String.class)));
            will(returnValue(new ValueImpl(PropertyType.STRING)));
            allowing(factory).createValue(with(any(Calendar.class)));
            will(returnValue(new ValueImpl(PropertyType.DATE)));
        }});
        // boolean
        assertEquals(PropertyType.BOOLEAN, EventUtil.getNodePropertyValue(factory, true).getType());
        assertEquals(PropertyType.BOOLEAN, EventUtil.getNodePropertyValue(factory, false).getType());
        assertEquals(PropertyType.BOOLEAN, EventUtil.getNodePropertyValue(factory, Boolean.TRUE).getType());
        assertEquals(PropertyType.BOOLEAN, EventUtil.getNodePropertyValue(factory, Boolean.FALSE).getType());
        // long
        assertEquals(PropertyType.LONG, EventUtil.getNodePropertyValue(factory, (long)5).getType());
        // int = not possible
        assertEquals(null, EventUtil.getNodePropertyValue(factory, 5));
        // string
        assertEquals(PropertyType.STRING, EventUtil.getNodePropertyValue(factory, "something").getType());
        // calendar
        assertEquals(PropertyType.DATE, EventUtil.getNodePropertyValue(factory, Calendar.getInstance()).getType());
    }

    private final static class ValueImpl implements Value {

        private final int type;

        public ValueImpl(int type) {
            this.type = type;
        }

        public boolean getBoolean() throws ValueFormatException,
                IllegalStateException, RepositoryException {
            return false;
        }

        public Calendar getDate() throws ValueFormatException,
                IllegalStateException, RepositoryException {
            return null;
        }

        public double getDouble() throws ValueFormatException,
                IllegalStateException, RepositoryException {
            return 0;
        }

        public long getLong() throws ValueFormatException,
                IllegalStateException, RepositoryException {
            return 0;
        }

        public InputStream getStream() throws IllegalStateException,
                RepositoryException {
            return null;
        }

        public String getString() throws ValueFormatException,
                IllegalStateException, RepositoryException {
            return null;
        }

        public int getType() {
            return this.type;
        }
    }
}
