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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * The <tt>AbstractFailOnUnexpectedEventsRule</tt> implements the plumbing needed for rules to react on events
 * 
 */
public abstract class AbstractFailOnUnexpectedEventsRule implements EventHandler, TestRule {

    private static final int SETTLE_TIMEOUT_MILLIS = 500;
    private ServiceRegistration<EventHandler> registration;
    private final List<Event> unexpectedEvents = new CopyOnWriteArrayList<>();

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
    
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("event.topics", "org/apache/sling/ide/transport");
        registration = Activator.getDefault().getBundle().getBundleContext()
                .registerService(EventHandler.class, this, props);
    
    }

    protected void after() throws InterruptedException {
    
        if (registration != null) {
            registration.unregister();
        }
    
        waitForEventsToSettle();
    
        if (unexpectedEvents.isEmpty()) {
            return;
        }
    
        StringBuilder desc = new StringBuilder();
        desc.append(getClass().getSimpleName() + " : " + unexpectedEvents.size() + " unexpected events captured:");
        for (Event event : unexpectedEvents) {
    
            String flags = (String) event.getProperty(CommandExecutionProperties.ACTION_FLAGS);
    
            desc.append('\n');
            desc.append(event.getProperty(CommandExecutionProperties.ACTION_TYPE));
            if (flags != null && flags.length() > 0) {
                desc.append(" (").append(flags).append(")");
            }
            desc.append(" -> ");
            desc.append(event.getProperty(CommandExecutionProperties.ACTION_TARGET));
            desc.append(" : ");
            desc.append(event.getProperty(CommandExecutionProperties.RESULT_TEXT));
        }
    
        fail(desc.toString());
    }

    /**
     * Clears the list of unexpected events after the event firing settles
     * 
     * <p>
     * This can be useful for instance when you want to validate that no import events take place after a certain point
     * in time.
     * </p>
     * 
     * <p>
     * Event firing settling is defined as no unexpected events being recorded for {@value #SETTLE_TIMEOUT_MILLIS}
     * milliseconds
     * </p>
     */
    public void clearUnexpectedEventsAfterSettling() throws InterruptedException {
    
        waitForEventsToSettle();
    
        unexpectedEvents.clear();
    }

    protected void addUnexpectedEvent(Event event) {
        unexpectedEvents.add(event);
    }

    protected List<Event> getUnexpectedEvents() {
        return Collections.unmodifiableList(unexpectedEvents);
    }

    private void waitForEventsToSettle() throws InterruptedException {
    
        int currentSize;
        do {
            currentSize = unexpectedEvents.size();
            Thread.sleep(SETTLE_TIMEOUT_MILLIS);
        } while (currentSize != unexpectedEvents.size());
    }

}