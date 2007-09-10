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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ComponentContextImpl</code> TODO
 */
public class ComponentContextImpl implements ComponentContext {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ComponentContextImpl.class);

    private ComponentRequestHandlerImpl requestHandler;

    public ComponentContextImpl(ComponentRequestHandlerImpl requestHandler) {
        this.requestHandler = requestHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return getServletContext().getAttribute(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return getServletContext().getAttributeNames();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return getServletContext().getInitParameter(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames() {
        return getServletContext().getInitParameterNames();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return getServletContext().getMajorVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String file) {
        return getServletContext().getMimeType(file);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return getServletContext().getMinorVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getComponentContextName
     */
    public String getComponentContextName() {
        return getServletContext().getServletContextName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#getRequestDispatcher(com.day.components.Content)
     */
    public ComponentRequestDispatcher getRequestDispatcher(Content content) {
        // return no dispatcher if content is null
        if (content == null) {
            log.error("getRequestDispatcher: No content, cannot create request dispatcher");
            return null;
        }
        
        return new ComponentRequestDispatcherImpl(content);
    }
    
    public RequestDispatcher getRequestDispatcher(String path) {
        // return no dispatcher if content is null
        if (path == null) {
            log.error("getRequestDispatcher: No path, cannot create request dispatcher");
            return null;
        }

        return new ComponentRequestDispatcherImpl(path);
    }

    public URL getResource(String path) throws MalformedURLException {
        // TODO Auto-generated method stub
        return null;
    }
    
    public InputStream getResourceAsStream(String path) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Set getResourcePaths(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return requestHandler.getServerInfo();
    }

    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }
    
    public void log(String msg) {
        log.info(msg);
    }
    
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        getServletContext().removeAttribute(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.ComponentContext#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        getServletContext().removeAttribute(name);
    }

    private ServletContext getServletContext() {
        return requestHandler.getServletContext();
    }

    public ServletContext getContext(String uripath) {
        // check whether to return ComponentContext ??
        return getServletContext().getContext(uripath);
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return getServletContext().getNamedDispatcher(name);
    }

    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    public Servlet getServlet(String name) throws ServletException {
        return getServletContext().getServlet(name);
    }

    public String getServletContextName() {
        return getServletContext().getServletContextName();
    }

    public Enumeration getServletNames() {
        return getServletContext().getServletNames();
    }

    public Enumeration getServlets() {
        return getServletContext().getServlets();
    }

    //---------- Inner class --------------------------------------------------
    
    private class ComponentRequestDispatcherImpl implements
            ComponentRequestDispatcher {

        private final Content content;
        private final String path;

        private ComponentRequestDispatcherImpl(Content content) {
            this.content = content;
            this.path = content.getPath();
        }
        
        private ComponentRequestDispatcherImpl(String path) {
            this.content = null;
            this.path = path;
        }

        public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
            // TODO Auto-generated method stub
            if (request instanceof ComponentRequest && response instanceof ComponentResponse) {
                include((ComponentRequest) request,
                    (ComponentResponse) response);
            } else {
                // TODO, use servlet container dispatcher !!
            }
        }
        
        public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
            // TODO Auto-generated method stub
            // TODO, use servlet container dispatcher !!
        }
      
        /*
         * (non-Javadoc)
         * 
         * @see com.day.components.ComponentRequestDispatcher#include(com.day.components.RenderRequest,
         *      com.day.components.ComponentResponse)
         */
        public void include(ComponentRequest request, ComponentResponse response)
                throws ComponentException, IOException {

            // if content is null, try to resolve it using the path
            Content target; 
            if (content != null) {
                target = content;
            } else {
                // this may throw an exception in case loading fails, which is
                // ok here, if no content is available at that path null is
                // return, which results in using the servlet container
                String absPath = getAbsolutePath(request, path);
                target = request.getContent(absPath);
            }
            
            if (target != null) {
                requestHandler.includeContent(request, response, target);
            } else {
                requestHandler.includeServlet(request, response, path);
            }
        }
        
        private String getAbsolutePath(ComponentRequest request, String path) {
            // path is already absolute
            if (path.startsWith("/")) {
                return path;
            }
            
            // get parent of current request
            String uri = request.getContent().getPath();
            int lastSlash = uri.lastIndexOf('/'); 
            if (lastSlash >= 0) { 
                uri = uri.substring(0, lastSlash);
            }
            
            // append relative path to parent
            return uri + '/' + path;
        }

    }

}
