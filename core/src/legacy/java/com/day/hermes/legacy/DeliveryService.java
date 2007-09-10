/*
 * $Url: $
 * $Id: DeliveryService.java 23649 2006-11-24 19:33:01Z fmeschbe $
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.day.services.Service;
import com.day.services.ServiceException;

/**
 * The <code>DeliveryService</code> TODO
 *
 * @author fmeschbe
 * @version $Rev:$, $Date: 2006-11-24 20:33:01 +0100 (Fre, 24 Nov 2006) $
 */
public class DeliveryService extends Service {

    public static final String SERVICE_NAME_DEFAULT = "Delivery";

    /* package */ void service(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

    }

    //---------- Service API --------------------------------------------------

    protected void doStart() throws ServiceException {

    }

    protected void doStop() {

    }
}
