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
package org.apache.sling.superimposing.impl;

import static org.apache.sling.superimposing.SuperimposingResourceProvider.MIXIN_SUPERIMPOSE;
import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_OVERLAYABLE;
import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_REGISTER_PARENT;
import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_SOURCE_PATH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.superimposing.SuperimposingManager;
import org.apache.sling.superimposing.SuperimposingResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the resource registrations for the {@link SuperimposingResourceProviderImpl}.
 * Provides read-only access to all registered providers.
 */
@Component(label = "Apache Sling Superimposing Resource Manager",
    description = "Manages the resource registrations for the Superimposing Resource Provider.",
    immediate = true, metatype = true)
@Service(SuperimposingManager.class)
public class SuperimposingManagerImpl implements SuperimposingManager, EventListener {

    @Property(label = "Enabled", description = "Enable/Disable the superimposing functionality.", boolValue = SuperimposingManagerImpl.ENABLED_DEFAULT)
    static final String ENABLED_PROPERTY = "enabled";
    static final boolean ENABLED_DEFAULT = false;
    private boolean enabled;

    @Property(label = "Find all Queries", description = "List of query expressions to find all existing superimposing registrations on service startup. "
            + "Query syntax is depending on underlying resource provdider implementation. Prepend the query with query syntax name separated by \"|\".",
            value={SuperimposingManagerImpl.FINDALLQUERIES_DEFAULT}, unbounded=PropertyUnbounded.ARRAY)
    static final String FINDALLQUERIES_PROPERTY = "findAllQueries";
    static final String FINDALLQUERIES_DEFAULT = "JCR-SQL2|SELECT * FROM [" + MIXIN_SUPERIMPOSE + "] WHERE ISDESCENDANTNODE('/content')";
    private String[] findAllQueries;

    @Property(label = "Obervation paths", description = "List of paths that should be monitored for resource events to detect superimposing content nodes.",
            value={SuperimposingManagerImpl.OBSERVATION_PATHS_DEFAULT}, unbounded=PropertyUnbounded.ARRAY)
    static final String OBSERVATION_PATHS_PROPERTY = "obervationPaths";
    static final String OBSERVATION_PATHS_DEFAULT = "/content";
    private String[] obervationPaths;
    private EventListener[] observationEventListeners;

    /**
     * Map for holding the superimposing mappings, with the superimpose path as key and the providers as values
     */
    private ConcurrentMap<String, SuperimposingResourceProviderImpl> superimposingProviders = new ConcurrentHashMap<String, SuperimposingResourceProviderImpl>();

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Administrative resource resolver (read only usage)
     */
    private ResourceResolver resolver;

    /**
     * A reference to the initialization task. Needed to check if
     * initialization has completed.
     */
    Future<?> initialization;

    /**
     * This bundle's context.
     */
    private BundleContext bundleContext;

    /**
     * The default logger
     */
    private static final Logger log = LoggerFactory.getLogger(SuperimposingManagerImpl.class);


    /**
     * Find all existing superimposing registrations using all query defined in service configuration.
     * @param resolver Resource resolver
     * @return All superimposing registrations
     */
    @SuppressWarnings("unchecked")
    private List<Resource> findSuperimposings(ResourceResolver resolver) {
        List<Resource> allResources = new ArrayList<Resource>();
        for (String queryString : this.findAllQueries) {
            if (!StringUtils.contains(queryString, "|")) {
                throw new IllegalArgumentException("Query string does not contain query syntax seperated by '|': " + queryString);
            }
            String queryLanguage = StringUtils.substringBefore(queryString, "|");
            String query = StringUtils.substringAfter(queryString, "|");
            allResources.addAll(IteratorUtils.toList(resolver.findResources(query, queryLanguage)));
        }
        return allResources;
    }

    private void registerAllSuperimposings() {
        log.debug("Start registering all superimposing trees...");
        final long start = System.currentTimeMillis();
        long countSuccess = 0;
        long countFailed = 0;

        final List<Resource> existingSuperimposings = findSuperimposings(resolver);
        for (Resource superimposingResource : existingSuperimposings) {
            boolean success = registerProvider(superimposingResource);
            if (success) {
                countSuccess++;
            } else {
                countFailed++;
            }
        }

        final long time = System.currentTimeMillis() - start;
        log.info("Registered {} SuperimposingResourceProvider(s) in {} ms, skipping {} invalid one(s).",
                new Object[] { countSuccess, time, countFailed });
    }

