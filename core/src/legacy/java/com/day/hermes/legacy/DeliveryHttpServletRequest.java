/*
 * $Id: DeliveryHttpServletRequest.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

/**
 * The <code>DeliveryHttpServletRequest</code> extends both the
 * <code>HttpServletRequest</code> and the <code>RequestInfo</code> interfaces
 * to serve as the basis for the request processing and as the source of
 * request specific information in one place.
 * <p>
 * With the definition of the <code>DeliveyHttpServletRequest</code> the
 * {@link RequestInfo} interface becomes obsolete and simply remains for
 * backwards compatibility. Future enhancements should be added to this
 * interface.
 *
 * @version $Revision: 1.12 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since coati
 * @audience wad
 */
public interface DeliveryHttpServletRequest extends HttpServletRequest {

    /** Attribute name: Request URI */
    public static final String INCLUDE_REQUEST_URI =
        "javax.servlet.include.request_uri";

    /** Attribute name: Context Path */
    public static final String INCLUDE_CONTEXT_PATH =
        "javax.servlet.include.context_path";

    /** Attribute name: Servlet Path */
    public static final String INCLUDE_SERVLET_PATH =
        "javax.servlet.include.servlet_path";

    /** Attribute name: Path Info */
    public static final String INCLUDE_PATH_INFO =
        "javax.servlet.include.path_info";

    /** Attribute name: Query string */
    public static final String INCLUDE_QUERY_STRING =
        "javax.servlet.include.query_string";

    /** The name of the cq-action header */
    public static final String HEADER_CQ_ACTION = "cq-action";

    //---------- wrapper stuff -------------------------------------------------

    /**
     * Returns the original HttpServletRequest which is contained in this.
     * Implementations are allowed to return the instance itself instead of
     * a wrapped object. Returning <code>null</code> is not allowed.
     */
    public HttpServletRequest getRequest();

    //---------- ContentBus data ----------------------------------------

    /**
     * Returns the ticket associated with the request. The ticket identifies the
     * use on whose behalf the request is being made.
     *
     * @return The ticket associated with the request.
     */
    public Session getRepositorySession();

    /**
     * Returns the user on whose behalf this request is being made. This is the
     * page object from which the request <code>Ticket</code> has been
     * generated.
     *
     * @return The page of the user on whose behalf this request is being made.
     *
     * @see #getTicket()
     */
    public Subject getUser();

    /**
     * Returns the id of the user on whose behalf this request is being made.
     * @return the id of the user on whose behalf this request is being made.
     *
     * @deprecated Use {@link #getTicket()}.{@link Ticket#getUserId() getUserId()}
     *      instead.
     */
    public String getUserId();

    /**
     * Returns the page that is the target of this request. This is the page
     * addressed by the handle of the decomposed request URL.
     *
     * @return The page that is target of this request.
     *
     * @see MappedURL#getHandle()
     */
    public Node getPageNode();

    /**
     * Returns the content element that is the target of this request. The
     * content element of the request is addressed by the selector string of
     * the request URL.
     * <p>
     * The selector string is used for a best-effort match. That is the string
     * must not exactly address a content element. It suffices it if only part
     * of the selector string (from the start) matches an element. If no part
     * of the selector string matches any content element or if the selector
     * string is empty, the top level container of the request's page is
     * returned.
     *
     * @return the content element that is the target of this request or
     *      the top level container if the request does not address a content
     *      element on the page returned by {@link #getPage()}.
     *
     * @see MappedURL#getSelectorString()
     */
    public Item getItem();

    //---------- from URI String ----------------------------------------

    /**
     * Returns the URL path associated with this request. Returns either the
     * {@link MappedURL#getOriginalURL()}, or the
     * <code>javax.servlet.include.request_uri</code> request attribute the
     * result from <code>ServletRequest.getRequestURI()</code> method.
     *
     * @deprecated Use {@link #getRealRequestURI()} instead.
     */
    public String getURLPath();

    /**
     * Returns the decomposed request URL or <code>null</code> if the request
     * URL has not yet been decomposed.
     *
     * @return The mapped URL.
     */
    public MappedURL getMappedURL();

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
    public String getHandle();

    /**
     * Returns the extension from the URL or an empty string if the URL has not
     * yet been decomposed or if the request URL does not contain an extension.
     *
     * @return The extension from the request URL.
     *
     * @see MappedURL#getExtension()
     */
    public String getExtension();

    /**
     * Returns the suffix part of the URL or an empty string if the request URL
     * has not yet been decomposed or if the request URL does not contain an
     * suffix.
     *
     * @return The suffix part of the request URL.
     *
     * @see MappedURL#getSuffix()
     */
    public String getSuffix();

    //---------- parameters differing in included servlets --------------

