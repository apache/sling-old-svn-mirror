/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.log;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.sling.RequestLog;
import org.apache.sling.component.ComponentRequest;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>RequestLoggerService</code> is a factory component which gets
 * configuration to register loggers for the {@link RequestLoggerFilter}.
 *
 * @scr.component label="%request.log.service.name" description="%request.log.service.description"
 *                factory="org.apache.sling.core.log.RequestLoggerService"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *                value="Factory for configuration based request/access loggers"
 * @scr.service interface="org.apache.sling.core.log.RequestLoggerService"
 */
public class RequestLoggerService {

    /** @scr.property */
    public static final String PARAM_FORMAT = "request.log.service.format";

    /** @scr.property value="request.log" */
    public static final String PARAM_OUTPUT = "request.log.service.output";

    /**
     * @scr.property value="0" type="Integer" options 0="Logger Name" 1="File
     *               Name" 2="RequestLog Service"
     */
    public static final String PARAM_OUTPUT_TYPE = "request.log.service.outputtype";

    /** @scr.property value="false" type="Boolean" */
    public static final String PARAM_ON_ENTRY = "request.log.service.onentry";

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

    RequestLoggerService(BundleContext bundleContext, Dictionary configuration) {
        setup(bundleContext, configuration);
    }

    void setup(BundleContext bundleContext, Dictionary configuration) {
        // whether to log on request entry or request exit
        Object onEntryObject = configuration.get(PARAM_ON_ENTRY);
        onEntry = (onEntryObject instanceof Boolean)
                ? ((Boolean) onEntryObject).booleanValue()
                : false;

        // shared or private CustomLogFormat
        Object format = configuration.get(PARAM_FORMAT);
        if (format != null) {
            logFormat = new CustomLogFormat(format.toString());
        }

        // where to log to
        Object output = configuration.get(PARAM_OUTPUT);
        if (output != null) {
            Object outputTypeObject = configuration.get(PARAM_OUTPUT_TYPE);
            int outputType = (outputTypeObject instanceof Number)
                    ? ((Number) outputTypeObject).intValue()
                    : OUTPUT_TYPE_LOGGER;
            log = getLog(bundleContext, output.toString(), outputType);
        }
    }

    void shutdown() {
        if (log != null) {
            log.close();
            log = null;
        }

        logFormat = null;
    }

    void log(ComponentRequest request, LoggerResponse response) {
        if (log != null && logFormat != null) {
            log.write(logFormat.format(request, response));
        }
    }

    boolean isOnEntry() {
        return onEntry;
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        setup(context.getBundleContext(), context.getProperties());
    }

    protected void deactivate(ComponentContext context) {
        shutdown();
    }

    private RequestLog getLog(BundleContext bundleContext, String output,
            int outputType) {
        switch (outputType) {
            case OUTPUT_TYPE_FILE:
                // file logging
                try {
                    return new FileRequestLog(output);
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
