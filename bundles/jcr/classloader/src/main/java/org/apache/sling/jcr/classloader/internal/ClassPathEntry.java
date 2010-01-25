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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.classloader.internal.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ClassPathEntry</code> class encapsulates entries in the class path
 * of the {@link DynamicRepositoryClassLoader}. The main task is to retrieve
 * {@link ClassLoaderResource} instances for classes or resources to load from it.
 * <p>
 * This implementation is not currently integrated with Java security. That is
 * protection domains and security managers are not supported yet.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 */
public final class ClassPathEntry {

    /** default logging */
    private static final Logger log =
        LoggerFactory.getLogger(ClassPathEntry.class);

    /** The session assigned to this class path entry */
    protected final Session session;

    /** The path to the item of this class path entry */
    protected final String path;

    /** The base URL for the class path entry to later construct resource URLs */
    protected URL baseURL;

    //---------- construction --------------------------------------------------

    /**
     * Creates an instance of the <code>ClassPathEntry</code> assigning the
     * session and path.
     *
     * @param session The <code>Session</code> to access the Repository.
     * @param path The path of the class path entry, this is either the
     *      path of a node containing a jar archive or is the path
     *      of the root of a hierarchy to look up resources in.
     */
    protected ClassPathEntry(Session session, String path) {
        this.path = path;
        this.session = session;
    }

    /**
     * Clones this instance of the <code>ClassPathEntry</code> setting the
     * path and session to the same value as the base instance.
     * <p>
     * Note that this constructor does not duplicate the session from the base
     * instance.
     *
     * @param base The <code>ClassPathEntry</code> from which to copy the path
     *      and the session.
     */
    protected ClassPathEntry(ClassPathEntry base) {
        this.path = base.path;
        this.session = base.session;
        this.baseURL = base.baseURL;
    }

    /**
     * Returns an instance of the <code>ClassPathEntry</code> class. This
     * instance will be a subclass correctly handling the type (directory or
     * jar archive) of class path entry is to be created.
     * <p>
     * If the path given has a trailing slash, it is taken as a directory root
     * else the path is first tested, whether it contains an archive. If not
     * the path is treated as a directory.
     *
     * @param session The <code>Session</code> to access the Repository.
     * @param path The path of the class path entry, this is either the
     *      path of a node containing a jar archive or is the path
     *      of the root of a hierharchy to look up resources in.
     *
     * @return An initialized <code>ClassPathEntry</code> instance for the
     *      path or <code>null</code> if an error occurred creating the
     *      instance.
     */
    static ClassPathEntry getInstance(Session session, String path) {

        // check we can access the path, don't care about content now
        try {
            session.checkPermission(path, "read");
        } catch (AccessControlException ace) {
            log.warn(
                "getInstance: Access denied reading from {}, ignoring entry",
                path);
            return null;
        } catch (RepositoryException re) {
            log.error("getInstance: Cannot check permission to " + path, re);
        }

        if (!path.endsWith("/")) {
            // assume the path designates a directory
            // append trailing slash now
            path += "/";
        }

        // we assume a directory class path entry, but we might have to check
        // whether the path refers to a node or not. On the other hande, this
        // class path entry will not be usable anyway if not, user beware :-)

        return new ClassPathEntry(session, path);
    }

    /**
     * Returns the path on which this <code>ClassPathEntry</code> is based.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns this <code>ClassPathEntry</code> represented as an URL to be
     * used in a list of URLs to further work on. If there is a problem creating
     * the URL for this instance, <code>null</code> is returned instead.
     */
    public URL toURL() {
        if (baseURL == null) {
            try {
                baseURL = URLFactory.createURL(session, path);
            } catch (MalformedURLException mue) {
                log.warn("DirectoryClassPathEntry: Creating baseURl for " +
                    path, mue);
            }
        }

        return baseURL;
    }

    /**
     * Returns a {@link ClassLoaderResource} for the named resource if it
     * can befound below this directory root identified by the path given
     * at construction time. Note that if the page would exist but does
     * either not contain content or is not readable by the current session,
     * no resource is returned.
     *
     * @param name The name of the resource to return. If the resource would
     *      be a class the name must already be modified to denote a valid
     *      path, that is dots replaced by dashes and the <code>.class</code>
     *      extension attached.
     *
     * @return The {@link ClassLoaderResource} identified by the name or
     *      <code>null</code> if no resource is found for that name.
     */
    public ClassLoaderResource getResource(final String name) {

        try {
            final Property prop = Util.getProperty(session.getItem(path + name));
            if (prop != null) {
                return new ClassLoaderResource(this, name, prop);
            }

            log.debug("getResource: resource {} not found below {} ", name,
                path);

        } catch (PathNotFoundException pnfe) {

            log.debug("getResource: Classpath entry {} does not have resource {}",
                path, name);

        } catch (RepositoryException cbe) {

            log.warn("getResource: problem accessing the resource {} below {}",
                new Object[] { name, path }, cbe);

        }
        // invariant : no page or problem accessing the page

        return null;
    }

    /**
     * Returns a <code>ClassPathEntry</code> with the same configuration as
     * this <code>ClassPathEntry</code>.
     * <p>
     * Becase the <code>DirectoryClassPathEntry</code> class does not have
     * internal state, this method returns this instance to be used as
     * the "copy".
     */
    ClassPathEntry copy() {
        return this;
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.append(": path: ");
        buf.append(path);
        buf.append(", user: ");
        buf.append(session.getUserID());
        return buf.toString();
    }

    //----------- internal helper ----------------------------------------------

}
