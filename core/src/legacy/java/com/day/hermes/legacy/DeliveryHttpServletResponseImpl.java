/**
 * $Id: DeliveryHttpServletResponseImpl.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.engine.Engine;
import com.day.hermes.DefaultHandlerData;
import com.day.hermes.contentbus.Ticket;
import com.day.hermes.linkchecker.LinkChecker;
import com.day.hermes.logging.FmtLogger;
import com.day.hermes.logging.Log;
import com.day.hermes.script.ScriptHandlerService;
import com.day.hermes.util.Finalizer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * The <code>DeliveryHttpServletResponseImpl</code> class is a
 * <code>HttpServletResponse</code> wrapper with the duty to handle errors and
 * redirects and properly wrapping OutputStreams and PrintWriters requested from
 * client, such that the appropriate handler services are enabled.
 * <p>
 * For included servlets this wrapper also prevents setting and/or modifying
 * header values in the wrapped <code>HttpServletResponse</code> object.
 * <p>
 * Instances of this class should be considered valid only for the duration of
 * the request for which the were prepared. As a request is expected to be
 * handled by one thread and only one thread, no provisions have been made to
 * make this class thread safe.
 * <p>
 * Just like the abstract base class this class is intended to be re-used to
 * optimize system performance.
 *
 * @version $Revision: 1.34 $
 * @author fmeschbe
 * @since coati
 * @audience core
 */
