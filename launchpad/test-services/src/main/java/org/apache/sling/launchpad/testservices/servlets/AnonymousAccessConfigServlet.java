/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Servlet which disables and enables anonymous access and ensures
 * that the authenticator service has accepted the configuration change.
 */
@SuppressWarnings("serial")
@Component(immediate = true)
@Service
@Properties({ @Property(name = "service.description", value = "Anonymous Access Config Servlet"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation"),
        @Property(name = "sling.servlet.paths", value = "/testing/AnonymousAccessConfigServlet"),
        @Property(name = "sling.servlet.extensions", value = "txt"),
        @Property(name = "event.topics", value = "org/osgi/framework/ServiceEvent/MODIFIED") })
public class AnonymousAccessConfigServlet extends SlingAllMethodsServlet implements EventHandler {

    private static final String PROP_AUTH_ANNONYMOUS = "auth.annonymous";

    private static final String AUTH_PID = "org.apache.sling.engine.impl.auth.SlingAuthenticator";

    private static final long TIMEOUT = 1000;

    @Reference
    private ConfigurationAdmin configAdmin;

    private int modifiedCounter;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType(request.getContentType());
        String action = request.getParameter("action");
        if ("disable".equals(action)) {
            int existingModifiedCounter = modifiedCounter;
            Configuration config = configAdmin.getConfiguration(AUTH_PID, null);
            Dictionary props = config.getProperties();
            if (props == null) {
                props = new Hashtable();
            }
            props.put(PROP_AUTH_ANNONYMOUS, Boolean.FALSE);
            config.update(props);
            waitForModified(existingModifiedCounter, TIMEOUT);
        } else if ("enable".equals(action)) {
            int existingModifiedCounter = modifiedCounter;
            Configuration config = configAdmin.getConfiguration(AUTH_PID, null);
            Dictionary props = config.getProperties();
            if (props == null) {
                props = new Hashtable();
            }
            props.put(PROP_AUTH_ANNONYMOUS, Boolean.TRUE);
            config.update(props);
            waitForModified(existingModifiedCounter, TIMEOUT);
        }

        response.getWriter().println("ok");
    }

    private void waitForModified(final int existingModifiedCounter, final long timeoutMsec) {
        final int targetCounter = existingModifiedCounter + 1;
        final long end = System.currentTimeMillis() + timeoutMsec;
        while (modifiedCounter < targetCounter && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        if (modifiedCounter < targetCounter) {
            throw new IllegalStateException("Event counter did not reach " + targetCounter + ", waited " + timeoutMsec
                    + " msec");
        }
    }

    public void handleEvent(Event event) {
        if (AUTH_PID.equals(event.getProperty("service.pid"))) {
            modifiedCounter++;
        }
    }

}
