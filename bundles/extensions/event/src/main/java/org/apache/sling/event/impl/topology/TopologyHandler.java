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
package org.apache.sling.event.impl.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The topology handler listens for topology events
 */
@Component(immediate=true)
@Service(value={TopologyHandler.class, TopologyEventListener.class})
public class TopologyHandler
    implements TopologyEventListener {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** List of topology awares. */
    private final List<TopologyAware> listeners = new ArrayList<TopologyAware>();

    @Reference
    private JobManagerConfiguration configuration;

    /** The topology capabilities. */
    private volatile TopologyCapabilities topologyCapabilities;

    private void stopProcessing() {
        // deactivate old capabilities - this stops all background processes
        if ( this.topologyCapabilities != null ) {
            this.topologyCapabilities.deactivate();
        }
        this.topologyCapabilities = null;
    }

    private void startProcessing(final TopologyView view) {
        // create new capabilities and update view
        this.topologyCapabilities = new TopologyCapabilities(view, this.configuration);
    }

    /**
     * @see org.apache.sling.discovery.TopologyEventListener#handleTopologyEvent(org.apache.sling.discovery.TopologyEvent)
     */
    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        this.logger.debug("Received topology event {}", event);

        // check if there is a change of properties which doesn't affect us
        if ( event.getType() == Type.PROPERTIES_CHANGED ) {
            final Map<String, String> newAllInstances = TopologyCapabilities.getAllInstancesMap(event.getNewView());
            if ( this.topologyCapabilities != null && this.topologyCapabilities.isSame(newAllInstances) ) {
                logger.debug("No changes in capabilities - ignoring event");
                return;
            }
        }

        synchronized ( this.listeners ) {

            if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
               this.stopProcessing();

               for(final TopologyAware l : this.listeners) {
                   l.topologyChanged(this.topologyCapabilities);
               }
            } else if ( event.getType() == Type.TOPOLOGY_INIT
                || event.getType() == Type.TOPOLOGY_CHANGED
                || event.getType() == Type.PROPERTIES_CHANGED ) {

                this.stopProcessing();

                this.startProcessing(event.getNewView());

                for(final TopologyAware l : this.listeners) {
                    l.topologyChanged(this.topologyCapabilities);
                }
            }

        }
    }

    public void addListener(final TopologyAware service) {
        synchronized ( this.listeners ) {
            this.listeners.add(service);
            service.topologyChanged(this.topologyCapabilities);
        }
    }

    public void removeListener(final TopologyAware service) {
        synchronized ( this.listeners )  {
            this.listeners.remove(service);
        }
    }
}
