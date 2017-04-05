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
package org.apache.sling.discovery.base.commons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The heartbeat handler is responsible and capable of issuing both local and
 * remote heartbeats and registers a periodic job with the scheduler for doing so.
 * <p>
 * Local heartbeats are stored in the repository. Remote heartbeats are POSTs to
 * remote TopologyConnectorServlets.
 */
public abstract class BaseViewChecker implements ViewChecker, Runnable {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Endpoint service registration property from RFC 189 */
    private static final String REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    protected static final String PROPERTY_ID_ENDPOINTS = "endpoints";

    protected static final String PROPERTY_ID_SLING_HOME_PATH = "slingHomePath";

    protected static final String PROPERTY_ID_RUNTIME = "runtimeId";

    /** the name used for the period job with the scheduler **/
    protected String NAME = "discovery.impl.heartbeat.runner.";

    /** the sling id of the local instance **/
    protected String slingId;
    
    /** SLING-2901: the runtimeId is a unique id, set on activation, used for robust duplicate sling.id detection **/
    protected String runtimeId;

    /** lock object for synchronizing the run method **/
    protected final Object lock = new Object();

    /** SLING-2895: avoid heartbeats after deactivation **/
    protected volatile boolean activated = false;

    /** keep a reference to the component context **/
    protected ComponentContext context;

    /** SLING-3382 : force ping instructs the servlet to start the backoff from scratch again **/
    private boolean forcePing;

    /** SLING-4765 : store endpoints to /clusterInstances for more verbose duplicate slingId/ghost detection **/
    protected final Map<Long, String[]> endpoints = new HashMap<Long, String[]>();

    protected PeriodicBackgroundJob periodicPingJob;
    
    protected abstract SlingSettingsService getSlingSettingsService();

    protected abstract ResourceResolverFactory getResourceResolverFactory();

    protected abstract ConnectorRegistry getConnectorRegistry();

    protected abstract AnnouncementRegistry getAnnouncementRegistry();

    protected abstract Scheduler getScheduler();

    protected abstract BaseConfig getConnectorConfig();

    @Activate
    protected void activate(ComponentContext context) {
    	synchronized(lock) {
    		this.context = context;

	        slingId = getSlingSettingsService().getSlingId();
	        NAME = "discovery.connectors.common.runner." + slingId;

	        doActivate();
	        activated = true;
            issueHeartbeat();
    	}
    }

    protected void doActivate() {
        try {
            final long interval = getConnectorConfig().getConnectorPingInterval();
            logger.info("doActivate: starting periodic connectorPing job for "+slingId+" with interval "+interval+" sec.");
            periodicPingJob = new PeriodicBackgroundJob(interval, NAME, this);
        } catch (Exception e) {
            logger.error("doActivate: Could not start connectorPing runner: " + e, e);
        }
        logger.info("doActivate: activated with slingId: {}, this: {}", slingId, this);
    }

    @Deactivate
    protected void deactivate() {
        // SLING-3365 : dont synchronize on deactivate
        activated = false;
        logger.info("deactivate: deactivated slingId: {}, this: {}", slingId, this);
        if (periodicPingJob != null) {
            periodicPingJob.stop();
            periodicPingJob = null;
        }
    }
    
    /** for testing only **/
    @Override
    public void checkView() {
        synchronized(lock) {
            doCheckView();
        }
    }
    
    public void run() {
        heartbeatAndCheckView();
    }
    
    @Override
    public void heartbeatAndCheckView() {
        logger.debug("heartbeatAndCheckView: start. [for slingId="+slingId+"]");
        synchronized(lock) {
        	if (!activated) {
        		// SLING:2895: avoid heartbeats if not activated
        	    logger.debug("heartbeatAndCheckView: not activated yet");
        		return;
        	}

            // issue a heartbeat
            issueHeartbeat();

            // check the view
            doCheckView();
        }
        logger.debug("heartbeatAndCheckView: end. [for slingId="+slingId+"]");
    }

    /** Trigger the issuance of the next heartbeat asap instead of at next heartbeat interval **/
    public void triggerAsyncConnectorPing() {
        forcePing = true;
        try {
            // then fire a job immediately
            // use 'fireJobAt' here, instead of 'fireJob' to make sure the job can always be triggered
            // 'fireJob' checks for a job from the same job-class to already exist
            // 'fireJobAt' though allows to pass a name for the job - which can be made unique, thus does not conflict/already-exist
            logger.info("triggerAsyncConnectorPing: firing job to trigger heartbeat");
            getScheduler().fireJobAt(NAME+UUID.randomUUID(), this, null, new Date(System.currentTimeMillis()-1000 /* make sure it gets triggered immediately*/));
        } catch (Exception e) {
            logger.info("triggerAsyncConnectorPing: Could not trigger heartbeat: " + e);
        }
    }
    
    /**
     * Issue a heartbeat.
     * <p>
     * This action consists of first updating the local properties,
     * then issuing a cluster-local heartbeat (within the repository)
     * and then a remote heartbeat (to all the topology connectors
     * which announce this part of the topology to others)
     */
    protected void issueHeartbeat() {
        updateProperties();

        issueConnectorPings();
    }

    protected abstract void updateProperties();

    /** Issue a remote heartbeat using the topology connectors **/
    protected void issueConnectorPings() {
        if (getConnectorRegistry() == null) {
            logger.error("issueConnectorPings: connectorRegistry is null");
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("issueConnectorPings: pinging outgoing topology connectors (if there is any) for "+slingId);
        }
        getConnectorRegistry().pingOutgoingConnectors(forcePing);
        forcePing = false;
    }

    /** Check whether the established view matches the reality, ie matches the
     * heartbeats
     */
    protected void doCheckView() {
        // check the remotes first
        if (getAnnouncementRegistry() == null) {
            logger.error("announcementRegistry is null");
            return;
        }
        getAnnouncementRegistry().checkExpiredAnnouncements();
    }

    /**
     * Bind a http service
     */
    protected void bindHttpService(final ServiceReference reference) {
        final String[] endpointUrls = toStringArray(reference.getProperty(REG_PROPERTY_ENDPOINTS));
        if ( endpointUrls != null ) {
            synchronized ( lock ) {
                this.endpoints.put((Long)reference.getProperty(Constants.SERVICE_ID), endpointUrls);
            }
        }
    }

    /**
     * Unbind a http service
     */
    protected void unbindHttpService(final ServiceReference reference) {
        synchronized ( lock ) {
            if ( this.endpoints.remove(reference.getProperty(Constants.SERVICE_ID)) != null ) {
            }
        }
    }
    
    private String[] toStringArray(final Object propValue) {
        if (propValue == null) {
            // no value at all
            return null;

        } else if (propValue instanceof String) {
            // single string
            return new String[] { (String) propValue };

        } else if (propValue instanceof String[]) {
            // String[]
            return (String[]) propValue;

        } else if (propValue.getClass().isArray()) {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray) {
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        } else if (propValue instanceof Collection<?>) {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection) {
                if (value != null) {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return null;
    }
    
    protected String getEndpointsAsString() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(final String[] points : endpoints.values()) {
            for(final String point : points) {
                if ( first ) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(point);
            }
        }
        return sb.toString();
        
    }

}
