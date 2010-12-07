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
package org.apache.sling.slingbucks.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Move confirmed orders under CONFIRMED_ORDERS_PATH, 
 *  by observing changes under ORDERS_PATH
 */
@Component
public class ConfirmedOrdersObserver implements EventListener, Runnable {
    private Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private SlingRepository repository;
    
    private Session session;
    private ObservationManager observationManager;
    
    private Set<String> changedPropertyPaths = new HashSet<String>();
    private static final long WAIT_AFTER_LAST_CHANGE_MSEC = 5000;
    private boolean running;
    
    protected void activate(ComponentContext context)  throws Exception {
        session = repository.loginAdministrative(null);
        running = true;
        
        final Thread t = new Thread(this, getClass().getName());
        t.setDaemon(true);
        t.start();
        
        // Listen for changes to our orders
        if (repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED).equals("true")) {
            observationManager = session.getWorkspace().getObservationManager();
            final String[] types = { "nt:unstructured" };
            final boolean isDeep = true;
            final boolean noLocal = true;
            final String path = SlingbucksConstants.ORDERS_PATH;
            observationManager.addEventListener(this, Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED, path, isDeep, null, types, noLocal);
            log.info("Observing property changes to {} nodes under {}", Arrays.asList(types), path);
        }
        
    }

    protected void deactivate(ComponentContext componentContext) throws RepositoryException {
        running = false;
        if(observationManager != null) {
            observationManager.removeEventListener(this);
        }
        if (session != null) {
            session.logout();
            session = null;
        }
    }

    public void onEvent(EventIterator it) {
        while (it.hasNext()) {
            // Accumulate the changed paths and store time of the last change event
            try {
                final String path = it.nextEvent().getPath();
                if(path.endsWith(SlingbucksConstants.CONFIRMED_ORDER_PROPERTY_NAME)) {
                    synchronized (changedPropertyPaths) {
                        log.debug("onEvent: property {} changed", path);
                        changedPropertyPaths.add(path);
                    }
                }
            } catch(Exception e) {
                log.warn(e.getClass().getName() + " in onEvent", e);
            }
        }
    }

    private void runOneCycle() throws Exception {
        if(changedPropertyPaths.isEmpty()) {
            return;
        }
        
        final List<String> paths = new ArrayList<String>();
        final List<String> toRetry = new ArrayList<String>();
        synchronized (changedPropertyPaths) {
            paths.addAll(changedPropertyPaths);
            changedPropertyPaths.clear();
        }
        
        try {
            while(!paths.isEmpty()) {
                final String path = paths.remove(0);
                if(session.itemExists(path)) {
                    final Item it = session.getItem(path);
                    if(!it.isNode()) {
                        final Property p = (Property)it;
                        final Node n = p.getParent();
                        if(!n.hasProperty(SlingbucksConstants.LAST_MODIFIED_PROPERTY_NAME)) {
                            log.debug("Node {} doesn't have property {}, ignored", n.getPath(), SlingbucksConstants.LAST_MODIFIED_PROPERTY_NAME);
                        } else {
                            Calendar lastMod = n.getProperty(SlingbucksConstants.LAST_MODIFIED_PROPERTY_NAME).getDate();
                            if(System.currentTimeMillis() - lastMod.getTime().getTime() < WAIT_AFTER_LAST_CHANGE_MSEC) {
                                log.debug("Node {} modified more recently than {} msec, ignored", n.getPath(), WAIT_AFTER_LAST_CHANGE_MSEC);
                                toRetry.add(path);
                            } else {
                                final String targetPath = SlingbucksConstants.CONFIRMED_ORDERS_PATH + "/" + n.getName();
                                session.getWorkspace().move(n.getPath(), targetPath);
                                log.info("Confirmed order node {} moved to {}", n.getPath(), targetPath);
                            }
                        }
                    }
                }
            }
        } finally {
            // Re-add any paths that we didn't process
            synchronized (changedPropertyPaths) {
                changedPropertyPaths.addAll(paths);
                changedPropertyPaths.addAll(toRetry);
            }
        }
    }

    public void run() {
        log.info("Background thread {} starting", Thread.currentThread().getName());
        while(running) {
            try {
                runOneCycle();
            } catch(Exception e) {
                log.warn(e.getClass().getName() + " in background thread", e);
            }
            
            try {
                Thread.sleep(1000L);
            } catch(InterruptedException ignore) {
            }
        } 
        log.info("Background thread {} done", Thread.currentThread().getName());
    }
}
