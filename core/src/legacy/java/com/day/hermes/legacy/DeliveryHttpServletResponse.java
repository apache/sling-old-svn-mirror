/*
 * $Url: $
 * $Id: DeliveryHttpServletResponse.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The <code>DeliveryHttpServletResponse</code> extends the
 * <code>HttpServletResponse</code> to support additional API. Specifically the
 * extensions are used by the {@link CacheHandlerService} and the
 * {@link LinkCheckerService} to access header information set previously
 * through one of the header setting methods.
 *
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
s */
public interface DeliveryHttpServletResponse extends HttpServletResponse {

    //---------- wrapper stuff -------------------------------------------------

    /**
     * Returns the original HttpServletResponse which is contained in this.
     * Implementations are allowed to return the instance itself instead of
     * a wrapped object. Returning <code>null</code> is not allowed.
     */
    public HttpServletResponse getResponse();

    /**
     * Sets the cache-control header field. Calling this method is equivalent
     * to calling <code>setHeader("Cache-Control", cacheControl)</code>.
     *
     * @param cacheControl The cache control setting.
     */
    public void setCacheControl(String cacheControl);

    /**
     * Returns the status of the response as set with the <code>setStatus</code>
     * method or 200 (OK) if not explicitly set yet.
     *
     * @return The status code of the request or 200 if not set yet.
     */
    public int getStatus();

    /**
     * Return the full content type stored in a previous
     * {link #setContentType(String)} invocation, or the default content type
     * if none is set.
     * <p>
     * <em>Note that the default content type returned by this method is not
     * set as the <code>Content-Type</code> header in the response. There is no
     * default content type header setting. The return value is simply a
     * convencience to the caller.</em>
     *
     * @return The content type if set or <code>text/html</code> if not set
     *      explicitly yet.
     */
    public String getContentType();

    /**
     * Returns the value of the cache control header as set by the
     * {@link #setHeader(String, String)} or the {@link #setCacheControl(String)}
     * methods.
     *
     * @return The cache control header setting or <code>null</code> if not set
     *      yet.
     */
    public String getCacheControl();

    /**
     * Maps the ContentBus handle to an URI string usable as the request URI.
     * The return value does NOT contain the context path needed to be
     * prepended to the URI if the delivery servlet runs in anything else than
     * the default root context.
     *
     * @param handle The ContentBus handle to map to a URI string.
     *
     * @return The URI string corresponding to the handle without the possibly
     *      also needed context path.
     */
    public String mapHandle(String handle);

//    /**
//     * Registers a linkchecker which is only used during link checking. This
//     * method may be called any time during request processing. The new
//     * link checker is immediately used from this point on but only until the
//     * processing of the request finishes.
//     *
//     * @param linkChecker The {@link LinkChecker} to be additionally used for
//     *      the further processing of this request's response data.
//     */
//    public void registerLinkChecker(LinkChecker linkChecker);
//
//    /**
//     * Unegisters a linkchecker which is only used during link checking. This
//     * method may be called any time during request processing. The new
//     * link checker is immediately removed from the list of linkcheckers.
//     * <p>
//     * This method should only remove per-request link checkers but not any
//     * system wide link checkers.
//     *
//     * @param linkChecker The {@link LinkChecker} to be removed from the
//     *      current list of linkcheckers.
//     *
//     * @since fennec
//     */
//    public void unregisterLinkChecker(LinkChecker linkChecker);

    /**
     * Returns a ServletOutputStream suitable for writing binary data in the
     * response. The servlet container does not encode the binary data.
     * <p>
     * Calling flush() on the ServletOutputStream commits the response. Either
     * this method or {@link #getWriter()} may be called to write the body, not
     * both.
     * <p>
     * If the {@link CacheHandlerService} decides to cache the generated output
     * it will be called to wrap the original <code>OutputStream</code> for its
     * caching purposes.
     *
     * @return a <code>ServletOutputStream</code> for writing binary data
     *
     * @throws IllegalStateException if the <code>getWriter</code> method has
     * 		been called on this response
     * @throws IOException if an input or output exception occurred
     *
     * @see #getWriter()
     */
    public ServletOutputStream getOutputStream() throws IOException;

    /**
     * Returns a PrintWriter object that can send character text to the client.
     * The character encoding used is the one specified in the
     * <code>charset=</code> property of the {@link #setContentType(String)}
     * method, which must be called before calling this method for the charset
     * to take effect.
     * <p>
     * If necessary, the MIME type of the response is modified to reflect the
     * character encoding used.
     * <p>
     * Calling flush() on the PrintWriter commits the response.
     * <p>
     * Either this method or <code>getOutputStream()</code> may be called to
     * write the body, not both.
     * <p>
     * If the {@link CacheHandlerService} decides to cache the generated output
     * it will be called to wrap the original <code>PrintWriter</code> for its
     * caching purposes. Also if the {@link LinkCheckerService} decides checking
     * the link in the output, it will be called to wrap either the original
     * or the {@link CacheHandlerService CacheHandlerService's}
     * <code>PrintWriter</code> for its link checking purposes.
     *
     * @return a <code>PrintWriter</code> object that can return character data
     * 		to the client
     *
     * @throws java.io.UnsupportedEncodingException if the charset specified in
     * 		<code>setContentType</code> cannot be used
     * @throws IllegalStateException if the <code>getOutputStream</code> method
     * 		has already been called for this response object
     * @throws IOException if an input or output exception occurred
     *
     * @see #getOutputStream()
     */
    public PrintWriter getWriter() throws IOException;

    /**
     * Reset the response buffer if possible. The actual implementation will
     * depend on whether we're running in a <code>2.2</code> environment or
     * in a <code>2.3</code> environment.
     */
    public void resetResponseBuffer();

    /**
     * Registers the handle as an additional dependency for the cache entry of
     * the response. If the response is not being cached, this method does
     * nothing.
     *
     * @param handle The name of the page on which the cache entry for the
     *      response additionally depends if the response is cached at all.
     *
     * @since echidna
     *
     * @see CacheHandlerService#registerDependency
     */
    public void registerDependency(String handle);

    /**
     * Tells this response to not cache the current request.
     * That is, even the CacheHandler initially thought is could cache the current
     * request, a template script can indicate with this method that the cache
     * entry should not be cached.
     *
     * @since echidna
     *
     * @see CacheHandlerService#stopCaching
     */
    public void stopCaching();

    /**
     * Specifies how long the response should be considered valid. This method
     * may be used to specify a maximum age of a cached response different from
     * the default setting applied by the cache handler.
     * <p>
     * This method has no effect, if the response is not cached at all.
     *
     * @param maxAge The maximum time in milliseconds that the response to the
     *      current should be considered valid and may therefore be delivered
     *      from cache.
     *
     * @since iguana
     */
    public void setCacheMaxAge(long maxAge);
}