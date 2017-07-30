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
package org.apache.sling.event.impl.jobs.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This topology handler is handling the topology change events asynchronously
 * and processes them by queuing them.
 */
@Component(service = TopologyEventListener.class,
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    })
public class TopologyHandler implements TopologyEventListener, Runnable {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Reference
    private JobManagerConfiguration configuration;

    /** A local queue for async handling of the events */
    private final BlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>();

    /** Active flag. */
    private final AtomicBoolean isActive = new AtomicBoolean(false);

    @Activate
    protected void activate() {
        this.isActive.set(true);
        final Thread thread = new Thread(this, "Apache Sling Job Topology Listener Thread");
        thread.setDaemon(true);

        thread.start();
    }

    @Deactivate
    protected void deactivate() {
        this.isActive.set(false);
        this.queue.clear();
        try {
            this.queue.put(new QueueItem());
        } catch ( final InterruptedException ie) {
            logger.warn("Thread got interrupted.", ie);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        final QueueItem item = new QueueItem();
        item.event = event;
        try {
            this.queue.put(item);
        } catch ( final InterruptedException ie) {
            logger.warn("Thread got interrupted.", ie);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while ( isActive.get() ) {
            QueueItem item = null;
            try {
                item = this.queue.take();
            } catch ( final InterruptedException ie) {
                logger.warn("Thread got interrupted.", ie);
                Thread.currentThread().interrupt();
                isActive.set(false);
            }
            if ( isActive.get() && item != null && item.event != null ) {
                final JobManagerConfiguration config = this.configuration;
                if ( config != null ) {
                    config.handleTopologyEvent(item.event);
                }
            }
        }
    }

    /**
     * We need a holder class to be able to put something into the queue to stop it.
     */
    public static final class QueueItem {
        public TopologyEvent event;
    }
}
