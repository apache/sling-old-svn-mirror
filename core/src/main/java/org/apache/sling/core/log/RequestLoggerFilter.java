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
import java.util.Properties;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.osgi.framework.BundleContext;

/**
 * The <code>RequestLoggerFilter</code> is a request level filter, which
 * provides customizable logging or requests handled by Sling. This filter is
 * inserted as the first filter in the request level filter chain and therefore
 * is the first filter called when processing a request and the last filter
 * acting just before the request handling terminates.
 * 
 * @scr.component immediate="true" label="%request.log.name"
 *                description="%request.log.description"
 * @scr.property name="service.description" value="Request Logger Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-2147483648" type="Integer"
 *               private="true"
 * @scr.service
 * @scr.reference name="RequestLoggerService"
 *                interface="org.apache.sling.core.log.RequestLoggerService"
 *                cardinality="0..n" policy="dynamic"
 */
public class RequestLoggerFilter implements ComponentFilter {

    /**
     * @scr.property value="logs/request.log"
     */
    public static final String PROP_REQUEST_LOG_OUTPUT = "request.log.output";

    /**
     * @scr.property value="0" type="Integer" options 0="Logger Name" 1="File
     *               Name" 2="RequestLog Service"
     */
    public static final String PROP_REQUEST_LOG_OUTPUT_TYPE = "request.log.outputtype";

    /**
     * @scr.property value="true" type="Boolean"
     */
    public static final String PROP_REQUEST_LOG_ENABLED = "request.log.enabled";

    /**
     * @scr.property value="logs/access.log"
     */
    public static final String PROP_ACCESS_LOG_OUTPUT = "access.log.output";

    /**
     * @scr.property value="0" type="Integer" options 0="Logger Name" 1="File
     *               Name" 2="RequestLog Service"
     */
    public static final String PROP_ACCESS_LOG_OUTPUT_TYPE = "access.log.outputtype";

    /**
     * @scr.property value="true" type="Boolean"
     */
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
     * extended/combined log format (value is "%h %l %u %t \"%r\" %>s %b
     * \"%{Referer}i\" \"%{User-Agent}i\"").
     */
    private static final String ACCESS_LOG_FORMAT = "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";

    /**
     * The counter for request gone through this filter. As this is the first
     * request level filter hit, this counter should actually count each request
     * which at least enters the request level component filter processing.
     * <p>
     * This counter is reset to zero, when this component is activated. That is,
     * each time this component is restarted (system start, bundle start,
     * reconfiguration), the request counter restarts at zero.
     */
    private int requestCounter;

    /**
     * The list of {@link RequestLoggerService} called when the request enters
     * processing. The order of the services in this list determined by the
     * registration order.
     */
    private RequestLoggerService[] requestEntry;

