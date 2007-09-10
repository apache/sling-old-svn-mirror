/*
 * $Id: ScriptEventListener.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import java.util.EventListener;

/**
 * The <code>ScriptEventListener</code> interface provides API to listen
 * on events sent by the {@link com.day.hermes.ScriptHandlerService}.
 *
 * @author fmeschbe
 * @version $Revision: 1.1 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @audience wad
 * @since iguana
 */
public interface ScriptEventListener extends EventListener {

    /**
     * Called when just before the
     * {@link com.day.hermes.ScriptHandlerService} calls the script to
     * handle the request.
     * <p>
     * The event object contains the request and response objects as well as
     * the {@link com.day.hermes.script.ScriptInfo} object describing the
     * script to execute.
     */
    public void beforeScriptHandling(ScriptEvent event);

    /**
     * Called after the {@link com.day.hermes.ScriptHandlerService} has
     * called the script to handle the request.
     * <p>
     * NOTE: There is no guarantee this method being called. Generally this
     *      method will not be called if any of the <code>errorSent</code>
     *      methods is called.
     * <p>
     * <p>
     * The event object contains the request and response objects as well as
     * the {@link com.day.hermes.script.ScriptInfo} object describing the
     * script to execute.
     */
    public void afterScriptHandling(ScriptEvent event);

    /**
     * Handles the situation of the request being terminated either because
     * a status code is sent to the client through the
     * <code>HttpServletResponse.sendError</code> method or because the script
     * is terminated due to an exception.
     * <p>
     * The event object contains the request and response objects as well as
     * the {@link com.day.hermes.script.ScriptInfo} object describing the
     * script to handle the status code or the <code>Throwable</code>. The
     * status code and status message or the <code>Throwable</code> are
     * available depending on the type request abort is occurring. Generally
     * speaking, if the status is <code>-1</code>, the <code>Throwable</code>
     * is non-<code>null</code> and vice-versa.
     */
    public void requestAborted(ScriptEvent event);
}
