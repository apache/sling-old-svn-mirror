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

package org.apache.sling.commons.log.logback.internal;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.StatusPrinter;

public class Activator implements BundleActivator {
    private static final String JUL_SUPPORT = "org.apache.sling.commons.log.julenabled";

    private LogbackManager logManager;

    private BundleContext context;

    private Timer timer;
    private boolean bridgeHandlerInstalled;
    private long startTime;
    private static final AtomicInteger counter = new AtomicInteger();
    public static final long INIT_TASK_PERIOD_MSEC = 1;

    public void start(BundleContext context) throws Exception {
        this.context = context;
        this.startTime = System.currentTimeMillis();
        this.timer = new Timer(getClass().getSimpleName() + "#" + counter.incrementAndGet());

        // SLING-3189 - Check if SLF4J is currently initialized then start
        // LogbackManager straightaway otherwise initialize
        // it in a separate thread
        if(isSlf4jInitialized()){
            initializeLogbackManager(true);
        } else {
            System.out.println("Slf4j is not initialized yet. Delaying Logback support initialization");
            timer.schedule(new LogbackInitializerTask(),0,INIT_TASK_PERIOD_MSEC);
        }
    }

    public void stop(BundleContext context) throws Exception {
        if(bridgeHandlerInstalled){
            SLF4JBridgeHandler.uninstall();
        }

        if(timer != null){
            timer.cancel();
            timer = null;
        }

        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
    }

    private void initializeLogbackManager(boolean immediateInit) throws InvalidSyntaxException {
        // SLING-2373
        if (Boolean.parseBoolean(context.getProperty(JUL_SUPPORT))) {
            // In config one must enable the LevelChangePropagator
            // http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
            // make sure configuration is empty unless explicitly set
            if (System.getProperty("java.util.logging.config.file") == null
                    && System.getProperty("java.util.logging.config.class") == null) {
                final Thread ct = Thread.currentThread();
                final ClassLoader old = ct.getContextClassLoader();
                try {
                    ct.setContextClassLoader(getClass().getClassLoader());
                    System.setProperty("java.util.logging.config.class",
                            "org.apache.sling.commons.log.internal.Activator.DummyLogManagerConfiguration");
                    java.util.logging.LogManager.getLogManager().reset();
                } finally {
                    ct.setContextClassLoader(old);
                    System.clearProperty("java.util.logging.config.class");
                }
            }

            SLF4JBridgeHandler.install();
            bridgeHandlerInstalled = true;
        }

        logManager = new LogbackManager(context);
        
        final Logger log = LoggerFactory.getLogger(getClass());
        if(immediateInit) {
            log.info("LogbackManager initialized at bundle startup");
        } else {
            log.info("LogbackManager initialized after waiting for Slf4j, {} msec after startup", System.currentTimeMillis() - startTime);
        }
    }

    private class LogbackInitializerTask extends TimerTask{
        public LogbackInitializerTask() {
        }

        @Override
        public void run() {
            if(!isSlf4jInitialized()){
                return;
            }
            try {
                initializeLogbackManager(false);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                StatusPrinter.buildStr(sb, "", new ErrorStatus("Error occurred " +
                        "while starting Logback integration",this,e));
                System.err.print(sb.toString());
            }
            cancel();
            timer.cancel();
        }
    }

    private static boolean isSlf4jInitialized(){
        return LoggerFactory.getILoggerFactory() instanceof LoggerContext;
    }


    /**
     * The <code>DummyLogManagerConfiguration</code> class is used as JUL
     * LogginManager configurator to preven reading platform default
     * configuration which just duplicate log output to be redirected to SLF4J.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class DummyLogManagerConfiguration {
    }
}
