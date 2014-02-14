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
package org.apache.sling.replication.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentsManager;
import org.apache.sling.replication.agent.impl.ReplicationAgentQueueResource;
import org.apache.sling.replication.event.ReplicationEvent;
import org.apache.sling.replication.event.ReplicationEventType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queue Server Sent Events servlet
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationAgentQueueResource.EVENT_RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = "GET")
})
public class ReplicationAgentQueueEventServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Map<String, Collection<String>> cachedEvents = new ConcurrentHashMap<String, Collection<String>>();

    @Reference
    private ReplicationAgentsManager replicationAgentsManager;

    private ServiceRegistration registration;

    @Activate
    protected void activate(BundleContext context) {
        log.info("activating SSE");
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, ReplicationEvent.getTopic(ReplicationEventType.PACKAGE_QUEUED));
        registration = context.registerService(EventHandler.class.getName(), new SSEListener(), properties);
        if (log.isInfoEnabled()) {
            log.info("SSE activated : {}", registration != null);
        }
    }

    @Deactivate
    protected void deactivate() throws Exception {
        log.info("deactivating SSE");
        if (registration != null) {
            registration.unregister();
        }
        synchronized (cachedEvents) {
            cachedEvents.clear();
            cachedEvents.notifyAll();
        }
    }


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // setup SSE headers
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        // needed to allow the JavaScript EventSource API to make a call from author to this server and listen for the events
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");

        String agentName = request.getResource().getParent().getParent().adaptTo(ReplicationAgent.class).getName();
        PrintWriter writer = response.getWriter();
        while (true) {
            try {
                synchronized (cachedEvents) {
                    cachedEvents.wait();
                    Collection<String> eventsForAgent = cachedEvents.get(agentName);
                    if (eventsForAgent != null) {
                        for (String event : eventsForAgent) {
                            writeEvent(writer, agentName + "-queue-event", event);
                        }
                    }
                }
            } catch (InterruptedException e) {
                if (log.isErrorEnabled()) {
                    log.error("error during SSE", e);
                }
                throw new ServletException(e);
            }
        }

    }

    /* Write a single server-sent event to the response stream for the given event and message */
    private void writeEvent(PrintWriter writer, String event, String message)
            throws IOException {

        // write the event type (make sure to include the double newline)
        writer.write("id: " + System.nanoTime() + "\n");

        // write the actual data
        // this could be simple text or could be JSON-encoded text that the
        // client then decodes
        writer.write("data: " + message + "\n\n");

        // flush the buffers to make sure the container sends the bytes
        writer.flush();
        if (log.isInfoEnabled()) {
            log.info("SSE event {}: {}", event, message);
        }
    }

    private class SSEListener implements EventHandler {
        public void handleEvent(Event event) {
            if (log.isInfoEnabled()) {
                log.info("SSE listener running on event {}", event);
            }
            Object pathProperty = event.getProperty("replication.package.paths");
            Object agentNameProperty = event.getProperty("replication.agent.name");
            if (log.isInfoEnabled()) {
                log.info("cached events {}", cachedEvents.size());
            }
            if (pathProperty != null && agentNameProperty != null) {
                String agentName = String.valueOf(agentNameProperty);
                String[] paths = (String[]) pathProperty;
                synchronized (cachedEvents) {
                    if (log.isInfoEnabled()) {
                        log.info("queue event for agent {} on paths {}", agentName, Arrays.toString(paths));
                    }
                    Collection<String> eventsForAgent = cachedEvents.get(agentName);
                    if (eventsForAgent == null) {
                        eventsForAgent = new LinkedList<String>();
                    }
                    eventsForAgent.add(Arrays.toString(paths));
                    cachedEvents.put(agentName, eventsForAgent);
                    cachedEvents.notifyAll();
                }
            }
        }
    }
}
