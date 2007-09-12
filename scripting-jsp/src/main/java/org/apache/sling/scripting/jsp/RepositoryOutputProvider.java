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
package org.apache.sling.scripting.jsp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.net.URLFactory;
import org.apache.jasper.OutputProvider;
import org.apache.sling.jcr.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RepositoryOutputProvider</code> TODO
 */
class RepositoryOutputProvider implements OutputProvider {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(RepositoryOutputProvider.class);

    private final SlingRepository repository;

    // shared session for read access
    private Session session;

    // private session for write access
    private Session privateSession;

    // the workspace to access here, null for SlingRepository default
    private String workspace;

    private Lock privateSessionLock = new Lock();

    // the maximum time in milliseconds to wait for the private session to
    // be released if locked
    private long privateSessionTimeout = 5 * 1000L;

    // the maximum number of times to retry getting the private session
    // when the timeout expired but the session could not be got
    private int privateSessionRetry = 100;

    RepositoryOutputProvider(SlingRepository repository) {
        this.repository = repository;
    }

    String getWorkspace() {
        return this.workspace;
    }

    void setWorkspace(String workspace) {
        this.workspace = workspace;

        this.closeSessions();
    }

    long getPrivateSessionTimeout() {
        return this.privateSessionTimeout;
    }

    void setPrivateSessionTimeout(long privateSessionTimeout) {
        this.privateSessionTimeout = privateSessionTimeout;
    }

    int getPrivateSessionRetry() {
        return this.privateSessionRetry;
    }

    // nothing if privateSessionRetry is smaller than +1
    void setPrivateSessionRetry(int privateSessionRetry) {
        if (privateSessionRetry > 0) {
            this.privateSessionRetry = privateSessionRetry;
        }
    }

    void dispose() {
        this.closeSessions();
    }

    //---------- OutputProvider interface -------------------------------------

    /* (non-Javadoc)
     * @see org.apache.jasper.OutputProvider#delete(java.lang.String)
     */
    public boolean delete(String fileName) {
        Node parentNode = null;
        try {
            fileName = this.cleanPath(fileName);
            Session session = this.getPrivateSession();
            if (session.itemExists(fileName)) {
                Item fileItem = session.getItem(fileName);
                parentNode = fileItem.getParent();
                fileItem.remove();
                parentNode.save();
                return true;
            }
        } catch (RepositoryException re) {
            log.error("Cannot remove " + fileName, re);
        } finally {
            checkNode(parentNode, fileName);
            this.ungetPrivateSession();
        }

        // fall back to false if item does not exist or in case of error
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.jasper.OutputProvider#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String fileName)
            throws FileNotFoundException, IOException {
        try {
            fileName = this.cleanPath(fileName);
            Session session = this.getSession();
            if (!session.itemExists(fileName)) {
                throw new FileNotFoundException("Item " + fileName + " does not exist");
            }

            Item item = session.getItem(fileName);
            while (item.isNode()) {
                // follow the primary item trail
                Node node = (Node) item;
                item = node.getPrimaryItem();
            }

            Property prop = (Property) item;
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                if (values.length == 0) {
                    throw new FileNotFoundException("Item " + fileName
                        + " resolves to a multivalue property without values");
                }
                log.debug("getInputStream: Stream from first value of multi values");
                return values[0].getStream();
            }


            log.debug("getInputStream: Stream from single value property ");
            return prop.getStream();

        } catch (ItemNotFoundException infe) {
            throw new FileNotFoundException("Item " + fileName
                + " does not resolve to a property");
        } catch (RepositoryException re) {
            throw (IOException) new IOException(
                "Failed to get InputStream for " + fileName).initCause(re);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.jasper.OutputProvider#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(String fileName) {
        fileName = this.cleanPath(fileName);
        return new RepositoryOutputStream(this, fileName);
    }

    /* (non-Javadoc)
     * @see org.apache.jasper.OutputProvider#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldFileName, String newFileName) {
        try {
            oldFileName = this.cleanPath(oldFileName);
            newFileName = this.cleanPath(newFileName);

            Session session = this.getPrivateSession();
            session.getWorkspace().move(oldFileName, newFileName);
            return true;
        } catch (RepositoryException re) {
            log.error("Cannot rename " + oldFileName + " to " + newFileName, re);
        } finally {
            this.ungetPrivateSession();
        }

        // fallback to false in case of error or non-existence of oldFileName
        return false;
    }

    public boolean mkdirs(String path) {
        Node parentNode = null;
        try {
            Session session = this.getPrivateSession();

            // quick test
            path = this.cleanPath(path);
            if (session.itemExists(path) && session.getItem(path).isNode()) {
                return false;
            }

            // check path walking it down
            Node current = session.getRootNode();
            String[] names = path.split("/");
            for (int i=0; i < names.length; i++) {
                if (names[i] == null || names[i].length() == 0) {
                    continue;
                } else  if (current.hasNode(names[i])) {
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
            this.ungetPrivateSession();
        }

        // false in case of error or no need to create
        return false;
    }

    public long lastModified(String fileName) {
        try {
            fileName = this.cleanPath(fileName);
            Session session = this.getSession();
            if (session.itemExists(fileName)) {
                Item item = session.getItem(fileName);
                Node resource;
                if (item.isNode()) {
                    resource = (Node) item;
                    if (resource.isNodeType("nt:file")) {
                        log.debug("lastModified: {} is an nt:file node, "
                            + "check jcr:content for last modification time",
                            fileName);
                        resource = resource.getNode("jcr:content");
                    } else {
                        log.debug("lastModified: {} is a node, check for "
                            + "last modification time", fileName);
                    }
                } else {
                    log.debug(
                        "lastModified: {} is a property, check parent for last modification time",
                        fileName);
                    resource = item.getParent();
                }

                if (resource.hasProperty("jcr:lastModified")) {
                    return resource.getProperty("jcr:lastModified").getLong();
                }

                // cannot decide on last modification time, use current system time
                log.info("Cannot get {} property for {}, using current system time",
                    "jcr:lastModified", fileName);
                return System.currentTimeMillis();
            }
        } catch (RepositoryException re) {
            log.error("Cannot get last modification time for " + fileName, re);
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    //---------- Helper Methods for JspServletContext -------------------------

    /* package */ URL getURL(String path) throws MalformedURLException {
        try {
            if (this.getSession().itemExists(path)) {
                return URLFactory.createURL(this.getSession(), path);
            }
            log.debug("getURL: {} does not address an item", path);
        } catch (MalformedURLException mue) {
            // forward
            throw mue;
        } catch (Exception e) {
            log.warn("Cannot get URL for " + path, e);
        }

        return null;
    }

    /* package */ Set getResourcePaths(String path) {
        log.error("getResourcePaths({}): Not yet implemented", path);
        return null;
    }

    //---------- internal -----------------------------------------------------

    private Session getSession() throws RepositoryException {
        if (this.session == null) {
            this.session = this.repository.loginAdministrative(this.getWorkspace());
        }
        return this.session;
    }

    private Session getPrivateSession() throws RepositoryException {
        for (int retries = this.getPrivateSessionRetry(); retries > 0; retries--) {
            if (this.privateSessionLock.tryAcquire(this.getPrivateSessionTimeout())) {
                if (this.privateSession == null) {
                    this.privateSession = this.repository.loginAdministrative(this.getWorkspace());
                }
                return this.privateSession;
            }
        }

        throw new RepositoryException("Cannot get private session, timed out waiting to acquire the lock");
    }

    private void ungetPrivateSession() {
        try {
            Session session = this.privateSession;
            if (session != null && session.isLive() && session.hasPendingChanges()) {
                session.refresh(false);
            }
        } catch (RepositoryException re) {
            log.error("Cannot check private session for pending changes or rollback changes", re);
        }

        this.privateSessionLock.release();
    }

    private void closeSessions() {
        Session oldSession = this.session;
        this.session = null;
        if (oldSession != null && oldSession.isLive()) {
            oldSession.logout();
        }

        oldSession = this.privateSession;
        this.privateSession = null;
        if (oldSession != null && oldSession.isLive()) {
            oldSession.logout();
        }
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
            path = path.substring(0, path.length()-1);
        }

        return path;
    }

