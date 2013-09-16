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
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITJULIntegration extends LogTestBase {

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions() {
        return composite(
            systemProperty("org.apache.sling.commons.log.julenabled").value("true"),
            frameworkProperty("org.apache.sling.commons.log.configurationFile").value(
                FilenameUtils.concat(new File(".").getAbsolutePath(), "src/test/resources/test-jul-config.xml")));
    }

    /**
     * Checks the default settings. It runs the bundle with minimum dependencies
     */
    @Test
    public void testJULLogging() throws Exception {
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("foo.jul.1");
        org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger("foo.jul.1");

        assertEquals(java.util.logging.Level.FINEST, julLogger.getLevel());
        assertTrue(slf4jLogger.isTraceEnabled());

        // Now add an appender and see if JUL logs are handled
        TestAppender ta = new TestAppender();
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        String[] loggers = {
            "foo.jul.1",
        };
        ch.qos.logback.classic.Logger bar = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(loggers[0]);
        bar.setLevel(Level.INFO);

        props.put("loggers", loggers);
        ServiceRegistration sr = bundleContext.registerService(Appender.class.getName(), ta, props);

        delay();

        // Level should be INFO now
        assertEquals(java.util.logging.Level.INFO, julLogger.getLevel());

        julLogger.info("Info message");
        julLogger.fine("Fine message");

        assertEquals(1, ta.events.size());

    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }
    }

}