class DeliveryHttpServletResponseImpl extends DeliveryHttpServletResponseWrapper
	implements DeliveryHttpServletResponse, Finalizer {

    //--------- static fields --------------------------------------------------

    /** default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(DeliveryHttpServletResponseImpl.class);

    /** The default content charset if not set in the ContentType Header */
    private static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";

    /** The system ticket used to execute the error handler */
    private static Ticket systemTicket;

    /** The URL Mapper service used to map handles to URI strings */
    private static URLMapperService urlMapperService;

    /** The cache handler to ask for the caching OuputStream/PrintWriter */
    private static CacheHandlerService cacheHandlerService;

    /** The link checker to ask for the link-checked PrintWriter */
    private static LinkCheckerService linkCheckerService;

    /** The {@link ScriptHandlerService} handling {@link #sendError} */
    private static ScriptHandlerService scriptHandlerService;

    /** The name of the current DeliveryHttpServletResponse attribute */
    private static final String CURRENT_RESPONSE = "com.day.hermes.current_response";

    //---------- fields --------------------------------------------------------

    /** The handler data returned from the checkCache method */
    private DefaultHandlerData cacheHandlerData;

    /** The handler data returned from the checkLinkChecker method */
    private DefaultHandlerData linkCheckerData;

    /** Content type */
    protected String contentType;

    /**
     * The charset value of the {@link #contentType}, set on demand by
     * {@link #getContentCharSet}
     */
    protected String contentCharSet;

    /** Cache control */
    protected String cacheControl;

    /** Status */
    private int status;

    /** The {@link DeliveryHttpServletRequest} for {@link #sendError} */
    private DeliveryHttpServletRequest request;

    /** The <code>HttpServlet</code> using this response for {@link #sendError} */
    private HttpServlet servlet;

    /** The parent DeliveryHttpServletResponseImpl object */
    protected DeliveryHttpServletResponseImpl parentResponse;

    /**
     * Flag set to <code>true</code>, when the <code>sendError</code> methods
     * in this implementation were successfull sending the error (status code
     * or <code>Throwable</code>) to the client. This is also set to
     * <code>true</code> before calling the script handler's <code>sendError</code>
     * method to prevent loops.
     */
    private boolean errorSent;

    /** The output stream we configured and returned */
    protected ServletOutputStream out;

    /** The PrintWriter we configured */
    protected PrintWriter writer;

    /**
     * The output buffer size.
     * @see #setBufferSize
     * @see #getBufferSize
     */
    private int bufferSize;

    /**
     * <code>true</code> if the response should be considered closed and no
     * output channel should be opened or returned any more.
     * @see #closeOutput
     */
    private boolean outputClosed;

    //---------- construction and recycling ------------------------------------

    /**
     * Sets up the class to know about the needed services. These services only
     * exist once in the system and are used to handle the response side of the
     * request.
     *
     * @param urlMapperService The {@link URLMapper} used to
     *      map internal handles to valid external request URI strings.
     * @param cacheHandlerService The {@link CacheHandlerService} used to obtain
     *      the caching <code>OutputStream</code> or <code>PrintWriter</code>.
     * @param linkCheckerService The {@link LinkCheckerService} used to obtain
     *      the link checking <code>PrintWriter</code>.
     */
    static void setup(URLMapperService urlMapperService,
	CacheHandlerService cacheHandlerService,
	LinkCheckerService linkCheckerService,
        ScriptHandlerService scriptHandlerService) {

        DeliveryHttpServletResponseImpl.urlMapperService = urlMapperService;
	com.day.hermes.legacy.cacheHandlerService = cacheHandlerService;
	DeliveryHttpServletResponseImpl.linkCheckerService = linkCheckerService;
        DeliveryHttpServletResponseImpl.scriptHandlerService = scriptHandlerService;

        com.day.hermes.legacy.systemTicket
                = (Ticket) Engine.getInstance().getAttribute(Engine.SYSTEM_TICKET);
    }

    /**
     * Creates a new <code>DeliveryHttpServletResponseImpl</code> wrapping the given
     * <code>HttpServletRequest</code>.
     *
     * @param response The <code>HttpServletResponse</code> to wrap.
     */
    protected DeliveryHttpServletResponseImpl(HttpServletResponse response) {
	super(response);

        // setup default values
        this.outputClosed = false;
        this.status = SC_OK;
        this.bufferSize = response.getBufferSize();
    }

    /**
     * Returns a <code>DeliveryHttpServletResponseImpl</code> wrapper for the given
     * <code>HttpServletResponse</code>. This instance may be newly created or
     * from the pool of recycled wrapper instances.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefore
     * the method is package private.
     *
     * @param delegatee The <code>HttpServletResponse</code> object to wrap
     * @param request The {@link DeliveryHttpServletRequest} of the request, which
     *      will later be used for the {@link #sendError} method.
     * @param servlet The <code>HttpServlet</code> handling the request, which
     *      will later be used for the {@link #sendError} method.
     *
     * @return A prepared <code>DeliveryHttpServletResponseImpl</code> object.
     */
    static DeliveryHttpServletResponseImpl getInstance(
	    ServletSpec servletSpec, HttpServletResponse delegatee,
	    DeliveryHttpServletRequest request, HttpServlet servlet) {

        // short helper
        boolean included = request.isIncluded();

	DeliveryHttpServletResponseImpl res;
        if (included) {
            res = servletSpec == ServletSpec.V2_2 ?
                    new DeliveryHttpServletResponseIncluded(delegatee) :
                    new DeliveryHttpServletResponseIncluded23(delegatee);
        } else {
            res = servletSpec == ServletSpec.V2_2 ?
                    new DeliveryHttpServletResponseImpl(delegatee) :
                    new DeliveryHttpServletResponseImpl23(delegatee);
        }

	    // assign properties
        res.request = request;
        res.servlet = servlet;

        // get parent response
        try {
            res.parentResponse = (DeliveryHttpServletResponseImpl)request.getAttribute(CURRENT_RESPONSE);
            request.setAttribute(CURRENT_RESPONSE, res);
        } catch (ClassCastException e) {
            log.warn("<init>: request attribute {0} is not of type DeliveryHttpServletResponseImpl!", CURRENT_RESPONSE);
        }
        // preset cache handler and link checker data
        res.cacheHandlerData = CacheHandlerService.DONT_CACHE;
        res.linkCheckerData = LinkCheckerService.NO_LINKCHECKER;

	return res;
    }

    /**
     * Takes back an instance of the wrapper to put in the recyclable list for
     * reuse by the {@link #getInstance} method.
     * <p>
     * By taking the instance back for re-use, all the internal fields are reset
     * to the same initial state as when initialized by allocation before
     * calling the constructor.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     *
     * @param res The <code>DeliveryHttpServletResponseImpl</code> object
     */
    static void recycleInstance(DeliveryHttpServletResponseImpl res) {
	if (res != null) {
            // finalize the response - we might want to be registered ?
	    try {
	        res.doFinalize();
	    } catch (RuntimeException re) {
	        log.error("recycleInstance: Unexpected exception: {0}", 
	            re.getMessage(), re);
	    }
	}
    }

    /**
     * Sets cache handler data.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     */
    void setCacheHandlerData(DefaultHandlerData cacheHandlerData) {
	this.cacheHandlerData = cacheHandlerData;
    }

    /**
     * Sets link checker data.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     */
    void setLinkCheckerData(DefaultHandlerData linkCheckerData) {
	this.linkCheckerData = linkCheckerData;
    }

    //---------- wrapping getOutputStream and getWriter ------------------------

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
    public ServletOutputStream getOutputStream() throws IOException {
        log.debug("DeliveryHttpServletResponseImpl.getOutputStream()");

        if (out == null) {

            OutputStack stack = OutputStack.getOutputStack(request, getResponse());
            OutputStream stream = stack.createOutputStream();

            if (outputClosed) {
                /**
                 * if the output has already been closed after a call to
                 * sendError, we close the output stream just received
                 */
                stream.close();
            } else {
                // wrap with caching OutputStream
                stream = cacheHandlerService.getOutputStream(stream, this, cacheHandlerData);
            }

            // register the final output stream as the top of the stack
            out = stack.pushOutputStream(stream);

        }

        return out;
    }

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
     * @see #setContentType(String)
     */
    public PrintWriter getWriter() throws IOException {
        log.debug("DeliveryHttpServletResponseImpl.getWriter()");

        if (writer == null) {

            if (request.isIncluded()) {
                /*
                 * force writer init and flushing.
                 * this is needed because a possible JspWriter
                 * might do buffering. That is, we need to make
                 * sure data is pumped through the stack before
                 * we insert another cache PluginWriter
                 */
                super.getWriter().flush();
            }

            // get the writer stack - must be called with wrapped response !!
            OutputStack stack = OutputStack.getOutputStack(request, getResponse());

            // get the offical writer to use from the stack
            Writer writer = stack.createWriter();

            if (outputClosed) {
                /**
                 * if the output has already been closed after a call to
                 * sendError, we close the writer just received
                 */
                writer.close();
            } else {
                // optionally wrap with the writer for the chache handler service
                writer = cacheHandlerService.getWriter(writer, this, cacheHandlerData);

                // optionally wrap with the writer for the link checker service
                if (isTextHtml()) { // is this the right place to check ???
                    writer = linkCheckerService.getWriter(writer, this, linkCheckerData);
                }
            }

            // register the final writer as the top of the stack,
            // replacing the writer retrieved above using stack.getWriter()
            this.writer = stack.pushWriter(writer);
        }

        return writer;
    }

    //---------- overwritten HttpServletResponseWrapper ------------------------

    /**
     * sets a header.
     */
    public void setHeader(String name, String value) {
	log.debug("DeliveryHttpServletResponseImpl.setHeader({0},{1})", name, value);

        if (name.equalsIgnoreCase("Content-Type")) {
            setContentType(value);
        } else if (name.equalsIgnoreCase("Cache-Control")) {
            setCacheControl(value);
        } else {

            // check whether we set a cookie through the header
            if (name.equalsIgnoreCase("Set-Cookie") ||
                    name.equalsIgnoreCase("Set-Cookie2")) {
                log.warn("setHeader: Set-Cookie[2] found, use addCookie()" +
                        " method instead");
            }

            super.setHeader(name, value);
        }
    }

    /**
     * Sets the content type of the response being sent to the client. The
     * content type may include the type of character encoding used, for
     * example, <code>text/html; charset=ISO-8859-4</code>.
     * <p>
     * If obtaining a <code>PrintWriter</code>, this method should be called
     * first.
     *
     * If the response has already been committed, such a call should be
     * ignored by the underlying servlet engine, but since IBM Websphere AS
     * breaks the specification in this point, we check first.
     *
     * @param contentType A <code>String</code> specifying the MIME type of the
     *      content.
     *
     * @see #getOutputStream
     * @see #getWriter
     */
    public void setContentType(String contentType) {
	log.debug("DeliveryHttpServletResponseImpl.setContentType({0})", contentType);

	if (!isCommitted()) {
	    this.contentType = contentType;
	    super.setContentType(contentType);
	} else {
	    log.info("ignoring invocation on committed response");
	}
    }

    /**
     * Set the cache-control header field
     */
    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
        super.setHeader("Cache-Control", cacheControl);
    }

    /**
     * Adds the given cookie to the response and informs the cache handler
     * service of this cookie.
     * <p>
     * If the response has already been committed, the cookie is not sent to
     * the client. The cache handler service is informed regardless of whether
     * the response has already been committed or not.
     *
     * @param cookie The cookie to be added to the response.
     */
    public void addCookie(Cookie cookie) {
        // inform the cache handler service but be prepared for old
        // implementations not implementing the method
        try {
            cacheHandlerService.cookieAdded(cacheHandlerData, cookie);
        } catch (AbstractMethodError ame) {
            log.debug("addCookie: Outdated CacheHandlerService implementation used.");
        }
        super.addCookie(cookie);
    }

    //---------- Buffer handling -----------------------------------------------

    /**
     * Returns whether any data (specifically the response headers) has been
     * sent to the client (<code>true</code>) or not (<code>false</code>).
     * <p>
     * This is not currently overwritten, because there is no added value in
     * here.
     *
     * @return <code>true</code> if the response is committed and headers have
     *      been sent to the client.
     */