    /**
     * Returns <code>true</code> if the servlet is executed through
     * <code>RequestDispatcher.include()</code>.
     *
     * @return <code>true</code> if the servlet is executed through
     *      <code>RequestDispatcher.include()</code>.
     */
    public boolean isIncluded();

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
    public String getRealRequestURI();

    /**
     * Returns the contents of the <code>javax.servlet.include.context_path</code>
     * attribute if {@link #isIncluded()} or <code>request.getContextPath()</code>.
     *
     * @return The relevant context path according to environment.
     */
    public String getRealContextPath();

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
    public String getRealServletPath();

    /**
     * Returns the contents of the <code>javax.servlet.include.path_info</code>
     * attribute if {@link #isIncluded()} or <code>request.getPathInfo()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the additional path info extending the
     * servlet path from the perspective of the servlet container. This is not
     * the same as the {@link #getSuffix() suffix}.
     * @return The relevant path info according to environment.
     */
    public String getRealPathInfo();

    /**
     * Returns the contents of the <code>javax.servlet.include.query_string</code>
     * attribute if {@link #isIncluded()} or <code>request.getQueryString()</code>.
     *
     * @return The relevant query string according to environment.
     */
    public String getRealQueryString();

    //---------- selectors from the URI ---------------------------------------

    /**
     * Returns the selector string contained in the request URL or an empty
     * string if the request URL has not yet been decomposed or no selectors
     * are contained in the URL.
     *
     * @return The selector string.
     *
     * @see MappedURL#getSelectorString()
     */
    public String getSelectorString();

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
    public String[] getSelectors();

    //---------- parameters ---------------------------------------------------

    /**
     * Returns a list of all known parameter names. In contrast to the
     * ServletRequest.getParameterNames() method, this also lists parameters
     * from the request body.
     * <p>
     * <strong>CONFLICT</strong>:  Implementations of both the
     * code>ServletRequest</code> and this interface should implement the
     * <code>RequestInfo</code> semantics.
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
    public Enumeration getParameterNames();

    /**
     * Returns <code>true</code> if a parameter with the given name is part of
     * the request. Parameters from both the request line and the <em>POST</em>
     * body are recognized.
     *
     * @param name The name of the parameter.
     *
     * @return <code>true</code> if the parameter exists.
     */
    public boolean hasParameter(String name);

    /**
     * Returns the first value of the named parameter. This method has the
     * same semantics as the <code>ServletRequest.getParameter(String)</code>
     * method, but also supports parameters contained in the request body.
     * <p>
     * <strong>CONFLICT</strong>: Implementations of both the
     * <code>ServletRequest</code> and this interface should implement the
     * <code>RequestInfo</code> semantics.
     *
     * @param name The name of the parameter.
     *
     * @return The first value of the named parameter.
     */
    public String getParameter(String name);

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
     *      supported by the platform.
     */
    public String getParameter(String name, String encoding)
        throws UnsupportedEncodingException;

