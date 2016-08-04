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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>RequestLogger</code> just registers {@link RequestLoggerService}
 * instance on behalf of the provided configuration.
 */
@Component(metatype = true, label = "%request.log.name", description = "%request.log.description")
@Properties({
    @Property(name = "service.description", value = "Request Logger"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation")
})
public class RequestLogger {

    @Property(value = "logs/request.log")
    public static final String PROP_REQUEST_LOG_OUTPUT = "request.log.output";

    @Property(intValue = 0, options = {
        @PropertyOption(name = "0", value = "Logger Name"), @PropertyOption(name = "1", value = "File Name"),
        @PropertyOption(name = "2", value = "RequestLog Service")
    })
    public static final String PROP_REQUEST_LOG_OUTPUT_TYPE = "request.log.outputtype";

    @Property(boolValue = true)
    public static final String PROP_REQUEST_LOG_ENABLED = "request.log.enabled";

    @Property(value = "logs/access.log")
    public static final String PROP_ACCESS_LOG_OUTPUT = "access.log.output";

    @Property(intValue = 0, options = {
        @PropertyOption(name = "0", value = "Logger Name"), @PropertyOption(name = "1", value = "File Name"),
        @PropertyOption(name = "2", value = "RequestLog Service")
    })
    public static final String PROP_ACCESS_LOG_OUTPUT_TYPE = "access.log.outputtype";

    @Property(boolValue = true)
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
     * RequestLoggerService instances created on behalf of the static
     * configuration.
     */
    private Map<ServiceRegistration, RequestLoggerService> services = new HashMap<ServiceRegistration, RequestLoggerService>();

    // ---------- SCR Integration ----------------------------------------------

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> props) {

        // prepare the request loggers if a name is configured and the
        // request loggers are enabled
        final String requestLogName = PropertiesUtil.toString(props.get(PROP_REQUEST_LOG_OUTPUT), null);
        final boolean requestLogEnabled = PropertiesUtil.toBoolean(props.get(PROP_REQUEST_LOG_ENABLED), false);
        if (requestLogName != null && requestLogEnabled) {
            final int requestLogType = PropertiesUtil.toInteger(props.get(PROP_REQUEST_LOG_OUTPUT_TYPE), 0);
            createRequestLoggerService(services, bundleContext, true, REQUEST_LOG_ENTRY_FORMAT, requestLogName, requestLogType);
            createRequestLoggerService(services, bundleContext, false, REQUEST_LOG_EXIT_FORMAT, requestLogName, requestLogType);
        }

        // prepare the access logger if a name is configured and the
        // access logger is enabled
        final String accessLogName = PropertiesUtil.toString(props.get(PROP_ACCESS_LOG_OUTPUT), null);
        final boolean accessLogEnabled = PropertiesUtil.toBoolean(props.get(PROP_ACCESS_LOG_ENABLED), false);
        if (accessLogName != null && accessLogEnabled) {
            final int accessLogType = PropertiesUtil.toInteger(props.get(PROP_ACCESS_LOG_OUTPUT_TYPE), 0);
            createRequestLoggerService(services, bundleContext, false, ACCESS_LOG_FORMAT, accessLogName, accessLogType);
        }
    }

    @Deactivate
    protected void deactivate() {
        for (Entry<ServiceRegistration, RequestLoggerService> entry : services.entrySet()) {
            entry.getKey().unregister();
            entry.getValue().shutdown();
        }
        services.clear();
    }

    private static void createRequestLoggerService(Map<ServiceRegistration, RequestLoggerService> services, BundleContext bundleContext, boolean onEntry, Object format, String output, Object outputType) {
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
