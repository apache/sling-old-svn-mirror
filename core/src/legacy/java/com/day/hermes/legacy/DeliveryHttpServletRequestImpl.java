/**
 * $Id: DeliveryHttpServletRequestImpl.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.contentbus.ContentBusException;
import com.day.hermes.contentbus.ContentElement;
import com.day.hermes.contentbus.Page;
import com.day.hermes.contentbus.Ticket;
import com.day.hermes.logging.FmtLogger;
import com.day.hermes.logging.Log;
import com.day.hermes.util.Finalizer;
import com.day.hermes.util.FinalizerHandler;
import com.day.hermes.util.HttpMultipartPost;
import com.day.hermes.util.TempFileFinalizer;
import com.day.util.IteratorEnumeration;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;

/**
 * The <code>DeliveryHttpServletRequestImpl</code> extends the
 * <code>HttpServletRequestWrapper</code> abstract class to implement additional
 * tasks required by the {@link DeliveryModule}.
 * <p>
 * Instances of this class should be considered valid only for the duration of
 * the request for which the were prepared. As a request is expected to be
 * handled by one thread and only one thread, no provisions have been made to
 * make this class thread safe.
 * <p>
 * Just like the abstract base class this class is intended to be re-used to
 * optimize system performance.
 *
 * @version $Revision: 1.21 $
 * @author fmeschbe
 * @since coati
 * @audience core
 */