    private static class RepositoryOutputStream extends ByteArrayOutputStream {

        private final RepositoryOutputProvider repositoryOutputProvider;
        private final String fileName;

        RepositoryOutputStream(
                RepositoryOutputProvider repositoryOutputProvider,
                String fileName) {
            this.repositoryOutputProvider = repositoryOutputProvider;
            this.fileName = fileName;
        }

        public void close() throws IOException {
            super.close();

            Node parentNode = null;
            try {
                Session session = this.repositoryOutputProvider.getPrivateSession();
                Node fileNode = null;
                Node contentNode = null;
                if (session.itemExists(this.fileName)) {
                    Item item = session.getItem(this.fileName);
                    if (item.isNode()) {
                        Node node = item.isNode() ? (Node) item : item.getParent();
                        if ("jcr:content".equals(node.getName())) {
                            // replace the content properties of the jcr:content node
                            parentNode = node;
                            contentNode = node;
                        } else if (node.isNodeType("nt:file")) {
                            // try to set the content properties of jcr:content node
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
                    int lastSlash = this.fileName.lastIndexOf('/');
                    if (lastSlash <= 0) {
                        parentNode = session.getRootNode();
                    } else {
                        Item parent = session.getItem(this.fileName.substring(0, lastSlash));
                        if (!parent.isNode()) {
                            // TODO: fail
                        }
                        parentNode = (Node) parent;
                    }
                    String name = this.fileName.substring(lastSlash + 1);
                    fileNode = parentNode.addNode(name, "nt:file");
                }

                // if we have a file node, create the contentNode
                if (fileNode != null) {
                    contentNode = fileNode.addNode("jcr:content", "nt:resource");
                }

                contentNode.setProperty("jcr:lastModified", System.currentTimeMillis());
                contentNode.setProperty("jcr:data", new ByteArrayInputStream(this.buf, 0, this.size()));
                contentNode.setProperty("jcr:mimeType", "application/octet-stream");

                parentNode.save();
            } catch (RepositoryException re) {
                log.error("Cannot write file " + this.fileName, re);
                throw new IOException("Cannot write file " + this.fileName
                    + ", reason: " + re.toString());
            } finally {
                checkNode(parentNode, this.fileName);
                this.repositoryOutputProvider.ungetPrivateSession();
            }
        }
    }
}
