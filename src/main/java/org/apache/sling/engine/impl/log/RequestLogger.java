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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>RequestLogger</code> is a request level filter, which
 * provides customizable logging or requests handled by Sling. This filter is
 * inserted as the first filter in the request level filter chain and therefore
 * is the first filter called when processing a request and the last filter
 * acting just before the request handling terminates.
 *
 */
@Component(metatype=true,label="%request.log.name",description="%request.log.description")
@Properties({
    @Property(name="service.description",value="Request Logger"),
    @Property(name="service.vendor",value="The Apache Software Foundation")
})
public class RequestLogger {

    @Property(value="logs/request.log")
    public static final String PROP_REQUEST_LOG_OUTPUT = "request.log.output";

    @Property(intValue=0,options={
            @PropertyOption(name = "0", value = "Logger Name"),
            @PropertyOption(name = "1", value = "File Name"),
            @PropertyOption(name = "2", value = "RequestLog Service")
    })
    public static final String PROP_REQUEST_LOG_OUTPUT_TYPE = "request.log.outputtype";

    @Property(boolValue=true)
    public static final String PROP_REQUEST_LOG_ENABLED = "request.log.enabled";

    @Property(value="logs/access.log")
    public static final String PROP_ACCESS_LOG_OUTPUT = "access.log.output";

    @Property(intValue=0,options={
            @PropertyOption(name = "0", value = "Logger Name"),
            @PropertyOption(name = "1", value = "File Name"),
            @PropertyOption(name = "2", value = "RequestLog Service")
    })
    public static final String PROP_ACCESS_LOG_OUTPUT_TYPE = "access.log.outputtype";

    @Property(boolValue=true)
    public static final String PROP_ACCESS_LOG_ENABLED = "access.log.enabled";

    /**
     * The log format string for the request log entry message (value is "%t
     * [%R] -> %m %U%q %H").
     */
    private static final String REQUEST_LOG_ENTRY_FORMAT = "%t [%R] -> %m %U%q %H";

    /**
     * The log format string for the request log exit message (value is "%{end}t
     * [%R] <- %s %{Content-Type}o %Dms").
     */
    private static final String REQUEST_LOG_EXIT_FORMAT = "%{end}t [%R] <- %s %{Content-Type}o %Dms";

    /**
     * The log format for the access log which is exactly the NCSA
     * extended/combined log format (value is "%a %l %u %t \"%r\" %>s %b
     * \"%{Referer}i\" \"%{User-Agent}i\"").
     */
    private static final String ACCESS_LOG_FORMAT = "%a %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";

    /**
     * A special request logger service, which writes the request log message at
     * request start time. This logger logs a message with the format
     * {@link #REQUEST_LOG_ENTRY_FORMAT}.
     */
    private RequestLoggerService requestLogEntry;

    /**
     * A special request logger service, which writes the request log message at
     * request termination time. This logger logs a message with the format
     * {@link #REQUEST_LOG_EXIT_FORMAT}.
     */
    private RequestLoggerService requestLogExit;

    /**
     * A special request logger service, which writes the access log message at
     * request termination time. This logger logs a message with the format
     * {@link #ACCESS_LOG_FORMAT NCSA extended/combined log format}.
     */
    private RequestLoggerService accessLog;

    /**
     * RequestLoggerService instances created on behalf of the static
     * configuration.
     */
    private Map<ServiceRegistration, RequestLoggerService> services = new HashMap<ServiceRegistration, RequestLoggerService>();

    // ---------- SCR Integration ----------------------------------------------

