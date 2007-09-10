/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.Constants;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestWrapper;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.ComponentResponseWrapper;
import org.apache.sling.component.Content;
import org.apache.sling.content.ContentManager;
import org.apache.sling.core.output.BufferProvider;
import org.apache.sling.core.parameters.ParameterSupport;
import org.apache.sling.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>RequestData</code> class provides access to objects which are set
 * on a Servlet Request wide basis such as the repository session, the
 * persistence manager, etc.
 * 
 * @see ContentData
 */
public class RequestData implements BufferProvider {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(RequestData.class);

    /** The original servlet Servlet Request Object */
    private HttpServletRequest servletRequest;

    /** The parameter support class */
    private ParameterSupport parameterSupport;
    
    /** The original servlet Servlet Response object */
    private HttpServletResponse servletResponse;

    /**
     * <code>true</code> if the servlet is
     * <code>RequestDispatcher.include()</code>-ed
     */
    private boolean included;

    /**
     * The prepared request URI. This URI is either the URI from the HTTP
     * request line or the request URI from the
     * <code>javax.servlet.include.request_uri</code> request attribute with
     * the context path removed.
     */
    private String requestURI;

    /** Caches the real context path returned by {@link #getRealContextPath()} */
    private String contextPath;

    /** Caches the real servlet path returned by {@link #getRealServletPath()} */
    private String servletPath;

    /** Caches the real path info returned by {@link #getRealPathInfo()} */
    private String pathInfo;

    /** Caches the real query string returned by {@link #getRealQueryString()} */
    private String queryString;

    /** Caches the real method name returned by {@link #getRealMethod()} */
    private String method;

    private Session session;

    private ContentManager contentManager;

    private Locale locale;

    private Theme theme;

    /** The request URI after applying any fake URLs */
    private String originalURL;

    /** The mapped selectors */
    private String[] selectors = new String[0];

    /** The combined selectors string */
    private String combinedSelectorString = "";

    /** The extension */
    private String extension = "";

    /** the suffix */
    private String suffix = "";

    /** the current ContentData */
    private ContentData currentContentData;

    /** the stack of ContentData objects */
    private LinkedList contentDataStack;

    public RequestData(HttpServletRequest request, HttpServletResponse response) {
        this.servletRequest = request;
        this.servletResponse = response;

        // some more preparation
        this.included = request.getAttribute(Constants.INCLUDE_REQUEST_URI) != null;
    }

    /* package */void dispose() {
        // make sure our request attributes do not exist anymore
        servletRequest.removeAttribute(Constants.ATTR_REQUEST_CONTENT);
        servletRequest.removeAttribute(Constants.ATTR_REQUEST_COMPONENT);
        servletRequest.removeAttribute(Constants.ATTR_CONTENT_MANAGER);

        // clear the content data stack
        if (contentDataStack != null) {
            while (!contentDataStack.isEmpty()) {
                ContentData cd = (ContentData) contentDataStack.removeLast();
                cd.dispose();
            }
        }

        // dispose current content data, if any
        if (currentContentData != null) {
            currentContentData.dispose();
        }

        // logout the session
        if (session != null) {
            session.logout();
        }

        // clear fields
        contentDataStack = null;
        currentContentData = null;
        servletRequest = null;
        servletResponse = null;
        contentManager = null;
        session = null;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }
    
    //---------- Request Helper
    
    public static ComponentRequestImpl unwrap(ComponentRequest request) throws ComponentException {
        while (request instanceof ComponentRequestWrapper) {
            request = ((ComponentRequestWrapper) request).getComponentRequest();
        }
    
        if (request instanceof ComponentRequestImpl) {
            return (ComponentRequestImpl) request;
        }
    
        throw new ComponentException("RenderRequest not of correct type");
    }

    public static ComponentResponseImpl unwrap(ComponentResponse response) throws ComponentException {
        while (response instanceof ComponentResponseWrapper) {
            response = ((ComponentResponseWrapper) response).getComponentResponse();
        }
    
        if (response instanceof ComponentResponseImpl) {
            return (ComponentResponseImpl) response;
        }
    
        throw new ComponentException("ComponentResponse not of correct type");
    }

