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
package org.apache.sling.jcr.classloader.internal;

import java.beans.Introspector;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>DynamicRepositoryClassLoader</code> class provides the
 * functionality to load classes and resources from the JCR Repository.
 * Additionally, this class supports the notion of getting 'dirty', which means,
 * that if a resource loaded through this class loader has been modified in the
 * Repository, this class loader marks itself dirty, which flag can get
 * retrieved. This helps the user of this class loader to decide on whether to
 * {@link #reinstantiate(Session, ClassLoader) reinstantiate} it or continue
 * using this class loader.
 * <p>
 * When a user of the class loader recognizes an instance to be dirty, it can
 * easily be reinstantiated with the {@link #reinstantiate} method. This
 * reinstantiation will also rebuild the internal real class path from the same
 * list of path patterns as was used to create the internal class path for the
 * original class loader. The resulting internal class path need not be the
 * same, though.
 */
public final class DynamicRepositoryClassLoader
    extends SecureClassLoader
    implements EventListener, DynamicClassLoader {

    /**
     * The special resource representing a resource which could not be
     * found in the class path.
     *
     * @see #cache
     * @see #findClassLoaderResource(String)
     */
    private static final ClassLoaderResource NOT_FOUND_RESOURCE =
        new ClassLoaderResource(null, "[sentinel]", null) {
            public boolean isExpired() {
                return false;
            }
        };


    /** default log category */
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Cache of resources used to check class loader expiry. The map is indexed
     * by the paths of the expiry properties of the cached resources. This map
     * is not complete in terms of resources which have been loaded through this
     * class loader. That is for resources loaded through an archive class path
     * entry, only one of those resources (the last one loaded) is kept in this
     * cache, while the others are ignored.
     *
     * @see #onEvent(EventIterator)
     * @see #findClassLoaderResource(String)
     */
    private final Map<String, ClassLoaderResource> modTimeCache = new HashMap<String, ClassLoaderResource>();

    /**
     * Flag indicating whether there are loaded classes which have later been
     * expired (e.g. invalidated or modified)
     */
    private boolean dirty = false;

    /** The registered event listeners. */
    private EventListener[] proxyListeners;

    /**
     * The classpath which this classloader searches for class definitions.
     * Each element of the vector should be either a directory, a .zip
     * file, or a .jar file.
     * <p>
     * It may be empty when only system classes are controlled.
     */
    private ClassPathEntry[] repository;

    /**
     * The list of paths to use as a classpath.
     */
    private String[] paths;

    /**
     * The <code>Session</code> grants access to the Repository to access the
     * resources.
     * <p>
     * This field is not final such that it may be cleared when the class loader
     * is destroyed.
     */
    private Session session;

    /**
     * Cache of resources found or not found in the class path. The map is
     * indexed by resource name and contains mappings to instances of the
     * {@link ClassLoaderResource} class. If a resource has been tried to be
     * loaded, which could not be found, the resource is cached with the
     * special mapping to {@link #NOT_FOUND_RESOURCE}.
     *
     * @see #NOT_FOUND_RESOURCE
     * @see #findClassLoaderResource(String)
     */
   private final Map<String, ClassLoaderResource> cache = new HashMap<String, ClassLoaderResource>();

    /**
     * Flag indicating whether the {@link #destroy()} method has already been
     * called (<code>true</code>) or not (<code>false</code>)
     */
    private boolean destroyed = false;

    /**
     * Creates a <code>DynamicRepositoryClassLoader</code> from a list of item
     * path strings containing globbing pattens for the paths defining the
     * class path.
     *
     * @param session The <code>Session</code> to use to access the class items.
     * @param classPath The list of path strings making up the (initial) class
     *      path of this class loader. The strings may contain globbing
     *      characters which will be resolved to build the actual class path.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *      <code>null</code>.
     *
     * @throws NullPointerException if either the session or the handles list
     *      is <code>null</code>.
     */
    public DynamicRepositoryClassLoader(final Session session,
                                        final String[] classPath,
                                        final ClassLoader parent) {
        // initialize the super class with an empty class path
        super(parent);

        // check session and handles
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (classPath == null || classPath.length == 0) {
            throw new NullPointerException("handles");
        }

        // set fields
        this.session = session;
        this.paths = classPath;

        // build the class repositories list
        buildRepository();

        // register with observation service and path pattern list
        registerListeners();

        log.debug("DynamicRepositoryClassLoader: {} ready", this);
    }

    /**
     * Creates a <code>DynamicRepositoryClassLoader</code> with the same
     * configuration as the given <code>DynamicRepositoryClassLoader</code>.
     * This constructor is used by the {@link #reinstantiate} method.
     * <p>
     * Before returning from this constructor the <code>old</code> class loader
     * is destroyed and may not be used any more.
     *
     * @param session The session to associate with this class loader.
     * @param old The <code>DynamicRepositoryClassLoader</code> to copy the
     *            cofiguration from.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *            <code>null</code>.
     */
    private DynamicRepositoryClassLoader(final Session session,
                                         final DynamicRepositoryClassLoader old,
                                         final ClassLoader parent) {
        // initialize the super class with an empty class path
        super(parent);

        // check session and handles
        if (session == null) {
            throw new NullPointerException("session");
        }
        // set fields
        this.session = session;
        this.paths = old.paths;

        repository = old.repository;
        buildRepository();

        // register with observation service and path pattern list
        registerListeners();

        // finally finalize the old class loader
        old.destroy();

        log.debug(
            "DynamicRepositoryClassLoader: Copied {}. Do not use that anymore",
            old);
    }

    /**
     * Destroys this class loader. This process encompasses all steps needed
     * to remove as much references to this class loader as possible.
     * <p>
     * <em>NOTE</em>: This method just clears all internal fields and especially
     * the class path to render this class loader unusable.
     * <p>
     * This implementation does not throw any exceptions.
     */
    public void destroy() {
        // we expect to be called only once, so we stop destroyal here
        if (destroyed) {
            log.debug("Instance is already destroyed");
            return;
        }

        // remove ourselves as listeners from other places
        unregisterListeners();

        // set destroyal guard
        destroyed = true;

        // clear caches and references
        repository = null;
        paths = null;
        session = null;

        // clear the cache of loaded resources and flush cached class
        // introspections of the JavaBean framework
        synchronized ( cache ) {
            final Iterator<ClassLoaderResource> ci = cache.values().iterator();
            while ( ci.hasNext() ) {
                final ClassLoaderResource res = ci.next();
                if (res.getLoadedClass() != null) {
                    Introspector.flushFromCaches(res.getLoadedClass());
                    res.setLoadedClass(null);
                }
            }
            cache.clear();
        }
        synchronized ( modTimeCache ) {
            modTimeCache.clear();
        }
    }

    //---------- URLClassLoader overwrites -------------------------------------

    /**
     * Finds and loads the class with the specified name from the class path.
     *
     * @param name the name of the class
     * @return the resulting class
     *
     * @throws ClassNotFoundException If the named class could not be found or
     *      if this class loader has already been destroyed.
     */
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (destroyed) {
            throw new ClassNotFoundException(name + " (Classloader destroyed)");
        }

        log.debug("findClass: Try to find class {}", name);

        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Class<?>>() {

                    public Class<?> run() throws ClassNotFoundException {
                        return findClassPrivileged(name);
                    }
                });
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    /**
     * Finds the resource with the specified name on the search path.
     *
     * @param name the name of the resource
     *
     * @return a <code>URL</code> for the resource, or <code>null</code>
     *      if the resource could not be found or if the class loader has
     *      already been destroyed.
     */
    public URL findResource(String name) {

        if (destroyed) {
            log.warn("Destroyed class loader cannot find a resource");
            return null;
        }

        log.debug("findResource: Try to find resource {}", name);

        ClassLoaderResource res = findClassLoaderResource(name);
        if (res != null) {
            log.debug("findResource: Getting resource from {}, created {}",
                res, new Date(res.getLastModificationTime()));
            return res.getURL();
        }

        return null;
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * on the search path having the specified name.
     *
     * @param name the resource name
     *
     * @return an <code>Enumeration</code> of <code>URL</code>s. This is an
     *      empty enumeration if no resources are found by this class loader
     *      or if this class loader has already been destroyed.
     */
    public Enumeration<URL> findResources(String name) {

        if (destroyed) {
            log.warn("Destroyed class loader cannot find resources");
            return new Enumeration<URL>() {
                public boolean hasMoreElements() {
                    return false;
                }
                public URL nextElement() {
                    throw new NoSuchElementException("No Entries");
                }
            };
        }

        log.debug("findResources: Try to find resources for {}", name);

        List<URL> list = new LinkedList<URL>();
        for (int i=0; i < repository.length; i++) {
            final ClassPathEntry cp = repository[i];
            log.debug("findResources: Trying {}", cp);

            ClassLoaderResource res = cp.getResource(name);
            if (res != null) {
                log.debug("findResources: Adding resource from {}, created {}",
                    res, new Date(res.getLastModificationTime()));
                URL url = res.getURL();
                if (url != null) {
                    list.add(url);
                }
            }

        }

        // return the enumeration on the list
        return Collections.enumeration(list);
    }

    //---------- Property access ----------------------------------------------

    /**
     * Removes all entries from the cache of loaded resources, which mark
     * resources, which have not been found as of yet.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private void cleanCache() {
        synchronized ( cache ) {
            final Iterator<ClassLoaderResource> ci = this.cache.values().iterator();
            while (ci.hasNext()) {
                if (ci.next() == NOT_FOUND_RESOURCE) {
                    ci.remove();
                }
            }
        }
    }

    //---------- internal ------------------------------------------------------

    /**
     * Builds the repository list from the list of path patterns and appends
     * the path entries from any added handles. This method may be used multiple
     * times, each time replacing the currently defined repository list.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private synchronized void buildRepository() {
        List<ClassPathEntry> newRepository = new ArrayList<ClassPathEntry>(paths.length);

        // build repository from path patterns
        for (int i=0; i < paths.length; i++) {
            final String entry = paths[i];
            ClassPathEntry cp = null;

            // try to find repository based on this path
            if (repository != null) {
                for (int j=0; j < repository.length; j++) {
                    final ClassPathEntry tmp = repository[i];
                    if (tmp.getPath().equals(entry)) {
                        cp = tmp;
                        break;
                    }
                }
            }

            // not found, creating new one
            if (cp == null) {
                cp = ClassPathEntry.getInstance(session, entry);
            }

            if (cp != null) {
                log.debug("Adding path {}", entry);
                newRepository.add(cp);
            } else {
                log.debug("Cannot get a ClassPathEntry for {}", entry);
            }
        }

        // replace old repository with new one
        ClassPathEntry[] newClassPath = new ClassPathEntry[newRepository.size()];
        newRepository.toArray(newClassPath);
        repository = newClassPath;

        // clear un-found resource cache
        cleanCache();
    }

    /**
     * Tries to find the class in the class path from within a
     * <code>PrivilegedAction</code>. Throws <code>ClassNotFoundException</code>
     * if no class can be found for the name.
     *
     * @param name the name of the class
     *
     * @return the resulting class
     *
     * @throws ClassNotFoundException if the class could not be found
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private Class<?> findClassPrivileged(String name) throws ClassNotFoundException {

        // prepare the name of the class
        final String path = name.replace('.', '/').concat(".class");
        log.debug("findClassPrivileged: Try to find path {} for class {}",
            path, name);

        ClassLoaderResource res = findClassLoaderResource(path);
        if (res != null) {

             // try defining the class, error aborts
             try {
                 log.debug(
                    "findClassPrivileged: Loading class from {}, created {}",
                    res, new Date(res.getLastModificationTime()));

                 Class<?> c = defineClass(name, res);
                 if (c == null) {
                     log.warn("defineClass returned null for class {}", name);
                     throw new ClassNotFoundException(name);
                 }
                 return c;

             } catch (IOException ioe) {
                 log.debug("defineClass failed", ioe);
                 throw new ClassNotFoundException(name, ioe);
             } catch (Throwable t) {
                 log.debug("defineClass failed", t);
                 throw new ClassNotFoundException(name, t);
             }
         }

        throw new ClassNotFoundException(name);
     }

    /**
     * Returns a {@link ClassLoaderResource} for the given <code>name</code> or
     * <code>null</code> if not existing. If the resource has already been
     * loaded earlier, the cached instance is returned. If the resource has
     * not been found in an earlier call to this method, <code>null</code> is
     * returned. Otherwise the resource is looked up in the class path. If
     * found, the resource is cached and returned. If not found, the
     * {@link #NOT_FOUND_RESOURCE} is cached for the name and <code>null</code>
     * is returned.
     *
     * @param name The name of the resource to return.
     *
     * @return The named <code>ClassLoaderResource</code> if found or
     *      <code>null</code> if not found.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private ClassLoaderResource findClassLoaderResource(String name) {
        ClassLoaderResource res = null;
        // check for cached resources first
        synchronized ( cache ) {
            res = cache.get(name);
            if (res == NOT_FOUND_RESOURCE) {
                log.debug("Resource '{}' known to not exist in class path", name);
                return null;
            } else if (res == null) {
                // walk the repository list and try to find the resource
                for (int i = 0; i < repository.length; i++) {
                    final ClassPathEntry cp = repository[i];
                    log.debug("Checking {}", cp);

                    res = cp.getResource(name);
                    if (res != null) {
                        log.debug("Found resource in {}, created ", res, new Date(
                            res.getLastModificationTime()));
                        cache.put(name, res);
                        break;
                    }
                }
                if ( res == null ) {
                    log.debug("No classpath entry contains {}", name);
                    cache.put(name, NOT_FOUND_RESOURCE);
                    return null;
                }
            }
        }
        // if it could be found, we register it with the caches
        // register the resource in the expiry map, if an appropriate
        // property is available
        Property prop = res.getExpiryProperty();
        if (prop != null) {
            try {
                synchronized ( modTimeCache ) {
                    modTimeCache.put(prop.getPath(), res);
                }
            } catch (RepositoryException re) {
                log.warn("Cannot register the resource " + res +
                    " for expiry", re);
            }
        }
        // and finally return the resource
        return res;
    }

    /**
     * Defines a class getting the bytes for the class from the resource
     *
     * @param name The fully qualified class name
     * @param res The resource to obtain the class bytes from
     *
     * @throws RepositoryException If a problem occurrs getting at the data.
     * @throws IOException If a problem occurrs reading the class bytes from
     *      the resource.
     * @throws ClassFormatError If the class bytes read from the resource are
     *      not a valid class.
     */
    private Class<?> defineClass(String name, ClassLoaderResource res)
            throws IOException, RepositoryException {

        log.debug("defineClass({}, {})", name, res);

        Class<?> clazz = res.getLoadedClass();
        if (clazz == null) {
            final byte[] data = res.getBytes();
            clazz = defineClass(name, data, 0, data.length);
            res.setLoadedClass(clazz);
        }

        return clazz;
    }

    //---------- reload support ------------------------------------------------

    /**
     * Returns whether the class loader is dirty. This can be the case if any
     * of the loaded class has been expired through the observation.
     * <p>
     * This method may also return <code>true</code> if the <code>Session</code>
     * associated with this class loader is not valid anymore.
     * <p>
     * Finally the method always returns <code>true</code> if the class loader
     * has already been destroyed. Note, however, that a destroyed class loader
     * cannot be reinstantiated. See {@link #reinstantiate(Session, ClassLoader)}.
     * <p>
     * If the class loader is dirty, it should be reinstantiated through the
     * {@link #reinstantiate} method.
     *
     * @return <code>true</code> if the class loader is dirty and needs
     *      reinstantiation.
     */
    public boolean isDirty() {
        return destroyed || dirty || !session.isLive();
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoader#isLive()
     */
    public boolean isLive() {
        return !this.isDirty();
    }

    /**
     * Reinstantiates this class loader. That is, a new ClassLoader with no
     * loaded class is created with the same configuration as this class loader.
     * <p>
     * When the new class loader is returned, this class loader has been
     * destroyed and may not be used any more.
     *
     * @param parent The parent <code>ClassLoader</code> for the reinstantiated
     * 	    <code>DynamicRepositoryClassLoader</code>, which may be
     *      <code>null</code>.
     *
     * @return a new instance with the same configuration as this class loader.
     *
     * @throws IllegalStateException if <code>this</code>
     *      {@link DynamicRepositoryClassLoader} has already been destroyed
     *      through the {@link #destroy()} method.
     */
    public DynamicRepositoryClassLoader reinstantiate(Session session, ClassLoader parent) {
        log.debug("reinstantiate: Copying {} with parent {}", this, parent);

        if (destroyed) {
            throw new IllegalStateException("Destroyed class loader cannot be recreated");
        }

        // create the new loader
        DynamicRepositoryClassLoader newLoader =
                new DynamicRepositoryClassLoader(session, this, parent);

        // return the new loader
        return newLoader;
    }

    //---------- EventListener interface -------------------------------

    /**
     * Handles a repository item modifcation events checking whether a class
     * needs to be expired. As a side effect, this method sets the class loader
     * dirty if a loaded class has been modified in the repository.
     *
     * @param events The iterator of repository events to be handled.
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            String path;
            try {
                path = event.getPath();
            } catch (RepositoryException re) {
                log.warn("onEvent: Cannot get path of event, ignoring", re);
                continue;
            }

            log.debug(
                "onEvent: Item {} has been modified, checking with cache", path);

            final ClassLoaderResource resource;
            synchronized ( modTimeCache ) {
                resource = modTimeCache.get(path);
            }
            if (resource != null) {
                log.debug("pageModified: Expiring cache entry {}", resource);
                expireResource(resource);
            } else {
                // might be in not-found cache - remove from there
                if (event.getType() == Event.NODE_ADDED
                    || event.getType() == Event.PROPERTY_ADDED) {
                    log.debug("pageModified: Clearing not-found cache for possible new class");
                    cleanCache();
                }
            }

        }
    }

    //----------- Object overwrite ---------------------------------------------

    /**
     * Returns a string representation of this class loader.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName());
        if (destroyed) {
            buf.append(" - destroyed");
        } else {
            buf.append(": parent: { ");
            buf.append(getParent());
            buf.append(" }, user: ");
            buf.append(session.getUserID());
            buf.append(", dirty: ");
            buf.append(isDirty());
        }
        return buf.toString();
    }

    //---------- internal ------------------------------------------------------

    /**
     * Registers this class loader with the observation service to get
     * information on page updates in the class path and to the path
     * pattern list to get class path updates.
     *
     * @throws NullPointerException if this class loader has already been
     *      destroyed.
     */
    private final void registerListeners() {
        log.debug("registerListeners: Registering to the observation service");

        this.proxyListeners = new EventListener[this.paths.length];
        for(int i=0; i < paths.length; i++ ) {
            final String path = paths[i];
            try {
                final EventListener listener = new ProxyEventListener(this);
                final ObservationManager om = session.getWorkspace().getObservationManager();
                om.addEventListener(listener, 255, path, true, null, null, false);
                proxyListeners[i] = listener;
            } catch (RepositoryException re) {
                log.error("registerModificationListener: Cannot register " +
                    this + " with observation manager", re);
            }
        }
    }

    /**
     * Removes this instances registrations from the observation service and
     * the path pattern list.
     *
     * @throws NullPointerException if this class loader has already been
     *      destroyed.
     */
    private final void unregisterListeners() {
        log.debug("unregisterListeners: Deregistering from the observation service");
        if ( this.proxyListeners != null ) {
            // check session first!
            if ( session.isLive() ) {
                for(final EventListener listener : this.proxyListeners) {
                    if ( listener != null ) {
                        try {
                            final ObservationManager om = session.getWorkspace().getObservationManager();
                            om.removeEventListener(listener);
                        } catch (RepositoryException re) {
                            log.error("unregisterListener: Cannot unregister " +
                                this + " from observation manager", re);
                        }
                    }
                }
            }
            this.proxyListeners = null;
        }
    }

    /**
     * Checks whether the page backing the resource has been updated with a
     * version, such that this new version would be used to access the resource.
     * In this case the resource has expired and the class loader needs to be
     * set dirty.
     *
     * @param resource The <code>ClassLoaderResource</code> to check for
     *      expiry.
     */
    private boolean expireResource(ClassLoaderResource resource) {

        // check whether the resource is expired (only if a class has been loaded)
        boolean exp = resource.getLoadedClass() != null && resource.isExpired();

        // update dirty flag accordingly
        dirty |= exp;
        log.debug("expireResource: Loader dirty: {}", isDirty());

        // return the expiry status
        return exp;
    }

    protected final static class ProxyEventListener implements EventListener {

        private final EventListener delegatee;

        public ProxyEventListener(final EventListener delegatee) {
            this.delegatee = delegatee;
        }
        /**
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        public void onEvent(EventIterator events) {
            this.delegatee.onEvent(events);
        }
    }
}
