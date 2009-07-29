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
import java.util.Dictionary;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderProvider;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RepositoryClassLoaderProviderImpl</code> TODO
 *
 * @scr.component immediate="false" label="%loader.name"
 *      description="%loader.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="Provides Repository ClassLoaders"
 * @scr.service servicefactory="true"
 */
public class RepositoryClassLoaderProviderImpl
        implements RepositoryClassLoaderProvider, DynamicClassLoaderProvider, ClassLoaderWriter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(RepositoryClassLoaderFacade.class);

    /**
     * @scr.property values0="/var/classes"
     */
    public static final String CLASS_PATH_PROP = "classpath";

    /**
     * @scr.property valueRef="OWNER_DEFAULT"
     */
    public static final String OWNER_PROP = "owner";

    // JSP Class Loader class path will be injected to the class loader !
    private static final String[] CLASS_PATH_DEFAULT = { };

    private static final String OWNER_DEFAULT = "admin";

    private BidiMap loaders = new DualHashBidiMap();

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    private String[] classPath;

    private String classLoaderOwner;

    private BundleProxyClassLoader parent;

    /** @scr.reference policy="dynamic" */
    private MimeTypeService mimeTypeService;

    public ClassLoader getClassLoader(String owner) {
        String classLoaderOwner = this.getClassLoaderOwner(owner);
        RepositoryClassLoaderFacade loader =
            (RepositoryClassLoaderFacade) this.loaders.get(classLoaderOwner);
        if (loader == null) {
            loader = new RepositoryClassLoaderFacade(this, this.parent,
                classLoaderOwner, this.classPath);
            this.loaders.put(classLoaderOwner, loader);
        }

        return loader;
    }

    public void ungetClassLoader(ClassLoader classLoader) {
        // nothing to do
    }

    //---------- Support for RepositoryClassLoaderFacade ----------------------

    /* package */ Session getSession(String owner) throws RepositoryException {
        // get an administrative session for potentiall impersonation
        Session admin = this.repository.loginAdministrative(null);

        // do use the admin session, if the admin's user id is the same as owner
        if (admin.getUserID().equals(owner)) {
            return admin;
        }

        // else impersonate as the owner and logout the admin session again
        try {
            return admin.impersonate(new SimpleCredentials(owner, new char[0]));
        } finally {
            admin.logout();
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoaderProvider#getClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader(final ClassLoader parent) {
        // we just make up a unique identifier
        final String classLoaderOwner = "DynamicClassLoaderProvider:" + parent.hashCode();
        RepositoryClassLoaderFacade loader =
            (RepositoryClassLoaderFacade) this.loaders.get(classLoaderOwner);
        if (loader == null) {
            loader = new RepositoryClassLoaderFacade(this, parent,
                    this.classLoaderOwner, this.classPath);
            this.loaders.put(classLoaderOwner, loader);
        }

        return loader;
    }

    //---------- SCR Integration ----------------------------------------------

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(String name) {
        name = cleanPath(name);
        Node parentNode = null;
        Session session = null;
        try {
            session = getSession(this.classLoaderOwner);
            if (session.itemExists(name)) {
                Item fileItem = session.getItem(name);
                parentNode = fileItem.getParent();
                fileItem.remove();
                parentNode.save();
                return true;
            }
        } catch (RepositoryException re) {
            log.error("Cannot remove " + name, re);
        } finally {
            checkNode(parentNode, name);
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
    public OutputStream getOutputStream(String name) {
        final String path = cleanPath(name);
        return new RepositoryOutputStream(this, path);
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldName, String newName) {
        Session session = null;
        try {
            oldName = cleanPath(oldName);
            newName = cleanPath(newName);

            session = this.getSession(this.classLoaderOwner);
            session.getWorkspace().move(oldName, newName);
            return true;
        } catch (RepositoryException re) {
            log.error("Cannot rename " + oldName + " to " + newName, re);
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
     */
    private boolean mkdirs(final Session session, String path) {
        Node parentNode = null;
        try {
            // quick test
            if (session.itemExists(path) && session.getItem(path).isNode()) {
                return true;
            }

            // check path walking it down
            Node current = session.getRootNode();
            String[] names = path.split("/");
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null || names[i].length() == 0) {
                    continue;
                } else if (current.hasNode(names[i])) {
                    current = current.getNode(names[i]);
                } else {
                    if (parentNode == null) {
                        parentNode = current;
                    }
                    current = current.addNode(names[i], "nt:folder");
                }
            }

            if (parentNode != null) {
                parentNode.save();
                return true;
            }

        } catch (RepositoryException re) {
            log.error("Cannot create folder path " + path, re);
        } finally {
            checkNode(parentNode, path);
        }

        // false in case of error or no need to create
        return false;
    }

    private static void checkNode(Node node, String path) {
        if (node != null && node.isModified()) {
            try {
                node.refresh(false);
            } catch (RepositoryException re) {
                log.error("Cannot refresh node for " + path
                    + " after failed save", re);
            }
        }
    }

    private String cleanPath(String path) {
        // replace backslash by slash
        path = path.replace('\\', '/');

        // cut off trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if ( this.classPath == null || this.classPath.length == 0 ) {
            return path;
        }
        return this.classPath[0] + path;
    }

    private static class RepositoryOutputStream extends ByteArrayOutputStream {

        private final RepositoryClassLoaderProviderImpl repositoryOutputProvider;

        private final String fileName;

        RepositoryOutputStream(RepositoryClassLoaderProviderImpl repositoryOutputProvider,
                String fileName) {
            this.repositoryOutputProvider = repositoryOutputProvider;
            this.fileName = fileName;
        }

        public void close() throws IOException {
            super.close();

            Node parentNode = null;
            Session session = null;
            try {
                session = repositoryOutputProvider.getSession(repositoryOutputProvider.classLoaderOwner);
                final int lastPos = fileName.lastIndexOf('/');
                if ( lastPos != -1 ) {
                    repositoryOutputProvider.mkdirs(session, fileName.substring(0, lastPos));
                }
                Node fileNode = null;
                Node contentNode = null;
                if (session.itemExists(fileName)) {
                    Item item = session.getItem(fileName);
                    if (item.isNode()) {
                        Node node = item.isNode()
                                ? (Node) item
                                : item.getParent();
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
                        String name = item.getName();
                        fileNode = parentNode.addNode(name, "nt:file");
                        item.remove();
                    }
                } else {
                    int lastSlash = fileName.lastIndexOf('/');
                    if (lastSlash <= 0) {
                        parentNode = session.getRootNode();
                    } else {
                        Item parent = session.getItem(fileName.substring(0,
                            lastSlash));
                        if (!parent.isNode()) {
                            // TODO: fail
                        }
                        parentNode = (Node) parent;
                    }
                    String name = fileName.substring(lastSlash + 1);
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

                contentNode.setProperty("jcr:lastModified",
                    System.currentTimeMillis());
                contentNode.setProperty("jcr:data", new ByteArrayInputStream(
                    buf, 0, size()));
                contentNode.setProperty("jcr:mimeType", mimeType);

                parentNode.save();
            } catch (RepositoryException re) {
                log.error("Cannot write file " + fileName, re);
                throw new IOException("Cannot write file " + fileName
                    + ", reason: " + re.toString());
            } finally {
                checkNode(parentNode, fileName);
                if ( session != null ) {
                    session.logout();
                }
            }
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String fileName)
    throws IOException {
        final String path = cleanPath(fileName) + "/jcr:content/jcr:data";
        Session session = null;
        try {
            session = getSession(this.classLoaderOwner);
            if ( session.itemExists(path) ) {
                final Property prop = (Property)session.getItem(path);
                return prop.getStream();
            }
            throw new FileNotFoundException("Unable to find " + fileName);
        } catch (RepositoryException re) {
            throw (IOException) new IOException(
                        "Failed to get InputStream for " + fileName).initCause(re);
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(String fileName) {
        final String path = cleanPath(fileName) + "/jcr:content/jcr:lastModified";
        Session session = null;
        try {
            session = getSession(this.classLoaderOwner);
            if ( session.itemExists(path) ) {
                final Property prop = (Property)session.getItem(path);
                return prop.getLong();
            }
        } catch (RepositoryException se) {
            log.error("Cannot get last modification time for " + fileName, se);
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    protected void activate(ComponentContext componentContext) {
        @SuppressWarnings("unchecked")
        Dictionary properties = componentContext.getProperties();

        Object prop = properties.get(CLASS_PATH_PROP);
        this.classPath = (prop instanceof String[]) ? (String[]) prop : CLASS_PATH_DEFAULT;

        prop = properties.get(OWNER_PROP);
        this.classLoaderOwner = (prop instanceof String)? (String) prop : OWNER_DEFAULT;

        Bundle owner = componentContext.getUsingBundle();

        // if there is no using bundle, we have an error !!
        if (owner == null) {
            throw new IllegalStateException("Using Bundle expected. Is this a servicefactory component ?");
        }

        this.parent = new BundleProxyClassLoader(owner, null);
    }

    @SuppressWarnings("unchecked")
    protected void deactivate(ComponentContext componentContext) {
        for (Iterator ci=this.loaders.values().iterator(); ci.hasNext(); ) {
            RepositoryClassLoaderFacade cl = (RepositoryClassLoaderFacade) ci.next();
            cl.destroy();
            ci.remove();
        }

        this.parent = null;
    }

    //---------- internal -----------------------------------------------------

    private String getClassLoaderOwner(String userId) {
        return this.classLoaderOwner;
    }
}