    /**
     * @param superimposingResource
     * @return true if registration was done, false if skipped (already registered)
     * @throws RepositoryException
     */
    private boolean registerProvider(Resource superimposingResource) {
        String superimposePath = superimposingResource.getPath();

        // use JCR API to get properties from superimposing resource to make sure superimposing does not delivery values from source node
        final String sourcePath = getJcrStringProperty(superimposePath, PROP_SUPERIMPOSE_SOURCE_PATH);
        final boolean registerParent = getJcrBooleanProperty(superimposePath, PROP_SUPERIMPOSE_REGISTER_PARENT);
        final boolean overlayable = getJcrBooleanProperty(superimposePath, PROP_SUPERIMPOSE_OVERLAYABLE);

        // check if superimposing definition is valid
        boolean valid = true;
        if (StringUtils.isBlank(sourcePath)) {
            valid = false;
        }
        else {
            // check whether the parent of the node should be registered as superimposing provider
            if (registerParent) {
                superimposePath = ResourceUtil.getParent(superimposePath);
            }
            // target path is not valid if it equals to a parent or child of the superimposing path, or to the superimposing path itself
            if (StringUtils.equals(sourcePath, superimposePath)
                    || StringUtils.startsWith(sourcePath, superimposePath + "/")
                    || StringUtils.startsWith(superimposePath, sourcePath + "/")) {
                valid = false;
            }
        }

        // register valid superimposing
        if (valid) {
            final SuperimposingResourceProviderImpl srp = new SuperimposingResourceProviderImpl(superimposePath, sourcePath, overlayable);
            final SuperimposingResourceProviderImpl oldSrp = superimposingProviders.put(superimposePath, srp);

            // unregister in case there was a provider registered before
            if (!srp.equals(oldSrp)) {
                log.debug("(Re-)registering resource provider {}.", superimposePath);
                if (null != oldSrp) {
                    oldSrp.unregisterService();
                }
                srp.registerService(bundleContext);
                return true;
            } else {
                log.debug("Skipped re-registering resource provider {} because there were no relevant changes.", superimposePath);
            }
        }

        // otherwise remove previous superimposing resource provider if new superimposing definition is not valid
        else {
            final SuperimposingResourceProviderImpl oldSrp = superimposingProviders.remove(superimposePath);
            if (null != oldSrp) {
                log.debug("Unregistering resource provider {}.", superimposePath);
                oldSrp.unregisterService();
            }
            log.warn("Superimposing definition '{}' pointing to '{}' is invalid.", superimposePath, sourcePath);
        }

        return false;
    }

    private String getJcrStringProperty(String pNodePath, String pPropertName) {
        String absolutePropertyPath = pNodePath + "/" + pPropertName;
        Session session = resolver.adaptTo(Session.class);
        try {
            if (!session.itemExists(absolutePropertyPath)) {
                return null;
            }
            return session.getProperty(absolutePropertyPath).getString();
        }
        catch (RepositoryException ex) {
            return null;
        }
    }

    private boolean getJcrBooleanProperty(String pNodePath, String pPropertName) {
        String absolutePropertyPath = pNodePath + "/" + pPropertName;
        Session session = resolver.adaptTo(Session.class);
        try {
            if (!session.itemExists(absolutePropertyPath)) {
                return false;
            }
            return session.getProperty(absolutePropertyPath).getBoolean();
        }
        catch (RepositoryException ex) {
            return false;
        }
    }

    private void registerProvider(String path) {
        final Resource provider = resolver.getResource(path);
        if (provider != null) {
            registerProvider(provider);
        }
    }

    private void unregisterProvider(String path) {
        final SuperimposingResourceProviderImpl srp = superimposingProviders.remove(path);
        if (null != srp) {
            srp.unregisterService();
        }
    }

    // ---------- SCR Integration

