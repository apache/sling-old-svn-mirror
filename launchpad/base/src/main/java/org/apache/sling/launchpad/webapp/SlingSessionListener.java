/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.sling.launchpad.base.shared.SharedConstants;

public class SlingSessionListener implements HttpSessionAttributeListener,
        HttpSessionListener, ServletContextListener {

    private static ServletContext servletContext;

    private static ServletContextListener delegateeContextListener;

    private static HttpSessionListener delegateeSessionListener;

    private static HttpSessionAttributeListener delegateeSessionAttributeListener;

    static void startDelegate(final ClassLoader classLoader) {

        // if the listener has not been configured, do nothing because
        // there is no servlet context to forward and there are no
        // events ever sent to this listener
        if (servletContext == null) {
            return;
        }

        Object delegatee = null;
        try {
            Class<?> delegateeClass = classLoader.loadClass(SharedConstants.DEFAULT_SLING_LISTENER);
            delegatee = delegateeClass.newInstance();
        } catch (Exception e) {
            servletContext.log(
                "Delegatee Event Listener class "
                    + SharedConstants.DEFAULT_SLING_LISTENER
                    + " cannot be loaded or instantiated; Http Session Event forwarding is disabled",
                e);
        }

        if (delegatee instanceof ServletContextListener) {
            delegateeContextListener = (ServletContextListener) delegatee;
            delegateeContextListener.contextInitialized(new ServletContextEvent(
                servletContext));

            delegateeSessionListener = (HttpSessionListener) delegatee;
            delegateeSessionAttributeListener = (HttpSessionAttributeListener) delegatee;
        }
    }

    static void stopDelegatee() {
        if (delegateeContextListener != null) {
            delegateeContextListener.contextDestroyed(new ServletContextEvent(
                servletContext));
        }

        delegateeContextListener = null;
        delegateeSessionListener = null;
        delegateeSessionAttributeListener = null;
    }

    public void contextInitialized(ServletContextEvent sce) {
        SlingSessionListener.servletContext = sce.getServletContext();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        stopDelegatee();
        SlingSessionListener.servletContext = null;
    }

    public void sessionCreated(HttpSessionEvent se) {
        final HttpSessionListener delegateeSessionListener = SlingSessionListener.delegateeSessionListener;
        if (delegateeSessionListener != null) {
            delegateeSessionListener.sessionCreated(se);
        }
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        final HttpSessionListener delegateeSessionListener = SlingSessionListener.delegateeSessionListener;
        if (delegateeSessionListener != null) {
            delegateeSessionListener.sessionDestroyed(se);
        }
    }

    public void attributeAdded(HttpSessionBindingEvent se) {
        final HttpSessionAttributeListener delegateeSessionAttributeListener = SlingSessionListener.delegateeSessionAttributeListener;
        if (delegateeSessionAttributeListener != null) {
            delegateeSessionAttributeListener.attributeAdded(se);
        }
    }

    public void attributeRemoved(HttpSessionBindingEvent se) {
        final HttpSessionAttributeListener delegateeSessionAttributeListener = SlingSessionListener.delegateeSessionAttributeListener;
        if (delegateeSessionAttributeListener != null) {
            delegateeSessionAttributeListener.attributeRemoved(se);
        }
    }

    public void attributeReplaced(HttpSessionBindingEvent se) {
        final HttpSessionAttributeListener delegateeSessionAttributeListener = SlingSessionListener.delegateeSessionAttributeListener;
        if (delegateeSessionAttributeListener != null) {
            delegateeSessionAttributeListener.attributeReplaced(se);
        }
    }

}
