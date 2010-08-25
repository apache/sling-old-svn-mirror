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
package org.apache.sling.adapter.internal;

import static org.apache.sling.api.adapter.AdapterFactory.ADAPTABLE_CLASSES;
import static org.apache.sling.api.adapter.AdapterFactory.ADAPTER_CLASSES;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * The <code>AdapterManagerImpl</code> class implements the
 * {@link AdapterManager} interface and is registered as a service for that
 * interface to be used by any clients.
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.property name="service.description" value="Sling Adapter Manager"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 * @scr.reference name="AdapterFactory"
 *                interface="org.apache.sling.api.adapter.AdapterFactory"
 *                cardinality="0..n" policy="dynamic"
 */
public class AdapterManagerImpl implements AdapterManager {

    /**
     * The singleton instance of this manager. This field is set when the
     * instance is {@link #activate(ComponentContext) activated} and cleared
     * when the instance is {@link #deactivate(ComponentContext) deactivated}.
     *
     * This field is set to public to make it easier for testing to provide
     * an own adapter manager implementation which can be used together
     * with {@link org.apache.sling.adapter.SlingAdaptable}s. (see SLING-1195).
     * As this class is private this field is not accessible in an OSGi environment!
     */
    public static AdapterManager INSTANCE;

    /**
     * Returns the instance of this class or <code>null</code> if no activate
     * yet.
     */
    public static AdapterManager getInstance() {
        return INSTANCE;
    }

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private LogService log;

    /** Whether to debug this class or not */
    private boolean debug = false;

    /**
     * The OSGi <code>ComponentContext</code> to retrieve
     * {@link AdapterFactory} service instances.
     */
    private volatile ComponentContext context;

    /**
     * A list of {@link AdapterFactory} services bound to this manager before
     * the manager has been activated. These bound services will be accessed as
     * soon as the manager is being activated.
     */
    private List<ServiceReference> boundAdapterFactories = new LinkedList<ServiceReference>();

    /**
     * A map of {@link AdapterFactoryDescriptorMap} instances. The map is
     * indexed by the fully qualified class names listed in the
     * {@link AdapterFactory#ADAPTABLE_CLASSES} property of the
     * {@link AdapterFactory} services.
     *
     * @see AdapterFactoryDescriptorMap
     */
    private Map<String, AdapterFactoryDescriptorMap> factories = new HashMap<String, AdapterFactoryDescriptorMap>();

    /**
     * Matrix of {@link AdapterFactory} instances primarily indexed by the fully
     * qualified name of the class to be adapted and secondarily indexed by the
     * fully qualified name of the class to adapt to (the target class).
     * <p>
     * This cache is built on demand by calling the
     * {@link #getAdapterFactories(Class)} class. It is removed altogether
     * whenever an adapter factory is registered on unregistered.
     */
    private Map<String, Map<String, AdapterFactory>> factoryCache;

    /**
     * The service tracker for the event admin
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private EventAdmin eventAdmin;

    // ---------- AdapterManager interface -------------------------------------

    /**
     * Returns the adapted <code>adaptable</code> or <code>null</code> if
     * the object cannot be adapted.
     */
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {

        // get the adapter factories for the type of adaptable object
        Map<String, AdapterFactory> factories = getAdapterFactories(adaptable.getClass());

        // get the factory for the target type
        AdapterFactory factory = factories.get(type.getName());

        // have the factory adapt the adaptable if the factory exists
        if (factory != null) {
            if (debug) {
                log(LogService.LOG_DEBUG, "Using adapter factory " + factory
                    + " to map " + adaptable + " to " + type, null);
            }

            return factory.getAdapter(adaptable, type);
        }

        // no factory has been found, so we cannot adapt
        if (debug) {
            log(LogService.LOG_DEBUG, "No adapter factory found to map "
                + adaptable + " to " + type, null);
        }

        return null;
    }

    // ----------- SCR integration ---------------------------------------------

    protected void activate(ComponentContext context) {
        this.context = context;

        // register all adapter factories bound before activation
        final List<ServiceReference> refs;
        synchronized ( this.boundAdapterFactories ) {
            refs = new ArrayList<ServiceReference>(this.boundAdapterFactories);
            boundAdapterFactories.clear();
        }
        for (ServiceReference reference : refs) {
            registerAdapterFactory(context, reference);
        }

        // final "enable" this manager by setting the instance
        // do not overwrite the field if already set (this is unexpected
        // actually)
        if (AdapterManagerImpl.INSTANCE == null) {
            AdapterManagerImpl.INSTANCE = this;
        } else {
            log(LogService.LOG_WARNING,
                "Not setting Instance field: Set to another manager "
                    + AdapterManagerImpl.INSTANCE, null);
        }
        SyntheticResource.setAdapterManager(this);
    }

