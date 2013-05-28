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
package org.apache.sling.event.impl.dea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

public class DistributingEventHandlerTest {

    private DistributedEventAdminConfiguration config;

    private DistributedEventReceiver receiver;

    private ResourceResolverFactory factory;
    @Before
    public void setup() throws Exception {
        Environment.APPLICATION_ID = "1234";

        this.factory = new MockResourceResolverFactory();

        this.config = new DistributedEventAdminConfiguration();
        this.config.activate(new HashMap<String, Object>());

        this.receiver = new DistributedEventReceiver();

        PrivateAccessor.setField(this.receiver, "config", this.config);
        PrivateAccessor.setField(this.receiver, "resourceResolverFactory", factory);
        this.receiver.activate();
    }

    @After
    public void cleanup() {
        this.receiver.deactivate();
        Environment.APPLICATION_ID = null;
    }

    @org.junit.Test public void testWriteEvent() throws Exception {
        final String topic = "write/event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", "some value");
        final Event e = new Event(topic, props);
        this.receiver.handleEvent(e);

        Thread.sleep(400);

        final ResourceResolver resolver = this.factory.getAdministrativeResourceResolver(null);
        final Resource rootResource = resolver.getResource(this.config.getOwnRootPath());
        assertNotNull(rootResource);
        final Resource yearResource = rootResource.getChildren().iterator().next();
        assertNotNull(yearResource);
        final Resource monthResource = yearResource.getChildren().iterator().next();
        assertNotNull(monthResource);
        final Resource dayResource = monthResource.getChildren().iterator().next();
        assertNotNull(dayResource);
        final Resource hourResource = dayResource.getChildren().iterator().next();
        assertNotNull(hourResource);
        final Resource minResource = hourResource.getChildren().iterator().next();
        assertNotNull(minResource);

        final Resource eventResource = minResource.getChildren().iterator().next();
        assertNotNull(eventResource);

        final ValueMap vm = ResourceUtil.getValueMap(eventResource);
        assertEquals(topic, vm.get(EventConstants.EVENT_TOPIC));
        assertEquals(Environment.APPLICATION_ID, vm.get(EventUtil.PROPERTY_APPLICATION));
        assertNotNull(vm.get("a property"));

        resolver.delete(eventResource);
        resolver.delete(minResource);
        resolver.delete(hourResource);
        resolver.delete(dayResource);
        resolver.delete(monthResource);
        resolver.delete(yearResource);

        resolver.commit();
    }

    @org.junit.Test public void testWriteEventPlusAppId() throws Exception {
        final String topic = "write/event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", "some value");
        // now we check if the application id is handled correctly
        props.put(EventUtil.PROPERTY_APPLICATION, "foo");

        final Event e = new Event(topic, props);
        this.receiver.handleEvent(e);

        Thread.sleep(400);

        final ResourceResolver resolver = this.factory.getAdministrativeResourceResolver(null);
        final Resource rootResource = resolver.getResource(this.config.getOwnRootPath());
        assertNotNull(rootResource);
        final Resource yearResource = rootResource.getChildren().iterator().next();
        assertNotNull(yearResource);
        final Resource monthResource = yearResource.getChildren().iterator().next();
        assertNotNull(monthResource);
        final Resource dayResource = monthResource.getChildren().iterator().next();
        assertNotNull(dayResource);
        final Resource hourResource = dayResource.getChildren().iterator().next();
        assertNotNull(hourResource);
        final Resource minResource = hourResource.getChildren().iterator().next();
        assertNotNull(minResource);

        final Resource eventResource = minResource.getChildren().iterator().next();
        assertNotNull(eventResource);

        final ValueMap vm = ResourceUtil.getValueMap(eventResource);
        assertEquals(topic, vm.get(EventConstants.EVENT_TOPIC));
        assertEquals(Environment.APPLICATION_ID, vm.get(EventUtil.PROPERTY_APPLICATION));
        assertNotNull(vm.get("a property"));

        resolver.delete(eventResource);
        resolver.delete(minResource);
        resolver.delete(hourResource);
        resolver.delete(dayResource);
        resolver.delete(monthResource);
        resolver.delete(yearResource);

        resolver.commit();
    }
}
