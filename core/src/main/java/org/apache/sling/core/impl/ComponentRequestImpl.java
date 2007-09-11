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
package org.apache.sling.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
import org.apache.sling.component.ComponentSession;
import org.apache.sling.component.Content;
import org.apache.sling.component.RequestParameter;
import org.apache.sling.content.ContentManager;
import org.apache.sling.content.jcr.JcrContentManager;
import org.apache.sling.core.RequestUtil;
import org.apache.sling.core.content.SelectableContent;
import org.apache.sling.core.content.Selector;
import org.apache.sling.core.impl.parameters.ParameterSupport;


/**
 * The <code>ComponentRequestImpl</code> TODO
 */
class ComponentRequestImpl extends HttpServletRequestWrapper implements ComponentRequest {

    /**
     * The <code>getLocalAddr</code> method name (value is "getLocalAddr").
     * This constant is used to call the <code>getLocalAddr</code> method on
     * the request object, which was added in the Servlet API 2.4 and thus might
     * not be available to the Servlet API 2.3.
     *
     * @see #postServlet23Methods
     * @see #callPostServlet23Method(ServletRequest, String)
     */
    private static String METHOD_GET_LOCAL_ADDR = "getLocalAddr";

    /**
     * The <code>getLocalName</code> method name (value is "getLocalName").
     * This constant is used to call the <code>getLocalName</code> method on
     * the request object, which was added in the Servlet API 2.4 and thus might
     * not be available to the Servlet API 2.3.
     *
     * @see #postServlet23Methods
     * @see #callPostServlet23Method(ServletRequest, String)
     */
    private static String METHOD_GET_LOCAL_NAME = "getLocalName";

    /**
     * The <code>getLocalPort</code> method name (value is "getLocalPort").
     * This constant is used to call the <code>getLocalPort</code> method on
     * the request object, which was added in the Servlet API 2.4 and thus might
     * not be available to the Servlet API 2.3.
     *
     * @see #postServlet23Methods
     * @see #callPostServlet23Method(ServletRequest, String)
     */
    private static String METHOD_GET_LOCAL_PORT = "getLocalPort";

    /**
     * The <code>getRemotePort</code> method name (value is "getRemotePort").
     * This constant is used to call the <code>getRemotePort</code> method on
     * the request object, which was added in the Servlet API 2.4 and thus might
     * not be available to the Servlet API 2.3.
     *
     * @see #postServlet23Methods
     * @see #callPostServlet23Method(ServletRequest, String)
     */
    private static String METHOD_GET_REMOTE_PORT = "getRemotePort";

    /**
     * The map of methods indexed by their name added in the Servlet API 2.4
     * and later and thus not available in Servlet API 2.3 containers. This map
     * is filled on demand as the respective methods are called.
     *
     * @see #callPostServlet23Method(ServletRequest, String)
     */
    private static Map postServlet23Methods = Collections.synchronizedMap(new HashMap());

    /**
     * Calls the named method on the given <code>request</code> object. The
     * method is expected to take no arguments and to be defined in the
     * <code>HttpServletRequest</code> interface provided by the servlet
     * container. If that interface does not provide such a method,
     * <code>null</code> is just returned. If calling the method fails for any
     * reason, <code>null</code> is also returned.
     *
     * @param request The <code>ServletRequest</code> object on which the
     *            named method is called. The object is expected to actually
     *            implement the <code>HttpServletRequest</code> interface, but
     *            this is not checked here.
     * @param methodName The name of the method to call. This method must be
     *            defined as a public method taking no arguments in the
     *            <code>HttpServletRequest</code> interface.
     *
     * @return The value returned by calling the method or <code>null</code>
     *         if the named method is not defined in the
     *         <code>HttpServletRequest</code> interface provided by the
     *         platform or invoking the method results in an error.
     * @see #postServlet23Methods
     */
    private static Object callPostServlet23Method(ServletRequest request, String methodName) {
        Method method = (Method) postServlet23Methods.get(methodName);
        if (method == null) {
            try {
                method = HttpServletRequestWrapper.class.getMethod(methodName, null);
                postServlet23Methods.put(methodName, method);
            } catch (Throwable t) {
                // cannot get the method, ignore and return null
                return null;
            }
        }

        try {
            return method.invoke(request, null);
        } catch (Throwable t) {
            // cannot invoke the method, ignore and return null
            return null;
        }
    }

    private final RequestData requestData;
    private ComponentSession session;
    private String remoteUser;

    protected ComponentRequestImpl(RequestData requestData) {
        super(requestData.getServletRequest());
        this.requestData = requestData;
    }

    /**
     * @return the requestData
     */
    public final RequestData getRequestData() {
        return this.requestData;
    }