    public static RequestData getRequestData(ComponentRequest request) throws ComponentException {
        return unwrap(request).getRequestData();
    }

    // ---------- Content inclusion stacking -----------------------------------

    public void pushContent(Content content) {
        BufferProvider parent;
        if (currentContentData != null) {
            if (contentDataStack == null) {
                contentDataStack = new LinkedList();
            }

            // remove the request attributes if the stack is empty now
            servletRequest.setAttribute(Constants.ATTR_REQUEST_CONTENT,
                currentContentData.getContent());
            servletRequest.setAttribute(Constants.ATTR_REQUEST_COMPONENT,
                currentContentData.getComponent());

            contentDataStack.add(currentContentData);
            parent = currentContentData;
        } else {
            parent = this;
        }

        currentContentData = new ContentData(content, parent);
    }

    public void popContent() {
        // dispose current content data before replacing it
        if (currentContentData != null) {
            currentContentData.dispose();
        }

        if (contentDataStack != null && !contentDataStack.isEmpty()) {
            // remove the topmost content data object
            currentContentData = (ContentData) contentDataStack.removeLast();

            // remove the request attributes if the stack is empty now
            if (contentDataStack.isEmpty()) {
                servletRequest.removeAttribute(Constants.ATTR_REQUEST_COMPONENT);
                servletRequest.removeAttribute(Constants.ATTR_REQUEST_CONTENT);
            }

        } else {
            currentContentData = null;
        }
    }

    public ContentData getContentData() {
        return currentContentData;
    }

    /**
     * Returns <code>true</code> if request processing is currently processing
     * a component which has been included by
     * <code>ComponentRequestDispatcher.include</code>.
     */
    public boolean isContentIncluded() {
        return contentDataStack != null && !contentDataStack.isEmpty();
    }

    // ---------- parameters differing in included servlets --------------------

    /**
     * Returns <code>true</code> if the servlet is executed through
     * <code>RequestDispatcher.include()</code>.
     * 
     * @return <code>true</code> if the servlet is executed through
     *         <code>RequestDispatcher.include()</code>.
     */
    public boolean isIncluded() {
        return included;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.request_uri</code> attribute if
     * {@link #isIncluded()} or <code>request.getRequestURI()</code>. The
     * context path has been removed from the beginning of the returned string.
     * That is for request, which is not {@link #isIncluded() included}:
     * <code>getRealRequestURI() == getRealContextPath() + getRequestURI()</code>.
     * 
     * @return The relevant request URI according to environment with the
     *         context path removed.
     */
    public String getRequestURI() {
        if (requestURI == null) {

            // get the unmodified request URI and context information
            requestURI = included
                    ? (String) servletRequest.getAttribute(Constants.INCLUDE_REQUEST_URI)
                    : servletRequest.getRequestURI();

            String ctxPrefix = getContextPath();

            if (log.isDebugEnabled()) {
                log.debug("getRequestURI: Servlet request URI is {}",
                    requestURI);
            }

            // check to remove the context prefix
            if (ctxPrefix == null) {
                log.error("getRequestURI: Context path not expected to be null");
            } else if (ctxPrefix.length() == 0) {
                // default root context, no change
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: Default root context, no change to uri");
                }
            } else if (ctxPrefix.length() < requestURI.length()
                && requestURI.startsWith(ctxPrefix)
                && requestURI.charAt(ctxPrefix.length()) == '/') {
                // some path below context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: removing '{}' from '{}'",
                        ctxPrefix, requestURI);
                }
                requestURI = requestURI.substring(ctxPrefix.length());
            } else if (ctxPrefix.equals(requestURI)) {
                // context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: URI equals context prefix, assuming '/'");
                }
                requestURI = "/";
            }
        }

