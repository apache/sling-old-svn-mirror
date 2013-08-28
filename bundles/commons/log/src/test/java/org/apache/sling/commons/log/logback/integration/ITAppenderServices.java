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

package org.apache.sling.commons.log.logback.integration;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

import org.apache.sling.commons.log.logback.integration.LogTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITAppenderServices extends LogTestBase {

    @Inject
    private BundleContext bundleContext;

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Test
    public void testAppenderService() throws Exception {
        TestAppender ta = new TestAppender();
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        String[] loggers = {
            "foo.bar:DEBUG", "foo.baz:INFO",
        };

        props.put("loggers", loggers);
        ServiceRegistration sr = bundleContext.registerService(Appender.class.getName(), ta, props);

        delay();

        Logger bar = LoggerFactory.getLogger("foo.bar");
        assertTrue(bar.isDebugEnabled());

        Logger baz = LoggerFactory.getLogger("foo.baz");
        assertTrue(baz.isInfoEnabled());

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        // One event should be logged.
        assertEquals(1, ta.events.size());

        // Now unregister the appender and check that log level state is also
        // reverted
        sr.unregister();
        delay();

        assertFalse(bar.isDebugEnabled());
    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }
    }

}
