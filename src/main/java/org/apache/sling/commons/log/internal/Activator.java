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
package org.apache.sling.commons.log.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * The <code>Activator</code> class is the <code>BundleActivator</code> for
 * the log service bundle. This activator sets up logging in NLog4J and
 * registers the <code>LogService</code> and <code>LogReaderService</code>.
 * When the bundle is stopped, the NLog4J subsystem is simply shutdown.
 */
public class Activator implements BundleActivator {

    private LogManager logManager;

    private LogSupport logSupport;

    public void start(BundleContext context) throws Exception {
        logManager = new LogManager(context);

        logSupport = new LogSupport();
        context.addBundleListener(logSupport);
        context.addFrameworkListener(logSupport);
        context.addServiceListener(logSupport);

        LogServiceFactory lsf = new LogServiceFactory(logSupport);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, lsf.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling LogService implementation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        context.registerService(LogService.class.getName(), lsf, props);

        LogReaderServiceFactory lrsf = new LogReaderServiceFactory(logSupport);
        props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, lrsf.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling LogReaderService implementation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        context.registerService(LogReaderService.class.getName(), lrsf, props);
    }

    public void stop(BundleContext context) throws Exception {
        if (logSupport != null) {
            logSupport.shutdown();
            logSupport = null;
        }

        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
    }
}