    /**
     * Returns all values of the named parameter. This method has the
     * same semantics as the <code>ServletRequest.getParameter(String)</code>
     * method, but also supports parameters contained in the request body.
     * <p>
     * <strong>CONFLICT</strong>: Implementations of both the
     * <code>ServletRequest</code> and this interface should implement the
     * <code>RequestInfo</code> semantics.
     *
     * @param name The name of the parameter
     *
     * @return An array of parameter value strings.
     */
    public String[] getParameterValues(String name);

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
     *      supported by the platform.
     */
    public String[] getParameterValues(String name, String encoding)
        throws UnsupportedEncodingException;

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
    public byte[] getParameterBytes(String name);

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
    public byte[][] getParameterValuesBytes(String name);

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
    public String getParameterType(String name);

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
     * <p>
     * The return value of this method is a string array which has the same
     * length as the string array returned by the {@link #getParameterValues}
     * method.
     *
     * @param name The name of the paramater whose content type is to be
     *      returned.
     *
     * @return The content type of the (first) named paramater if known else
     *      <code>null</code> is returned.
     */
    public String[] getParameterTypes(String name);

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
    public Enumeration getFileParameterNames();

    /**
     * Returns <code>true</code> if the named parameter is from a <em>multipart
     * POST</em> request an can be accessed through a <code>File</code> object.
     *
     * @param name The name of the parameter.
     *
     * @return <code>true</code> if a file is associated with the parameter
     *      name specified.
     */
    public boolean isFileParameter(String name);

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
    public File getFileParameter(String name);

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
    public File[] getFileParameterValues(String name);

    //---------- misc ---------------------------------------------------

    /**
     * Returns the method associated with this request. This is either the
     * value of the <i>cq-action</i> header or the value returned
     * by <code>HttpServletRequest.getMethod()</code>.
     *
     * @return method From the <code>CQ-Action</code> header or from the
     *      HTTP request line.
     */
    public String getRealMethod();

    //---------- overwrites ----------------------------------------------------

    /**
     * Returns the name of the user placing the request. This name is taken
     * from the <code>Ticket.getUserId</code> method if the ticket is set. Else
     * the method returns <code>null</code>.
     *
     * @return The user id of the request <code>Ticket</code> if set. If the
     *      <code>Ticket</code> is not set yet <code>null</code> is returned.
     */
    public String getRemoteUser();

    /**
     * Returns the method name from the HTTP request line. According to the
     * specification of this method in the {@link RequestInfo#getMethod()
     * RequestInfo} interface, this method implements the
     * <code>ServletRequest</code> semantics and returns the method from the
     * HTTP request line. Use {@link RequestInfo#getRealMethod()} to get the
     * method according to the Communiqué 3 semantics.
     *
     * @return The method name from the HTTP request line.
     *
     * @see RequestInfo#getRealMethod()
     *
     *
     * Returns the method associated with this request. This is either the
     * value of the <i>cq-action</i> header or the value returned by
     * <code>HttpServletRequest.getMethod()</code>.
     * <p>
     * <strong>CONFLICT</strong>: This specification conflicts with the
     * specification of <code>HttpServletRequest.getMethod()</code>.
     * Implementations of both this interface and the
     * <code>HttpServletRequest</code> interface are expected to implement the
     * latter specification.
     *
     * @return The method name. See above remarks for details.
     *
     * @deprecated use {@link #getRealMethod()} instead
     */
    public String getMethod();

    /**
     * Returns the request URI from the HTTP request line. According to the
     * specification of this method in the {@link RequestInfo#getRequestURI()
     * RequestInfo} interface, this method implements the
     * <code>ServletRequest</code> semantics and returns the request URI from
     * the HTTP request line. Use {@link RequestInfo#getRealRequestURI()} to
     * get the request URI according to the Communiqué 3 semantics.
     *
     * @return The request URI from the HTTP request line.
     *
     * @see RequestInfo#getRealRequestURI()
     *
     *
     * Returns the request URI which is either the contents of the
     * <code>javax.servlet.include.request_uri</code> request attribute or the
     * result from <code>ServletRequest.getRequestURI()</code> method.
     * <p>
     * <strong>CONFLICT</strong>: This specification conflicts with the
     * specification of <code>ServletRequest.getRequestURI()</code>.
     * Implementations of both this interface and the
     * <code>HttpServletRequest</code> interface are expected to implement the
     * latter specification.
     *
     * @return The request URI either from the
     * <code>javax.servlet.include.request_uri</code> request attribute or the
     * result from <code>ServletRequest.getRequestURI()</code> method.
     *
     * @deprecated Use {@link #getRealRequestURI()} instead.
     */
    public String getRequestURI();

    /**
     * Returns the query string from the HTTP request line. According to the
     * specification of this method in the {@link RequestInfo#getQueryString()
     * RequestInfo} interface, this method implements the
     * <code>ServletRequest</code> semantics and returns the query string from
     * the HTTP request line. Use {@link RequestInfo#getRealQueryString()} to
     * get the query string according to the Communiqué 3 semantics.
     *
     * @return The query string from the HTTP request line.
     *
     * @see RequestInfo#getRealQueryString()
     **
     * Returns the query string which is either the contents of the
     * <code>javax.servlet.include.query_string</code> request attribute or the
     * result from <code>ServletRequest.getQueryString()</code> method.
     * <p>
     * <strong>CONFLICT</strong>: This specification conflicts with the
     * specification of <code>ervletRequest.getQueryString()</code>.
     * Implementations of both this interface and the
     * <code>HttpServletRequest</code> interface are expected to implement the
     * latter specification.
     *
     * @return The query string either from the
     * <code>javax.servlet.include.query_string</code> request attribute or the
     * result from <code>ServletRequest.getQueryString()</code> method.
     *
     * @deprecated Use {@link #getRealQueryString()} instead.
     */
    public String getQueryString();

    /**
     * Returns the context path from the HTTP request line. According to the
     * specification of this method in the {@link RequestInfo#getContextPath()
     * RequestInfo} interface, this method implements the
     * <code>ServletRequest</code> semantics and returns the context path from
     * the HTTP request line. Use {@link RequestInfo#getRealContextPath()} to
     * get the context path according to the Communiqué 3 semantics.
     *
     * @return The context path either for the request URI on the HTTP request
     *      line.
     *
     * @see RequestInfo#getRealContextPath()
     * Return the context path, either from the request or from the request
     * attributes.
     */
    public String getContextPath();
}