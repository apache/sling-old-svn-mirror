/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.testservices.events;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Count the number of OSGi events that we receive on specific topics, and
 *  report them to clients.
 */
@SuppressWarnings("serial")
@Component(immediate=true, metatype=false)
@Service
@Properties({
    @Property(name="service.description", value="Paths Test Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.paths", value="/testing/EventsCounter"), 
    @Property(name="sling.servlet.extensions", value="json"), 
    @Property(
            name=org.osgi.service.event.EventConstants.EVENT_TOPIC,
            value= {
                    org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED,
                    "org/apache/sling/api/resource/ResourceResolverMapping/CHANGED"
            })
})
public class EventsCounterImpl extends SlingSafeMethodsServlet implements EventHandler,EventsCounter {

    private final Map<String, AtomicInteger> counters = new HashMap<String, AtomicInteger>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public synchronized void handleEvent(Event event) {
        final String topic = event.getTopic();
        AtomicInteger counter = counters.get(topic);
        if(counter == null) {
            counter = new AtomicInteger();
            counters.put(topic, counter);
        }
        counter.incrementAndGet();
        log.debug("{} counter is now {}", topic, counter.get());
    }
    
    public int getEventsCount(String topic) {
        final AtomicInteger counter = counters.get(topic);
        return counter == null ? 0 : counter.get();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,SlingHttpServletResponse response) 
    throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            final JSONWriter w = new JSONWriter(response.getWriter());
            w.setTidy(true);
            w.object();
            for(Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {
                w.key(entry.getKey()).value(entry.getValue());
            }
            w.endObject();
        } catch(JSONException je) {
            throw (IOException)new IOException("JSONException in doGet").initCause(je);
        }
    }
}