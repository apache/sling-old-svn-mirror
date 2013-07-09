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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.jcr.classloader.internal.net.JCRURLHandler;
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
    implements DynamicClassLoader {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Set of loaded resources and classes. */
    private final Set<String> usedResources = new HashSet<String>();

    /**
     * Flag indicating whether there are loaded classes which have later been
     * expired (e.g. invalidated or modified)
     */
    private volatile boolean dirty = false;

    /**
     * The path to use as a classpath.
     */
    private final String repositoryPath;

    /**
     * The <code>ClassLoaderWriterImpl</code> grants access to the repository
     * <p>
     * This field is not final such that it may be cleared when the class loader
     * is destroyed.
     */
    private final ClassLoaderWriterImpl writer;

    /**
     * Flag indicating whether the {@link #destroy()} method has already been
     * called (<code>true</code>) or not (<code>false</code>)
     */
    private volatile boolean destroyed = false;

    /**
     * Creates a <code>RepositoryClassLoader</code> for a given
     * repository path.
     *
     * @param classPath The path making up the class path of this class
     *                  loader
     * @param writer The class loader write to get a jcr session.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *      <code>null</code>.
     *
     * @throws NullPointerException if either the session or the classPath
     *      is <code>null</code>.
     */
    public RepositoryClassLoader(final String classPath,
                                 final ClassLoaderWriterImpl writer,
                                 final ClassLoader parent) {
        // initialize the super class with an empty class path
        super(parent);

        // check writer and classPath
        if (writer == null) {
            throw new NullPointerException("writer");
        }
        if (classPath == null) {
            throw new NullPointerException("classPath");
        }

        // set fields
        this.writer = writer;
        this.repositoryPath = classPath;

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
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (!this.writer.isActivate()) {
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
    @Override
    public URL findResource(final String name) {
        if (!this.writer.isActivate()) {
            logger.warn("Destroyed class loader cannot find a resource: " + name, new IllegalStateException());
            return null;
        }

        logger.debug("findResource: Try to find resource {}", name);

        final String path = this.repositoryPath + '/' + name;
        try {
            if ( findClassLoaderResource(path) ) {
                logger.debug("findResource: Getting resource from {}", path);
                return JCRURLHandler.createURL(this.writer, path);
            }
        } catch (final Exception e) {
            logger.warn("findResource: Cannot getURL for " + name, e);
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
    @Override
    public Enumeration<URL> findResources(final String name) {
        if (!this.writer.isActivate()) {
            logger.warn("Destroyed class loader cannot find a resources: " + name, new IllegalStateException());
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
        final List<URL> list = Collections.singletonList(url);
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

        final String path = this.repositoryPath + '/' + name.replace('.', '/') + (".class");

         // try defining the class, error aborts
         try {
             final byte[] data = this.findClassLoaderClass(path);
             if (data != null) {

                 logger.debug("findClassPrivileged: Loading class from {} bytes", data.length);

                 final Class<?> c = defineClass(name, data);
                 if (c == null) {
                     logger.warn("defineClass returned null for class {}", name);
                     throw new ClassNotFoundException(name);
                 }
                 return c;
             }

         } catch (final IOException ioe) {
             logger.debug("defineClass failed", ioe);
             throw new ClassNotFoundException(name, ioe);
         } catch (final Throwable t) {
             logger.debug("defineClass failed", t);
             throw new ClassNotFoundException(name, t);
         }

        throw new ClassNotFoundException(name);
     }

    /**
     * Returns the contents for the given <code>path</code> or
     * <code>null</code> if not existing.
     *
     * @param path The repository path of the resource to return.
     *
     * @return The contents if found or <code>null</code> if not found.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private boolean findClassLoaderResource(final String path) throws IOException {
        Session session = null;
        boolean res = false;
        try {
            session = this.writer.createSession();
            if ( session.itemExists(path) ) {
                logger.debug("Found resource at {}", path);
                res = true;
            } else {
                logger.debug("No classpath entry contains {}", path);
            }
        } catch (final RepositoryException re) {
            logger.debug("Error while trying to get node at " + path, re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }

        return res;
    }

    /**
     * Returns the contents for the given <code>path</code> or
     * <code>null</code> if not existing.
     *
     * @param path The repository path of the resource to return.
     *
     * @return The contents if found or <code>null</code> if not found.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private byte[] findClassLoaderClass(final String path) throws IOException {
        Session session = null;
        byte[] res = null;
        try {
            session = this.writer.createSession();
            if ( session.itemExists(path) ) {
                final Node node = (Node)session.getItem(path);
                logger.debug("Found resource at {}", path);
                res = Util.getBytes(node);
            } else {
                logger.debug("No classpath entry contains {}", path);
            }
        } catch (final RepositoryException re) {
            logger.debug("Error while trying to get node at " + path, re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }
        if ( !this.dirty ) {
            synchronized ( this.usedResources ) {
                this.usedResources.add(path);
            }
        }
        return res;
    }

    /**
     * Defines a class using the bytes
     *
     * @param name The fully qualified class name
     * @param contents The class in bytes
     *
     * @throws RepositoryException If a problem occurrs getting at the data.
     * @throws IOException If a problem occurrs reading the class bytes from
     *      the resource.
     * @throws ClassFormatError If the class bytes read from the resource are
     *      not a valid class.
     */
    private Class<?> defineClass(final String name, final byte[] contents) {
        logger.debug("defineClass({}, {})", name, contents.length);

        final Class<?> clazz = defineClass(name, contents, 0, contents.length);

        return clazz;
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoader#isLive()
     */
    public boolean isLive() {
        return !destroyed && !dirty && this.writer.isActivate();
    }

    /**
     * Handle a modification event.
     */
    public void handleEvent(final String path) {
        synchronized ( this.usedResources ) {
            if ( this.usedResources.contains(path) ) {
                logger.debug("handleEvent: Item {} has been modified - marking class loader as dirty {}", path, this);
                this.dirty = true;
            }
        }

    }
    //----------- Object overwrite ---------------------------------------------

    /**
     * Returns a string representation of this class loader.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName());
        if (destroyed) {
            buf.append(" - destroyed");
        } else {
            buf.append(": parent: { ");
            buf.append(getParent());
            buf.append(" }, live: ");
            buf.append(isLive());
        }
        return buf.toString();
    }
}