    ParameterSupport getParameterSupport() {
        return this.getRequestData().getParameterSupport();
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#getAuthType()
     */
    public String getAuthType() {
        return null; // getRequest().getAuthType();
    }

    /**
     * @return
     */
    public ComponentSession getComponentSession() {
        return this.getComponentSession(true);
    }

    /**
     * @param create
     * @return
     */
    public ComponentSession getComponentSession(boolean create) {
        if (this.session == null) {
            HttpSession session = super.getSession(create);
            if (session != null) {
                this.session = new ComponentSessionImpl(null, session);
            }
        }
        return this.session;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getContent()
     */
    public Content getContent() {
        return this.getRequestData().getContentData().getContent();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getContent(java.lang.String)
     */
    public Content getContent(String path) throws ComponentException {
        if (!path.startsWith("/")) {
            path = this.getContent().getPath() + "/" + path;
        }

        try {
            return this.getContentInternal(path);
        } catch (ObjectContentManagerException pe) {
            throw new ComponentException("Cannot map item " + path, pe);
        }

    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestDispatcher(org.apache.sling.core.component.Content)
     */
    public ComponentRequestDispatcher getRequestDispatcher(Content content) {
        ContentData cd = this.getRequestData().getContentData();
        if (cd == null) {
            // in case of issue, this may happen, but should, just taking care
            return null;
        }

        ComponentContext ctx = cd.getComponent().getComponentContext();
        return ctx.getRequestDispatcher(content);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        ContentData cd = this.getRequestData().getContentData();
        if (cd == null) {
            // in case of issue, this may happen, but should, just taking care
            return null;
        }

        ComponentContext ctx = cd.getComponent().getComponentContext();
        return ctx.getRequestDispatcher(path);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getChildren(org.apache.sling.core.component.Content)
     */
    public Enumeration getChildren(Content content) throws ComponentException {
        try {
            Session session = this.getRequestData().getSession();
            final String path = content.getPath();
            Item item = session.getItem(path);

            // fail if not a node
            if (!item.isNode()) {
                throw new ComponentException("Cannot get children of poperty " + path);
            }

            final NodeIterator children = ((Node) item).getNodes();
            return new Enumeration() {
                private Content next;
                {
                    this.next = this.seek();
                }
                public boolean hasMoreElements() {
                    return this.next != null;
                }
                public Object nextElement() {
                    if (!this.hasMoreElements()) {
                        throw new NoSuchElementException();
                    }

                    Content toReturn = this.next;
                    this.next = this.seek();
                    return toReturn;
                }
                private Content seek() {
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        try {
                            String childPath = path + "/" + child.getName();
                            Content content = ComponentRequestImpl.this.getContentInternal(childPath);
                            if (content != null) {
                                return content;
                            }
                        } catch (ObjectContentManagerException pe) {
                            // TODO: log
                        } catch (RepositoryException re) {
                            // TODO: log
                        } catch (Exception e) {
                            // TODO: log
                        }
                    }

                    // exhausted nodes, return null
                    return null;
                }
                };
        } catch (RepositoryException re) {
            throw new ComponentException(re);
        }
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#getContextPath()
     */
    public String getContextPath() {
        return this.getRequestData().getContextPath();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getLocale()
     */
    public Locale getLocale() {
        return (this.getRequestData() != null) ? this.getRequestData().getLocale() : super.getLocale();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
     */
    public String getParameter(String name) {
        return this.getParameterSupport().getParameter(name);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterMap()
     */
    public Map getParameterMap() {
        return this.getParameterSupport().getParameterMap();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterNames()
     */
    public Enumeration getParameterNames() {
        return this.getParameterSupport().getParameterNames();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name) {
        return this.getParameterSupport().getParameterValues(name);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameter(java.lang.String)
     */
    public RequestParameter getRequestParameter(String name) {
        return this.getParameterSupport().getRequestParameter(name);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameters(java.lang.String)
     */
    public RequestParameter[] getRequestParameters(String name) {
        return this.getParameterSupport().getRequestParameters(name);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameterMap()
     */
    public Map getRequestParameterMap() {
        return this.getParameterSupport().getRequestParameterMap();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getCookie(java.lang.String)
     */
    public Cookie getCookie(String name) {
        return RequestUtil.getCookie(this, name);
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#getRequestURI()
     */
    public String getRequestURI() {
        return this.getRequestData().getRequestURI();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getSelectorString()
     */
    public String getSelectorString() {
        return this.getRequestData().getSelectorString();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getSelectors()
     */
    public String[] getSelectors() {
        return this.getRequestData().getSelectors();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getSelector(int)
     */
    public String getSelector(int i) {
        return this.getRequestData().getSelector(i);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getExtension()
     */
    public String getExtension() {
        return this.getRequestData().getExtension();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getSuffix()
     */
    public String getSuffix() {
        return this.getRequestData().getSuffix();
    }

    public String getQueryString() {
        return this.getRequestData().getQueryString();
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#getRemoteUser()
     */
    public String getRemoteUser() {
        if (this.remoteUser == null) {
            ContentManager cm = this.getRequestData().getContentManager();
            if (cm instanceof JcrContentManager) {
                this.remoteUser = ((JcrContentManager) cm).getSession().getUserID();
            } else {
                this.remoteUser = "[unknown]";
            }
        }

        return this.remoteUser;
    }

    public String getLocalAddr() {
        return (String) callPostServlet23Method(this.getRequest(), METHOD_GET_LOCAL_ADDR);
    }

    public String getLocalName() {
        return (String) callPostServlet23Method(this.getRequest(), METHOD_GET_LOCAL_NAME);
    }

    public int getLocalPort() {
        Object result = callPostServlet23Method(this.getRequest(), METHOD_GET_LOCAL_PORT);
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }

        return -1;
    }

    public int getRemotePort() {
        Object result = callPostServlet23Method(this.getRequest(), METHOD_GET_REMOTE_PORT);
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }

        return -1;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResourceBundle(java.util.Locale)
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        // TODO should use our resource bundle !!
        return null;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResponseContentType()
     */
    public String getResponseContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResponseContentTypes()
     */
    public Enumeration getResponseContentTypes() {
        // TODO Auto-generated method stub
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    protected Content getContentInternal(String path) {
        Content content = this.getRequestData().getContentManager().load(path);
        if (content instanceof SelectableContent) {
            SelectableContent selectable = (SelectableContent) content;
            Selector sel = selectable.getSelector();
            if (sel != null) {
                return sel.select(this, content);
            }
        }

        return content;
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException {
        return this.getRequestData().getInputStream();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getReader()
     */
    public BufferedReader getReader() throws UnsupportedEncodingException,
            IOException {
        return this.getRequestData().getReader();
    }
}
