/*
 * $Id: DeliveryException.java 22189 2006-09-07 11:47:26Z fmeschbe $
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
package com.day.hermes.legacy;

/**
 * Main exception thrown by classes in this package. May either contain
 * an error message or another exception wrapped inside this exception.
 *
 * @version $Revision: 1.8 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since antbear
 * @audience core
 */
public class DeliveryException extends Exception {

    /**
     * Creates a <code>DeliveryException</code> given
     * a message describing the failure cause
     * @param s description
     */
    public DeliveryException(String s) {
        super(s);
    }

    /**
     * Creates a <code>DeliveryException</code> given
     * a message describing the failure cause and a root
     * exception
     * @param s description
     * @param e root failure cause
     */
    public DeliveryException(String s, Exception e) {
        super(s, e);
    }

    /**
     * Creates a <code>DeliveryException</code> given
     * a root exception
     * @param e root failure cause
     */
    public DeliveryException(Exception e) {
        super(e);
    }
}