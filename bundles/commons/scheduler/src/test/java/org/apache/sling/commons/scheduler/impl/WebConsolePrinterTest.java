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
package org.apache.sling.commons.scheduler.impl;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.quartz.SchedulerException;

public class WebConsolePrinterTest {
    private WebConsolePrinter consolePrinter;
    private QuartzScheduler quartzScheduler;
    private BundleContext context;

    @Before
    public void setUp() throws Exception {
        consolePrinter = new WebConsolePrinter();
        context = MockOsgi.newBundleContext();
        quartzScheduler = ActivatedQuartzSchedulerFactory.create(context, "testName");

        Field quartzSchedulerField = WebConsolePrinter.class.getDeclaredField("scheduler");
        quartzSchedulerField.setAccessible(true);
        quartzSchedulerField.set(consolePrinter, quartzScheduler);
    }

    @Test
    public void testConsolePrinter() throws IOException, SchedulerException {
        quartzScheduler.addJob(1L, 1L, "testName1", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        quartzScheduler.addJob(2L, 2L, "testName2", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        quartzScheduler.addJob(3L, 3L, "testName3", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        StringWriter stringWriter = new StringWriter();
        PrintWriter w = new PrintWriter(stringWriter);
        consolePrinter.printConfiguration(w);
        w.close();

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));

        try {
            assertRegexp(reader.readLine(), ".*Apache Sling Scheduler.*");
            reader.readLine();
            assertRegexp(reader.readLine(), ".*Status.*active.*");
            assertRegexp(reader.readLine(), ".*Name.*ApacheSling.*");
            assertRegexp(reader.readLine(), ".*ThreadPool.*testName.*");
            assertRegexp(reader.readLine(), ".*Id.*");
            reader.readLine();
            assertRegexp(reader.readLine(), "^Job.*testName[123].*");
            assertRegexp(reader.readLine(), "^Trigger.*Trigger.*DEFAULT.testName[123].*");
            reader.readLine();
            assertRegexp(reader.readLine(), "^Job.*testName[123].*");
            assertRegexp(reader.readLine(), "^Trigger.*Trigger.*DEFAULT.testName[123].*");
            reader.readLine();
            assertRegexp(reader.readLine(), "^Job.*testName[123].*");
            assertRegexp(reader.readLine(), "^Trigger.*Trigger.*DEFAULT.testName[123].*");
        } finally {
            reader.close();
        }
    }

    private void assertRegexp(String input, String regexp) {
        assertTrue("Expecting regexp match: '" + input + "' / '" + regexp + "'", Pattern.matches(regexp, input));
    }

    @After
    public void deactivateScheduler() {
        quartzScheduler.deactivate(context);
    }
}
