/*
 * $Url: $
 * $Id: DeliveryHttpServletRequestWrapper.java 30093 2007-08-16 09:20:48Z fmeschbe $
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import com.day.logging.FmtLogger;

/**
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2007-08-16 11:20:48 +0200 (Don, 16 Aug 2007) $
 */
public class DeliveryHttpServletRequestWrapper
        extends HttpServletRequestWrapper {

    /** default log category */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(DeliveryHttpServletRequestWrapper.class);

    public DeliveryHttpServletRequestWrapper(HttpServletRequest delegatee) {
        super(delegatee);
    }

    /**
     * Sets the request object being wrapped and resets all internal fields to
     * their initial state, as if the object would be newly allocated. This
     * is primarily used to recycle wrapper instances.
     * <p>
     * <strong>NOTE: THIS METHOD IS NOT FOR THE FAINT OF HEART AND MUST NOT
     * BE CALLED DELIBERATELY AS THE STABILITY OF THE SYSTEM MAY SUFFER.</strong>
     *
     * @param delegatee The <code>HttpServletRequest</code> to be wrapped.
     *
     * @throws ClassCastException if the delegatee is not an instance
     * 		of <code>HttpServletRequest</code>
     * @throws IllegalArgumentException if the request is null.
     */
    public void setRequest(HttpServletRequest delegatee) {
        super.setRequest(delegatee);
    }


    public StringBuffer getRequestURL() {
        log.warn("Servlet 2.3 API function invoked");
        throw new UnsupportedOperationException();
    }

    public void setCharacterEncoding(String s) {
        log.warn("Servlet 2.3 API function invoked");
        throw new UnsupportedOperationException();
    }

    public Map getParameterMap() {
    	log.warn("Servlet 2.3 API function invoked");
    	throw new UnsupportedOperationException();
    }
}