    /**
     * Activates this component by setting up the special request entry and exit
     * request loggers and the access logger as configured in the context
     * properties. In addition the <code>FileRequestLog</code> class is
     * initialized with the value of the <code>sling.home</code> context
     * property to resolve relative log file names.
     *
     * @param osgiContext The OSGi Component Context providing the configuration
     *            data and access into the system.
     */
    protected void activate(BundleContext bundleContext, Map<String, Object> props) {

        // prepare the request loggers if a name is configured and the
        // request loggers are enabled
        Object requestLogName = props.get(PROP_REQUEST_LOG_OUTPUT);
        Object requestLogEnabled = props.get(PROP_REQUEST_LOG_ENABLED);
        if (requestLogName != null && requestLogEnabled instanceof Boolean
            && ((Boolean) requestLogEnabled).booleanValue()) {
            Object requestLogType = props.get(PROP_REQUEST_LOG_OUTPUT_TYPE);
            createRequestLoggerService(services, bundleContext, true, REQUEST_LOG_ENTRY_FORMAT, requestLogName,
                requestLogType);
            createRequestLoggerService(services, bundleContext, false, REQUEST_LOG_EXIT_FORMAT, requestLogName,
                requestLogType);
        }

        // prepare the access logger if a name is configured and the
        // access logger is enabled
        Object accessLogName = props.get(PROP_ACCESS_LOG_OUTPUT);
        Object accessLogEnabled = props.get(PROP_ACCESS_LOG_ENABLED);
        if (accessLogName != null && accessLogEnabled instanceof Boolean && ((Boolean) accessLogEnabled).booleanValue()) {
            Object accessLogType = props.get(PROP_ACCESS_LOG_OUTPUT_TYPE);
            createRequestLoggerService(services, bundleContext, false, ACCESS_LOG_FORMAT, accessLogName, accessLogType);
        }
    }

    /**
     * Deactivates this component by unbinding and shutting down all loggers
     * setup during activation and finally dispose off the
     * <code>FileRequestLog</code> class to make sure all shared writers are
     * closed.
     *
     * @param osgiContext The OSGi Component Context providing the configuration
     *            data and access into the system.
     */
    protected void deactivate() {
        for (Entry<ServiceRegistration, RequestLoggerService> entry : services.entrySet()) {
            entry.getKey().unregister();
            entry.getValue().shutdown();
        }
        services.clear();
    }

    /**
     * Create a {@link RequestLoggerService} instance from the given
     * configuration data. This method creates a <code>Properties</code>
     * object from the data which may be handled by the
     * {@link RequestLoggerService#RequestLoggerService(BundleContext, Dictionary)}
     * constructor to set itself up.
     *
     * @param bundleContext The <code>BundleContext</code> used to setup a
     *            <code>ServiceTracker</code> should the output be a
     *            <code>RequestLog</code> service.
     * @param onEntry Whether the logger is to be called on request entry (true)
     *            or not (false).
     * @param format The log format string. This is expected to be a String.
     * @param output The name of the output, which may be an SLF4J logger, a
     *            relative or absolute file name or the name of a
     *            <code>RequestLog</code> service. This is expected to be a
     *            String.
     * @param outputType The type of output, 0 for SLF4J logger, 1 for file name
     *            and 2 for a service name. This is expected to be an Integer.
     * @return The functional and prepared <code>RequestLoggerService</code>
     *         instance.
     */
    private static void createRequestLoggerService(Map<ServiceRegistration, RequestLoggerService> services,
            BundleContext bundleContext, boolean onEntry, Object format, Object output, Object outputType) {
        final Hashtable<String, Object> config = new Hashtable<String, Object>();
        config.put(RequestLoggerService.PARAM_ON_ENTRY, onEntry ? Boolean.TRUE : Boolean.FALSE);
        config.put(RequestLoggerService.PARAM_FORMAT, format);
        config.put(RequestLoggerService.PARAM_OUTPUT, output);
        config.put(RequestLoggerService.PARAM_OUTPUT_TYPE, outputType);

        final RequestLoggerService service = new RequestLoggerService(bundleContext, config);
        final ServiceRegistration reg = bundleContext.registerService(service.getClass().getName(), service, config);
        services.put(reg, service);
    }
}