    /**
     * @param context Not used
     */
    protected void deactivate(ComponentContext context) {
        SyntheticResource.unsetAdapterManager(this);
        // "disable" the manager by clearing the instance
        // do not clear the field if not set to this instance
        if (AdapterManagerImpl.INSTANCE == this) {
            AdapterManagerImpl.INSTANCE = null;
        } else {
            log(LogService.LOG_WARNING,
                "Not clearing instance field: Set to another manager "
                    + AdapterManagerImpl.INSTANCE, null);
        }
        this.context = null;
    }

    protected void bindAdapterFactory(ServiceReference reference) {
        boolean create = true;
        if (context == null) {
            synchronized ( this.boundAdapterFactories ) {
                if (context == null) {
                    boundAdapterFactories.add(reference);
                    create = false;
                }
            }
        }
        if ( create ) {
            registerAdapterFactory(context, reference);
        }
    }

    protected void unbindAdapterFactory(ServiceReference reference) {
        unregisterAdapterFactory(reference);
    }

    // ---------- unit testing stuff only --------------------------------------

    /**
     * Returns the active adapter factories of this manager.
     * <p>
     * <strong><em>THIS METHOD IS FOR UNIT TESTING ONLY. IT MAY BE REMOVED OR
     * MODIFIED WITHOUT NOTICE.</em></strong>
     */
    Map<String, AdapterFactoryDescriptorMap> getFactories() {
        return factories;
    }

    /**
     * Returns the current adapter factory cache.
     * <p>
     * <strong><em>THIS METHOD IS FOR UNIT TESTING ONLY. IT MAY BE REMOVED OR
     * MODIFIED WITHOUT NOTICE.</em></strong>
     */
    Map<String, Map<String, AdapterFactory>> getFactoryCache() {
        return factoryCache;
    }

    // ---------- internal -----------------------------------------------------

    private void log(int level, String message, Throwable t) {
        LogService logger = this.log;
        if (logger != null) {
            logger.log(level, message, t);
        } else {
            System.out.println(message);
            if (t != null) {
                t.printStackTrace(System.out);
            }
        }
    }

    /**
     * Unregisters the {@link AdapterFactory} referred to by the service
     * <code>reference</code> from the registry.
     */
    private void registerAdapterFactory(ComponentContext context,
            ServiceReference reference) {
        final String[] adaptables = OsgiUtil.toStringArray(reference.getProperty(ADAPTABLE_CLASSES));
        final String[] adapters = OsgiUtil.toStringArray(reference.getProperty(ADAPTER_CLASSES));

        if (adaptables == null || adaptables.length == 0 || adapters == null
            || adapters.length == 0) {
            return;
        }

        final AdapterFactoryDescriptorKey factoryKey = new AdapterFactoryDescriptorKey(
            reference);
        final AdapterFactoryDescriptor factoryDesc = new AdapterFactoryDescriptor(context,
            reference, adapters);

        synchronized (factories) {
            for (final String adaptable : adaptables) {
                AdapterFactoryDescriptorMap adfMap = factories.get(adaptable);
                if (adfMap == null) {
                    adfMap = new AdapterFactoryDescriptorMap();
                    factories.put(adaptable, adfMap);
                }
                adfMap.put(factoryKey, factoryDesc);
            }
        }

        // clear the factory cache to force rebuild on next access
        factoryCache = null;

        // send event
        final EventAdmin localEA = this.eventAdmin;
        if ( localEA != null ) {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(SlingConstants.PROPERTY_ADAPTABLE_CLASSES, adaptables);
            props.put(SlingConstants.PROPERTY_ADAPTER_CLASSES, adapters);
            localEA.postEvent(new Event(SlingConstants.TOPIC_ADAPTER_FACTORY_ADDED,
                    props));
        }
    }

    /**
     * Unregisters the {@link AdapterFactory} referred to by the service
     * <code>reference</code> from the registry.
     */
    private void unregisterAdapterFactory(ServiceReference reference) {
        synchronized ( this.boundAdapterFactories ) {
            boundAdapterFactories.remove(reference);
        }
        final String[] adaptables = OsgiUtil.toStringArray(reference.getProperty(ADAPTABLE_CLASSES));
        final String[] adapters = OsgiUtil.toStringArray(reference.getProperty(ADAPTER_CLASSES));

        if (adaptables == null || adaptables.length == 0 || adapters == null
            || adapters.length == 0) {
            return;
        }

        AdapterFactoryDescriptorKey factoryKey = new AdapterFactoryDescriptorKey(
            reference);

        boolean factoriesModified = false;
        synchronized (factories) {
            for (String adaptable : adaptables) {
                AdapterFactoryDescriptorMap adfMap = factories.get(adaptable);
                if (adfMap != null) {
                    factoriesModified |= (adfMap.remove(factoryKey) != null);
                    if (adfMap.isEmpty()) {
                        factories.remove(adaptable);
                    }
                }
            }
        }

        // only remove cache if some adapter factories have actually been
        // removed
        if (factoriesModified) {
            factoryCache = null;
        }

        // send event
        final EventAdmin localEA = this.eventAdmin;
        if ( localEA != null ) {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(SlingConstants.PROPERTY_ADAPTABLE_CLASSES, adaptables);
            props.put(SlingConstants.PROPERTY_ADAPTER_CLASSES, adapters);
            localEA.postEvent(new Event(SlingConstants.TOPIC_ADAPTER_FACTORY_REMOVED,
                    props));
        }
    }

