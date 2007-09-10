/*
 * $Url: $
 * $Id: DeliveryHttpServletResponseWrapper.java 30093 2007-08-16 09:20:48Z fmeschbe $
 *
 * Copyright 1997-2005 Day Management AG
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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.day.logging.FmtLogger;

/**
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2007-08-16 11:20:48 +0200 (Don, 16 Aug 2007) $
 */
public class DeliveryHttpServletResponseWrapper
        extends HttpServletResponseWrapper {

    /** default log category */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(DeliveryHttpServletResponseWrapper.class);

    public DeliveryHttpServletResponseWrapper(HttpServletResponse delegatee) {
        super(delegatee);
    }

    /**
     * Sets the response being wrapped.
     *
     * @param delegatee The <code>HttpServletResponse</code> to wrap
     *
     * @throws IllegalArgumentException if the response is <code>null</code>.
     * @throws ClassCastException if the delegatee is not a
     * 		<code>HttpServletResponse</code> object.
     */
    public void setResponse(HttpServletResponse delegatee) {
        super.setResponse(delegatee);
    }

    public void resetBuffer() {
        log.warn("Servlet 2.3 API function invoked");
    }
}
