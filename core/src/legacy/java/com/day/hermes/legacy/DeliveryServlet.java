/*
 * $Url: $
 * $Id: DeliveryServlet.java 30093 2007-08-16 09:20:48Z fmeschbe $
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
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.day.engine.EngineServlet;
import com.day.hermes.bootstrap.Master;
import com.day.logging.FmtLogger;
import com.day.net.activation.MimeTab;
import com.day.services.ServiceException;
import com.day.services.ServiceManager;

/**
 * The <code>DeliveryServlet</code> is installed in the servlet container as
 * the servlet handling the requests on behalf of the Communiqu� 3 system. This
 * servlet depends on the {@link DeliveryModule} being instantiated and loaded
 * before the servlet is instantiated. If not, the service method throws a
 * <code>ServiceException</code> indicating the delivery module is not ready.
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2007-08-16 11:20:48 +0200 (Don, 16 Aug 2007) $
 */
public class DeliveryServlet extends HttpServlet {

    /** Default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(DeliveryServlet.class);

    /**
     * The name of the servlet initialization parameter containing the name
     * of the delivery service. If this parameter is missing, name is looked
     * up in the following order:
     * <ol>
     * <li>A property of the given name in the Master properties of the web
     *      application.
     * <li>The default name as specified by the {@link DeliveryService#SERVICE_NAME_DEFAULT}
     *      constant.
     * </ol>
     */
    public static final String INIT_PARAM_DELIVERY_SERVICE_NAME =
        "cq.delivery.name";

    /** The DeliveryModule i use to handle requests */
    private DeliveryService deliveryService;

    /**
     * Creates an instance of the DeliveryServlet
     */
    public DeliveryServlet() {
        deliveryService = getDeliveryService();
    }

    /**
     * Initializes the servlet and sets the servlet context to use for
     * MIME type resolution in the {@link MimeTab} class.
     */
    public void init() throws ServletException {
    	super.init();
    	MimeTab.setServletContext(getServletContext());
    }

    /**
     * Resets any configuration values and also removes the servlet context
     * resolution in the {@link MimeTab} class.
     */
    public void destroy() {
    	MimeTab.setServletContext(null);
    	super.destroy();
    }

    protected void service(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        // check whether DeliveryModule is loaded
        if (deliveryService == null) {
            // try again
            deliveryService = getDeliveryService();

            // check again and send 503/Service unavailable if not
            if (deliveryService == null) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "System not ready yet");
                return;
            }
        }

        // let the module handle the request
        deliveryService.service(request, response);
    }

    /**
     * Tries to get the delivery module and returns null if it is not ready (yet).
     */
    protected DeliveryService getDeliveryService() {

        // check the servlet configuration for the delivery service name
        String deliveryServiceName =
            getInitParameter(INIT_PARAM_DELIVERY_SERVICE_NAME);

        // if not an init-param look in the Master configuration
        if (deliveryServiceName == null || deliveryServiceName.length() == 0) {
            Master master = Master.getInstance(getServletContext());
            Properties props = master.getConfigurationProperties();
            deliveryServiceName = props.getProperty(INIT_PARAM_DELIVERY_SERVICE_NAME);
        }

        // if not in the Master configuration either, use default
        if (deliveryServiceName == null || deliveryServiceName.length() == 0) {
            deliveryServiceName = DeliveryService.SERVICE_NAME_DEFAULT;
        }

        // return the service from the service manager
        ServiceManager sm = EngineServlet.getServiceManager(getServletContext());
        try {
            return (DeliveryService) sm.getService(deliveryServiceName);
        } catch (ServiceException se) {
            log.warn("Delivery service ''{0}'' not available",
                deliveryServiceName, se);
            return null;
        }
    }
}
