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
package org.apache.sling.event.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.event.EventUtil;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;

@RunWith(JMock.class)
public class DistributingEventHandlerTest extends AbstractRepositoryEventHandlerTest {

    protected Mockery context;

    public DistributingEventHandlerTest() {
        this.handler = new DistributingEventHandler();
        this.context = new JUnit4Mockery();
    }

    @Override
    protected Mockery getMockery() {
        return this.context;
    }

    @org.junit.Test public void testWriteEvent() throws Exception {
        final String topic = "write/event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", "some value");
        final Event e = new Event(topic, props);
        this.handler.writeEvent(e, null);

        final Node rootNode = (Node) session.getItem(this.handler.repositoryPath);
        final NodeIterator iter = rootNode.getNodes();
        iter.hasNext();
        final Node eventNode = iter.nextNode();
        assertEquals(topic, eventNode.getProperty(EventHelper.NODE_PROPERTY_TOPIC).getString());
        assertEquals(handler.applicationId, eventNode.getProperty(EventHelper.NODE_PROPERTY_APPLICATION).getString());
        assertTrue(Calendar.getInstance().compareTo(eventNode.getProperty(EventHelper.NODE_PROPERTY_CREATED).getDate()) >= 0);
        // as a starting point we just check if the properties property exists
        assertTrue(eventNode.hasProperty(ISO9075.encode("a property")));
    }

    @org.junit.Test public void testWriteEventPlusAppId() throws Exception {
        final String topic = "write/event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", "some value");
        // now we check if the application id is handled correctly
        props.put(EventUtil.PROPERTY_APPLICATION, "foo");
        this.handler.writeEvent(new Event(topic, props), null);
        final Node rootNode = (Node) session.getItem(this.handler.repositoryPath);
        final NodeIterator iter = rootNode.getNodes();
        iter.hasNext();
        final Node eventNode = iter.nextNode();
        assertEquals(topic, eventNode.getProperty(EventHelper.NODE_PROPERTY_TOPIC).getString());
        assertEquals(handler.applicationId, eventNode.getProperty(EventHelper.NODE_PROPERTY_APPLICATION).getString());
        assertTrue(Calendar.getInstance().compareTo(eventNode.getProperty(EventHelper.NODE_PROPERTY_CREATED).getDate()) >= 0);
        // as a starting point we just check if the properties property exists
        assertTrue(eventNode.hasProperty(ISO9075.encode("a property")));
    }
}
