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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import static org.apache.sling.commons.log.logback.internal.LogConfigManager.FACTORY_PID_CONFIGS;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.PID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.util.PathUtils.getBaseDir;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITConfigPrinter extends LogTestBase {
    private static final String logDir = FilenameUtils.concat(getBaseDir(), "target/ITConfigPrinter");

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions(){
        return composite(
                frameworkProperty("sling.log.root").value(logDir),
                configAdmin(),
                mavenBundle("commons-io", "commons-io").versionAsInProject()
        );
    }

    @Inject
    private ConfigurationAdmin ca;

    @Inject
    private BundleContext bundleContext;

    private ServiceTracker tracker;

    private Object configPrinter;

    @After
    public void closeTracker(){
        if (tracker != null){
            tracker.close();
        }
    }

    @Test
    public void simpleWorking() throws Exception {
        waitForPrinter();
        createLogConfig("simpleWorking.log", "a.b", "a.b.c");
        StringWriter sw = new StringWriter();
        invoke("printConfiguration", new PrintWriter(sw), "txt");
        assertThat(sw.toString(), containsString("simpleWorking.log"));
    }

    @Test
    public void includeOnlyLastNFiles() throws Exception {
        waitForPrinter();
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConfigManager.PRINTER_MAX_INCLUDED_FILES, 3);
        p.put(LogConfigManager.LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        createLogConfig("error.log", "includeOnlyLastNFiles", "includeOnlyLastNFiles.1");

        //txt mode log should at least have mention of all files
        for (int i = 0; i < 10; i++) {
            FileUtils.touch(new File(logDir, "error.log." + i));
        }

        StringWriter sw = new StringWriter();
        invoke("printConfiguration", new PrintWriter(sw), "txt");
        assertThat(sw.toString(), containsString("error.log"));
        for (int i = 0; i < 10; i++) {
            assertThat(sw.toString(), containsString("error.log." + i));
        }

        //Attachment should only be upto 3
        assertTrue(((URL[]) invoke("getAttachments", "zip")).length > 3);
    }

    private void createLogConfig(String fileName, String... logConfigs) throws IOException {
        Configuration config = ca.createFactoryConfiguration(FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConfigManager.LOG_LOGGERS, logConfigs);
        p.put(LogConfigManager.LOG_LEVEL, "DEBUG");
        p.put(LogConfigManager.LOG_FILE, fileName);
        config.update(p);

        delay();
    }


    private Object invoke(String methodName, Object... args) throws
            InvocationTargetException, IllegalAccessException {
        for (Method m : configPrinter.getClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                return m.invoke(configPrinter, args);
            }
        }
        fail("No method with name " + methodName + " found for object of type " + configPrinter.getClass());
        return null;
    }

    private void waitForPrinter() throws InterruptedException {
        if (configPrinter == null){
            tracker = new ServiceTracker(bundleContext,
                    "org.apache.sling.commons.log.logback.internal.SlingConfigurationPrinter", null);
            tracker.open();
            configPrinter = tracker.waitForService(0);
        }
    }
}
