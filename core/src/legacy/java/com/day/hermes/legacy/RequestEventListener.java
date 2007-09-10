/*
 * $Id: RequestEventListener.java 22189 2006-09-07 11:47:26Z fmeschbe $
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
package com.day.hermes.legacy;

import java.util.EventListener;

/**
 * The <code>RequestEventListener2</code> class replaces the
 * {@link com.day.hermes.RequestEventListener} interface replacing the
 * original argument list with the {@link RequestEvent}.
 *
 * @version $Revision: 1.1 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since hawk
 * @audience wad
 */
public interface RequestEventListener extends EventListener {

    /**
     * Handles request attachement of the indicated request.
     * <p>
     * The attachement event is sent to registered listeners after the
     * <code>Ticket</code> has been extracted from the request and the burst
     * cache was handled. That is, if a request is handled through the burst
     * cache, the attachement event is not sent.
     * <p>
     * Note that though the ticket field of this request object is valid, the
     * request URL has not been mapped yet. Also access has not yet been granted
     * on the request resource.
     *
     * @param event The {@link RequestEvent} for this attachement event.
     */
    void requestAttached(RequestEvent event);

    /**
     * Handles request detachement of the indicated request.
     * <p>
     * The detachment event is sent immediately before the request object is
     * recycled at the end of request processing. Specifically, all script
     * handling including error or exception has already been done for this
     * request.
     * <p>
     * This event is only sent for any given request, if the attachement event
     * event has also been sent. Specifically if the <code>Ticket</code> could
     * not be extracted from the request or if the request was handled through
     * the burst cache, this event is not  sent.
     *
     * @param event The {@link RequestEvent} for this detachement event.
     */
    void requestDetached(RequestEvent event);
}
