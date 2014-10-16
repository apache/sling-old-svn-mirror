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
package org.apache.sling.event.impl.jobs.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueConfigurationChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The topology handler listens for topology events.
 *
 * TODO - config changes should actually do a real stop/start
 */
@Component(immediate=true)
@Service(value={TopologyHandler.class, TopologyEventListener.class})
public class TopologyHandler
    implements TopologyEventListener, QueueConfigurationChangeListener {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** List of topology awares. */
    private final List<TopologyAware> listeners = new ArrayList<TopologyAware>();

    @Reference
    private JobManagerConfiguration configuration;

    @Reference
    private QueueConfigurationManager queueConfigManager;

    /** The topology capabilities. */
    private volatile TopologyCapabilities topologyCapabilities;

    @Activate
    protected void activate() {
        this.queueConfigManager.addListener(this);
    }

    @Deactivate
    protected void dectivate() {
        this.queueConfigManager.removeListener(this);
    }


    @Override
    public void configChanged() {
        final TopologyCapabilities caps = this.topologyCapabilities;
        if ( caps != null ) {
            synchronized ( this.listeners ) {
 //               this.stopProcessing(false);

                this.startProcessing(Type.PROPERTIES_CHANGED, caps, true);
            }
        }
    }

    private void stopProcessing(final boolean deactivate) {
        boolean notify = this.topologyCapabilities != null;
        // deactivate old capabilities - this stops all background processes
        if ( deactivate && this.topologyCapabilities != null ) {
            this.topologyCapabilities.deactivate();
        }
        this.topologyCapabilities = null;

        if ( notify ) {
            // stop all listeners
            this.notifiyListeners();
        }
    }

    private void startProcessing(final Type eventType, final TopologyCapabilities newCaps, final boolean isConfigChange) {
        // create new capabilities and update view
        this.topologyCapabilities = newCaps;

        // before we propagate the new topology we do some maintenance
        if ( eventType == Type.TOPOLOGY_INIT ) {
            final UpgradeTask task = new UpgradeTask();
            task.run(this.configuration, this.topologyCapabilities, queueConfigManager);

            final FindUnfinishedJobsTask rt = new FindUnfinishedJobsTask();
            rt.run(this.configuration);
        }

        final CheckTopologyTask mt = new CheckTopologyTask(this.configuration, this.queueConfigManager);
        mt.run(topologyCapabilities, !isConfigChange, isConfigChange);

        if ( !isConfigChange ) {
            // start listeners
            this.notifiyListeners();
        }
    }

    private void notifiyListeners() {
        for(final TopologyAware l : this.listeners) {
            l.topologyChanged(this.topologyCapabilities);
        }
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
               this.stopProcessing(true);

            } else if ( event.getType() == Type.TOPOLOGY_INIT
                || event.getType() == Type.TOPOLOGY_CHANGED
                || event.getType() == Type.PROPERTIES_CHANGED ) {

                this.stopProcessing(true);

                this.startProcessing(event.getType(), new TopologyCapabilities(event.getNewView(), this.configuration), false);
            }

        }
    }

    /**
     * Add a topology aware listener
     * @param service Listener to notify about changes.
     */
    public void addListener(final TopologyAware service) {
        synchronized ( this.listeners ) {
            this.listeners.add(service);
            service.topologyChanged(this.topologyCapabilities);
        }
    }

    /**
     * Remove a topology aware listener
     * @param service Listener to notify about changes.
     */
    public void removeListener(final TopologyAware service) {
        synchronized ( this.listeners )  {
            this.listeners.remove(service);
        }
    }
}
