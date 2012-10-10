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
package org.apache.sling.jcr.classloader.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>DynamicClassLoaderProviderImpl</code> provides
 * a class loader which loads classes from configured paths
 * in the repository.
 * It implements the {@link ClassLoaderWriter} interface
 * for clients to use for writing and reading such
 * classes and resources.
 */
@Component(metatype=true, label="%loader.name", description="%loader.description",
           name="org.apache.sling.jcr.classloader.internal.DynamicClassLoaderProviderImpl")
@Service(value = ClassLoaderWriter.class, serviceFactory = true)
@Properties({
    @org.apache.felix.scr.annotations.Property(name="service.vendor", value="The Apache Software Foundation"),
    @org.apache.felix.scr.annotations.Property(name="service.description", value="Repository based classloader writer")
})
public class ClassLoaderWriterImpl
    implements ClassLoaderWriter {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(ClassLoaderWriterImpl.class);

    private static final String CLASS_PATH_DEFAULT = "/var/classes";

    @org.apache.felix.scr.annotations.Property(value=CLASS_PATH_DEFAULT)
    private static final String CLASS_PATH_PROP = "classpath";


    /** Node type for packages/folders. */
    private static final String NT_FOLDER = "nt:folder";

    /** Default class loader owner. */
    private static final String OWNER_DEFAULT = "admin";

    @org.apache.felix.scr.annotations.Property(value=OWNER_DEFAULT)
    private static final String OWNER_PROP = "owner";

    /** The owner of the class loader / jcr user. */
    private String classLoaderOwner;

    @Reference
    private SlingRepository repository;

    /** The configured class path. */
    private String classPath;

    @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private MimeTypeService mimeTypeService;

    @Reference(
            referenceInterface = DynamicClassLoaderManager.class,
            bind = "bindDynamicClassLoaderManager",
            unbind = "unbindDynamicClassLoaderManager")
    private ServiceReference dynamicClassLoaderManager;

    /** The bundle asking for this service instance */
    private Bundle callerBundle;

    /** Cached repository class loader. */
    private volatile RepositoryClassLoader repositoryClassLoader;

    /**
     * The dynamic class loader used as the parent of the repository
     * class loader.
     */
    private volatile ClassLoader dynamicClassLoader;

    /**
     * Activate this component.
     * @param props The configuration properties
     */
    @Activate
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        Object prop = properties.get(CLASS_PATH_PROP);
        if ( prop instanceof String[] && ((String[])prop).length > 0 ) {
            this.classPath = ((String[])prop)[0];
        } else {
            this.classPath = CLASS_PATH_DEFAULT;
        }
        if ( this.classPath.endsWith("/") ) {
            this.classPath = this.classPath.substring(0, this.classPath.length() - 1);
        }

        prop = properties.get(OWNER_PROP);
        this.classLoaderOwner = (prop instanceof String)? (String) prop : OWNER_DEFAULT;

        this.callerBundle = componentContext.getUsingBundle();
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        destroyRepositoryClassLoader();
    }

    /**
     * Called to handle binding the DynamicClassLoaderManager service
     * reference
     */
    @SuppressWarnings("unused")
    private void bindDynamicClassLoaderManager(final ServiceReference ref) {
        this.dynamicClassLoaderManager = ref;
    }

    /**
     * Called to handle unbinding the DynamicClassLoaderManager service
     * reference
     */
    @SuppressWarnings("unused")
    private void unbindDynamicClassLoaderManager(final ServiceReference ref) {
        if (this.dynamicClassLoaderManager == ref) {
            this.dynamicClassLoaderManager = null;
        }
    }

    /**
     * Destroys the repository class loader if existing and ungets the
     * DynamicClassLoaderManager service if a dynamic class loader is
     * being used.
     */
    private void destroyRepositoryClassLoader() {
        if (this.repositoryClassLoader != null) {
            this.repositoryClassLoader.destroy();
            this.repositoryClassLoader = null;
        }

        if (this.dynamicClassLoader != null) {
            this.callerBundle.getBundleContext().ungetService(this.dynamicClassLoaderManager);
            this.dynamicClassLoader = null;
        }
    }

    /**
     * Return a new session.
     */
    public Session createSession() throws RepositoryException {
        // get an administrative session for potentiall impersonation
        final Session admin = this.repository.loginAdministrative(null);

        // do use the admin session, if the admin's user id is the same as owner
        if (admin.getUserID().equals(this.classLoaderOwner)) {
            return admin;
        }

        // else impersonate as the owner and logout the admin session again
        try {
            return admin.impersonate(new SimpleCredentials(this.classLoaderOwner, new char[0]));
        } finally {
            admin.logout();
        }
    }

    /**
     * Is this still active?
     */
    public boolean isActivate() {
        return this.repository != null;
    }

    private synchronized ClassLoader getOrCreateClassLoader() {
        if ( this.repositoryClassLoader == null || !this.repositoryClassLoader.isLive() ) {

            // make sure to cleanup any existing class loader
            this.destroyRepositoryClassLoader();

            // get the dynamic class loader for the bundle using this
            // class loader writer
            DynamicClassLoaderManager dclm = (DynamicClassLoaderManager) this.callerBundle.getBundleContext().getService(
                this.dynamicClassLoaderManager);
            this.dynamicClassLoader = dclm.getDynamicClassLoader();

            this.repositoryClassLoader = new RepositoryClassLoader(
                    this.classPath,
                    this,
                    this.dynamicClassLoader);
        }
        return this.repositoryClassLoader;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(final String name) {
        final String path = cleanPath(name);
        Session session = null;
        try {
            session = createSession();
            if (session.itemExists(path)) {
                Item fileItem = session.getItem(path);
                fileItem.remove();
                session.save();
                this.repositoryClassLoader.handleEvent(path);
                return true;
            }
        } catch (final RepositoryException re) {
            logger.error("Cannot remove " + path, re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }

        // fall back to false if item does not exist or in case of error
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(final String name) {
        final String path = cleanPath(name);
        return new RepositoryOutputStream(this, path);
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(final String oldName, final String newName) {
        Session session = null;
        try {
            final String oldPath = cleanPath(oldName);
            final String newPath = cleanPath(newName);

            session = this.createSession();
            session.move(oldPath, newPath);
            session.save();
            this.repositoryClassLoader.handleEvent(oldName);
            this.repositoryClassLoader.handleEvent(newName);
            return true;
        } catch (final RepositoryException re) {
            logger.error("Cannot rename " + oldName + " to " + newName, re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }

        // fallback to false in case of error or non-existence of oldFileName
        return false;
    }

    /**
     * Creates a folder hierarchy in the repository.
     * We synchronize this method to reduce potential conflics.
     * Although each write uses its own session it might occur
     * that more than one session tries to create the same path
     * (or parent path) at the same time. By synchronizing this
     * we avoid this situation - however this method is written
     * in a failsafe manner anyway.
     */
    private synchronized boolean mkdirs(final Session session, final String path) {
        try {
            // quick test
            if (session.itemExists(path) && session.getItem(path).isNode()) {
                return true;
            }

            // check path walking it down
            Node current = session.getRootNode();
            final String[] names = path.split("/");
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null || names[i].length() == 0) {
                    continue;
                } else if (current.hasNode(names[i])) {
                    current = current.getNode(names[i]);
                } else {
                    final Node parentNode = current;
                    try {
                        // adding the node could cause an exception
                        // for example if another thread tries to
                        // create the node "at the same time"
                        current = parentNode.addNode(names[i], NT_FOLDER);
                        session.save();
                    } catch (RepositoryException re) {
                        // let's first refresh the session
                        // we don't catch an exception here, because if
                        // session refresh fails, we might have a serious problem!
                        session.refresh(false);
                        // let's check if the node is available now
                        if ( parentNode.hasNode(names[i]) ) {
                            current = parentNode.getNode(names[i]);
                        } else {
                            // we try it one more time to create the node - and fail otherwise
                            current = parentNode.addNode(names[i], NT_FOLDER);
                            session.save();
                        }
                    }
                }
            }

            return true;

        } catch (final RepositoryException re) {
            logger.error("Cannot create folder path:" + path, re);
            // discard changes
            try {
                session.refresh(false);
            } catch (final RepositoryException e) {
                // we simply ignore this
            }
        }

        // false in case of error or no need to create
        return false;
    }

    /**
     * Helper method to clean the path.
     * It replaces backslashes with slashes and cuts off trailing spaces.
     * It uses the first configured class path to access the path.
     */
    private String cleanPath(String path) {
        // replace backslash by slash
        path = path.replace('\\', '/');

        // cut off trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return this.classPath + path;
    }

    private static class RepositoryOutputStream extends ByteArrayOutputStream {

        private final ClassLoaderWriterImpl repositoryOutputProvider;

        private final String fileName;

        RepositoryOutputStream(ClassLoaderWriterImpl repositoryOutputProvider,
                String fileName) {
            this.repositoryOutputProvider = repositoryOutputProvider;
            this.fileName = fileName;
        }

        /**
         * @see java.io.ByteArrayOutputStream#close()
         */
        public void close() throws IOException {
            super.close();

            Session session = null;
            try {
                // get an own session for writing
                session = repositoryOutputProvider.createSession();
                final int lastPos = fileName.lastIndexOf('/');
                final String path = (lastPos == -1 ? null : fileName.substring(0, lastPos));
                final String name = (lastPos == -1 ? fileName : fileName.substring(lastPos + 1));
                if ( lastPos != -1 ) {
                    if ( !repositoryOutputProvider.mkdirs(session, path) ) {
                        throw new IOException("Unable to create path for " + path);
                    }
                }
                Node fileNode = null;
                Node contentNode = null;
                Node parentNode = null;
                if (session.itemExists(fileName)) {
                    final Item item = session.getItem(fileName);
                    if (item.isNode()) {
                        final Node node = item.isNode() ? (Node) item : item.getParent();
                        if ("jcr:content".equals(node.getName())) {
                            // replace the content properties of the jcr:content
                            // node
                            parentNode = node;
                            contentNode = node;
                        } else if (node.isNodeType("nt:file")) {
                            // try to set the content properties of jcr:content
                            // node
                            parentNode = node;
                            contentNode = node.getNode("jcr:content");
                        } else { // fileName is a node
                            // try to set the content properties of the node
                            parentNode = node;
                            contentNode = node;
                        }
                    } else {
                        // replace property with an nt:file node (if possible)
                        parentNode = item.getParent();
                        item.remove();
                        session.save();
                        fileNode = parentNode.addNode(name, "nt:file");
                    }
                } else {
                    if (lastPos <= 0) {
                        parentNode = session.getRootNode();
                    } else {
                        Item parent = session.getItem(path);
                        if (!parent.isNode()) {
                            throw new IOException("Parent at " + path + " is not a node.");
                        }
                        parentNode = (Node) parent;
                    }
                    fileNode = parentNode.addNode(name, "nt:file");
                }

                // if we have a file node, create the contentNode
                if (fileNode != null) {
                    contentNode = fileNode.addNode("jcr:content", "nt:resource");
                }

                final MimeTypeService mtService = this.repositoryOutputProvider.mimeTypeService;

                String mimeType = (mtService == null ? null : mtService.getMimeType(fileName));
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                contentNode.setProperty("jcr:lastModified", System.currentTimeMillis());
                contentNode.setProperty("jcr:data", new ByteArrayInputStream(buf, 0, size()));
                contentNode.setProperty("jcr:mimeType", mimeType);

                session.save();
                this.repositoryOutputProvider.repositoryClassLoader.handleEvent(fileName);
            } catch (final RepositoryException re) {
                throw (IOException)new IOException("Cannot write file " + fileName + ", reason: " + re.toString()).initCause(re);
            } finally {
                if ( session != null ) {
                    session.logout();
                }
            }
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(final String name)
    throws IOException {
        final String path = cleanPath(name) + "/jcr:content/jcr:data";
        Session session = null;
        try {
            session = this.createSession();
            if ( session.itemExists(path) ) {
                final Property prop = (Property)session.getItem(path);
                return prop.getStream();
            }
            throw new FileNotFoundException("Unable to find " + name);
        } catch (final RepositoryException re) {
            throw (IOException) new IOException(
                        "Failed to get InputStream for " + name).initCause(re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(final String name) {
        final String path = cleanPath(name) + "/jcr:content/jcr:lastModified";
        Session session = null;
        try {
            session = this.createSession();
            if ( session.itemExists(path) ) {
                final Property prop = (Property)session.getItem(path);
                return prop.getLong();
            }
        } catch (final RepositoryException se) {
            logger.error("Cannot get last modification time for " + name, se);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        return this.getOrCreateClassLoader();
    }
}
