/*
 * $Id: RequestEvent.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.hermes.legacy.DeliveryHttpServletResponse;

/**
 * The <code>RequestEvent</code> class provides the contents of a request
 * event sent to the methods of the {@link RequestEventListener2} interface.
 *
 * @author fmeschbe
 * @version $Revision: 1.1 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @audience wad
 * @since iguana
 */
public class RequestEvent {

    /** The request object attached to this event. */
    private final DeliveryHttpServletRequest request;

    /** The response object attached to this event. */
    private final DeliveryHttpServletResponse response;

    /**
     * Creates an event for the given event identification.
     *
     * @param request The request object, which should not be <code>null</code>.
     * @param response The response object, which should not be
     *      <code>null</code>.
     */
    public RequestEvent(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response) {
        this.response = response;
        this.request = request;
    }

    /**
     * Returns the {@link DeliveryHttpServletRequest request} object pertaining
     * to the HTTP request represented by this event.
     * <p>
     * This property is available for all events.
     */
    public DeliveryHttpServletRequest getRequest() {
        return request;
    }


    /**
     * Returns the {@link DeliveryHttpServletResponse response} object
     * pertaining to the HTTP request represented by this event.
     * <p>
     * This property is available for all events.
     */
    public DeliveryHttpServletResponse getResponse() {
        return response;
    }
}