//    public boolean isCommitted() {
//        return super.isCommitted();
//    }

    /**
     * Flushes any data optionally buffered to the client. If the response has
     * not been committed before the call to this method, the response headers
     * are sent first and the response is committed after the call.
     *
     * @throws IOException If writing the data (and optionally the headers)
     *      results in problems.
     */
    public void flushBuffer() throws IOException {

        // flush wrappers first
        if (out != null) {
            out.flush();
        } else if (writer != null) {
            writer.flush();
        }

        // call the original flush method
        super.flushBuffer();
    }

    /**
     * Returns the size of the buffer in bytes or 0 (zeor) if buffering is not
     * used or supported.
     *
     * @return The size of the output buffer in bytes.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the size of the output buffer in output stream dependant units: If
     * the output is a <code>ServletOutputStream</code> the unit is <em>bytes</em>
     * while for the <code>PrintWriter</code> the unit is <em>characters</em>.
     * <p>
     * Note the buffer need not have the exact size requested, rather the size
     * argument specifies a minimal buffer size. The {@link #getBufferSize}
     * method may be used to retrieve the exact size of the buffer used.
     *
     * @param size The minimal requested buffer size in bytes or characters.
     *
     * @throws IllegalStateException  If any data has already been written
     *      using the <code>OutputStream</code> or the <code>Writer</code>.
     */
    public void setBufferSize(int size) {
        // set the internal buffer size
        // throws if not allowed
        super.setBufferSize(size);

        // adapt our buffer(s) to the real buffer size
        bufferSize = super.getBufferSize();

        // adapt the buffersize of our wrapped writer/output stream
        // might throw if not allowed
        try {
            OutputStack stack = OutputStack.getOutputStack(request, getResponse());
            stack.setBufferSize(bufferSize);
        } catch (IOException ioe) {
            log.warn("setBufferSize: Cannot get OutputStack to set " +
                    "buffer size: {0}", ioe.getMessage());
        }
    }

    /**
     * Resets the response if not yet committed. Resetting the response means
     * clearing the status code, resetting all headers already set and clearing
     * the output buffer if output buffering is used.
     *
     * @throws IllegalStateException if the response has already been committed
     *      prior to calling this method.
     */
    public void reset() {
        super.reset();

        // reset our output buffers
        resetOutputBuffer();
    }

    /**
     * Resets the output buffer of the response if the response has not yet been
     * committed.
     * <p>
     * This method has been introduced in the Servlet Specification Version 2.3
     * and is not supported yet in Version 2.2. As this class is 2.2 compliant
     * this method's implementation simply logs an error message and does
     * nothing at all. The overwriting class implementing 2.3 additions will
     * do the actual expected work in a Servlet 2.3 environment.
     *
     * @throws IllegalStateException if the response has already been committed
     *      prior to calling this method.
     */
    public void resetBuffer() {
        // check whether committed
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }

        // reset internal output buffer
        resetOutputBuffer();
    }

    /**
     * Resets the output buffer of the response if the response has not yet been
     * committed.
     *
     * @deprecated As of echidna, use {@link #resetBuffer} instead which
     *      correctly handles the differences between the Servlet API 2.2 and
     *      2.3 versions.
     */
    public void resetResponseBuffer() {
        Log.deprecated("resetResponseBuffer", "resetBuffer");
	resetBuffer();
    }

    //---------- error handling ------------------------------------------------

    /**
     * Sends an error response to the client using the specified status code
     * and descriptive message.  The server generally creates the response to
     * look like a normal server error page.
     * <p>
     * If the response has already been committed, this method throws an
     * <code>IllegalStateException</code>. After using this method, the
     * response should be considered to be committed and should not be written
     * to.
     * <p>
     * If an <code>OutputStream</code> has already been obtained from this
     * response object, the error handler is not called and only the response
     * status is set.
     *
     * @param sc The error status code.
     * @param msg The descriptive message.
     *
     * @throws IOException If an input or output exception occurs.
     * @throws IllegalStateException If the response was committed before this
     *      method call.
     */
    public void sendError(int sc, String msg) throws IOException {
	log.debug("DeliveryHttpServletResponseImpl.sendError({0},{1})",
		String.valueOf(sc), msg);

        // do call the internal error sender
        sendErrorInternal(sc, msg, null);

        // Call the base class implementation if the error has not been sent
        // (successfully) and if the response has not yet been committed.
        if (!errorSent && !isCommitted()) {
	    super.sendError(sc, msg);
        }

        // prevent further data sending
        closeOutput();
    }

    /**
     * Sends an error response to the client using the specified status.
     * The server generally creates the response to look like a normal server
     * error page.
     * <p>
     * If the response has already been committed, this method throws an
     * <code>IllegalStateException</code>. After using this method, the
     * response should be considered to be committed and should not be written
     * to.
     *
     * @param sc The error status code.
     *
     * @throws IOException If an input or output exception occurs.
     * @throws IllegalStateException If the response was committed.
     */
    public void sendError(int sc) throws IOException {
	log.debug("DeliveryHttpServletResponseImpl.sendError({0})", String.valueOf(sc));

        // do call the internal error sender
        sendErrorInternal(sc, null, null);

        // Call the base class implementation if the error has not been sent
        // (successfully) and if the response has not yet been committed.
        if (!errorSent && !isCommitted()) {
	    super.sendError(sc);
        }

        // prevent further data sending
        closeOutput();
    }

    /**
     * Sends an error response based on the thrown <code>Throwable</code>.
     * <p>
     * An <code>ERROR</code> log message is written with the Exception
     * message, however the full stacktrace is only written if logging
     * is set to <code>DEBUG</code>.
     * <p>
     * If the response has already been committed, no error is sent to
     * the client and this method returns <code>false</code>.
     *
     * @param t The <code>Throwable</code> thrown during request processing.
     *
     * @return <code>true</code> if exception handling has been done by this
     *      method and no further actions are needed otherwise <code>false</code>
     *      due to the response being committed.
     */
    boolean sendError(Throwable t) {
        log.error("Unhandled Exception: {0}", t.toString(), t);

        // do call the internal error sender
        try {
            sendErrorInternal(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, t);
        } catch (IllegalStateException ise) {
            log.debug("sendError: Response has already been commited");
            return false;
        }

        // success (error has been sent) or fake success (no handling possible)
        return errorSent || isCommitted();
    }

    /**
     * Internal handling of a {@link #sendError(int)},
     * {@link #sendError(int, String)}, {@link #sendError(Throwable)}
     * invocation. Calls error script if still possible. Upon return, the field
     * {@link #errorSent} will be set to <code>true</code> if the
     * base implementation should still be called, <code>false</code>
     * otherwise.
     * <p>
     * This method calls any of the {@link ScriptHandlerService}'s
     * <code>sendError</code> methods depending on the parameters. If the
     * <code>Throwable</code> is not <code>null</code> exception handling is
     * called else if the status code is not negative error handling is
     * called. If the <code>Throwable</code> is <code>null</code> and the
     * status code is negative no handler is called at all.
     * <p>
     * For a NOT MODIFIED (304) status this method does nothing but setting the
     * {@link #errorSent} flag.
     *
     * @param sc The status code to send to the client. This is ignored if
     *      the method is called for <code>Throwable</code> processing, in which
     *      case the throwable parameter must not be <code>null</code>, or if
     *      the code is negative.
     * @param msg The status message to send, may be <code>null</code>
     * @param t The <code>Throwable</code> thrown during request processing.
     *      This may be <code>null</code> for the normal status code error
     *      handling.
     *
     * @throws IllegalStateException If the response has already been committed.
     */
    private void sendErrorInternal(int sc, String msg, Throwable t) {

        // if the error has already been sent, ignore another call
        if (errorSent) {
            log.debug("sendErrorInternal: Already sent, not doing again");
            return;
        }

        // set the status on the response first
        // must be done according to standard, might be duplicated in the script
        if (sc >= 100 && sc < 1000) {
            // status codes are three place positive numbers
            setStatus(sc);
        }

        /**
         * mark the error handling running - in case of an NOT MODIFIED (304)
         * status, this will be the real status, because 304 does not result in
         * an error script being called - this is normal status for non modified
         * pages.
         */
        errorSent = true;

        // do nothing for a NOT MODIFIED (304) status
        if (sc == HttpServletResponse.SC_NOT_MODIFIED) {
            log.debug("sendErrorInternal: Don't really handle NOT MODIFIED status");
            return;
        }

        // check committed state
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }

        // #5958 - reset buffer to not mess with what's out
        //    but ignore if this is not possible anymore
        //  according to Servlet 2.3, SRV.5.3 this belongs here
        try {
            resetBuffer();
        } catch (Exception ignore) {}

        // prepare for possible OutputStream wrapping and Ticket replacement
        StringWriter sw = null; // the temporary writer for the script
        ServletOutputStream servletOut = null; // the original stream
        Ticket oldRequestTicket = null; // the request ticket
        boolean replacedRequestTicket = false; // true if request ticket is set

        try {

            // make sure the script can write output even if
            // an OutputStream has already been obtained
            if (out != null) {
                // move OutputStream aside
                servletOut = out;
                out = null;

                // prepare a fake writer for the script
                sw = new StringWriter();
                writer = new PrintWriter(sw);
            }

            // temporarily replace original request ticket
            // we need a flag indicating we set the ticket otherwise the
            // ticket would not be set back if it was null originally
            // see bug #9154 for details.
            oldRequestTicket = request.getTicket();
            replacedRequestTicket = true;
            ((DeliveryHttpServletRequestImpl) request).setTicket(systemTicket);

            // handle the Throwable or status code
            if (t != null) {
                errorSent = scriptHandlerService.sendError(t, servlet,
                        request, this);
            } else if (sc >= 0) {
                errorSent = scriptHandlerService.sendError(sc, msg, servlet,
                        request, this);
            } else {
                // neither Throwable nor status code, nothing done
                errorSent = false;
            }

            // if handling succeeded, forward temporary writer to stream if needed
            if (errorSent) {

                // pump script output to stream, if needed
                if (sw != null) {

                    try {

                        byte[] data =
                                sw.toString().getBytes(getContentCharSet());
                        servletOut.write(data);

                    } catch (UnsupportedEncodingException uee) {

                        log.error("sendError: Cannot convert script output" +
                                " to characterset ''{0}'': {1}",
                                getContentCharSet(), uee.getMessage());

                        // we had the script output, but output cannot be sent
                        // let the delegatee try again...
                        errorSent = false;

                    } catch (IOException ioe) {

                        log.error("sendError: Cannot send script response" +
                                " to client: {0}", ioe.toString());

                        // reset partially sent data - if possible
                        if (!isCommitted()) {
                            resetBuffer();
                        }

                        // we had the script output, but output cannot be sent
                        // let the delegatee try again...
                        errorSent = false;
                    }
                }
            }

        } finally {
            // reset original request ticket
            if (replacedRequestTicket) {
                ((DeliveryHttpServletRequestImpl) request)
                        .setTicket(oldRequestTicket);
            }

            // we had to hack the output, reset the stream now
            if (sw != null) {
                writer.close();
                writer = null;
                out = servletOut;
            }
        }
    }

    /**
     * Sends a temporary redirect response to the client using the
     * specified redirect location URL.  This method can accept relative URLs;
     * the servlet container will convert the relative URL to an absolute URL
     * before sending the response to the client.
     * <p>
     * If the response has already been committed, this method throws an
     * <code>IllegalStateException</code>. After using this method, the
     * response should be considered to be committed and should not be written
     * to.
     *
     * @param location The redirect location URL.
     *
     * @throws IOException If an input or output exception occurs.
     * @throws IllegalStateException If the response was committed.
     */
    public void sendRedirect(String location) throws IOException {
	log.debug("sendRedirect({0})", location);

	this.status = HttpServletResponse.SC_MOVED_TEMPORARILY;
	super.sendRedirect(location);
    }

    /**
     * Sets the status code and message for this response.
     *
     * @param sc The status code.
     * @param sm The status message.
     *
     * @deprecated As of version 2.1, due to ambiguous meaning of the
     * message parameter. To set a status code
     * use <code>setStatus(int)</code>, to send an error with a description
     * use <code>sendError(int, String)</code>.
     */
    public void setStatus(int sc, String sm) {
	log.debug("setStatus({0},{1})", String.valueOf(sc), sm);

	this.status = sc;
	super.setStatus(sc, sm);
    }

    /**
     * Sets the status code for this response.  This method is used to
     * set the return status code when there is no error (for example,
     * for the status codes SC_OK or SC_MOVED_TEMPORARILY).  If there
     * is an error, the <code>sendError</code> method should be used
     * instead.
     *
     * @param sc The status code.
     *
     * @see #sendError
     */
    public void setStatus(int sc) {
	log.debug("setStatus({0})", String.valueOf(sc));

	this.status = sc;
	super.setStatus(sc);
    }

    //---------- misc ----------------------------------------------------------

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
    public void registerDependency(String handle) {
        // we only forward to the cache handler service of course, but we have
        // the handler data in hands, which is needed to do the registry
        cacheHandlerService.registerDependency(cacheHandlerData, handle);

        // also register the dependency on the parentResponse
        if (parentResponse != null) {
            parentResponse.registerDependency(handle);
        }
    }

    /**
     * Tells this response to not cache the current request.
     * That is, even the CacheHandler initially thought is could cache the current
     * request, a template script can indicate with this method that the cache
     * entry should not be cached.
     *
     * @since echidna
     */
    public void stopCaching() {
        // delegate the request to the cache handler service
        cacheHandlerService.stopCaching(cacheHandlerData);

        // also propagate the stop caching to the parentResponse
        if (parentResponse != null) {
            parentResponse.stopCaching();
        }
    }

    /**
     * Specifies how long the response should be considered valid. This method
     * may be used to specify a maximum age of a cached response different from
     * the default setting applied by the cache handler. This method has no
     * effect, if the response is not cached at all.
     * 
     * @param maxAge The maximum time in milliseconds that the response to the
     *      current should be considered valid and may therefore be delivered
     *      from cache.
     * 
     * @since iguana
     */
    public void setCacheMaxAge(long maxAge) {
        // delegate the request to the cache handler service
        cacheHandlerService.setMaxAge(cacheHandlerData, maxAge);
        
        // also propagate the max age to the parentResponse
        if (parentResponse != null) {
            parentResponse.setCacheMaxAge(maxAge);
        }
    }
    
    //---------- wrapper special methods ---------------------------------------

    public int getStatus() {
	return status == 0 ? SC_OK : status;
    }

    public String getContentType() {
	return contentType == null ? "text/html" : contentType;
    }

    public String getCacheControl() {
	return cacheControl;
    }

    public String mapHandle(String handle) {
	return urlMapperService.handleToURL(handle);
    }

    public void registerLinkChecker(LinkChecker linkChecker) {
	linkCheckerService.registerLinkChecker(linkChecker, linkCheckerData);
    }

    /**
     * Unegisters a linkchecker which is only used during link checking. This
     * method may be called any time during request processing. The new
     * link checker is immediately removed from the list of linkcheckers.
     * <p>
     * This method should only remove per-request link checkers but not any
     * system wide link checkers.
     *
     * @param linkChecker The {@link LinkChecker} to be removed from the
     *      current list of linkcheckers.
     *
     * @since fennec
     */
    public void unregisterLinkChecker(LinkChecker linkChecker) {
        linkCheckerService.unregisterLinkChecker(linkChecker, linkCheckerData);
    }

    /**
     * Return a flag indicating whether the content type is text/html
     */
    boolean isTextHtml() {
	if (contentType == null) {
	    return true;
	}
	int idx = contentType.indexOf(';');
	if (idx != -1) {
	    contentType = contentType.substring(0, idx);
	}
	contentType = contentType.trim();
	return contentType.equalsIgnoreCase("text/html");
    }

    /**
     * Returns the value <em>charset</em> parameter of the <em>Content-Type</em>
     * header. If the header has not been set yet or if the header does not
     * contain a character set specification, the default value as per the
     * HTTP standard (<em>ISO-8859-1</em>) is returned.
     *
     * @return The character set specification of the <em>Content-Type</em>
     *      header or <em>ISO-8859-1</em> if not specified.
     */
    String getContentCharSet() {
        if (contentCharSet == null) {

            // if contentType has not been set yet, return but dont store default
            if (contentType == null) {
                return DEFAULT_CONTENT_CHARSET;
            }

            int charsetIdx = contentType.indexOf("charset=");
            if (charsetIdx != -1) {
                charsetIdx += "charset=".length();

                // end delimiter, might be followe by more parameters
                int endCharSet = contentType.indexOf(';', charsetIdx);
                if (endCharSet < 0) {
                    endCharSet = contentType.length();
                }

                // get the charset substring and remove leading/trailing space
                contentCharSet = contentType.substring(charsetIdx, endCharSet);
                contentCharSet = contentCharSet.trim();
            } else {
                // contentType set, but no charset specified
                contentCharSet = DEFAULT_CONTENT_CHARSET;
            }
        }

        return contentCharSet;
    }

    //---------- Finalizer interface -------------------------------------------

    /**
     * This is the method called by the {@link com.day.hermes.util.FinalizerHandler}
     * when the finalizing work has to be done.
     */
    public void doFinalize() {

        // close and reset output
        closeOutput();

        // finalize the handler data objects
        linkCheckerData.doFinalize();
        cacheHandlerData.doFinalize();

        // restore the request attribute for current delivery response object
        request.setAttribute(CURRENT_RESPONSE, parentResponse);

        // reset fields - help gc
        cacheControl = null;
        cacheHandlerData = null;
        contentType = null;
        contentCharSet = null;
        linkCheckerData = null;
        request = null;
        servlet = null;
    }

    //---------- internal ------------------------------------------------------

    /**
     * Closes the output channel and marks the response as closed to prevent
     * re-opening of the channels
     */
    private void closeOutput() {

        // if already closed, ignore this call
        if (outputClosed) {
            log.debug("closeOutput: Output channel has already been closed");
            return;
        }

        // close output
        try {
            // only try to pop if ever pushed !!
            if (out != null || writer != null) {
                OutputStack stack = OutputStack.getOutputStack(request, getResponse());
                stack.popOutput();
            }
        } catch (IOException ioe) {
            log.info("closeOutput: Problem closing output channel: {0}",
                    ioe.toString());
        } finally {
            outputClosed = true;
            out = null;
            writer = null;
        }
    }

    /**
     * Resets the buffer of the OutputStream or Writer.
     */
    protected void resetOutputBuffer() {
        // reset internal buffer
        // set output buffer
        try {
            OutputStack stack = OutputStack.getOutputStack(request, getResponse());
            stack.resetBuffer();
        } catch (IOException ioe) {
            log.warn("setBufferSize: Cannot get OutputStack to set " +
                    "buffer size: {0}", ioe.getMessage());
        }
    }
}

