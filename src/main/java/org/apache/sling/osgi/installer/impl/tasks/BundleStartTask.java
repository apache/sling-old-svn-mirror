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
package org.apache.sling.osgi.installer.impl.tasks;

import java.text.DecimalFormat;

import org.apache.sling.osgi.installer.impl.EventsCounter;
import org.apache.sling.osgi.installer.impl.OsgiControllerTask;
import org.apache.sling.osgi.installer.impl.OsgiControllerTaskContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/** Task that starts a bundle */
public class BundleStartTask extends OsgiControllerTask {

	private final long bundleId;
	private final String sortKey;
	private long eventsCountForRetrying;
	private int retryCount = 0;
	
	public BundleStartTask(long bundleId) {
		this.bundleId = bundleId;
		sortKey = TaskOrder.BUNDLE_START_ORDER + new DecimalFormat("00000").format(bundleId); 
	}
	
	@Override
	public String getSortKey() {
		return sortKey; 
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (bundle " + bundleId + ")";
	}

	public void execute(OsgiControllerTaskContext tctx) throws Exception {
		final Bundle b = tctx.getBundleContext().getBundle(bundleId);
		final LogService log = tctx.getOsgiControllerServices().getLogService();
		boolean needToRetry = false;
		
		if(b == null) {
			if(log != null) {
				log.log(LogService.LOG_INFO, "Cannot start bundle, id not found:" + bundleId);
			}
			return;
		}
		
		try {
	        if(b.getState() == Bundle.ACTIVE) {
	            if(log != null) {
	                log.log(LogService.LOG_DEBUG, "Bundle already started, no action taken:" + bundleId + "/" + b.getSymbolicName());
	            }
	        } else {
	            // Try to start bundle, and if that doesn't work we'll need to retry
	            try {
	                b.start();
	                if(log != null) {
	                    log.log(LogService.LOG_INFO, 
	                            "Bundle started (retry count=" + retryCount + ", bundle ID=" + bundleId + ") " + b.getSymbolicName());
	                }
	            } catch(BundleException e) {
	                if(log != null) {
	                    log.log(LogService.LOG_INFO, 
	                            "Could not start bundle (retry count=" + retryCount + ", " + e 
	                            + "), will retry: " + bundleId + "/" + b.getSymbolicName());
	                }
	                needToRetry = true;
	            }
	            
	        }
		} finally {
	        if(needToRetry) {
	            
	            // Do the first retry immediately (in case "something" happenened right now
	            // that warrants a retry), but for the next ones wait for at least one bundle
	            // event or framework event
	            if(retryCount == 0) {
	                eventsCountForRetrying = getEventsCount(tctx.getBundleContext());
	            } else {
                    eventsCountForRetrying = getEventsCount(tctx.getBundleContext()) + 1;
	            }
	            
	            tctx.addTaskToNextCycle(this);
	        }
		}
		retryCount++;
	}
	
	/** Do not execute this task if waiting for events */
    public boolean isExecutable(OsgiControllerTaskContext tctx) {
        final long eventsCount = getEventsCount(tctx.getBundleContext()); 
        final boolean result = eventsCount >= eventsCountForRetrying; 
        if(!result) {
            if(tctx.getOsgiControllerServices().getLogService() != null) {
                tctx.getOsgiControllerServices().getLogService().log(LogService.LOG_DEBUG, 
                        this + " is not executable at this time, counters=" + eventsCountForRetrying + "/" + eventsCount);
            }
        }
        return result;
    }
    
    /** Return current events count */
    protected long getEventsCount(BundleContext bc) {
        final ServiceReference sr = bc.getServiceReference(EventsCounter.class.getName());
        if(sr == null) {
            throw new IllegalStateException("EventsCounter service not found");
        }
        final EventsCounter ec = (EventsCounter)bc.getService(sr);
        if(ec == null) {
            throw new IllegalStateException("EventsCounter service not found, although its ServiceReference was found");
        }
        return ec.getTotalEventsCount();
    }
}
