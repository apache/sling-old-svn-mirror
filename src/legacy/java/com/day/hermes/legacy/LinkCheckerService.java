/**
 * $Id: LinkCheckerService.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.engine.Service;
import com.day.engine.ServiceException;
import com.day.hermes.DefaultHandlerData;
import com.day.hermes.linkchecker.LinkChecker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * The <code>LinkCheckerService</code> interface is implemented by classes
 * configured in the &lt;linkcheck> element of the <code>DeliveryModule</code>
 * configuration.
 *
 * @version $Revision: 1.7 $
 * @author fmeschbe
 * @since coati
 * @audience core
 */
public interface LinkCheckerService extends Service {

    /** request output is not to be cached – no special treatment needed */
    public static final DefaultHandlerData NO_LINKCHECKER = new DefaultHandlerData(null, null);

    /**
     * Decides on how to handle the request in respect to link checking.
     * Depending on the information found in the request, the method has two
     * options, which are communicated back to the caller in the return value :
     * <ol>
     * <li>No link checking – There exists no link checker applicaple to the
     * 	    page on which the page is based. The return value of the method is
     *      the constant {@link #NO_LINKCHECKER} if the
     *      <code>LinkCheckerService</code> is not willing to participate in
     *      the request handling process.
     * <li>Checking Link – The link checker wants to be involved in further
     *      request processing. The return value of the method is handler
     *      specific data, which will be used when getting the
     *      <code>PrintWriter</code> through the {@link #getWriter} method.
     * </ol>
     *
     * @param req The {@link DeliveryHttpServletRequest} object defining
     *          this request.
     * @param res The {@link DeliveryHttpServletResponse} object used to
     *          send the data to the client.
     *
     * @return {@link #NO_LINKCHECKER} if the <code>LinkCheckerService</code>
     *          does not want to be involved in further request processing
     *          else any object data is returned, which will subsequently be
     *          handed over to the <code>LinkCheckerService</code> in the
     *          {@link #getWriter} method. Implementations must not return
     *          <code>null</code>.
     *
     * @throws ServiceException May be thrown by implementations in case of
     *      problems.
     */
    public DefaultHandlerData checkLinkChecker(DeliveryHttpServletRequest req,
	DeliveryHttpServletResponse res) throws ServiceException;

    /**
     * Creates a <code>PrintWriter</code> wrapping the delegatee suitable for
     * checking links contained in HTML pages. It is assumed, that the content
     * type of the response object suitably defines whether the
     * <code>LinkCheckerService</code> should create a wrapper or not. If the
     * <code>LinkCheckerService</code> decides not to create a wrapper it must
     * return the delegatee object.
     * <p>
     * It is guaranteed that the <code>PrintWriter</code> returned – either the
     * wrapper or the delegatee - is closed at the end of request processing,
     * allowing for necessary cleanup work to be done.
     * <p>
     * The <code>PrintWriter</code> returned must make sure that calls to the
     * <code>PrintWriter.checkError</code> method always returns accurate
     * results.
     *
     * @param delegatee The <code>PrintWriter</code> to be wrapped.
     * @param res The {@link DeliveryHttpServletResponse} object for which to
     *      return a <code>PrintWriter</code>.
     * @param handlerData The {@link DefaultHandlerData} returned from the
     *      {@link #checkLinkChecker} call.
     *
     * @return The link checking <code>PrintWriter</code> or the delegatee if
     *      not checking.
     *
     * @throws IOException If the wrapping <code>PrintWriter</code> cannot be
     *      created.
     */
    public PrintWriter getWriter(PrintWriter delegatee,
            DeliveryHttpServletResponse res, DefaultHandlerData handlerData)
            throws IOException;

    /**
     * Creates a <code>Writer</code> wrapping the delegatee suitable for
     * checking links contained in HTML pages. It is assumed, that the content
     * type of the response object suitably defines whether the
     * <code>LinkCheckerService</code> should create a wrapper or not. If the
     * <code>LinkCheckerService</code> decides not to create a wrapper it must
     * return the delegatee object.
     * <p>
     * It is guaranteed that the <code>Writer</code> returned – either the
     * wrapper or the delegatee - is closed at the end of request processing,
     * allowing for necessary cleanup work to be done.
     *
     * @param delegatee The <code>Wrapper</code> to be wrapped.
     * @param res The {@link DeliveryHttpServletResponse} object for which to
     *      return a <code>PrintWriter</code>.
     * @param handlerData The {@link DefaultHandlerData} returned from the
     *      {@link #checkLinkChecker} call.
     *
     * @return The link checking <code>Writer</code> or the delegatee if not
     *      checking.
     *
     * @throws IOException If the wrapping <code>Writer</code> cannot be created.
     */
    public Writer getWriter(Writer delegatee, DeliveryHttpServletResponse res,
            DefaultHandlerData handlerData) throws IOException;

    /**
     * Registers a link checker, which is only used for the current request
     *
     * @param handlerData The link checker handler data returned from the
     * 		{@link #checkLinkChecker} method.
     */
    public void registerLinkChecker(LinkChecker linkChecker,
        DefaultHandlerData handlerData);

    /**
     * Unregisters a link checker, which was only used for the current request
     *
     * @param handlerData The link checker handler data returned from the
     * 		{@link #checkLinkChecker} method.
     */
    public void unregisterLinkChecker(LinkChecker linkChecker,
        DefaultHandlerData handlerData);

}
