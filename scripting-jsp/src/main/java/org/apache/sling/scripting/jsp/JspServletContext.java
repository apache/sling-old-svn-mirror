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
package org.apache.sling.scripting.jsp;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JspServletContext</code> TODO
 */
public class JspServletContext implements ServletContext {

    public static final Enumeration EMPTY_ENUMERATION = new Enumeration() {
        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspServletContext.class);
    
    private final ComponentContext componentContext;
    private final TldLocationsCacheSupport tcs;
    private final RepositoryOutputProvider outputProvider;
    
    JspServletContext(ComponentContext componentContext, TldLocationsCacheSupport tcs, RepositoryOutputProvider outputProvider) {
        this.componentContext = componentContext;
        this.tcs = tcs;
        this.outputProvider = outputProvider;
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return componentContext.getAttribute(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return componentContext.getAttributeNames();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getContext(java.lang.String)
     */
    public ServletContext getContext(String uripath) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return componentContext.getInitParameter(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames() {
        return componentContext.getInitParameterNames();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return componentContext.getMajorVersion();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String file) {
        return componentContext.getMimeType(file);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return componentContext.getMinorVersion();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    public URL getResource(String path) throws MalformedURLException {
        // path might be an URL, so only check repository in case of absolute
        // path - assuming URLs have no leading slash at all, we don't care
        // for the scheme separating colon here
        if (path.startsWith("/")) {
            URL url = outputProvider.getURL(path);
            if (url != null) {
                return url;
            }
        }
        
        // fall back to trying a real URL
        return getUrlForResource(path);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String path) {
        // path might be an URL, so only check repository in case of absolute
        // path - assuming URLs have no leading slash at all, we don't care
        // for the scheme separating colon here
        if (path.startsWith("/")) {
            try {
                return outputProvider.getInputStream(path);
            } catch (Exception e) {
                log.debug("getResourceAsStream: Cannot get resource {}: {}", path,
                    e.getMessage());
            }
        }
        
        // check whether we can resolve as an URL ...
        try {
            // create the URL and try to access
            URL url = getUrlForResource(path);
            if (url != null) {
                return url.openStream();
            }
        } catch (Exception e) {
            log.debug(
                "getResourceAsStream: Cannot access resource {} through URL: {}",
                path, e.getMessage());
        }

        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    public Set getResourcePaths(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return componentContext.getServerInfo();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServlet(java.lang.String)
     */
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName() {
        return componentContext.getServletContextName();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServletNames()
     */
    public Enumeration getServletNames() {
        return EMPTY_ENUMERATION;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServlets()
     */
    public Enumeration getServlets() {
        return EMPTY_ENUMERATION;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#log(java.lang.String)
     */
    public void log(String msg) {
        log.info(msg);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
     */
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        componentContext.removeAttribute(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        componentContext.setAttribute(name, object);
    }
    
    //---------- internal -----------------------------------------------------
    
    private URL getUrlForResource(String path) {
        int cs = path.indexOf(":/");
        if (cs > 0 && cs < path.length()-2) {
            // insert second slash after scheme (was canonicalized away)
            cs += 2;
            if (cs < path.length() && path.charAt(cs) != '/') {
                path = path.substring(0, cs) + "/" + path.substring(cs);
            }
            
            // TODO: tmp workaround while URLStreamHandler support is not available
            URL url = tcs.getTldLocationURL(path);
            if (url != null) {
                return url;
            }

            // create the URL and try to access
            try {
                return new URL(path);
            } catch (MalformedURLException mue) {
                log.debug("getUrlForResource: Cannot create URL for {}: {}",
                    path, mue.getMessage());
            }
        }
        
        return null;
    }
}
