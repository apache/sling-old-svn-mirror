/**
 * $Id: ScriptHandlerService.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.script;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.hermes.legacy.DeliveryHttpServletResponse;
import com.day.services.Service;

/**
 * The <code>ScriptHandlerService</code> interface is implemented by classes
 * configured in the &lt;script> element of the <code>DeliveryModule</code>
 * configuration.
 *
 * @version $Revision: 1.8 $
 * @author fmeschbe
 * @since coati, enhanced with support for
 *      {@link com.day.hermes.event.ScriptEvent script events} in iguana.
 * @audience core
 */
public abstract class ScriptHandlerService extends Service {

    /**
     * Request attribute containing the status code. This attribute is only
     * set for handling the {@link DeliveryHttpServletResponse#sendError} method.
     */
    public static final String JSE_STATUS_CODE = "javax.servlet.error.status_code";

    /**
     * The class of the exception thrown in exception handling. This attribute
     * is only set for exception handling.
     */
    public static final String JSE_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

    /**
     * Request attribute containing the status message if any. This attribute
     * is set to the message of the
     * {@link DeliveryHttpServletResponse#sendError(int, String)} method or to
     * the exception message for exception handling.
     */
    public static final String JSE_MESSAGE = "javax.servlet.error.message";

    /**
     * Request attribute containing the exception thrown. This attribute is
     * only set for exception handling.
     */
    public static final String JSE_EXCEPTION = "javax.servlet.error.exception";

    /** Request attribute containing the original URI */
    public static final String JSE_REQUEST_URI = "javax.servlet.error.request_uri";

    /**
     * Request attribute containing the name of the servlet which was executed
     * when the error or exception occurred.
     */
    public static final String JSE_SERVLET_NAME = "javax.servlet.error.servlet_name";

    /**
     * Calls the named script handling the request. It is the sole duty of
     * this script to handle the request.
     * <p>
     * The script to execute is defined in the page template. The DeliveryModule
     * calling the call() method is responsible for evaluating the template
     * configuration for the request.
     * <p>
     * Before and after calling the script, all registered
     * {@link com.day.hermes.event.ScriptEventListener listeners} are
     * notified.
     *
     * @param servlet The servlet instance called to handle the current request.
     * @param req The wrapped request object
     * @param res The wrapped response object
     *
     * @throws ServletException if a problem occurrs sending or handling the
     *         request. The eventual root cause should be set in the exception.
     */
    public abstract void call(HttpServlet servlet,
        DeliveryHttpServletRequest req, DeliveryHttpServletResponse res)
        throws ServletException, IOException;

    /**
     * Executes an error handling script for the status code to send back an
     * error to the client. If the method cannot or doesn’t want to handle the
     * error condition, it returns false, else it is assumed, the client is
     * informed on the status.
     * <p>
     * Before actually calling the exception handling script, all registered
     * {@link com.day.hermes.event.ScriptEventListener listeners} are
     * notified.
     *
     * @param statusCode The status code to inform the client on.
     * @param statusMessage The message to the code, may be <code>null</code>.
     * @param servlet The servlet instance called to handle the current request.
     * @param req The wrapped request object
     * @param res The wrapped response object
     *
     * @return <code>true</code> if the status code handling has been done by
     *      the method or <code>false</code>, if the client is not informed yet.
     */
    public abstract boolean sendError(int statusCode, String statusMessage,
        HttpServlet servlet, DeliveryHttpServletRequest req,
        DeliveryHttpServletResponse res);

    /**
     * Executes the exception handling script for the exception. Like with the
     * Servlet API specification this method's implementation should obey the
     * class hierarchy and use the most specific class's configuration.
     * <p>
     * Before actually calling the exception handling script, all registered
     * {@link com.day.hermes.event.ScriptEventListener listeners} are
     * notified.
     *
     * @param throwable The <code>Throwable</code> for which to call the.
     *      Generally this is either a <code>RuntimeException</code>, an
     *      <code>IOException</code>, or a <code>ServletException</code>. If
     *      any other type is contained as the root cause in a
     *      <code>ServletException</code>, this may also be used as the
     *      argument.
     * @param servlet The servlet instance called to handle the current request.
     * @param req The wrapped request object
     * @param res The wrapped response object
     *
     * @return <code>true</code> if the exception  handling has been done by
     *      the method or <code>false</code>, if the client is not informed yet.
     */
    public abstract boolean sendError(Throwable throwable, HttpServlet servlet,
        DeliveryHttpServletRequest req, DeliveryHttpServletResponse res);

    /**
     * Adds the given script event listener to the list of registered event
     * listeners. If the listener is already registered it is not added again.
     * Implementations are free to implement whatever notion they choose to
     * define equality but are encouraged to document which notion they employ.
     * <p>
     * Implementations should act gracefully if called with a <code>null</code>
     * argument.
     *
     * @param listener The {@link com.day.hermes.event.ScriptEventListener}
     *      listener to add.
     *
     * @since iguana
     */
    public abstract void addScriptEventListener(ScriptEventListener listener);

    /**
     * Removes the given script event listener from the list of registered event
     * listeners. If the listener is not registered nothing is done.
     * Implementations are free to implement whatever notion they choose to
     * define equality but are encouraged to document which notion they employ.
     * <p>
     * Implementations should act gracefully if called with a <code>null</code>
     * argument.
     *
     * @param listener The {@link com.day.hermes.event.ScriptEventListener}
     *      listener to remove.
     *
     * @since iguana
     */
    public abstract void removeScriptEventListener(ScriptEventListener listener);
}

