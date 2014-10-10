/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl.helpers;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * The <tt>FailOnModificationEventsRule</tt> registers an event listener and fails the test under execution if
 * modification events are fired, whether they are successful or not
 *
 */
public class FailOnModificationEventsRule implements EventHandler, TestRule {

    private ServiceRegistration<EventHandler> registration;
    private List<Event> unexpectedEvents = new ArrayList<Event>();

    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    protected void before() {

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("event.topics", "org/apache/sling/ide/transport");
        registration = Activator.getDefault().getBundle().getBundleContext()
                .registerService(EventHandler.class, this, props);

    }

    protected void after() {

        if (registration != null) {
            registration.unregister();
        }

        if (unexpectedEvents.isEmpty()) {
            return;
        }

        StringBuilder desc = new StringBuilder();
        desc.append("Unexpected events captured during import : ");
        for (Event event : unexpectedEvents) {
            desc.append('\n');
            desc.append(event.getProperty(CommandExecutionProperties.ACTION_TYPE));
            desc.append(" -> ");
            desc.append(event.getProperty(CommandExecutionProperties.ACTION_TARGET));
            desc.append(" : ");
            desc.append(event.getProperty(CommandExecutionProperties.RESULT_TEXT));
        }

        fail(desc.toString());
    }

    @Override
    public void handleEvent(Event event) {

        String type = (String) event.getProperty(CommandExecutionProperties.ACTION_TYPE);

        if ("AddOrUpdateNodeCommand".equals(type) || "ReorderChildNodesCommand".equals(type)) {
            unexpectedEvents.add(event);
        }
    }

}