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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import javax.inject.Inject;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@SuppressWarnings("UnusedDeclaration")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITConfigAdminSupport extends LogTestBase {
    // Constants taken from LogbackManager as they are not publically accessible
    public static final String LOG_LEVEL = "org.apache.sling.commons.log.level";

    public static final String LOG_FILE = "org.apache.sling.commons.log.file";

    public static final String LOGBACK_FILE = "org.apache.sling.commons.log.configurationFile";

    public static final String LOG_FILE_NUMBER = "org.apache.sling.commons.log.file.number";

    public static final String LOG_FILE_SIZE = "org.apache.sling.commons.log.file.size";

    public static final String LOG_PATTERN = "org.apache.sling.commons.log.pattern";

    public static final String LOG_PATTERN_DEFAULT = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %msg%n";

    public static final String LOG_LOGGERS = "org.apache.sling.commons.log.names";

    public static final String LOG_LEVEL_DEFAULT = "INFO";

    public static final int LOG_FILE_NUMBER_DEFAULT = 5;

    public static final String LOG_FILE_SIZE_DEFAULT = "'.'yyyy-MM-dd";

    public static final String PID = "org.apache.sling.commons.log.LogManager";

    public static final String FACTORY_PID_WRITERS = PID + ".factory.writer";

    public static final String FACTORY_PID_CONFIGS = PID + ".factory.config";

    public static final String LOG_PACKAGING_DATA = "org.apache.sling.commons.log.packagingDataEnabled";

    @Inject
    private ConfigurationAdmin ca;

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject());
    }

    @Test
    public void testChangeLogLevelWithConfig() throws Exception {
        // Set log level to debug for foo1.bar
        Configuration config = ca.createFactoryConfiguration(FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_LOGGERS, new String[] {
            "foo1.bar"
        });
        p.put(LOG_LEVEL, "DEBUG");
        config.update(p);

        delay();

        Logger slf4jLogger = LoggerFactory.getLogger("foo1.bar");
        assertTrue(slf4jLogger.isDebugEnabled());
        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isInfoEnabled());
        assertFalse(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());

        // foo1.bar should not have explicit appender attached with it
        Iterator<Appender<ILoggingEvent>> itr = ((ch.qos.logback.classic.Logger) slf4jLogger).iteratorForAppenders();
        assertFalse(itr.hasNext());
    }

    @Test
    public void testChangeGlobalConfig() throws Exception {
        // Set log level to debug for Root logger
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_LEVEL, "DEBUG");
        config.update(p);

        delay();

        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());

        // Reset back to Info
        config = ca.getConfiguration(PID, null);
        p = new Hashtable<String, Object>();
        p.put(LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isInfoEnabled());
        assertFalse(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());
    }

    @Test
    public void testPackagingDataConfig() throws Exception {
        // Set log level to debug for Root logger
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_PACKAGING_DATA, Boolean.FALSE);
        p.put(LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        assertFalse(((LoggerContext)LoggerFactory.getILoggerFactory()).isPackagingDataEnabled());
    }

    @Test
    public void testExternalConfig() throws Exception {
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_LEVEL, "DEBUG");
        p.put(LOGBACK_FILE,absolutePath("test1-external-config.xml"));
        config.update(p);

        delay();

        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull(rootLogger.getAppender("FILE"));
    }
}
