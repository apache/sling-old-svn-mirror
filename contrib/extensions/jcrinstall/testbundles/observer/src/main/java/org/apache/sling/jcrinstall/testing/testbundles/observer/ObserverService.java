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
package org.apache.sling.jcrinstall.testing.testbundles.observer;

import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Dummy service for jcrinstall integration testing - listens to
 * 	repository and framework events and logs them.
 * 
 * @scr.component 
 *  label="jcrinstall test ObserverService" 
 *  description="Listens to repository and framework events and logs them"
 *  immediate="true"
 *  @scr.service
 *  @scr.property 
 *      name="service.description" 
 *      value="Sling jcrinstall Test ObserverService"
 *  @scr.property 
 *      name="service.vendor" 
 *      value="The Apache Software Foundation"
*/
public class ObserverService implements EventListener, FrameworkListener {
	/** @scr.reference */
	private SlingRepository repository;
	
	private Session session;
    private final Logger log = LoggerFactory.getLogger(getClass());
	
    protected void activate(ComponentContext context) throws Exception {
    	context.getBundleContext().addFrameworkListener(this);
    	session = repository.loginAdministrative(repository.getDefaultWorkspace());
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED 
        	| Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
		final boolean isDeep = true;
		final boolean noLocal = true;
		final String path = "/content";
		session.getWorkspace().getObservationManager().addEventListener(
				this, eventTypes, path,
		        isDeep, null, null, noLocal);
    }

    protected void deactivate(ComponentContext context) throws Exception {
    	context.getBundleContext().removeFrameworkListener(this);
    	if(session != null) {
    		session.getWorkspace().getObservationManager().removeEventListener(this);
    		session.logout();
    		session = null;
    	}
    }

	public void onEvent(EventIterator it) {
		log.debug("onEvent()");
	}

	public void frameworkEvent(FrameworkEvent e) {
		log.debug("FrameworkEvent of type {}", e.getType());
	}
}