        return requestURI;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.context_path</code> attribute if
     * {@link #isIncluded()} or <code>request.getContextPath()</code>.
     * 
     * @return The relevant context path according to environment.
     */
    public String getContextPath() {
        if (contextPath == null) {
            contextPath = included
                    ? (String) servletRequest.getAttribute(Constants.INCLUDE_CONTEXT_PATH)
                    : servletRequest.getContextPath();
        }

        return contextPath;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.servlet_path</code> attribute if
     * {@link #isIncluded()} or <code>request.getServletPath()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the path to the servlet being executed
     * from the perspective of the servlet container. Thus this path is really
     * the path to the {@link DeliveryServlet}.
     * 
     * @return The relevant servlet path according to environment.
     */
    public String getServletPath() {
        if (servletPath == null) {
            servletPath = included
                    ? (String) servletRequest.getAttribute(Constants.INCLUDE_SERVLET_PATH)
                    : servletRequest.getServletPath();
        }

        return servletPath;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.path_info</code>
     * attribute if {@link #isIncluded()} or <code>request.getPathInfo()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the additional path info extending the
     * servlet path from the perspective of the servlet container. This is not
     * the same as the {@link #getSuffix() suffix}.
     * 
     * @return The relevant path info according to environment.
     */
    public String getPathInfo() {
        if (pathInfo == null) {
            pathInfo = included
                    ? (String) servletRequest.getAttribute(Constants.INCLUDE_PATH_INFO)
                    : servletRequest.getPathInfo();
        }

        return pathInfo;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.query_string</code> attribute if
     * {@link #isIncluded()} or <code>request.getQueryString()</code>.
     * 
     * @return The relevant query string according to environment.
     */
    public String getQueryString() {
        if (queryString == null) {
            queryString = included
                    ? (String) servletRequest.getAttribute(Constants.INCLUDE_QUERY_STRING)
                    : servletRequest.getQueryString();
        }

        return queryString;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the persistenceManager
     */
    public ContentManager getContentManager() {
        return contentManager;
    }

    /**
     * @param persistenceManager the persistenceManager to set
     */
    public void setContentManager(ContentManager contentManager) {
        this.contentManager = contentManager;

        // provide the content manager to components as request attribute
        servletRequest.setAttribute(Constants.ATTR_CONTENT_MANAGER,
            contentManager);
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * @return the theme
     */
    public Theme getTheme() {
        return theme;
    }

    /**
     * @param theme the theme to set
     */
    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public void setOriginalURL(String originalURL) {
        this.originalURL = originalURL;
    }

    public String getOriginalURL() {
        return originalURL;
    }

    /**
     * The selectors to set here is the string of dot-separated words after the
     * path but before any optional subsequent slashes. This string includes the
     * request URL extension, which is extracted here, too.
     */
    public void setSelectorsExtension(String selectors) {
        int lastDot = selectors.lastIndexOf('.');
        if (lastDot < 0) {
            // no selectors, just the extension
            extension = selectors;
            return;
        }

        // extension comes after last dot, rest are selectors
        this.extension = selectors.substring(lastDot + 1);

        // cut off extension to split selectors
        this.combinedSelectorString = selectors.substring(0, lastDot);
        this.selectors = combinedSelectorString.split("\\.");
    }

    public String getExtension() {
        return extension;
    }

    /**
     * The i-th selector string or null if i&lt;0 or i&gt;getSelectors().length
     */
    public String getSelector(int i) {
        return (i >= 0 && i < selectors.length) ? selectors[i] : null;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public String getSelectorString() {
        return combinedSelectorString;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    // ---------- BufferProvider -----------------------------------------

    public BufferProvider getBufferProvider() {
        return (currentContentData != null)
                ? (BufferProvider) currentContentData
                : this;
    }
    
    public ServletOutputStream getOutputStream() throws IOException {
        return getServletResponse().getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return getServletResponse().getWriter();
    }
    
    // ---------- Parameter support -------------------------------------------
    
    ServletInputStream getInputStream() throws IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException("Request Data has already been read");
        }
        
        // may throw IllegalStateException if the reader has already been acquired
        return getServletRequest().getInputStream();
    }
    
    BufferedReader getReader() throws UnsupportedEncodingException, IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException("Request Data has already been read");
        }
        
        // may throw IllegalStateException if the input stream has already been acquired
        return getServletRequest().getReader();
    }
    
    ParameterSupport getParameterSupport() {
        if (parameterSupport == null) {
            parameterSupport = new ParameterSupport(this /* getServletRequest() */);
        }
        
        return parameterSupport;
    }
}
