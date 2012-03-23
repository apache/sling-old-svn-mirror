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

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.jcr.classloader.internal.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>RepositoryClassLoader</code> class provides the
 * functionality to load classes and resources from the JCR Repository.
 * Additionally, this class supports the notion of getting 'dirty', which means,
 * that if a resource loaded through this class loader has been modified in the
 * repository, this class loader marks itself dirty, which flag can get
 * retrieved.
 */
public final class RepositoryClassLoader
    extends SecureClassLoader
    implements EventListener, DynamicClassLoader {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Set of loaded resources and classes. */
    private final Set<String> usedResources = new HashSet<String>();

    /**
     * Flag indicating whether there are loaded classes which have later been
     * expired (e.g. invalidated or modified)
     */
    private boolean dirty = false;

    /**
     * The path to use as a classpath.
     */
    private String repositoryPath;

    /**
     * The <code>Session</code> grants access to the Repository to access the
     * resources.
     * <p>
     * This field is not final such that it may be cleared when the class loader
     * is destroyed.
     */
    private Session session;

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
     * @throws NullPointerException if either the session or the classPath
     *      is <code>null</code>.
     */
    public RepositoryClassLoader(final Session session,
                                 final String classPath,
                                 final ClassLoader parent) {
        // initialize the super class with an empty class path
        super(parent);

        // check session and handles
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (classPath == null) {
            throw new NullPointerException("classPath");
        }

        // set fields
        this.session = session;
        this.repositoryPath = classPath;

        // register with observation service and path pattern list
        registerListener();

        logger.debug("RepositoryClassLoader: {} ready", this);
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
            logger.debug("Instance is already destroyed");
            return;
        }

        // set destroyal guard
        destroyed = true;

        // remove ourselves as listeners from other places
        unregisterListener();

        // close session
        if ( session != null ) {
            session.logout();
            session = null;
        }
        repositoryPath = null;
        synchronized ( this.usedResources ) {
            this.usedResources.clear();
        }
    }

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

        logger.debug("findClass: Try to find class {}", name);

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
    public URL findResource(final String name) {
        if (destroyed) {
            logger.warn("Destroyed class loader cannot find a resource");
            return null;
        }

        logger.debug("findResource: Try to find resource {}", name);

        final Node res = findClassLoaderResource(name);
        if (res != null) {
            logger.debug("findResource: Getting resource from {}",
                res);
            try {
                return URLFactory.createURL(session, res.getPath());
            } catch (Exception e) {
                logger.warn("findResource: Cannot getURL for " + name, e);
            }
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
    public Enumeration<URL> findResources(final String name) {
        if (destroyed) {
            logger.warn("Destroyed class loader cannot find resources");
            return new Enumeration<URL>() {
                public boolean hasMoreElements() {
                    return false;
                }
                public URL nextElement() {
                    throw new NoSuchElementException("No Entries");
                }
            };
        }

        logger.debug("findResources: Try to find resources for {}", name);

        final URL url = this.findResource(name);
        final List<URL> list = new LinkedList<URL>();
        if (url != null) {
            list.add(url);
        }

        // return the enumeration on the list
        return Collections.enumeration(list);
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
    private Class<?> findClassPrivileged(final String name) throws ClassNotFoundException {

        // prepare the name of the class
        logger.debug("findClassPrivileged: Try to find path {class {}",
            name);

        final Node res = this.findClassLoaderResource(name);
        if (res != null) {

             // try defining the class, error aborts
             try {
                 logger.debug(
                    "findClassPrivileged: Loading class from {}", res);

                 final Class<?> c = defineClass(name, res);
                 if (c == null) {
                     logger.warn("defineClass returned null for class {}", name);
                     throw new ClassNotFoundException(name);
                 }
                 return c;

             } catch (final IOException ioe) {
                 logger.debug("defineClass failed", ioe);
                 throw new ClassNotFoundException(name, ioe);
             } catch (final Throwable t) {
                 logger.debug("defineClass failed", t);
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
    private Node findClassLoaderResource(final String name) {
        final String path = this.repositoryPath + '/' + name.replace('.', '/') + (".class");
        Node res = null;
        try {
            if ( session.itemExists(path) ) {
                final Node node = (Node)session.getItem(path);
                logger.debug("Found resource at {}", path);
                synchronized ( this.usedResources ) {
                    this.usedResources.add(path);
                }
                res = node;
            } else {
                logger.debug("No classpath entry contains {}", path);
            }
        } catch (final RepositoryException re) {
            logger.debug("Error while trying to get node at " + path, re);
        }
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
    private Class<?> defineClass(final String name, final Node res)
    throws IOException, RepositoryException {
        logger.debug("defineClass({}, {})", name, res);

        final byte[] data = Util.getBytes(res);
        final Class<?> clazz = defineClass(name, data, 0, data.length);

        return clazz;
    }

    /**
     * Returns whether the class loader is dirty. This can be the case if any
     * of the loaded class has been expired through the observation.
     * <p>
     * This method may also return <code>true</code> if the <code>Session</code>
     * associated with this class loader is not valid anymore.
     * <p>
     * Finally the method always returns <code>true</code> if the class loader
     * has already been destroyed.
     * <p>
     *
     * @return <code>true</code> if the class loader is dirty and needs
     *      recreation.
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

    //---------- EventListener interface -------------------------------

    /**
     * Handles a repository item modifcation events checking whether a class
     * needs to be expired. As a side effect, this method sets the class loader
     * dirty if a loaded class has been modified in the repository.
     *
     * @param events The iterator of repository events to be handled.
     */
    public void onEvent(final EventIterator events) {
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            String path;
            try {
                path = event.getPath();
            } catch (RepositoryException re) {
                logger.warn("onEvent: Cannot get path of event, ignoring", re);
                continue;
            }

            if ( event.getType() == Event.PROPERTY_ADDED || event.getType() == Event.PROPERTY_CHANGED || event.getType() == Event.PROPERTY_REMOVED ) {
                final int lastSlash = path.lastIndexOf('/');
                path = path.substring(0, lastSlash);
            }
            if ( path.endsWith("/jcr:content") ) {
                path = path.substring(0, path.length() - 12);
            }
            this.handleEvent(path);
        }
    }

    /**
     * Handle a modification event.
     */
    public void handleEvent(final String path) {
        synchronized ( this.usedResources ) {
            if ( this.usedResources.contains(path) ) {
                logger.debug("handleEvent: Item {} has been modified - marking class loader as dirty {}", this);
                this.dirty = true;
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
    private final void registerListener() {
        logger.debug("registerListener: Registering to the observation service");

        try {
            final ObservationManager om = session.getWorkspace().getObservationManager();
            om.addEventListener(this,
                    Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                    repositoryPath, true, null, null, false);
        } catch (final RepositoryException re) {
            logger.error("registerModificationListener: Cannot register " +
                this + " with observation manager", re);
        }
    }

    /**
     * Removes this instances registrations from the observation service and
     * the path pattern list.
     *
     * @throws NullPointerException if this class loader has already been
     *      destroyed.
     */
    private final void unregisterListener() {
        logger.debug("unregisterListener: Deregistering from the observation service");
        // check session first!
        if ( session.isLive() ) {
            try {
                final ObservationManager om = session.getWorkspace().getObservationManager();
                om.removeEventListener(this);
            } catch (RepositoryException re) {
                logger.error("unregisterListener: Cannot unregister " +
                    this + " from observation manager", re);
            }
        }
    }
}
