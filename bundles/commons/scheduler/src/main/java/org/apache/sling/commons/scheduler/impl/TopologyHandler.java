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
package org.apache.sling.commons.scheduler.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Optional service - if the Sling discovery service is running, additional features
 *                    are available
 */
@Component(
    service = TopologyEventListener.class,
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
public class TopologyHandler implements TopologyEventListener {

    @Activate
    public void activate() {
        QuartzJobExecutor.DISCOVERY_AVAILABLE.set(true);
    }

    @Deactivate
    public void deactivate() {
        QuartzJobExecutor.DISCOVERY_AVAILABLE.set(false);
    }

    /**
     * @see org.apache.sling.discovery.TopologyEventListener#handleTopologyEvent(org.apache.sling.discovery.TopologyEvent)
     */
    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if ( event.getType() == Type.TOPOLOGY_INIT || event.getType() == Type.TOPOLOGY_CHANGED ) {
            final List<String> ids = new ArrayList<>();
            for(final InstanceDescription desc : event.getNewView().getInstances()) {
                ids.add(desc.getSlingId());
            }
            Collections.sort(ids);
            QuartzJobExecutor.IS_LEADER.set(event.getNewView().getLocalInstance().isLeader());
            QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.set(true);
            QuartzJobExecutor.SLING_IDS.set(ids.toArray(new String[ids.size()]));
        } else if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
            QuartzJobExecutor.IS_LEADER.set(false);
            QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.set(false);
            QuartzJobExecutor.SLING_IDS.set(null);
        }
    }
}
