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

import java.util.Iterator;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITDefaultConfig extends LogTestBase {

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addDefaultOptions() {
        return null; // Disable adding of default option
    }

    /**
     * Checks the default settings. It runs the bundle with minimum dependencies
     */
    @Test
    public void testDefaultSettings() throws Exception {
        Logger slf4jLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertTrue("Default Log level should be INFO", slf4jLogger.isInfoEnabled());

        // Check for Logback being used
        assertTrue(LoggerFactory.getILoggerFactory() instanceof LoggerContext);
        assertTrue(slf4jLogger instanceof ch.qos.logback.classic.Logger);

        // Test that root logger has one FileAppender attached
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) slf4jLogger;
        Iterator<Appender<ILoggingEvent>> itr = logger.iteratorForAppenders();
        assertTrue("One appender should be attached with root logger", itr.hasNext());
    }

    @Test
    public void supportNestedClassesWithNestedDot() throws Exception {
        //SLING-3037 - No illegalArgumentException thrown
        LoggerFactory.getLogger("com.foo.Bar$Nested.dot");
    }

}
