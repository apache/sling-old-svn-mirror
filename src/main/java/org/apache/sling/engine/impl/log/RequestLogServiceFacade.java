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
package org.apache.sling.engine.impl.log;

import org.apache.sling.engine.RequestLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The <code>RequestLogServiceFacade</code> is a <code>RequestLog</code>
 * implementation used to redirect output requests to <code>RequestLog</code>
 * services registered with a specific logger name. Each service thus registered
 * is called with the message by the {@link #write(String)} method.
 * <p>
 * This class is initialized with the service name which is used to find request
 * log services to send log messages to. Such services are selected as follows:
 * <ol>
 * <li>The service must be registered with service interface
 * <code>org.apache.sling.engine.RequestLog</code>.</li>
 * <li>The service must be registered with a service property
 * <code>requestlog.name</code> (defined in the
 * {@link RequestLog#REQUEST_LOG_NAME} constant whose value (or one of its
 * values, if multi-valued) must be the service name.</li>
 * </ol>
 */
class RequestLogServiceFacade implements RequestLog {

    // The service tracker used to access the service(s)
    private final ServiceTracker requestLogTracker;

    // private copy of services currently available. We cache them to gain some
    // milliseconds, as each call to ServiceTracker.getServices() looks the
    // services up in the service registry
    private Object[] loggers;

    // tracking count of the time when we got the service list, if the
    // service tracker changes its count, we have to reaquire the services
    private int trackingCount;

    /**
     * Creates an instance of this facade class calling request log services
     * with the given <code>serviceName</code>.
     * 
     * @param context The <code>BundleContext</code> used to acquire the request
     *            log services.
     * @param serviceName The name of the services used for logging. This value
     *            is used to check the {@link RequestLog#REQUEST_LOG_NAME}
     *            service property for service selection.
     */
    public RequestLogServiceFacade(BundleContext context, String serviceName) {
        String filter = "(&(" + Constants.OBJECTCLASS + "=" + RequestLog.class.getName() + ")("
            + RequestLog.REQUEST_LOG_NAME + "=" + serviceName + "))";
        this.requestLogTracker = new ServiceTracker(context, filter, null);
        this.requestLogTracker.open();

        // use negative initial tracking count to force acquiry of services
        this.trackingCount = -1;
    }

    /**
     * @see org.apache.sling.engine.RequestLog#write(java.lang.String)
     */
    public void write(String message) {

        // acquire the current logger list
        Object[] tmpLoggers = this.loggers;

        // if services have been added/removed reacquire from the tracker
        if (this.trackingCount != this.requestLogTracker.getTrackingCount()) {
            tmpLoggers = this.requestLogTracker.getServices();
            this.loggers = tmpLoggers;
        }

        // finally call the loggers with the message
        if (tmpLoggers != null) {
            for (int i = 0; i < tmpLoggers.length; i++) {
                ((RequestLog) tmpLoggers[i]).write(message);
            }
        }
    }

    public void close() {
        // drop the service references and reinitialize tracking counter
        this.loggers = null;
        this.trackingCount = -1;

        // terminate using the RequestLog service(s)
        this.requestLogTracker.close();
    }
}