    @Activate
    protected synchronized void activate(final ComponentContext ctx) throws LoginException, RepositoryException {

        // check enabled state
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> props = ctx.getProperties();
        this.enabled = PropertiesUtil.toBoolean(props.get(ENABLED_PROPERTY), ENABLED_DEFAULT);
        log.debug("Config: " + "Enabled={} ", enabled);
        if (!isEnabled()) {
            return;
        }

        // get "find all" queries
        this.findAllQueries = PropertiesUtil.toStringArray(props.get(FINDALLQUERIES_PROPERTY), new String[] { FINDALLQUERIES_DEFAULT });
        this.obervationPaths = PropertiesUtil.toStringArray(props.get(OBSERVATION_PATHS_PROPERTY), new String[] { OBSERVATION_PATHS_DEFAULT });

        if (null == resolver) {
            bundleContext = ctx.getBundleContext();
            resolver = resolverFactory.getAdministrativeResourceResolver(null);

            // Watch for events on the root to register/deregister superimposings at runtime
            // For each observed path create an event listener object which redirects the event to the main class
            final Session session = resolver.adaptTo(Session.class);
            if (session!=null) {
                this.observationEventListeners = new EventListener[this.obervationPaths.length];
                for (int i=0; i<this.obervationPaths.length; i++) {
                    this.observationEventListeners[i] = new EventListener() {
                        public void onEvent(EventIterator events) {
                            SuperimposingManagerImpl.this.onEvent(events);
                        }
                    };
                    session.getWorkspace().getObservationManager().addEventListener(
                            this.observationEventListeners[i],
                            Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                            this.obervationPaths[i], // absolute path
                            true, // isDeep
                            null, // uuids
                            null, // node types
                            true); // noLocal
                }
            }

            // register all superimposing definitions that already exist
            initialization = Executors.newSingleThreadExecutor().submit(new Runnable() {
                public void run() {
                    try {
                        registerAllSuperimposings();
                    }
                    catch (Throwable ex) {
                        log.warn("Error registering existing superimposing resources on service startup.", ex);
                    }
                }
            });
        }
    }

    @Deactivate
    protected synchronized void deactivate(final ComponentContext ctx) throws RepositoryException {
        try {
            // make sure initialization has finished
            if (null != initialization && !initialization.isDone()) {
                initialization.cancel(/* myInterruptIfRunning */ true);
            }

            // de-register JCR observation
            if (resolver!=null) {
                final Session session = resolver.adaptTo(Session.class);
                if (session!=null && this.observationEventListeners!=null) {
                    for (EventListener eventListener : this.observationEventListeners) {
                        session.getWorkspace().getObservationManager().removeEventListener(eventListener);
                    }
                }
            }

            // de-register all superimpsing resource providers
            for (final SuperimposingResourceProviderImpl srp : superimposingProviders.values()) {
                srp.unregisterService();
            }

        } finally {
            if (null != resolver) {
                resolver.close();
                resolver = null;
            }
            initialization = null;
            superimposingProviders.clear();
        }
    }

    /**
     * Handle resource events to add or remove superimposing registrations
     */
    public void onEvent(EventIterator events) {
        if (!isEnabled()) {
            return;
        }
        try {
            // collect all actions to be performed for this event
            final Map<String, Boolean> actions = new HashMap<String, Boolean>();
            boolean nodeAdded = false;
            boolean nodeRemoved = false;
            while (events.hasNext()) {
                final Event event = events.nextEvent();
                final String path = event.getPath();
                final String name = ResourceUtil.getName(path);
                if (event.getType() == Event.NODE_ADDED) {
                    nodeAdded = true;
                } else if (event.getType() == Event.NODE_REMOVED && superimposingProviders.containsKey(path)) {
                    nodeRemoved = true;
                    actions.put(path, false);
                } else if (StringUtils.equals(name, PROP_SUPERIMPOSE_SOURCE_PATH)
                        || StringUtils.equals(name, PROP_SUPERIMPOSE_REGISTER_PARENT)
                        || StringUtils.equals(name, PROP_SUPERIMPOSE_OVERLAYABLE)) {
                    final String nodePath = ResourceUtil.getParent(path);
                    actions.put(nodePath, true);
                }
            }

            // execute all collected actions (having this outside the above
            // loop prevents repeated registrations within one transaction
            // but allows for several superimposings to be added within a single
            // transaction)
            for (Map.Entry<String, Boolean> action : actions.entrySet()) {
                if (action.getValue()) {
                    registerProvider(action.getKey());
                } else {
                    unregisterProvider(action.getKey());
                }
            }

            if (nodeAdded && nodeRemoved) {
                // maybe a superimposing was moved, re-register all superimposings
                // (existing ones will be skipped)
                registerAllSuperimposings();
            }
        } catch (RepositoryException e) {
            log.error("Unexpected repository exception during event processing.");
        }
    }

    /**
     * @return true if superimposing mode is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @return Immutable map with all superimposing resource providers currently registered
     */
    public Map<String, SuperimposingResourceProvider> getRegisteredProviders() {
        Map<String, SuperimposingResourceProvider> mapcopy = new HashMap<String, SuperimposingResourceProvider>(superimposingProviders);
        return Collections.unmodifiableMap(mapcopy);
    }

    SuperimposingManagerImpl withResourceResolverFactory(ResourceResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
        return this;
    }

}
