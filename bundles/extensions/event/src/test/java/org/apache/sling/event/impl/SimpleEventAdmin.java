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
package org.apache.sling.event.impl;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * Simple event admin implementation for testing.
 */
public class SimpleEventAdmin implements EventAdmin {

    private final String[] topics;
    private final EventHandler[] handler;

    public SimpleEventAdmin(final String[] topics, final EventHandler[] handler) {
        this.topics = topics;
        this.handler = handler;
        if ( topics == null && handler != null ) {
            throw new IllegalArgumentException("If topics is null, handler must be null as well");
        }
        if ( topics.length != handler.length ) {
            throw new IllegalArgumentException("Topics and handler must have the same size.");
        }
    }

    public void postEvent(final Event event) {
        new Thread() {
            public void run() {
                sendEvent(event);
            }
        }.start();
    }

    public void sendEvent(Event event) {
        if ( topics != null ) {
            for(int i=0; i<topics.length; i++) {
                if ( topics[i].equals(event.getTopic()) ) {
                    handler[i].handleEvent(event);
                }
            }
        }
    }
}
