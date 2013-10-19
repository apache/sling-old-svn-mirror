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
package org.apache.sling.event.it;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.sling.event.EventUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class DeprecatedTimedJobsTest extends AbstractJobHandlingTest {

    private static final String TOPIC = "timed/test/topic";

    @Inject
    private EventAdmin eventAdmin;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testTimedJob() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        final ServiceRegistration ehReg = this.registerEventHandler(TOPIC, new EventHandler() {

            @Override
            public void handleEvent(final Event event) {
                if ( TOPIC.equals(event.getTopic()) ) {
                    counter.incrementAndGet();
                }
            }
        });
        try {
            final Date d = new Date();
            d.setTime(System.currentTimeMillis() + 2000); // run in 2 seconds
            // send timed event
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventUtil.PROPERTY_TIMED_EVENT_TOPIC, TOPIC);
            props.put(EventUtil.PROPERTY_TIMED_EVENT_DATE, d);
            this.eventAdmin.sendEvent(new Event(EventUtil.TOPIC_TIMED_EVENT, props));

            while ( counter.get() == 0 ) {
                this.sleep(1000);
            }
        } finally {
            ehReg.unregister();
        }
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testPeriodicTimedJob() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        final ServiceRegistration ehReg = this.registerEventHandler(TOPIC, new EventHandler() {

            @Override
            public void handleEvent(final Event event) {
                if ( TOPIC.equals(event.getTopic()) ) {
                    counter.incrementAndGet();
                }
            }
        });
        try {
            // send timed event
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventUtil.PROPERTY_TIMED_EVENT_TOPIC, TOPIC);
            props.put(EventUtil.PROPERTY_TIMED_EVENT_PERIOD, 1L);
            props.put(EventUtil.PROPERTY_TIMED_EVENT_ID, "id");
            this.eventAdmin.sendEvent(new Event(EventUtil.TOPIC_TIMED_EVENT, props));

            while ( counter.get() < 5 ) {
                this.sleep(1000);
            }
            final Dictionary<String, Object> props2 = new Hashtable<String, Object>();
            props2.put(EventUtil.PROPERTY_TIMED_EVENT_TOPIC, TOPIC);
            props2.put(EventUtil.PROPERTY_TIMED_EVENT_ID, "id");

            this.eventAdmin.sendEvent(new Event(EventUtil.TOPIC_TIMED_EVENT, props2));
            int current = counter.get();
            this.sleep(2000);
            if ( counter.get() != current && counter.get() != current + 1 ) {
                fail("Events are still sent");
            }
        } finally {
            ehReg.unregister();
        }
    }
}