    /**
     * Returns a map of {@link AdapterFactory} instances for the given class to
     * be adapted. The returned map is indexed by the fully qualified name of
     * the target classes (to adapt to) registered.
     *
     * @param clazz The type of the object for which the registered adapter
     *            factories are requested
     * @return The map of adapter factories. If there is no adapter factory
     *         registered for this type, the returned map is empty.
     */
    private Map<String, AdapterFactory> getAdapterFactories(Class<?> clazz) {
        Map<String, Map<String, AdapterFactory>> cache = factoryCache;
        if (cache == null) {
            cache = new HashMap<String, Map<String, AdapterFactory>>();
            factoryCache = cache;
        }

        synchronized (cache) {
            return getAdapterFactories(clazz, cache);
        }
    }

    /**
     * Returns the map of adapter factories index by adapter (target) class name
     * for the given adaptable <code>clazz</code>. If no adapter exists for
     * the <code>clazz</code> and empty map is returned.
     *
     * @param clazz The adaptable <code>Class</code> for which to return the
     *            adapter factory map by target class name.
     * @param cache The cache of already defined adapter factory mappings
     * @return The map of adapter factories by target class name. The map may be
     *         empty if there is no adapter factory for the adaptable
     *         <code>clazz</code>.
     */
    private Map<String, AdapterFactory> getAdapterFactories(Class<?> clazz,
            Map<String, Map<String, AdapterFactory>> cache) {

        String className = clazz.getName();
        Map<String, AdapterFactory> entry = cache.get(className);
        if (entry == null) {
            // create entry
            entry = createAdapterFactoryMap(clazz, cache);
            cache.put(className, entry);
        }

        return entry;
    }

    /**
     * Creates a new target adapter factory map for the given <code>clazz</code>.
     * First all factories defined to support the adaptable class by
     * registration are taken. Next all factories for the implemented interfaces
     * and finally all base class factories are copied. Later adapter factory
     * entries do NOT overwrite earlier entries.
     *
     * @param clazz The adaptable <code>Class</code> for which to build the
     *            adapter factory map by target class name.
     * @param cache The cache of already defined adapter factory mappings
     * @return The map of adapter factories by target class name. The map may be
     *         empty if there is no adapter factory for the adaptable
     *         <code>clazz</code>.
     */
    private Map<String, AdapterFactory> createAdapterFactoryMap(Class<?> clazz,
            Map<String, Map<String, AdapterFactory>> cache) {
        Map<String, AdapterFactory> afm = new HashMap<String, AdapterFactory>();

        // AdapterFactories for this class
        AdapterFactoryDescriptorMap afdMap;
        synchronized (factories) {
            afdMap = factories.get(clazz.getName());
        }
        if (afdMap != null) {
            for (AdapterFactoryDescriptor afd : afdMap.values()) {
                String[] adapters = afd.getAdapters();
                for (String adapter : adapters) {
                    if (!afm.containsKey(adapter)) {
                        final AdapterFactory factory = afd.getFactory();
                        if ( factory != null ) {
                            afm.put(adapter, factory);
                        }
                    }
                }
            }
        }

        // AdapterFactories for the interfaces
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iFace : interfaces) {
            copyAdapterFactories(afm, iFace, cache);
        }

        // AdapterFactories for the super class
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            copyAdapterFactories(afm, superClazz, cache);
        }

        return afm;
    }

    /**
     * Copies all adapter factories for the given <code>clazz</code> from the
     * <code>cache</code> to the <code>dest</code> map except for those
     * factories whose target class already exists in the <code>dest</code>
     * map.
     *
     * @param dest The map of target class name to adapter factory into which
     *            additional factories are copied. Existing factories are not
     *            replaced.
     * @param clazz The adaptable class whose adapter factories are considered
     *            for adding into <code>dest</code>.
     * @param cache The adapter factory cache providing the adapter factories
     *            for <code>clazz</code> to consider for copying into
     *            <code>dest</code>.
     */
    private void copyAdapterFactories(Map<String, AdapterFactory> dest,
            Class<?> clazz, Map<String, Map<String, AdapterFactory>> cache) {

        // get the adapter factories for the adaptable clazz
        Map<String, AdapterFactory> scMap = getAdapterFactories(clazz, cache);

        // for each target class copy the entry to dest if dest does
        // not contain the target class already
        for (Map.Entry<String, AdapterFactory> entry : scMap.entrySet()) {
            if (!dest.containsKey(entry.getKey())) {
                dest.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