/**
 * Extension class that will propagate calls to the Servlet 2.3 specific API
 * to the underlying delegatee instead of simply blocking it.
 */
class DeliveryHttpServletResponseImpl23 extends DeliveryHttpServletResponseImpl {

    /**
     * Create a new <code>DeliveryHttpServletResponseImpl23</code>
     */
    protected DeliveryHttpServletResponseImpl23(HttpServletResponse response) {
	super(response);
    }

    //---------- Servlet API 2.3 Buffer handling -------------------------------

    /**
     * Resets the output buffer of the response if the response has not yet been
     * committed.
     *
     * @throws IllegalStateException if the response has already been committed
     *      prior to calling this method.
     */
    public void resetBuffer() {
        // reset wrapped response buffer
        getResponse().resetBuffer();

        // do the original resetBuffer work
        super.resetBuffer();
    }
}

/**
 * The <code>DeliveryHttpServletResponseIncluded</code> class extends the
 * base class to overwrite and ignore all header manipulating methods.
 * <p>
 * According to the Servlet API specification, servlets (and JSPs) included
 * through the <code>RequestDispatcher.include()</code> method are limited
 * in the use of the response object :
 * <blockquote>
 * It can only write information to the <code>ServletOutputStream</code> or
 * <code>Writer</code> of the response object and commit a response by
 * writing content past the end of the response buffer, or by explicitly
 * calling the <code>flushBuffer</code> method of the
 * <code>ServletResponse</code> interface. It cannot set headers or call
 * any method that affects the headers of the response. <em>Any attempt to
 * do so must be ignored.</em>
 * </blockquote>
 * <p>
 * Because not all servlet container enforce ignorance of header setting,
 * this wrapper explicitly prevents such actions. It should be noted however
 * that the linkchecker might not work reliably in included situations, that
 * is the linkchecker might do more work than might be needed according to
 * content type of the response.
 *
 * @version $Revision: 1.34 $
 * @author fmeschbe
 * @since coati
 * @audience core
 */
