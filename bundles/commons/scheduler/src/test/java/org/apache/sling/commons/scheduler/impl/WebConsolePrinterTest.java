package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.commons.threads.impl.DefaultThreadPoolManager;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.quartz.SchedulerException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebConsolePrinterTest {
    private Map<String, Object> scheduleActivationProps;
    private WebConsolePrinter consolePrinter;
    private QuartzScheduler quartzScheduler;
    private BundleContext context;

    @Before
    public void setUp() throws Exception {
        consolePrinter = new WebConsolePrinter();
        context = MockOsgi.newBundleContext();
        quartzScheduler = createScheduler();

        scheduleActivationProps = new HashMap<String, Object>();
        scheduleActivationProps.put("poolName", "testName");
        quartzScheduler.activate(context, scheduleActivationProps);

        Field privateQuartzScheduler = WebConsolePrinter.class.getDeclaredField("scheduler");
        privateQuartzScheduler.setAccessible(true);
        privateQuartzScheduler.set(consolePrinter, quartzScheduler);
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

    private QuartzScheduler createScheduler() throws NoSuchFieldException, IllegalAccessException {
        QuartzScheduler quartzScheduler = new QuartzScheduler();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");
        props.put(Constants.SERVICE_PID, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");


        if (context == null) {
            context = MockOsgi.newBundleContext();
        }
        Field f = QuartzScheduler.class.getDeclaredField("threadPoolManager");
        f.setAccessible(true);
        f.set(quartzScheduler, new DefaultThreadPoolManager(context, props));
        return quartzScheduler;
    }
}
