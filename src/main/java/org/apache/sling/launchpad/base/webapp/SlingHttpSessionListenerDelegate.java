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
package org.apache.sling.launchpad.base.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.proxy.ProxyListener;

public class SlingHttpSessionListenerDelegate implements
        HttpSessionAttributeListener, HttpSessionListener,
        ServletContextListener {

    private final ProxyListener proxyListener = new ProxyListener();

    // ---------- ServletContextListener

    public void contextInitialized(final ServletContextEvent sce) {
        this.proxyListener.contextInitialized(sce);
    }

    public void contextDestroyed(final ServletContextEvent sce) {
        this.proxyListener.contextDestroyed(sce);
    }

    // ---------- HttpSessionListener

    public void sessionCreated(HttpSessionEvent se) {
        proxyListener.sessionCreated(se);
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        proxyListener.sessionDestroyed(se);
    }

    // ---------- HttpSessionAttributeListener

    public void attributeAdded(HttpSessionBindingEvent se) {
        proxyListener.attributeAdded(se);
    }

    public void attributeRemoved(HttpSessionBindingEvent se) {
        proxyListener.attributeRemoved(se);
    }

    public void attributeReplaced(HttpSessionBindingEvent se) {
        proxyListener.attributeReplaced(se);
    }

}
