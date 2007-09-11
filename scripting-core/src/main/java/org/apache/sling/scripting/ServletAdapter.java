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
package org.apache.sling.scripting;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentExtension;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;

/**
 * The <code>ServletAdapter</code> is a simple component which bridges a
 * component to a standard Servlet API servlet. The component must be registered
 * providing the fully qualified name of the Servlet to bridge to as the
 * {@link #PROP_SERVLET_CLASS} parameter. If this parameter is missing, the
 * component fails to initialize
 */
public class ServletAdapter implements Component, ServletConfig {

    public static final String PROP_SERVLET_CLASS = "org.apache.sling.core.scripting.bridge.servlet";
    public static final String PROP_CONTENT_CLASS = "org.apache.sling.core.scripting.bridge.content";

    private ComponentContext componentContext;
    private org.osgi.service.component.ComponentContext osgiComponentContext;
    private Dictionary osgiProperties;
    private Servlet delegatee;

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#getComponentContext()
     */
    public ComponentContext getComponentContext() {
        return componentContext;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#getId()
     */
    public String getId() {
        // the ID is the fully qualified name of the delegatee servlet class
        try {
            return getDelegatee().getClass().getName();
        } catch (ComponentException ce) {
            // log
        }

        // fall back to the property
        String className = getProperty(PROP_SERVLET_CLASS, null);
        if (className == null) {
            throw new IllegalStateException(
                "Missing Servlet class name property " + PROP_SERVLET_CLASS);
        }
        return className;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#createContentInstance()
     */
    public Content createContentInstance() {
        String className = getContentClassName();
        if (className != null) {
            try {
                return (Content) createInstance(className, Content.class);
            } catch (ComponentException ce) {
                // TODO: log
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#getContentClassName()
     */
    public String getContentClassName() {
        return getProperty(PROP_CONTENT_CLASS, null);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#getExtension(java.lang.String)
     */
    public ComponentExtension getExtension(String arg0) {
        // this class has no extensions by default
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#getExtensions()
     */
    public Enumeration getExtensions() {
        // this class has no extensions by default
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#init(org.apache.sling.core.component.ComponentContext)
     */
    public void init(ComponentContext componentContext) throws ComponentException {
        this.componentContext = componentContext;

        try {
            getDelegatee().init(this);
        } catch (ServletException se) {
            Throwable cause = (se.getRootCause() != null) ? se.getRootCause() : se;
            throw new ComponentException(se.getMessage(), cause);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#service(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse)
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws ComponentException, IOException {
        try {
            getDelegatee().service(request, response);
        } catch (ServletException se) {
            Throwable cause = (se.getRootCause() != null) ? se.getRootCause() : se;
            throw new ComponentException(se.getMessage(), cause);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.sling.core.component.Component#destroy()
     */
    public void destroy() {
        // get a copy of the delegatee and set delegatee null
        Servlet servlet = delegatee;
        delegatee = null;

        // destroy the servlet if not null
        if (servlet != null) {
            try {
                servlet.destroy();
            } catch (Throwable t) {
                // log
            }
        }
    }

    //---------- (optional) SCR Integration -----------------------------------

    protected void activate(org.osgi.service.component.ComponentContext osgiComponentContext) {
        this.osgiComponentContext = osgiComponentContext;
        this.osgiProperties = osgiComponentContext.getProperties();
    }

    protected void deactivate(org.osgi.service.component.ComponentContext osgiComponentContext) {
        // ensure servlet has been destroyed
        destroy();

        this.osgiProperties = null;
        this.osgiComponentContext = null;
    }

    //---------- internal -----------------------------------------------------

    private Servlet getDelegatee() throws ComponentException {
        if (delegatee == null) {
            String className = getProperty(PROP_SERVLET_CLASS, null);
            if (className == null) {
                throw new ComponentException(
                    "Missing Servlet class name property " + PROP_SERVLET_CLASS);
            }

            delegatee = (Servlet) createInstance(className, Servlet.class);
        }

        return delegatee;
    }

    private Object createInstance(String className, Class requiredClass)
            throws ComponentException {

        // get the class
        Class delegateeClass;
        try {
            // try to load the class through the bundle of the OSGi context
            if (osgiComponentContext != null) {
                delegateeClass = osgiComponentContext.getBundleContext().getBundle().loadClass(
                    className);
            } else {
                delegateeClass = getClass().getClassLoader().loadClass(
                    className);
            }
        } catch (Throwable t) {
            throw new ComponentException("Cannot get class " + className, t);
        }

        // now, that we have the class, create an instance and check type
        try {
            Object delegateeObject = delegateeClass.newInstance();
            if (!requiredClass.isInstance(delegateeObject)) {
                throw new ComponentException("Delegate class "
                    + className + " is not a " + requiredClass.getName());
            }

            return delegateeObject;
        } catch (Throwable t) {
            throw new ComponentException(
                "Cannot get instance of servlet " + className, t);
        }
    }

    private String getProperty(String propertyName, String defaultValue) {
        if (osgiProperties != null) {
            Object value = osgiProperties.get(propertyName);
            if (value instanceof String) {
                return (String) value;
            }
        }

        if (componentContext != null) {
            String value = componentContext.getInitParameter(propertyName);
            if (value != null) {
                return value;
            }

            Object valueO = componentContext.getAttribute(propertyName);
            if (valueO instanceof String) {
                return (String) valueO;
            }
        }

        // fall back to default value
        return defaultValue;
    }

    //---------- ServletConfig for the delegatee ------------------------------

    /**
     * Returns the value of the name OSGi Component Property if the property
     * happens to be a string. If the property does not exist or is not a
     * <code>java.lang.String</code> this method returns <code>null</code>.
     */
    public String getInitParameter(String name) {
        if (osgiProperties != null) {
            Object value = osgiProperties.get(name);
            if (value instanceof String) {
                return (String) value;
            }
        }

        return null;
    }

    /**
     * Returns the names of the keys of the OSGi Component properties.
     */
    public Enumeration getInitParameterNames() {
        Collection names = (osgiProperties != null)
                ? Collections.list(osgiProperties.keys())
                : Collections.EMPTY_LIST;
        return Collections.enumeration(names);
    }

    public ServletContext getServletContext() {
        return componentContext;
    }

    /**
     * Returns the {@link #getId() component ID} as the servlet name.
     */
    public String getServletName() {
        return getId();
    }
}