class DeliveryHttpServletResponseIncluded extends DeliveryHttpServletResponseImpl {

    /** default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(DeliveryHttpServletResponseIncluded.class);

    /**
     * Creates a new <code>DeliveryHttpServletResponseIncluded</code>
     * wrapping the given <code>HttpServletRequest</code>.
     *
     * @param response The <code>HttpServletResponse</code> to wrap.
     */
    protected DeliveryHttpServletResponseIncluded(HttpServletResponse response) {
        super(response);
    }

    /**
     * Ignores requests to add cookies to the response.
     */
    public void addCookie(Cookie cookie) {
	log.debug("addCoookie: Ignored for included requests");
    }

    /**
     * Ignores requests to add date headers to the response.
     */
    public void addDateHeader(String s, long l) {
	log.debug("addDateHeader: Ignored for included requests");
    }

    /**
     * Ignores requests to add string headers to the response.
     */
    public void addHeader(String s, String s1) {
	log.debug("addHeader: Ignored for included requests");
    }

    /**
     * Ignores requests to add numeric headers to the response.
     */
    public void addIntHeader(String s, int i) {
	log.debug("addIntHeader: Ignored for included requests");
    }

    /**
     * Ignores requests to set the cache-control header in the response.
     */
    public void setCacheControl(String cacheControl) {
	// sets internal cache control to hint the cache handler
	this.cacheControl = cacheControl;

	log.debug("setCacheControl: Ignored for included requests");
    }

