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
import org.apache.sling.RequestUtil;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
import org.apache.sling.component.ComponentSession;
import org.apache.sling.component.Content;
import org.apache.sling.component.RequestParameter;
import org.apache.sling.content.ContentManager;
import org.apache.sling.content.SelectableContent;
import org.apache.sling.content.Selector;
import org.apache.sling.content.jcr.JcrContentManager;
import org.apache.sling.core.parameters.ParameterSupport;


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
        return requestData;
    }

    ParameterSupport getParameterSupport() {
        return getRequestData().getParameterSupport();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getAuthType()
     */
    public String getAuthType() {
        return null; // getRequest().getAuthType();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getComponentSession()
     */
    public ComponentSession getComponentSession() {
        return getComponentSession(true);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getComponentSession(boolean)
     */
    public ComponentSession getComponentSession(boolean create) {
        if (session == null) {
            HttpSession session = super.getSession(create);
            if (session != null) {
                this.session = new ComponentSessionImpl(null, session);
            }
        }
        return session;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getContent()
     */
    public Content getContent() {
        return getRequestData().getContentData().getContent();
    }

    /*
     * (non-Javadoc)
     * @see com.day.components.ComponentContext#getContent(java.lang.String)
     */
    public Content getContent(String path) throws ComponentException {
        if (!path.startsWith("/")) {
            path = getContent().getPath() + "/" + path;
        }
        
        try {
            return getContentInternal(path);
        } catch (ObjectContentManagerException pe) {
            throw new ComponentException("Cannot map item " + path, pe);
        }
        
    }
    
    /*
     * (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestDispatcher(com.day.components.Content)
     */
    public ComponentRequestDispatcher getRequestDispatcher(Content content) {
        ContentData cd = getRequestData().getContentData();
        if (cd == null) {
            // in case of issue, this may happen, but should, just taking care
            return null;
        }

        ComponentContext ctx = cd.getComponent().getComponentContext();
        return ctx.getRequestDispatcher(content);
    }
    
    /*
     * (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        ContentData cd = getRequestData().getContentData();
        if (cd == null) {
            // in case of issue, this may happen, but should, just taking care
            return null;
        }
        
        ComponentContext ctx = cd.getComponent().getComponentContext();
        return ctx.getRequestDispatcher(path);
    }
    
    /*
     * (non-Javadoc)
     * @see com.day.components.ComponentContext#getChildren(com.day.components.Content, boolean)
     */
    public Enumeration getChildren(Content content) throws ComponentException {
        try {
            Session session = getRequestData().getSession();
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
                    next = seek();
                }
                public boolean hasMoreElements() {
                    return next != null;
                }
                public Object nextElement() {
                    if (!hasMoreElements()) {
                        throw new NoSuchElementException();
                    }
                    
                    Content toReturn = next;
                    next = seek();
                    return toReturn;
                }
                private Content seek() {
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        try {
                            String childPath = path + "/" + child.getName();
                            Content content = getContentInternal(childPath);
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
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getContextPath()
     */
    public String getContextPath() {
        return getRequestData().getContextPath();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getLocale()
     */
    public Locale getLocale() {
        return (getRequestData() != null) ? getRequestData().getLocale() : super.getLocale();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getParameter(java.lang.String)
     */
    public String getParameter(String name) {
        return getParameterSupport().getParameter(name);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getParameterMap()
     */
    public Map getParameterMap() {
        return getParameterSupport().getParameterMap();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getParameterNames()
     */
    public Enumeration getParameterNames() {
        return getParameterSupport().getParameterNames();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name) {
        return getParameterSupport().getParameterValues(name);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestParameter(java.lang.String)
     */
    public RequestParameter getRequestParameter(String name) {
        return getParameterSupport().getRequestParameter(name);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestParameters(java.lang.String)
     */
    public RequestParameter[] getRequestParameters(String name) {
        return getParameterSupport().getRequestParameters(name);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestParameterMap()
     */
    public Map getRequestParameterMap() {
        return getParameterSupport().getRequestParameterMap();
    }

    /*
     * (non-Javadoc)
     * @see com.day.components.ComponentRequest#getCookie(java.lang.String)
     */
    public Cookie getCookie(String name) {
        return RequestUtil.getCookie(this, name);
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRequestURI()
     */
    public String getRequestURI() {
        return getRequestData().getRequestURI();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getSelectorString()
     */
    public String getSelectorString() {
        return getRequestData().getSelectorString();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getSelectors()
     */
    public String[] getSelectors() {
        return getRequestData().getSelectors();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getSelector(int)
     */
    public String getSelector(int i) {
        return getRequestData().getSelector(i);
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getExtension()
     */
    public String getExtension() {
        return getRequestData().getExtension();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getSuffix()
     */
    public String getSuffix() {
        return getRequestData().getSuffix();
    }
    
    public String getQueryString() {
        return getRequestData().getQueryString();
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getRemoteUser()
     */
    public String getRemoteUser() {
        if (remoteUser == null) {
            ContentManager cm = getRequestData().getContentManager();
            if (cm instanceof JcrContentManager) {
                remoteUser = ((JcrContentManager) cm).getSession().getUserID();
            } else {
                remoteUser = "[unknown]";
            }
        }
        
        return remoteUser;
    }

    public String getLocalAddr() {
        return (String) callPostServlet23Method(getRequest(), METHOD_GET_LOCAL_ADDR);
    }
    
    public String getLocalName() {
        return (String) callPostServlet23Method(getRequest(), METHOD_GET_LOCAL_NAME);
    }
    
    public int getLocalPort() {
        Object result = callPostServlet23Method(getRequest(), METHOD_GET_LOCAL_PORT);
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        
        return -1;
    }
    
    public int getRemotePort() {
        Object result = callPostServlet23Method(getRequest(), METHOD_GET_REMOTE_PORT);
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        
        return -1;
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getResourceBundle(java.util.Locale)
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        // TODO should use our resource bundle !! 
        return null;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getResponseContentType()
     */
    public String getResponseContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentRequest#getResponseContentTypes()
     */
    public Enumeration getResponseContentTypes() {
        // TODO Auto-generated method stub
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    protected Content getContentInternal(String path) {
        Content content = getRequestData().getContentManager().load(path);
        if (content instanceof SelectableContent) {
            SelectableContent selectable = (SelectableContent) content;
            Selector sel = selectable.getSelector();
            if (sel != null) {
                return sel.select(this, content);
            }
        }

        return content;
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ActionRequest#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException {
        return getRequestData().getInputStream();
    }

    /* (non-Javadoc)
     * @see com.day.components.ActionRequest#getReader()
     */
    public BufferedReader getReader() throws UnsupportedEncodingException,
            IOException {
        return getRequestData().getReader();
    }
}
