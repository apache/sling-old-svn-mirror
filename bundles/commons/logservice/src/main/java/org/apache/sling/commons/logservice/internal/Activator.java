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
package org.apache.sling.commons.logservice.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>Activator</code> class is the <code>BundleActivator</code> for the
 * log service bundle. This activator registers the <code>LogService</code> and
 * <code>LogReaderService</code>.
 */
public class Activator implements BundleActivator {

    private static final String VENDOR = "The Apache Software Foundation";

    private LogSupport logSupport;

    /** Reference to the start level service. */
    private ServiceReference startLevelRef;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // get start level service, it's always there (required by the spec)
        startLevelRef = context.getServiceReference(StartLevel.class.getName());

        logSupport = new LogSupport((StartLevel)context.getService(startLevelRef));
        context.addBundleListener(logSupport);
        context.addFrameworkListener(logSupport);
        context.addServiceListener(logSupport);

        LogServiceFactory lsf = new LogServiceFactory(logSupport);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, lsf.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling LogService implementation");
        props.put(Constants.SERVICE_VENDOR, VENDOR);
        context.registerService(LogService.class.getName(), lsf, props);

        LogReaderServiceFactory lrsf = new LogReaderServiceFactory(logSupport);
        props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, lrsf.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling LogReaderService implementation");
        props.put(Constants.SERVICE_VENDOR, VENDOR);
        context.registerService(LogReaderService.class.getName(), lrsf, props);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        if ( startLevelRef != null ) {
            context.ungetService(startLevelRef);
        }
        if (logSupport != null) {
            context.removeBundleListener(logSupport);
            context.removeFrameworkListener(logSupport);
            context.removeServiceListener(logSupport);
            logSupport.shutdown();
            logSupport = null;
        }
    }
}
