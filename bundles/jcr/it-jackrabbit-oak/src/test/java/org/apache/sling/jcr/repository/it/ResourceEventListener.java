/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.jcr.repository.it;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.sling.api.SlingConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceEventListener implements EventHandler {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<String> paths = new HashSet<String>();
    
    ServiceRegistration register(BundleContext ctx) {
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, SlingConstants.TOPIC_RESOURCE_ADDED);
        return ctx.registerService(EventHandler.class.getName(), this, props);
    }
    
    @Override
    public void handleEvent(Event event) {
        final String path = (String) event.getProperty("path");
        if(path != null) {
            if(paths.isEmpty()) {
                log.info("Got first event, path={}", path);
            }
            synchronized (paths) {
                paths.add(path);
            }
        }
    }
    
    void clear() {
        synchronized (paths) {
            paths.clear();
        }
    }
    
    Set<String> getPaths() {
        synchronized (paths) {
            return Collections.unmodifiableSet(paths);
        }
    }
}