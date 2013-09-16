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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.MatchingFilter;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITFilterSupport extends LogTestBase{

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Test
    public void testTurboFilter() throws Exception {
        TestAppender ta = registerAppender("turbofilter");

        org.slf4j.Logger bar = LoggerFactory.getLogger("turbofilter.foo.bar");
        assertTrue(bar.isDebugEnabled());

        bar.debug("Test");
        assertEquals(1, ta.events.size());

        SimpleTurboFilter stf = new SimpleTurboFilter();
        ServiceRegistration sr  = bundleContext.registerService(TurboFilter.class.getName(), stf, null);

        delay();

        assertNotNull("Filter should have context set",stf.getContext());
        assertTrue("Filter should be started", stf.isStarted());

        ta.events.clear();

        //Filter would reject calls for this logger hence it should not be false
        assertFalse(bar.isDebugEnabled());

        //No events should be logged as filter would have rejected that
        bar.debug("Test");
        assertTrue(ta.events.isEmpty());

        //Now unregister and earlier asserts should work
        sr.unregister();

        delay();
        ta.events.clear();

        assertTrue(bar.isDebugEnabled());

        bar.debug("Test");
        assertEquals(1, ta.events.size());
    }

    @Test
    public void testNormalFilter() {
        TestAppender ta = registerAppender("filter");

        org.slf4j.Logger bar = LoggerFactory.getLogger("filter.foo.bar");
        assertTrue(bar.isDebugEnabled());

        bar.debug("Test");
        assertEquals(1, ta.events.size());

        SimpleFilter stf = new SimpleFilter();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("appenders", "TestAppender");
        ServiceRegistration sr  = bundleContext.registerService(Filter.class.getName(), stf, props);

        delay();

        assertNotNull("Filter should have context set",stf.getContext());
        assertTrue("Filter should be started", stf.isStarted());

        ta.events.clear();

        //A filter attached to an appender cannot influence isXXXEnabled call
        assertTrue(bar.isDebugEnabled());

        //No events should be logged as filter would have rejected that
        bar.debug("Test");
        assertTrue(ta.events.isEmpty());

        //Now unregister and earlier asserts should work
        sr.unregister();

        delay();
        ta.events.clear();

        assertTrue(bar.isDebugEnabled());

        bar.debug("Test");
        assertEquals(1, ta.events.size());

    }

    private TestAppender registerAppender(String prefix) {
        TestAppender ta = new TestAppender();
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        String[] loggers = {
                prefix + ".foo.bar",
                prefix + ".foo.baz",
        };
        ch.qos.logback.classic.Logger bar = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(loggers[0]);
        bar.setLevel(Level.DEBUG);
        ch.qos.logback.classic.Logger baz = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(loggers[1]);
        baz.setLevel(Level.INFO);

        props.put("loggers", loggers);
        ServiceRegistration sr = bundleContext.registerService(Appender.class.getName(), ta, props);

        delay();
        return ta;
    }

    private static class SimpleTurboFilter extends MatchingFilter {
        @Override
        public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
            if(logger.getName().equals("turbofilter.foo.bar")){
                    return FilterReply.DENY;
            }
            return FilterReply.NEUTRAL;
        }
    }

    private static class SimpleFilter extends Filter<ILoggingEvent> {

        @Override
        public FilterReply decide(ILoggingEvent event) {
            if(event.getLoggerName().equals("filter.foo.bar")){
                return FilterReply.DENY;
            }
            return FilterReply.NEUTRAL;
        }
    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        @Override
        public String getName() {
            return "TestAppender";
        }
    }
}
