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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * The <code>RequestLogger</code> just registers {@link RequestLoggerService}
 * instance on behalf of the provided configuration.
 */
@Component(property = {
        "service.description=Request Logger",
        "service.vendor=The Apache Software Foundation"
})
@Designate(ocd = RequestLogger.Config.class)
public class RequestLogger {

    @ObjectClassDefinition(name = "Apache Sling Request Logger",
            description="Configures the main loggers of the request logger, " +
                     "namely the request log and the access log. Further loggers may be configured " +
                     "by creating configurations for the Request Logger Service.")
    public @interface Config {

        @AttributeDefinition(name = "Request Log Name",
                description = "Name of the destination for the request log. "+
                     "The request log logs the entry and exit of each request into and "+
                     "out of the system together with the entry time, exit time, time to process "+
                     "the request, a request counter as well as the final status code and response "+
                     "content type. In terms of Request Logger Service formats, request entry is "+
                     "logged with the format \"%t [%R] -> %m %U%q %H\" and request exit is logged "+
                     "with the format \"%{end}t [%R] <- %s %{Content-Type}o %Dms\".")
        String request_log_output() default "logs/request.log";

        @AttributeDefinition(name = "Request Log Type",
                description = "Type of request log destination. Select "+
                     "\"Logger Name\" to write the access log to an SLF4J logger, \"File Name\" to "+
                     "write the access log to a file (relative paths resolved against sling.home) "+
                     "or \"RequestLog Service\" to use a named OSGi service registered with the "+
                     "service interface \"org.apache.sling.engine.RequestLog\" and a service property "+
                     "\"requestlog.name\" equal to the Logger Name setting.",
                options = {
                    @Option(label = "Logger Name", value = "0"),
                    @Option(label = "File Name", value = "1"),
                    @Option(label = "RequestLog Service", value = "2")
        })
        int request_log_outputtype() default 0;

        @AttributeDefinition(name = "Enable Request Log",
                description = "Whether to enable Request logging or not.")
        boolean request_log_enabled() default true;

        @AttributeDefinition(name = "Access Log Name",
                description = "Name of the destination for the request log. "+
                     "The access log writes an entry for each request as the request terminates "+
                     "using the NCSA extended/combined log format. In terms of Request Logger "+
                     "Service formats the access log is written with the format "+
                     "\"%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"\".")
        String access_log_output() default "logs/access.log";

        @AttributeDefinition(name = "Access Log Type",
                description = "Type of access log destination. Select "+
                     "\"Logger Name\" to write the access log to an SLF4J logger, \"File Name\" to "+
                     "write the access log to a file (relative paths resolved against sling.home) "+
                     "or \"RequestLog Service\" to use a named OSGi service registered with the "+
                     "service interface \"org.apache.sling.engine.RequestLog\" and a service property "+
                     "\"requestlog.name\" equal to the Logger Name setting.",
                options = {
                    @Option(label = "Logger Name", value = "0"),
                    @Option(label = "File Name", value = "1"),
                    @Option(label = "RequestLog Service", value = "2")
        })
        int access_log_outputtype() default 0;

        @AttributeDefinition(name = "Enable Access Log",
                description = "Whether to enable Access logging or not.")
        boolean access_log_enabled() default true;
    }

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
    private Map<ServiceRegistration, RequestLoggerService> services = new HashMap<>();

    // ---------- SCR Integration ----------------------------------------------

    @Activate
    protected void activate(BundleContext bundleContext, Config config) {

        // prepare the request loggers if a name is configured and the
        // request loggers are enabled
        if (config.request_log_output() != null && config.request_log_enabled()) {
            createRequestLoggerService(services, bundleContext, true, REQUEST_LOG_ENTRY_FORMAT, config.request_log_output(), config.request_log_outputtype());
            createRequestLoggerService(services, bundleContext, false, REQUEST_LOG_EXIT_FORMAT, config.request_log_output(), config.request_log_outputtype());
        }

        // prepare the access logger if a name is configured and the
        // access logger is enabled
        if (config.access_log_output() != null && config.access_log_enabled()) {
            createRequestLoggerService(services, bundleContext, false, ACCESS_LOG_FORMAT, config.access_log_output(), config.access_log_outputtype());
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

    private static void createRequestLoggerService(Map<ServiceRegistration, RequestLoggerService> services,
            final BundleContext bundleContext,
            final boolean onEntry,
            final String format,
            final String output,
            final int outputType) {
        final RequestLoggerService service = new RequestLoggerService(bundleContext, new RequestLoggerService.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return  RequestLoggerService.Config.class;
            }

            @Override
            public int request_log_service_outputtype() {
                return outputType;
            }

            @Override
            public String request_log_service_output() {
                return output;
            }

            @Override
            public boolean request_log_service_onentry() {
                return onEntry;
            }

            @Override
            public String request_log_service_format() {
                return format;
            }
        });
        final ServiceRegistration reg = bundleContext.registerService(service.getClass().getName(), service, null);
        services.put(reg, service);
    }
}