class DeliveryHttpServletRequestImpl extends DeliveryHttpServletRequestWrapper
	implements DeliveryHttpServletRequest, Finalizer {

    //--------- static fields --------------------------------------------------

    /** Default logging */
    private static FmtLogger log =
            (FmtLogger) FmtLogger.getLogger(DeliveryHttpServletRequestImpl.class);

    /** The POST method string */
    private static final String METHOD_POST = "POST";

    /** The name of the request attribute storing the current DeliveryHttpServletRequest object */
    private static final String CURRENT_REQUEST = "com.day.hermes.current_request";

    /** The CmsService for this request */
    private static CmsService cmsService;

    //--------- fields ---------------------------------------------------------

    /**
     * The finalizer handler finalizing registered objects. The finalizer
     * handler may be re-used without being newly allocated because during
     * finalizing, it clears its internal registry, which may be refilled
     * afterwards.
     *
     * @see FinalizerHandler
     */
    private final FinalizerHandler finalizerHandler;

    /** The ticket for this request */
    private Ticket ticket;

    /** The page for this request */
    private Page page;

    /** The content element addressed by the selectors for this request */
    private ContentElement contentElement;

    /** <code>true</code> if the servlet is <code>RequestDispatcher.include()</code>-ed */
    private boolean included;

    /**
     * The prepared request URI. This URI is either the URI from the HTTP
     * request line or the request URI from the
     * <code>javax.servlet.include.request_uri</code> request attribute with the
     * context path removed.
     */
    private String realRequestURI;

    /** Caches the real context path returned by {@link #getRealContextPath()} */
    private String realContextPath;

    /** Caches the real servlet path returned by {@link #getRealServletPath()} */
    private String realServletPath;

    /** Caches the real path info returned by {@link #getRealPathInfo()} */
    private String realPathInfo;

    /** Caches the real query string returned by {@link #getRealQueryString()} */
    private String realQueryString;

    /** Caches the real method name returned by {@link #getRealMethod()} */
    private String realMethod;

    /** The URL mapped to handles, selectors, etc. - by the URLMapper */
    private MappedURL mappedURL;

    /** The paramters from the POST request */
    private HttpMultipartPost post;

    /** Flag indicating, if the request was checked for a multipart post */
    private boolean postChecked;

    /** The parent request if any */
    private DeliveryHttpServletRequestImpl parentRequest;

    //---------- construction and setup ----------------------------------------

    /**
     * Sets up the class to know about the needed services. These services only
     * exist once in the system and are used to handle the response side of the
     * request.
     *
     * @param cmsService The <code>CmsService</code> instance providing CMS
     *      system support.
     */
    static void setup(CmsService cmsService) {
        com.day.hermes.legacy.cmsService = cmsService;
    }

    /**
     * Creates a new <code>DeliveryHttpServletRequestImpl</code> wrapping the given
     * <code>HttpServletRequest</code>.
     *
     * @param delegatee The <code>HttpServletRequest</code> object to wrap
     */
    protected DeliveryHttpServletRequestImpl(HttpServletRequest delegatee) {
	super(delegatee);

        // The finalizer handler is never replaced
        finalizerHandler = new FinalizerHandler();

        // some more preparation
        this.included = delegatee.getAttribute(INCLUDE_REQUEST_URI) != null;

        // get parent request
        try {
            parentRequest = (DeliveryHttpServletRequestImpl)delegatee.getAttribute(CURRENT_REQUEST);
            delegatee.setAttribute(CURRENT_REQUEST, this);
        } catch (ClassCastException e) {
            log.warn("<init>: request attribute {0} is not of type DeliveryHttpServletRequestImpl!", CURRENT_REQUEST);
        }

        // set the instance as the request info attribute - compatibility ;-)
        delegatee.setAttribute(ATTRIBUTE_NAME, this);

        // register ourselves for finalizing, too
        registerObject(this);
    }

    /**
     * Returns a <code>DeliveryHttpServletRequestImpl</code> wrapper for the given
     * <code>HttpServletRequest</code>. This instance may be newly created or
     * from the pool of recycled wrapper instances.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     *
     * @param delegatee The <code>HttpServletRequest</code> object to wrap
     *
     * @return A prepared <code>DeliveryHttpServletRequest</code> object.
     */
    static DeliveryHttpServletRequestImpl getInstance(
            ServletSpec servletSpec, HttpServletRequest delegatee) {

        return (servletSpec == ServletSpec.V2_2) ?
                new DeliveryHttpServletRequestImpl(delegatee) :
                new DeliveryHttpServletRequestImpl23(delegatee);
    }

    /**
     * Takes back an instance of the wrapper to put in the recyclable list for
     * reuse by the {@link #getInstance(ServletSpec, HttpServletRequest)} method.
     * <p>
     * By taking the instance back for re-use, the registered <code>Finalizer</code>
     * objects are called to finalize themselves.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     *
     * @param req The <code>DeliveryHttpServletRequestImpl</code> object
     */
    static void recycleInstance(DeliveryHttpServletRequestImpl req) {
        if (req != null) {
            // call the registered finalizers
            try {
                req.finalizerHandler.callFinalizers();
            } catch (RuntimeException re) {
                log.error("recycleInstance: Unexpected exception: {0}", 
                    re.getMessage(), re);
            }
        }
    }

    /**
     * Sets the {@link Ticket} representing the user on whose behalf this
     * request is handled.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     *
     * @param ticket The <code>Ticket</code> to use for request handling.
     */
    void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    /**
     * Sets the {@link MappedURL} for this request. This object contains the
     * decomposed URI string mapped to the ContentBus handle.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     *
     * @param mappedURL The <code>MappedURL</code> object to set for this
     *      request.
     */
    void setMappedURL(MappedURL mappedURL) {
        this.mappedURL = mappedURL;
    }

    /**
     * Loads the page for the request and if available according to the
     * selectors the atom.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     */
    void loadContent() throws ContentBusException {

        this.page = ticket.getPage(getHandle());
        this.contentElement = page.getNearestElement(getSelectorString());
    }

    /**
     * Handles the input from the request. First this is comprised of analyzing
     * the <em>multipart POST</em> body if available. Second the
     * {@link com.day.hermes.cms.ContentBusAction} for this request is created.
     * This nasty little thing does all the ContentBus processing initiated by
     * the CMS system and has certain side effects on the state of the system.
     * <p>
     * This method is for the eyes of the {@link DeliveryModule} only, therefor
     * the method is package private.
     */
    void handleInput() throws DeliveryException {
        // callback action
        cmsService.executeContentBusAction(this);
    }

    /**
     * Checks, if the request contains a multipart-post. if so, it parses the
     * post-body, and provides the information for the getParameter methods.
     *
     * if this method returns <code>true</code> the {@link #post} field is
     * not <code>null</code> and can be used to retrieve parameter inforation.
     *
     * @return <code>true</code> if this request contains parameter information
     *         in the {@link #post} field;
     *         <code>false</code> otherwise.
     */
    private boolean hasMultipartPost() {
        if (!postChecked) {
	    postChecked = true;

	    // bug #8783; post could already be read by the parent request
	    if (parentRequest!=null && parentRequest.hasMultipartPost()) {
		post = parentRequest.post;
	    } else if (METHOD_POST.equalsIgnoreCase(getMethod())) {
		try {
		    post = HttpMultipartPost.create(this, this);
		} catch (MessagingException e) {
		    log.error("Unable to decompose multipart-post: {0}", e.toString());
		} catch (IOException e) {
		    log.error("Unable to decompose multipart-post: {0}", e.toString());
		}
	    }
        }
        return post!=null;
    }

    //---------- overwrites ----------------------------------------------------

    /**
     * Returns the name of the user placing the request. This name is taken
     * from the <code>Ticket.getUserId</code> method if the ticket is set. Else
     * the method returns <code>null</code>.
     *
     * @return The user id of the request <code>Ticket</code> if set. If the
     *      <code>Ticket</code> is not set yet <code>null</code> is returned.
     */
    public String getRemoteUser() {
        return (ticket != null) ? ticket.getUserId() : null;
    }

    //---------- methods -------------------------------------------------------

    /**
     * Returns the ticket associated with the request. The ticket identifies the
     * use on whose behalf the request is being made.
     *
     * @return The ticket associated with the request.
     */
    public Ticket getTicket() {
        return ticket;
    }

    /**
     * Returns the user on whose behalf this request is being made. This is the
     * page object from which the request <code>Ticket</code> has been
     * generated.
     *
     * @return The page of the user on whose behalf this request is being made.
     *
     * @see #getTicket()
     */
    public Page getUser() {
        return ticket.getUserPage();
    }

    /**
     * Returns the id of the user on whose behalf this request is being made.
     * @return the id of the user on whose behalf this request is being made.
     *
     * @deprecated Use {@link #getTicket()}.{@link Ticket#getUserId() getUserId()}
     *      instead.
     */
    public String getUserId() {
        Log.deprecated("DeliveryHttpServletRequestImpl.getUserId()",
		"DeliveryHttpServletRequestImpl.getTicket().getUserId()");
        return ticket.getUserId();
    }

    /**
     * Returns the page that is target of this request. This is the page
     * addressed by the handle of the decomposed request URL.
     *
     * @return The page that is target of this request.
     *
     * @see MappedURL#getHandle()
     */
    public Page getPage() {
        return page;
    }

    /**
     * Returns the content element that is the target of this request. The
     * content element of the request is addressed by the selector string of
     * the request URL.
     *
     * @return the content element that is the target of this request.
     *
     * @see MappedURL#getSelectorString()
     */
    public ContentElement getAtom() {
        return contentElement;
    }

    /**
     * @see DeliveryHttpServletRequest#externalizeHref(String)
     */
    public String externalizeHref(String href) {
        if(isExternal(href)) {
            log.debug("Link {0} is external.",href);
            return href;
        } else {
	URLMapperService urlMapper = DeliveryModule.getURLMapperService();
	URLTranslationInfo info =
	    urlMapper.externalizeHref(ticket, getContextPath(), href, getHandle(), ".html");
	return info==null ? "" : info.getExternalHref();
        }
    }

    /**
     * @see DeliveryHttpServletRequest#externalizeHandle(String)
     */
    public String externalizeHandle(String handle) {
	URLMapperService urlMapper = DeliveryModule.getURLMapperService();
	return urlMapper.handleToURL(getContextPath(), handle, null);
    }

    /**
     * Return true if href starts with a protocol
     * @param href
     * @return true if href starts with a protocol
     */
    private boolean isExternal(String href) {
        if(href!=null) {
            int pos = href.indexOf(':');
            if(pos > -1 && pos < href.indexOf('/')) {
                return true;
            }
        }
        return false;
    }

    //---------- from URI String ----------------------------------------

    /**
     * Returns the URL path associated with this request. Returns either the
     * {@link MappedURL#getOriginalURL()}, or the
     * <code>javax.servlet.include.request_uri</code> request attribute the
     * result from <code>ServletRequest.getRequestURI()</code> method.
     *
     * @deprecated Use {@link #getRealRequestURI()} instead.
     */
    public String getURLPath() {
        Log.deprecated("DeliveryHttpServletRequestImpl.getURLPath()",
		"DeliveryHttpServletRequestImpl.getRealRequestURI()");
        return getRealRequestURI();
    }

    /**
     * Returns the decomposed request URL or <code>null</code> if the request
     * URL has not yet been decomposed.
     *
     * @return The mapped URL.
     */
    public MappedURL getMappedURL() {
        return mappedURL;
    }

    /**
     * Returns the handle from the URL or an empty string if the URL has not yet
     * been decomposed. After decomposition, the return value will never be an
     * empty string.
     *
     * @return The ContentBus handle from the request URI or an empty
     *      string if the request URL has not yet been decomposed.
     *
     * @see MappedURL#getHandle()
     */
    public String getHandle() {
        return (mappedURL != null) ? mappedURL.getHandle() : "";
    }

    /**
     * Returns the extension from the URL or an empty string if the URL has not
     * yet been decomposed or if the request URL does not contain an extension.
     *
     * @return The extension from the request URL.
     *
     * @see MappedURL#getExtension()
     */
    public String getExtension() {
        return (mappedURL != null) ? mappedURL.getExtension() : "";
    }

    /**
     * Returns the suffix part of the URL or an empty string if the request URL
     * has not yet been decomposed or if the request URL does not contain an
     * suffix.
     *
     * @return The suffix part of the request URL.
     *
     * @see MappedURL#getSuffix()
     */
    public String getSuffix() {
        return (mappedURL != null) ? mappedURL.getSuffix() : "";
    }

    //---------- parameters differing in included servlets --------------

    /**
     * Returns <code>true</code> if the servlet is executed through
     * <code>RequestDispatcher.include()</code>.
     *
     * @return <code>true</code> if the servlet is executed through
     *      <code>RequestDispatcher.include()</code>.
     */
    public boolean isIncluded() {
        return included;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.request_uri</code>
     * attribute if {@link #isIncluded()} or <code>request.getRequestURI()</code>.
     * The context path has been removed from the beginning of the returned
     * string. That is for request, which is not {@link #isIncluded() included}:
     * <code>getRealRequestURI() == getRealContextPath() + getRequestURI()</code>.
     *
     * @return The relevant request URI according to environment with the
     *      context path removed.
     */
    public String getRealRequestURI() {
        if (realRequestURI == null) {

            // get the unmodified request URI and context information
            realRequestURI = included
                    ? (String) super.getAttribute(INCLUDE_REQUEST_URI)
                    : super.getRequestURI();

            String ctxPrefix = getRealContextPath();

            if (log.isDebugEnabled()) {
                log.debug("getRequestURI: Servlet request URI is {0}", realRequestURI);
            }

            // check to remove the context prefix
            if (ctxPrefix == null) {
                log.error("getRequestURI: Context path not expected to be null");
            } else if (ctxPrefix.length() == 0) {
                // default root context, no change
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: Default root context, no change to uri");
                }
            } else if (ctxPrefix.length() < realRequestURI.length() &&
                    realRequestURI.startsWith(ctxPrefix) &&
                    realRequestURI.charAt(ctxPrefix.length()) == '/') {
                // some path below context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: removing ''{0}'' from ''{1}''", ctxPrefix,
                            realRequestURI);
                }
                realRequestURI = realRequestURI.substring(ctxPrefix.length());
            } else if (ctxPrefix.equals(realRequestURI)) {
                // context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: URI equals context prefix, assuming ''/''");
                }
                realRequestURI = "/";
            }
        }

        return realRequestURI;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.context_path</code>
     * attribute if {@link #isIncluded()} or <code>request.getContextPath()</code>.
     *
     * @return The relevant context path according to environment.
     */
    public String getRealContextPath() {
        if (realContextPath == null) {
            realContextPath = included
                    ? (String) super.getAttribute(INCLUDE_CONTEXT_PATH)
                    : super.getContextPath();
        }

        return realContextPath;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.servlet_path</code>
     * attribute if {@link #isIncluded()} or <code>request.getServletPath()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the path to the servlet being executed from
     * the perspective of the servlet container. Thus this path is really the
     * path to the {@link DeliveryServlet}.
     *
     * @return The relevant servlet path according to environment.
     */
    public String getRealServletPath() {
        if (realServletPath == null) {
            realServletPath = included
                    ? (String) super.getAttribute(INCLUDE_SERVLET_PATH)
                    : super.getServletPath();
        }

        return realServletPath;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.path_info</code>
     * attribute if {@link #isIncluded()} or <code>request.getPathInfo()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the additional path info extending the
     * servlet path from the perspective of the servlet container. This is not
     * the same as the {@link #getSuffix() suffix}.
     * @return The relevant path info according to environment.
     */
    public String getRealPathInfo() {
        if (realPathInfo == null) {
            realPathInfo = included
                    ? (String) super.getAttribute(INCLUDE_PATH_INFO)
                    : super.getPathInfo();
        }

        return realPathInfo;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.query_string</code>
     * attribute if {@link #isIncluded()} or <code>request.getQueryString()</code>.
     *
     * @return The relevant query string according to environment.
     */
    public String getRealQueryString() {
        if (realQueryString == null) {
            realQueryString = included
                    ? (String) super.getAttribute(INCLUDE_QUERY_STRING)
                    : super.getQueryString();
        }

        return realQueryString;
    }

    //---------- query from the URI -------------------------------------

    /**
     * Returns the selector string contained in the request URL or an empty
     * string if the request URL has not yet been decomposed or no selectors
     * are contained in the URL.
     *
     * @return The selector string.
     *
     * @see MappedURL#getSelectorString()
     *
     * @deprecated use {@link #getSelectorString()} instead.
     */
    public String getCombinedQuery() {
        Log.deprecated("DeliveryHttpServletRequestImpl.getCombinedQuery()",
		"DeliveryHttpServletRequestImpl.getSelectorString()");
        return getSelectorString();
    }

    /**
     * Returns the selector string contained in the request URL or an empty
     * string if the request URL has not yet been decomposed or no selectors
     * are contained in the URL.
     *
     * @return The selector string.
     *
     * @see MappedURL#getSelectorString()
     */
    public String getSelectorString() {
        return (mappedURL != null) ? mappedURL.getSelectorString() : "";
    }

    /**
     * Returns the selectors from the request URL or an empty array if the
     * request URL has not yet been decomposed or if the request URL did not
     * contain a selector string.
     *
     * @return The selectors from the request URL as an array of
     *      <code>String</code>s.
     *
     * @see MappedURL#getSelectors()
     *
     * @deprecated use {@link #getSelectors()} instead.
     */
    public String[] getQuery() {
        Log.deprecated("DeliveryHttpServletRequestImpl.getQuery()",
		"DeliveryHttpServletRequestImpl.getSelectors()");
        return getSelectors();
    }

    /**
     * Returns the selectors from the request URL or an empty array if the
     * request URL has not yet been decomposed or if the request URL did not
     * contain a selector string.
     *
     * @return The selectors from the request URL as an array of
     *      <code>String</code>s.
     *
     * @see MappedURL#getSelectors()
     */
    public String[] getSelectors() {
        return (mappedURL != null) ? mappedURL.getSelectors() : new String[]{};
    }

    //---------- parameters ---------------------------------------------

    /**
     * Returns a list of all known parameter names. In contrast to the
     * <code>ServletRequest.getParameterNames()</code> method, this also lists
     * parameters from the request body.
     * <p>
     * According to the specification of this method in the
     * {@link RequestInfo#getParameterNames() RequestInfo} interface, this
     * method implements the new Communiqué 3 semantics and returns the names of
     * the parameters from the request line and from the optional <em>multipart
     * POST</em> body.
     * <p>
     * <strong>NOTE</strong>: The return value of this method as been changed
     * from earlier versions of this API. The change was induced by the adaption
     * of the method signature to the Servlet API specification, which defines
     * the return value of this method to be an <code>Enumeration</code>.
     *
     * @return An enumeration of all parameters for the request. Parameters from
     *      the request line and from the optional <em>POST</em> request body
     *      are returned.
     */
    public Enumeration getParameterNames() {
        if (hasMultipartPost()) {
            return new IteratorEnumeration(post.getParameterNames());
        } else {
            return super.getParameterNames();
        }
    }

    /**
     * Returns <code>true</code> if a parameter with the given name is part of
     * the request. Parameters from both the request line and the <em>POST</em>
     * body are recognized.
     *
     * @param name The name of the parameter.
     *
     * @return <code>true</code> if the parameter exists.
     */
    public boolean hasParameter(String name) {
        return getParameter(name) != null;
    }

    /**
     * Returns the first value of the named parameter. This method has the
     * same semantics as the <code>ServletRequest.getParameter(String)</code>
     * method, but also supports parameters contained in the request body.
     * <p>
     * According to the specification of this method in the
     * {@link RequestInfo#getParameter(String) RequestInfo} interface, this
     * method implements the new Communiqué 3 semantics and returns the names of
     * the parameters from the request line and from the optional <em>multipart
     * POST</em> body.
     *
     * @param name The name of the parameter.
     *
     * @return The first value of the named parameter.
     */
    public String getParameter(String name) {
        if (hasMultipartPost()) {
            return post.getParameter(name);
        } else {
            return super.getParameter(name);
        }
    }

    /**
     * Returns the first value of the named parameter. This method has the
     * same semantics as the <code>ServletRequest.getParameter(String)</code>
     * method, but also supports parameters contained in the request body.
     *
     * @param name The name of the parameter
     * @param encoding The encoding of the parameter value
     *
     * @return The first value of the named parameter.
     *
     * @throws UnsupportedEncodingException if the <code>encoding</code> is not
     * 		supported by the platform.
     */
    public String getParameter(String name, String encoding)
            throws UnsupportedEncodingException {

        if (hasMultipartPost()) {
            return post.getParameter(name, encoding);
        } else {
            return super.getParameter(name);
        }
    }

    /**
     * Returns all values of the named parameter. This method has the
     * same semantics as the <code>ServletRequest.getParameter(String)</code>
     * method, but also supports parameters contained in the request body.
     * <p>
     * According to the specification of this method in the
     * {@link RequestInfo#getParameterValues(String) RequestInfo} interface, this
     * method implements the new Communiqué 3 semantics and returns the names of
     * the parameters from the request line and from the optional <em>multipart
     * POST</em> body.
     *
     * @param name The name of the parameter
     *
     * @return An array of parameter value strings.
     */
    public String[] getParameterValues(String name) {
        if (hasMultipartPost()) {
            return post.getParameterValues(name);
        } else {
            return super.getParameterValues(name);
        }
    }

    /**
     * Returns all values of the named parameter decoded according to the given
     * <code>encoding</code>. This method has the same semantics as the
     * <code>ServletRequest.getParameter(String)</code> method, but also
     * supports parameters contained in the request body.
     *
     * @param name The name of the parameter
     * @param encoding The encoding of the parameter values
     *
     * @return An array of parameter value strings.
     *
     * @throws UnsupportedEncodingException if the <code>encoding</code> is not
     * 		supported by the platform.
     */
    public String[] getParameterValues(String name, String encoding)
            throws UnsupportedEncodingException {

        if (hasMultipartPost()) {
            return post.getParameterValues(name, encoding);
        } else {
            return super.getParameterValues(name);
        }
    }

    /**
     * Returns the first value of a parameter as an array of bytes. If the
     * parameter was from a multipart post, then the original byte sequence is
     * returned, if it comes from a query, the result of
     * <code>String.getBytes()</code> is returned. Specifically the platform
     * default encoding is used to decode the <code>String</code> into an array
     * of bytes.
     *
     * @param name The name of the parameter.
     *
     * @return An array of bytes containing the parameter first value.
     */
    public byte[] getParameterBytes(String name) {
        if (hasMultipartPost()) {
            return post.getParameterBytes(name);
        } else {
            String ret = super.getParameter(name);
            return ret == null?null:ret.getBytes();
        }
    }

    /**
     * Returns all values of a parameter as an array of byte arrays. If the
     * parameter was from a multipart post, then the original byte sequences are
     * returned, if it comes from a query, the result of
     * <code>String.getBytes()</code> is returned. Specifically the platform
     * default encoding is used to decode the <code>String</code> into an array
     * of bytes.
     *
     * @param name The name of the parameter.
     *
     * @return An array of byte arrays containing the parameter first value.
     */
    public byte[][] getParameterValuesBytes(String name) {
        if (hasMultipartPost()) {
            return post.getParameterValuesBytes(name);
        } else {
            String[] vals = super.getParameterValues(name);
            byte[][] ret = new byte[vals == null?0:vals.length][];
            for (int i = 0; i < vals.length; i++) {
                ret[i] = vals[i].getBytes();
            }
            return ret;
        }
    }

    /**
     * Returns the content type of the first paramater with the given name. If
     * the parameter does not exist or if the content type is not known this
     * method returns <code>null</code>.
     * <p>
     * The content type of a paramater is only known here if the information
     * has been sent by the client browser. This is generally only the case
     * for file upload fields of HTML forms which have been posted using the
     * HTTP <em>POST</em> with <em>multipart/form-data</em> encoding.
     * <p>
     * Example : For the form
     * <pre>
         <form name="Upload" method="POST" ENCTYPE="multipart/form-data">
            <input type="file" name="Upload"><br>
            <input type="text" name="Text"><br>
            <input type="submit">
         </form>
     * </pre>
     * the content type will only be knwon for the <em>Upload</em> paramater.
     *
     * @param name The name of the paramater whose content type is to be
     *      returned.
     *
     * @return The content type of the (first) named paramater if known else
     *      <code>null</code> is returned.
     */
    public String getParameterType(String name) {
        if (hasMultipartPost()) {
            return post.getParameterType(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the content types of the paramaters with the given name. If
     * the parameter does not exist an empty array is returned. If the content
     * type of any of the parameter values is not known, the corresponding entry
     * in the array returned is <code>null</code>.
     * <p>
     * The content type of a paramater is only known here if the information
     * has been sent by the client browser. This is generally only the case
     * for file upload fields of HTML forms which have been posted using the
     * HTTP <em>POST</em> with <em>multipart/form-data</em> encoding.
     * <p>
     * Example : For the form
     * <pre>
         <form name="Upload" method="POST" ENCTYPE="multipart/form-data">
            <input type="file" name="Upload"><br>
            <input type="text" name="Upload"><br>
            <input type="submit">
         </form>
     * </pre>
     * this method will return an array of two entries when called for the
     * <em>Upload</em> parameter. The first entry will contain the content
     * type (if transmitted by the client) of the file uploaded. The second
     * entry will be <code>null</code> because the content type of the text
     * input field will generally not be sent by the client.
     *
     * @param name The name of the paramater whose content type is to be
     *      returned.
     *
     * @return The content type of the (first) named paramater if known else
     *      <code>null</code> is returned.
     */
    public String[] getParameterTypes(String name) {
        if (hasMultipartPost()) {
            return post.getParameterTypes(name);
        } else {
            // no multipart post, fake response with null-valued array
            String[] vals = super.getParameterValues(name);
            return new String[(vals == null) ? 0 : vals.length];
        }
    }

    //---------- File oriented POST parameters ---------------------------------

    /**
     * Returns a list of all known parameter names. This only contains parameters
     * from a <em>multipart POST</em> request.
     * <p>
     * <strong>NOTE</strong>: The return value of this method as been changed
     * from earlier versions of this API. The change was made to adapt this
     * method's signature to the signature of the <code>getParameterNames()</code>
     * method in the Servlet API specification.
     *
     * @return An enumeration of all parameters for the request. If the request
     *      is not a <em>multipart POST</em> request, the enumeration is empty.
     */
    public Enumeration getFileParameterNames() {
        return hasMultipartPost() ?
                new IteratorEnumeration(post.getFileParameterNames()) : null;
    }

    /**
     * Returns <code>true</code> if the named parameter is from a <em>multipart
     * POST</em> request an can be accessed through a <code>File</code> object.
     *
     * @param name The name of the parameter.
     *
     * @return <code>true</code> if a file is associated with the parameter
     *      name specified.
     */
    public boolean isFileParameter(String name) {
        return hasMultipartPost() ? post.isFileParameter(name) : false;
    }

    /**
     * Returns the first parameter of the <em>multipart POST</em> request as a
     * <code>File</code> object.
     *
     * @param name The name of the parameter.
     *
     * @return The <code>File</code> object to access the parameter value or
     *      <code>null</code> if no <code>File</code> can be associated with the
     *      parameter.
     */
    public File getFileParameter(String name) {
        return hasMultipartPost() ? post.getFileParameter(name) : null;
    }

    /**
     * Returns the parameters of the <em>multipart POST</em> request as an array
     * of <code>File</code> objects.
     *
     * @param name The name of the parameter.
     *
     * @return An array of <code>File</code> objects to access the parameter
     *      values or <code>null</code> if no <code>File</code> objects can be
     *      associated with the parameter.
     */
    public File[] getFileParameterValues(String name) {
        return hasMultipartPost() ? post.getFileParameterValues(name) : null;
    }

    //---------- misc ---------------------------------------------------

    /**
     * Returns the method associated with this request. This is either the
     * value of the <i>cq-action</i> header or the value returned
     * by <code>HttpServletRequest.getMethod()</code>.
     *
     * @return method From the <code>CQ-Action</code> header or from the
     *      HTTP request line.
     */
    public String getRealMethod() {
        if (realMethod == null) {

            realMethod = getHeader(HEADER_CQ_ACTION);
            if (realMethod == null) {

                realMethod = getMethod();

            } else {

                realMethod = realMethod.toUpperCase();

            }
        }

        return realMethod;
    }

    /**
     * Returns the CMS service 'responsible' for this request
     */
    public CmsService getCmsService() {
        return cmsService;
    }

    //---------- old web dav support -------------------------------------------

    private boolean forceDebug = false;

    /**
     * Sets the flag to force debugging of the request. This of course is purely
     * ECMA specific and definitely does not belong here !
     *
     * @deprecated  This of course is purely ECMA specific and definitely does
     * 		not belong here !
     */
    void setForceDebug(boolean forceDebug) {
        this.forceDebug = forceDebug;
    }

    /**
     * Returns <code>true</code> if debugging of the request is forced regardless
     * of the request URI's extension.
     *
     * @return <code>true</code> if the ECMA debugger should be activated
     *      regardless of the request URI.
     *
     * @deprecated This method is very ECMA specific and was used to WebDAV
     *      scripting. This method must not be used anymore. Implementations are
     *      encouraged to always return <code>false</code>.
     */
    public boolean forceDebug() {
        return forceDebug;
    }

    //---------- ExecutionContext interface ------------------------------------

    /**
     * Add a generic temporary object that implements the {@link Finalizer}
     * interface. Its {@link Finalizer#doFinalize()} method will be called
     * when the <code>ExecutionContext</code> is destroyed.
     */
    public void registerObject(Finalizer object) {
        finalizerHandler.registerObject(object);
    }

    /**
     * Add a temporary file to this <code>ExecutionContext</code>. The
     * temporary file will be deleted when the context is destroyed.
     * @param file file to remember
     */
    public void addTempFile(File file) {
        finalizerHandler.registerObject(new TempFileFinalizer(file));
    }

    //---------- Finalizer interface -------------------------------------------

    /**
     * Resets the <code>DeliveryHttpServletRequestWrapper</code> to its initial
     * values and initializes it to wrap the new <code>HttpServletRequest</code>
     * with the given ticket. This is jsut like constructing a new wrapper
     * instance but without allocating new memory.
     */
    public void doFinalize() {

        // close an open ticket
        if (ticket != null) {
            try {
                log.debug("doFinalize: closing ticket");
                ticket.close();
            } catch (Exception e) {
                log.info("doFinalize: closing ticket failed: {0}", e.toString());
                log.debug("doFinalize: dump", e);
            }
        }

        // restore the request attribute for current delivery request object
        setAttribute(CURRENT_REQUEST, parentRequest);

        // remove ourselves from our attributes list
        removeAttribute(ATTRIBUTE_NAME);

        // reset fields - help GC
        ticket = null;
        page = null;
        contentElement = null;
        realRequestURI = null;
        realContextPath = null;
        realServletPath = null;
        realPathInfo = null;
        realQueryString = null;
        realMethod = null;
        mappedURL = null;
        post = null;
        postChecked = false;
    }
}

/**
 * Extension class that will propagate calls to the Servlet 2.3 specific API
 * to the underlying delegatee instead of simply blocking it.
 */
class DeliveryHttpServletRequestImpl23 extends DeliveryHttpServletRequestImpl {

    /**
     * Create a new <code>DeliveryHttpServletRequestImpl23</code>
     */
    protected DeliveryHttpServletRequestImpl23(HttpServletRequest request) {
	super(request);
    }

    /**
     * @see HttpServletRequest#getRequestURL
     */
    public StringBuffer getRequestURL() {
	return getRequest().getRequestURL();
    }

    /**
     * @see HttpServletRequest#setCharacterEncoding(String)
     */
    public void setCharacterEncoding(String s)
	    throws UnsupportedEncodingException {
	getRequest().setCharacterEncoding(s);
    }

    /**
     * @see HttpServletRequest#getParameterMap
     */
    public Map getParameterMap() {
	return getRequest().getParameterMap();
    }
}
