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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITAppenderServices extends LogTestBase {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private ConfigurationAdmin ca;

    private ServiceRegistration sr;

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject());
    }

    @Test
    public void testAppenderService() throws Exception {
        TestAppender ta = registerAppender("foo.bar", "foo.baz");
        delay();

        Logger bar = (Logger)LoggerFactory.getLogger("foo.bar");
        bar.setLevel(Level.DEBUG);
        Logger baz = (Logger)LoggerFactory.getLogger("foo.baz");
        baz.setLevel(Level.INFO);

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        // One event should be logged.
        assertEquals(1, ta.events.size());
    }

    @Test
    public void testOsgiAppenderRef() throws Exception {
        Configuration config = ca.getConfiguration(ITConfigAdminSupport.PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(ITConfigAdminSupport.LOG_LEVEL, "INFO");
        p.put(ITConfigAdminSupport.LOGBACK_FILE,absolutePath("test-osg-appender-ref-config.xml"));
        config.update(p);

        delay();

        Logger ref = (Logger)LoggerFactory.getLogger("foo.ref.osgi");
        assertTrue(ref.isDebugEnabled());

        TestAppender ta = registerAppender("foo.bar", "foo.baz");
        delay();

        Logger bar = (Logger)LoggerFactory.getLogger("foo.bar");
        bar.setLevel(Level.DEBUG);
        Logger baz = (Logger)LoggerFactory.getLogger("foo.baz");
        baz.setLevel(Level.INFO);

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        ref.debug("Test message ref");

        // One event should be logged.
        assertEquals(2, ta.events.size());
    }

    @After
    public void unregisterAppender(){
        sr.unregister();
    }


    private TestAppender registerAppender(String... loggers){
        TestAppender ta = new TestAppender();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("loggers", loggers);
        sr = bundleContext.registerService(Appender.class.getName(), ta, props);
        delay();
        return ta;
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
