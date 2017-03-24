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

import java.io.File;
import java.io.IOException;

import org.apache.sling.engine.RequestLog;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * The <code>RequestLoggerService</code> is a factory component which gets
 * configuration to register loggers for the {@link RequestLogger}.
 */
@Component(service = RequestLoggerService.class, configurationPolicy = ConfigurationPolicy.REQUIRE,
     property = {
             "service.description=Factory for configuration based request/access loggers",
             "service.vendor=The Apache Software Foundation"
     })
@Designate(ocd = RequestLoggerService.Config.class, factory = true)
public class RequestLoggerService {

    @ObjectClassDefinition(name = "Apache Sling Customizable Request Data Logger",
            description="This configuration creates customizable "+
                 "loggers for request content. Each configuration results in a logger writing "+
                 "the requested data. Deleting an existing configuration removes the respective "+
                 "logger.")
    public @interface Config {

        @AttributeDefinition(name = "Log Format",
                description="The format for log entries. This is "+
                    "a format string as defined at http://sling.apache.org/site/client-request-logging.html#ClientRequestLogging-LogFormatSpecification.")
        String request_log_service_format();

        @AttributeDefinition(name = "Logger Name",
                description="Name of the destination for the log "+
                     "output. Depending on the output type this is a file name (absolute or "+
                     "relative), a SLF4J logger name or the name under which a RequestLog service "+
                     "has been registered.")
        String request_log_service_output() default "reuest.log";

        @AttributeDefinition(name = "Logger Type",
                description = "Type of log destination. Select "+
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
        int request_log_service_outputtype() default 0;

        @AttributeDefinition(name = "Request Entry",
                description="Check if the logger is called on "+
                     "request entry. Otherwise leave unchecked and the logger will be called on "+
                     "request exit (aka termination), which is the default for access logger type "+
                     "loggers.")
        boolean request_log_service_onentry() default false;
    }


    private static final int OUTPUT_TYPE_LOGGER = 0;

    private static final int OUTPUT_TYPE_FILE = 1;

    private static final int OUTPUT_TYPE_CLASS = 2;

    private boolean onEntry;

    private CustomLogFormat logFormat;

    private RequestLog log;

    /**
     * Public default constructor for SCR integration
     */
    public RequestLoggerService() {
    }

    RequestLoggerService(BundleContext bundleContext, Config configuration) {
        this.setup(bundleContext, configuration);
    }

    @Activate
    void setup(BundleContext bundleContext, Config configuration) {
        // whether to log on request entry or request exit
        this.onEntry = configuration.request_log_service_onentry();

        // shared or private CustomLogFormat
        final String format = configuration.request_log_service_format();
        if (format != null) {
            this.logFormat = new CustomLogFormat(format);
        }

        // where to log to
        final String output = configuration.request_log_service_output();
        if (output != null) {
            this.log = this.getLog(bundleContext, output, configuration.request_log_service_outputtype());
        }
    }

    @Deactivate
    void shutdown() {
        if (this.log != null) {
            this.log.close();
            this.log = null;
        }

        this.logFormat = null;
    }

    void log(RequestLoggerRequest request, RequestLoggerResponse response) {
        if (this.log != null && this.logFormat != null) {
            this.log.write(this.logFormat.format(request, response));
        }
    }

    boolean isOnEntry() {
        return this.onEntry;
    }

    private RequestLog getLog(BundleContext bundleContext, String output, int outputType) {
        switch (outputType) {
            case OUTPUT_TYPE_FILE:
                // file logging
                try {
                    // ensure the path is absolute
                    File file = new File(output);
                    if (!file.isAbsolute()) {
                        final String home = bundleContext.getProperty("sling.home");
                        if (home != null) {
                            file = new File(home, output);
                        }
                        file = file.getAbsoluteFile();
                    }

                    return new FileRequestLog(file);
                } catch (IOException ioe) {
                    // TODO: log
                }
                break;

            case OUTPUT_TYPE_CLASS:
                // only try to use service if we have a bundle context
                if (bundleContext != null) {
                    return new RequestLogServiceFacade(bundleContext, output);
                }
                break;

            case OUTPUT_TYPE_LOGGER:
            default:
                return new LoggerRequestLog(output);
        }

        // fallback in case of issue or so...
        return null;
    }
}
