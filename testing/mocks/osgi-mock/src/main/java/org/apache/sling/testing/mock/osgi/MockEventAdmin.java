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
package org.apache.sling.testing.mock.osgi;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link EventAdmin}.
 * From {@link EventConstants} currently only {@link EventConstants#EVENT_TOPIC} is supported. 
 */
@Component(immediate = true)
@Service(value = EventAdmin.class)
public final class MockEventAdmin implements EventAdmin {
    
    @Reference(name="eventHandler", referenceInterface=EventHandler.class,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    private final Map<Object, EventHandlerItem> eventHandlers = new TreeMap<Object, EventHandlerItem>();

    private ExecutorService asyncHandler;
    
    private static final Logger log = LoggerFactory.getLogger(MockEventAdmin.class);
    
    @Activate
    protected void activate(ComponentContext componentContext) {
        asyncHandler = Executors.newCachedThreadPool();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        asyncHandler.shutdownNow();
    }

    @Override
    public void postEvent(final Event event) {
        asyncHandler.execute(new Runnable() {
            @Override
            public void run() {
                distributeEvent(event);
            }
        });
    }

    @Override
    public void sendEvent(final Event event) {
        distributeEvent(event);
    }
    
    private void distributeEvent(Event event) {
        synchronized (eventHandlers) {
            for (EventHandlerItem item : eventHandlers.values()) {
                if (item.matches(event)) {
                    try {
                        item.getEventHandler().handleEvent(event);
                    }
                    catch (Throwable ex) {
                        log.error("Error handlihng event {} in {}", event, item.getEventHandler());
                    }
                }
            }
        }
    }
    
    protected void bindEventHandler(EventHandler eventHandler, Map<String, Object> props) {
        synchronized (eventHandlers) {
            eventHandlers.put(ServiceUtil.getComparableForServiceRanking(props), new EventHandlerItem(eventHandler, props));
        }
    }

    protected void unbindEventHandler(EventHandler eventHandler, Map<String, Object> props) {
        synchronized (eventHandlers) {
            eventHandlers.remove(ServiceUtil.getComparableForServiceRanking(props));
        }
    }
    
    private static class EventHandlerItem {
        
        private final EventHandler eventHandler;
        private final Pattern[] topicPatterns;

        private static final Pattern WILDCARD_PATTERN = Pattern.compile("[^*]+|(\\*)");
        
        public EventHandlerItem(EventHandler eventHandler, Map<String, Object> props) {
            this.eventHandler = eventHandler;
            topicPatterns = generateTopicPatterns(props.get(EventConstants.EVENT_TOPIC));
        }
        
        public boolean matches(Event event) {
            if (topicPatterns.length == 0) {
                return true;
            }
            String topic = event.getTopic();
            if (topic != null) {
                for (Pattern topicPattern : topicPatterns) {
                    if (topicPattern.matcher(topic).matches()) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        public EventHandler getEventHandler() {
            return eventHandler;
        }

        private static Pattern[] generateTopicPatterns(Object topic) {
            String[] topics;
            if (topic == null) {
                topics = new String[0];
            }
            else if (topic instanceof String) {
                topics = new String[] { (String)topic };
            }
            else if (topic instanceof String[]) {
                topics = (String[])topic;
            }
            else {
                throw new IllegalArgumentException("Invalid topic: " + topic);
            }
            Pattern[] patterns = new Pattern[topics.length];
            for (int i=0; i<topics.length; i++) {
                patterns[i] = toWildcardPattern(topics[i]);
            }
            return patterns;
        }
        
        /**
         * Converts a wildcard string with * to a regex pattern (from http://stackoverflow.com/questions/24337657/wildcard-matching-in-java)
         * @param wildcard
         * @return Regexp pattern
         */
        private static Pattern toWildcardPattern(String wildcard) {
            Matcher matcher = WILDCARD_PATTERN.matcher(wildcard);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                if(matcher.group(1) != null) matcher.appendReplacement(result, ".*");
                else matcher.appendReplacement(result, "\\\\Q" + matcher.group(0) + "\\\\E");
            }
            matcher.appendTail(result);
            return Pattern.compile(result.toString());
        }
        
    }

}