    /**
     * The list of {@link RequestLoggerService} called when the request is about
     * to exit processing. The order of the services in this list determined by
     * the registration order.
     */
    private RequestLoggerService[] requestExit;

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
     * No further initialization needed by this instance as the full
     * configuration has already been done by the
     * {@link #activate(org.osgi.service.component.ComponentContext)} method.
     * 
     * @param context Not used.
     */
    public void init(ComponentContext context) {
    }

    /**
     * Filters the request as follows:
     * <ol>
     * <li>Creates a wrapper around the <code>response</code> object to catch
     * headers and provide request information at the end. The wrapper also
     * provides customized PrintWriter and ServletOutputStream object which
     * count the number of characters and bytes transferred.</li>
     * <li>Calls loggers configured to be used at request entry time.</li>
     * <li>Forwards the request to the next filter in the chain.</li>
     * <li>Records the time of request termination.</li>
     * <li>Calls loggers configured to be used at request exit time.</li>
     * </ol>
     * 
     * @param request The <code>ComponentRequest</code> representing the
     *            request input sent from the client.
     * @param response The <code>ComponentResponse</code> representing the
     *            response to be sent back to the client.
     * @param filterChain The <code>ComponentFilterChain</code> used to
     *            forward the request on to the next filter.
     * @throws IOException Forwarded if thrown by any filter in the chain or by
     *             the Component called to handle the request.
     * @throws ComponentException Forwarded if thrown by any filter in the chain
     *             or by the Component called to handle the request.
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        LoggerResponse loggerResponse = new LoggerResponse(response,
            requestCounter++);

        // log the request start
        if (requestEntry != null) {
            for (int i = 0; i < requestEntry.length; i++) {
                requestEntry[i].log(request, loggerResponse);
            }
        }

        try {

            // continue request processing without any more intervention
            filterChain.doFilter(request, loggerResponse);

        } finally {

            // signal the end of the request
            loggerResponse.requestEnd();

            // log the request end
            if (requestExit != null) {
                for (int i = 0; i < requestExit.length; i++) {
                    requestExit[i].log(request, loggerResponse);
                }
            }
        }
    }

    /**
     * No further shutdown needed by this instance as the full configuration
     * will bee done by the
     * {@link #deactivate(org.osgi.service.component.ComponentContext)} method.
     */
    public void destroy() {
    }

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
    protected void activate(
            org.osgi.service.component.ComponentContext osgiContext) {

        BundleContext bundleContext = osgiContext.getBundleContext();
        Dictionary props = osgiContext.getProperties();

        // initialize the FileRequestLog with sling.home as the root for
        // relative log file paths
        FileRequestLog.init(bundleContext.getProperty("sling.home"));

        // reset the request counter
        requestCounter = 0;

        // prepare the request loggers if a name is configured and the
        // request loggers are enabled
        Object requestLogName = props.get(PROP_REQUEST_LOG_OUTPUT);
        Object requestLogEnabled = props.get(PROP_REQUEST_LOG_ENABLED);
        if (requestLogName != null && requestLogEnabled instanceof Boolean
            && ((Boolean) requestLogEnabled).booleanValue()) {

            Object requestLogType = props.get(PROP_REQUEST_LOG_OUTPUT_TYPE);

            requestLogEntry = createRequestLoggerService(bundleContext, true,
                REQUEST_LOG_ENTRY_FORMAT, requestLogName, requestLogType);
            requestLogExit = createRequestLoggerService(bundleContext, false,
                REQUEST_LOG_EXIT_FORMAT, requestLogName, requestLogType);
        }

        // prepare the access logger if a name is configured and the
        // access logger is enabled
        Object accessLogName = props.get(PROP_ACCESS_LOG_OUTPUT);
        Object accessLogEnabled = props.get(PROP_ACCESS_LOG_ENABLED);
        if (accessLogName != null && accessLogEnabled instanceof Boolean
            && ((Boolean) accessLogEnabled).booleanValue()) {

            Object accessLogType = props.get(PROP_ACCESS_LOG_OUTPUT_TYPE);

            accessLog = createRequestLoggerService(bundleContext, false,
                ACCESS_LOG_FORMAT, accessLogName, accessLogType);
        }

        // finally have the loggers added to the respective lists for later use
        bindRequestLoggerService(requestLogEntry);
        bindRequestLoggerService(requestLogExit);
        bindRequestLoggerService(accessLog);
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
    protected void deactivate(
            org.osgi.service.component.ComponentContext osgiContext) {

        // remove the loggers if they have been set up
        if (requestLogEntry != null) {
            unbindRequestLoggerService(requestLogEntry);
            requestLogEntry.shutdown();
            requestLogEntry = null;
        }
        if (requestLogExit != null) {
            unbindRequestLoggerService(requestLogExit);
            requestLogExit.shutdown();
            requestLogExit = null;
        }
        if (accessLog != null) {
            unbindRequestLoggerService(accessLog);
            accessLog.shutdown();
            accessLog = null;
        }

        // hack to ensure all log files are closed
        FileRequestLog.dispose();
    }

    /**
     * Binds a <code>RequestLoggerService</code> to be used during request
     * filter.
     * 
     * @param requestLoggerService The <code>RequestLoggerService</code> to
     *            use.
     */
    protected void bindRequestLoggerService(
            RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            requestEntry = addService(requestEntry, requestLoggerService);
        } else {
            requestExit = addService(requestExit, requestLoggerService);
        }
    }

    /**
     * Binds a <code>RequestLoggerService</code> to be used during request
     * filter.
     * 
     * @param requestLoggerService The <code>RequestLoggerService</code> to
     *            use.
     */
    protected void unbindRequestLoggerService(
            RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            requestEntry = removeService(requestEntry, requestLoggerService);
        } else {
            requestExit = removeService(requestExit, requestLoggerService);
        }
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
    private RequestLoggerService createRequestLoggerService(
            BundleContext bundleContext, boolean onEntry, Object format,
            Object output, Object outputType) {
        Properties config = new Properties();
        config.put(RequestLoggerService.PARAM_ON_ENTRY, onEntry
                ? Boolean.TRUE
                : Boolean.FALSE);
        config.put(RequestLoggerService.PARAM_FORMAT, format);
        config.put(RequestLoggerService.PARAM_OUTPUT, output);
        config.put(RequestLoggerService.PARAM_OUTPUT_TYPE, outputType);

        return new RequestLoggerService(bundleContext, config);
    }

    /**
     * Creates a new list of request logger services from the existing list
     * appending the new logger. This method does not check, whether the logger
     * has already been added or not and so may add the the logger multiple
     * times. It is the responsibility of the caller to make sure to not add
     * services multiple times.
     * 
     * @param list The list to add the new service to
     * @param requestLoggerService The service to append to the list
     * @param A new list with the added service at the end.
     */
    private RequestLoggerService[] addService(RequestLoggerService[] list,
            RequestLoggerService requestLoggerService) {
        if (list == null) {
            return new RequestLoggerService[] { requestLoggerService };
        }

        // add the service to the list, must not be in the list yet due to
        // the SCR contract
        RequestLoggerService[] newList = new RequestLoggerService[list.length + 1];
        System.arraycopy(list, 0, newList, 0, list.length);
        newList[list.length] = requestLoggerService;

        return newList;
    }

    /**
     * Creates a new list of request logger services from the existing list by
     * removing the named logger. The logger is searched for by referential
     * equality (comparing the object references) and not calling the
     * <code>equals</code> method. If the last element is being removed from
     * the list, <code>null</code> is returned instead of an empty list.
     * 
     * @param list The list from which the service is to be removed.
     * @param requestLoggerService The service to remove.
     * @return The list without the service. This may be the same list if the
     *         service is not in the list or may be <code>null</code> if the
     *         last service has just been removed from the list.
     */
    private RequestLoggerService[] removeService(RequestLoggerService[] list,
            RequestLoggerService requestLoggerService) {

        RequestLoggerService[] newList = null;
        for (int i = 0; list != null && i < list.length; i++) {
            if (list[i] == requestLoggerService) {
                newList = new RequestLoggerService[list.length - 1];

                // if not first take over the leading elements
                if (i > 0) {
                    System.arraycopy(list, 0, newList, 0, i);
                }

                // if not the last element, shift rest to the left
                if (i < list.length - 1) {
                    System.arraycopy(list, i + 1, newList, 0, newList.length
                        - i);
                }
            }
        }

        // return the new list if at least one entry is contained
        return (newList != null && newList.length > 0) ? newList : null;
    }

}
