/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.sling.component.Component;
import org.apache.sling.content.jcr.JcrContentManager;
import org.apache.sling.content.jcr.JcrContentManagerFactory;
import org.apache.sling.core.components.AbstractRepositoryComponent;
import org.apache.sling.jcr.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>RepositoryComponentRegistration</code> TODO
 *
 * @scr.component immediate="true" label="%registry.name" description="%registry.description"
 * @scr.property name="service.description" value="Registration Support for Repository Based Components"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="event.topics" value="org/apache/sling/jcr/ContentEvent/*" private="true"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.reference name="repository" interface="org.apache.sling.core.jcr.SlingRepository"
 */
public class RepositoryComponentRegistration implements EventListener,
        EventHandler {

    /** The node path of a component loaded from the repository */
    public static final String COMPONENT_PATH = "org.apache.sling.core.components.path";

    /** The fully qualified name of the component class */
    public static final String COMPONENT_SOURCE = "org.apache.sling.core.components.source";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(RepositoryComponentRegistration.class);

    private ComponentContext componentContext;

    private Session session;

    /**
     * @scr.reference
     */
    private JcrContentManagerFactory contentManagerFactory;

    // Map of registered components, indexed by component ID, value is
    // ServiceRegistration, used for unregistration
    private Map registeredComponents = new HashMap();

    // ---------- EventListener ------------------------------------------------

    public void onEvent(EventIterator eventIterator) {
        String STATE_ADDED = "added";
        String STATE_UPDATED = "updated";
        String STATE_REMOVED = "removed";

        synchronized ( this.registeredComponents ) {
            Map states = new TreeMap();
            while (eventIterator.hasNext()) {
                Event event = eventIterator.nextEvent();
                try {
                    String path = event.getPath();

                    if (this.registeredComponents.containsKey(path)) {
                        if (event.getType() == Event.NODE_ADDED) {
                            Object state = states.get(path);
                            if (state == STATE_REMOVED) {
                                state = STATE_UPDATED;
                            } else if (state != STATE_UPDATED) {
                                state = STATE_ADDED;
                            }
                            states.put(path, state);
                        } else if (event.getType() == Event.NODE_REMOVED) {
                            states.put(path, STATE_REMOVED);
                        }
                    } else {
                        String cPath = null;
                        if (path.endsWith("/sling:contentClass")) {
                            cPath = path.substring(0, path.length()
                                - "/sling:contentClass".length());
                        } else if (path.endsWith("/sling:baseComponent")) {
                            cPath = path.substring(0, path.length()
                                - "/sling:baseComponent".length());
                        } else if (path.endsWith("/sling:scripts")) {
                            cPath = path.substring(0, path.length()
                                - "/sling:scripts".length());
                        } else if (path.indexOf("/sling:scripts/") > 0) {
							cPath = path.substring(0, path
									.indexOf("/sling:scripts/"));
						} else if (path.endsWith("/sling:extensions")) {
							cPath = path.substring(0, path.length()
									- "/sling:extensions".length());
						} else if (path.indexOf("/sling:extensions") > 0) {
							cPath = path.substring(0, path
									.indexOf("/sling:extensions"));
						} else {
                            // no match, ignore
                        }

                        if (cPath != null) {
                            if (this.registeredComponents.containsKey(cPath)) {
                                states.put(cPath, STATE_UPDATED);
                            } else {
                                states.put(cPath, STATE_ADDED);
                            }
                        }
                    }

                    // String type;
                    // switch (event.getType()) {
                    // case Event.NODE_ADDED:
                    // type = "[Node Added]";
                    // break;
                    // case Event.NODE_REMOVED:
                    // type = "[Node Removed]";
                    // break;
                    // case Event.PROPERTY_ADDED:
                    // type = "[Property Added]";
                    // break;
                    // case Event.PROPERTY_CHANGED:
                    // type = "[Property Changed]";
                    // break;
                    // case Event.PROPERTY_REMOVED:
                    // type = "[Property Removed]";
                    // break;
                    // default:
                    // type = "[Unknown: " + event.getType() + "]";
                    // }
                    // log.info("onEvent: Got event {} for {}", type, path);
                } catch (RepositoryException re) {
                    log.warn("Cannot handle event", re);
                }
            }

            // handle
            if (states.isEmpty()) {
                // nothing to do, return
                return;
            }

            // try to register COmponents
            JcrContentManager cMgr = this.getContentManager();
            for (Iterator ei = states.entrySet().iterator(); ei.hasNext();) {
                Map.Entry entry = (Map.Entry) ei.next();
                String path = (String) entry.getKey();

                // unregister if removed or updated
                if (entry.getValue() == STATE_REMOVED
                    || entry.getValue() == STATE_UPDATED) {
                    ServiceRegistration sr = (ServiceRegistration) this.registeredComponents.remove(path);
                    if (sr != null) {
                        sr.unregister();
                    }
                }

                // register if updated or added
                if (entry.getValue() == STATE_UPDATED
                    || entry.getValue() == STATE_ADDED) {
                    Object componentObject;
                    try {
                        componentObject = cMgr.getObject(path);
                    } catch (JcrMappingException jme) {
                        log.info("Cannot load object from {}: {}", path, jme);
                        log.debug("dump", jme);
                        continue;
                    } catch (Throwable t) {
                        log.warn("Unexpect problem mapping " + path
                            + " (Probably unknown mapping)", t);
                        continue;
                    }

                    if (!(componentObject instanceof AbstractRepositoryComponent)) {
                        log.debug("Ignoring mapped object {}", componentObject);
                        continue;
                    }

                    AbstractRepositoryComponent component = (AbstractRepositoryComponent) componentObject;

                    Dictionary props = new Hashtable();
                    props.put(Constants.SERVICE_PID, component.getId());
                    props.put(COMPONENT_SOURCE, this.getClass().getName());
                    props.put(COMPONENT_PATH, component.getPath());

                    ServiceRegistration sr = this.componentContext.getBundleContext().registerService(
                        Component.class.getName(), component, props);
                    this.registeredComponents.put(component.getPath(), sr);
                }
            }
        }
    }

    // ---------- EventHandler -------------------------------------------------

    public void handleEvent(final org.osgi.service.event.Event event) {
        new Thread() {
            public void run() {
                RepositoryComponentRegistration.this.handleEventInternal(event);
            }
        }.start();
    }

    private void handleEventInternal(org.osgi.service.event.Event event) {
        if (org.apache.sling.content.jcr.Constants.EVENT_MAPPING_ADDED.equals(event.getTopic())) {
            // rerun registration
            this.registerComponents();

        } else if (org.apache.sling.content.jcr.Constants.EVENT_MAPPING_REMOVED.equals(event.getTopic())) {
            // remove components whose class is not in the mapped class set
            String[] values = (String[]) event.getProperty(org.apache.sling.content.jcr.Constants.MAPPING_CLASS);
            Set classes = (values == null || values.length == 0)
                    ? Collections.EMPTY_SET
                    : new HashSet(Arrays.asList(values));

            synchronized ( this.registeredComponents ) {
                for (Iterator ri = this.registeredComponents.values().iterator(); ri.hasNext();) {
                    ServiceRegistration sr = (ServiceRegistration) ri.next();
                    String className = (String) sr.getReference().getProperty(
                        COMPONENT_SOURCE);
                    if (classes.contains(className)) {
                        // still mapped, ignore this
                        continue;
                    }

                    // the service is not mapped anymore, unregister and remove
                    log.debug("Unregistering Component {}",
                        sr.getReference().getProperty(COMPONENT_PATH));
                    sr.unregister();
                    ri.remove();
                }
            }
        }
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;

        this.registerComponents();

        try {
            this.session.getWorkspace().getObservationManager().addEventListener(
                this,
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                    | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/",
                true, null, null, true);
        } catch (RepositoryException re) {
            log.error("Cannot register RepositoryComponentRegistration as observation listener");
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        try {
            this.session.getWorkspace().getObservationManager().removeEventListener(
                this);
        } catch (RepositoryException re) {
            log.error("Cannot unregister RepositoryComponentRegistration as observation listener");
        }

        this.unregisterComponents();

        this.dropSession();
        this.componentContext = null;
    }

    protected void bindRepository(SlingRepository repository) {
        try {
            this.session = repository.loginAdministrative(null);
        } catch (RepositoryException re) {
            log.error("Cannot get Repository Session", re);
        }
    }

    protected void unbindRepository(SlingRepository repository) {
        this.dropSession();
    }

    protected void bindContentManagerFactory(
            JcrContentManagerFactory contentManagerFactory) {
        this.contentManagerFactory = contentManagerFactory;
    }

    protected void unbindContentManagerFactory(
            JcrContentManagerFactory contentManagerFactory) {
        this.contentManagerFactory = null;
    }

    // ---------- internal -----------------------------------------------------

    private void registerComponents() {
        JcrContentManager cMgr = this.getContentManager();
        Filter filter = cMgr.getQueryManager().createFilter(
            AbstractRepositoryComponent.class);
        Query query = cMgr.getQueryManager().createQuery(filter);
        synchronized ( this.registeredComponents ) {
            Iterator oi = cMgr.getObjectIterator(query);
            while (oi.hasNext()) {
                // load the components and register
                Object componentObject = oi.next();
                if (!(componentObject instanceof AbstractRepositoryComponent)) {
                    log.debug("Ignoring mapped object {}", componentObject);
                    continue;
                }

                AbstractRepositoryComponent component = (AbstractRepositoryComponent) componentObject;

                // prevent double registration of the same component
                if (this.registeredComponents.containsKey(component.getPath())) {
                    log.debug("Component {} is already registered",
                        component.getPath());
                    continue;
                }

                Dictionary props = new Hashtable();
                props.put(Constants.SERVICE_PID, component.getId());
                props.put(COMPONENT_SOURCE, this.getClass().getName());
                props.put(COMPONENT_PATH, component.getPath());

                ServiceRegistration sr = this.componentContext.getBundleContext().registerService(
                    Component.class.getName(), component, props);
                this.registeredComponents.put(component.getPath(), sr);
            }
        }
    }

    private void unregisterComponents() {
        final Map components;
        synchronized ( this.registeredComponents ) {
            components = new HashMap(this.registeredComponents);
            this.registeredComponents.clear();
        }
        for (Iterator ci = components.values().iterator(); ci.hasNext();) {
            ServiceRegistration sr = (ServiceRegistration) ci.next();
            sr.unregister();
        }
        components.clear();

        // ServiceReference[] sr =
        // componentContext.getBundleContext().getBundle().getRegisteredServices();
        // if (sr != null) {
        // log.warn("Not all services unregistered....");
        // }
    }

    private JcrContentManager getContentManager() {
        if (this.contentManagerFactory == null) {
            throw new IllegalStateException(
                "Cannot get Content Manager Factory");
        }
        return this.contentManagerFactory.getContentManager(this.session);
    }

    private synchronized void dropSession() {
        if (this.session != null) {
            this.session.logout();
            this.session = null;
        }
    }
}