    /**
     * Ignores requests to set the Content-Length header in the response.
     */
    public void setContentLength(int i) {
	log.debug("setContentLength: Ignored for included requests");
    }

    /**
     * Ignores requests to set the Content-Type header in the response.
     */
    public void setContentType(String contentType) {
	// sets internal content type to hint the link checker
	this.contentType = contentType;

	log.debug("setContentType: Ignored for included requests");
    }

    /**
     * Ignores requests to set the date header in the response.
     */
    public void setDateHeader(String s, long l) {
	log.debug("setDateHeader: Ignored for included requests");
    }

    /**
     * Ignores requests to set the string headers in the response.
     */
    public void setHeader(String name, String value) {
	if (name.equalsIgnoreCase("Content-Type")) {
	    setContentType(value);
	} else if (name.equalsIgnoreCase("Cache-Control")) {
	    setCacheControl(value);
	} else {
	    log.debug("setHeader: Ignored for included requests");
	}
    }

    /**
     * Ignores requests to set the numeric header in the response.
     */
    public void setIntHeader(String s, int i) {
	log.debug("setIntHeader: Ignored for included requests");
    }
}

/**
 * Extension class that will propagate calls to the Servlet 2.3 specific API
 * to the underlying delegatee instead of simply blocking it.
 */
class DeliveryHttpServletResponseIncluded23 extends DeliveryHttpServletResponseIncluded {

    /**
     * Create a new <code>DeliveryHttpServletResponseImpl23</code>
     */
    protected DeliveryHttpServletResponseIncluded23(HttpServletResponse response) {
	super(response);
    }

    /**
     * @see HttpServletResponse#resetBuffer
     */
    public void resetBuffer() {
    	getResponse().resetBuffer();
        // reset internal output buffer
        resetOutputBuffer();
    }
}
