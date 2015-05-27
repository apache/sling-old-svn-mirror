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

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.quartz.SchedulerException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        File f = new File("target/test.txt");
        f.createNewFile();
        PrintWriter w = new PrintWriter(f);
        consolePrinter.printConfiguration(w);
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(f));

        assertEquals("Apache Sling Scheduler", reader.readLine());
        reader.readLine();
        assertEquals("Status : active", reader.readLine());
        assertEquals("Name   : ApacheSling", reader.readLine());
        assertTrue(reader.readLine().startsWith("Id     : "));
        reader.readLine();
        assertTrue(reader.readLine().startsWith("Job : testName3"));
        assertTrue(reader.readLine().startsWith("Trigger : Trigger 'DEFAULT.testName3'"));
        reader.readLine();
        assertTrue(reader.readLine().startsWith("Job : testName2"));
        assertTrue(reader.readLine().startsWith("Trigger : Trigger 'DEFAULT.testName2'"));
        reader.readLine();
        assertTrue(reader.readLine().startsWith("Job : testName1"));
        assertTrue(reader.readLine().startsWith("Trigger : Trigger 'DEFAULT.testName1'"));

        f.delete();
    }

    @After
    public void deactivateScheduler() {
        quartzScheduler.deactivate(context);
    }
}
