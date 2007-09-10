/*
 * $Id: ScriptEvent.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright (c) 1997-2003 Day Management AG
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

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.hermes.legacy.DeliveryHttpServletResponse;
import com.day.hermes.legacy.RequestEvent;

/**
 * The <code>ScriptEvent</code> class
 *
 * @author fmeschbe
 * @version $Revision: 1.1 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @audience
 */
public class ScriptEvent extends RequestEvent {

    /** The script info object of the event or <code>null</code> */
    private final ScriptInfo scriptInfo;

    /** The status code of the event or -1 */
    private final int status;

    /** The status message of the event or null */
    private final String message;

    /** The throwable of the event or null */
    private final Throwable error;

    /**
     * Creates a script event for the given request and response.
     *
     * @param request The request object, which should not be <code>null</code>.
     * @param response The response object, which should not be
     *      <code>null</code>.
     * @param scriptInfo The {@link com.day.hermes.script.ScriptInfo}
     *      instance describing the script to be executed for the request.
     */
    public ScriptEvent(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response, ScriptInfo scriptInfo) {
        super(request, response);

        this.scriptInfo = scriptInfo;
        this.status = -1;
        this.message = null;
        this.error = null;
    }

    /**
     * Creates an status script event event with the given status code and
     * message.
     *
     * @param request The request object, which should not be <code>null</code>.
     * @param response The response object, which should not be
     *      <code>null</code>.
     * @param scriptInfo The {@link com.day.hermes.script.ScriptInfo}
     *      instance describing the script to be executed to handle the
     *      error situation.
     * @param status The status code of the event. This code should be in the
     *      range of valid status codes according to the HTTP RFC.
     * @param message The status message of the event. May be <code>null</code>
     *      if no message is supplied.
     */
    public ScriptEvent(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response, ScriptInfo scriptInfo,
            int status, String message) {
        super(request, response);

        this.scriptInfo = scriptInfo;
        this.status = status;
        this.message = message;
        this.error = null;
    }

    /**
     * Creates a script event with the given <code>Throwable</code>.
     *
     * @param request The request object, which should not be <code>null</code>.
     * @param response The response object, which should not be
     *      <code>null</code>.
     * @param scriptInfo The {@link com.day.hermes.script.ScriptInfo}
     *      instance describing the script to be executed to handle the
     *      error situation.
     * @param throwable The <code>Throwable</code> causing the request to be
     *      terminated. This value should not be <code>null</code>.
     */
    public ScriptEvent(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response, ScriptInfo scriptInfo,
            Throwable throwable) {
        super(request, response);

        this.scriptInfo = scriptInfo;
        this.status = -1;
        this.message = null;
        this.error = throwable;
    }

    /**
     * Returns the {@link com.day.hermes.script.ScriptInfo} object related
     * to this event. This could be <code>null</code> if the information is
     * not available.
     */
    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }

    /**
     * Returns the status code which is sent to the HTTP client. If the event
     * is not sent for the execution of the status code related error handling
     * script, this method returns <code>-1</code>.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the status message which is sent to the HTTP client. This method
     * might return <code>null</code> if the message is not available or if the
     * event is not sent for the execution of the status code related error
     * handling.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the <code>Throwable</code> causing the request abortion. If the
     * event is not sent for the execution of the <code>Throwable</code>
     * related error handling script, this method returns <code>null</code>.
     */
    public Throwable getError() {
        return error;
    }
}
