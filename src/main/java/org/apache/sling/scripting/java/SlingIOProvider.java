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
package org.apache.sling.scripting.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingIOProvider</code> TODO
 */
public class SlingIOProvider  {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SlingIOProvider.class);

    private ThreadLocal<ResourceResolver> requestResourceResolver;

    SlingIOProvider() {
        this.requestResourceResolver = new ThreadLocal<ResourceResolver>();
    }

    void setRequestResourceResolver(ResourceResolver resolver) {
        requestResourceResolver.set(resolver);
    }

    void resetRequestResourceResolver() {
        requestResourceResolver.remove();
    }

    // ---------- IOProvider interface -----------------------------------------

    /**
     * Returns an InputStream for the file name which is looked up with the
     * ResourceProvider and retrieved from the Resource if the StreamProvider
     * interface is implemented.
     */
    public InputStream getInputStream(String fileName)
    throws FileNotFoundException, IOException {

        try {

            Resource resource = getResourceInternal(fileName);
            if (resource == null) {
                throw new FileNotFoundException("Cannot find " + fileName);
            }

            InputStream stream = resource.adaptTo(InputStream.class);
            if (stream == null) {
                throw new FileNotFoundException("Cannot find " + fileName);
            }

            return stream;

        } catch (SlingException se) {
            throw (IOException) new IOException(
                "Failed to get InputStream for " + fileName).initCause(se);
        }
    }

    /**
     * Returns the value of the last modified meta data field of the resource
     * found at file name or zero if the meta data field is not set. If the
     * resource does not exist or an error occurrs finding the resource, -1 is
     * returned.
     */
    public long lastModified(String fileName) {
        try {
            Resource resource = getResourceInternal(fileName);
            if (resource != null) {
                ResourceMetadata meta = resource.getResourceMetadata();
                long modTime = meta.getModificationTime();
                return (modTime > 0) ? modTime : 0;
            }

        } catch (SlingException se) {
            log.error("Cannot get last modification time for " + fileName, se);
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    /**
     * Removes the named item from the repository.
     */
    public boolean delete(String fileName) {
        Node parentNode = null;
        try {
            fileName = cleanPath(fileName);
            Session session = getPrivateSession();
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
        }

        // fall back to false if item does not exist or in case of error
        return false;
    }

    /**
     * Returns an output stream to write to the repository.
     */
    public OutputStream getOutputStream(String fileName) {
        fileName = cleanPath(fileName);
        return new RepositoryOutputStream(this, fileName);
    }

    /* package */URL getURL(String path) throws MalformedURLException {
        try {
            Resource resource = getResourceInternal(path);
            return (resource != null) ? resource.adaptTo(URL.class) : null;
        } catch (SlingException se) {
            throw (MalformedURLException) new MalformedURLException(
                "Cannot get URL for " + path).initCause(se);
        }
    }

    /* package */Set<String> getResourcePaths(String path) {
        Set<String> paths = new HashSet<String>();

        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                Resource resource = resolver.getResource(cleanPath(path));
                if (resource != null) {
                    Iterator<Resource> entries = resolver.listChildren(resource);
                    while (entries.hasNext()) {
                        paths.add(entries.next().getPath());
                    }
                }
            } catch (SlingException se) {
                log.warn("getResourcePaths: Cannot list children of " + path,
                    se);
            }
        }

        return paths.isEmpty() ? null : paths;
    }

    private Resource getResourceInternal(String path) throws SlingException {
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            return resolver.getResource(cleanPath(path));
        }

        return null;
    }

    // ---------- internal -----------------------------------------------------

    private Session getPrivateSession() throws RepositoryException {
        return requestResourceResolver.get().adaptTo(Session.class);
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

        return path;
    }

    private static class RepositoryOutputStream extends ByteArrayOutputStream {

        private final SlingIOProvider repositoryOutputProvider;

        private final String fileName;

        RepositoryOutputStream(SlingIOProvider repositoryOutputProvider,
                String fileName) {
            this.repositoryOutputProvider = repositoryOutputProvider;
            this.fileName = fileName;
        }

        public void close() throws IOException {
            super.close();

            Node parentNode = null;
            try {
                Session session = repositoryOutputProvider.getPrivateSession();
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
                    fileNode = createPath(fileName, "nt:folder", "nt:file", session);
                    parentNode = session.getRootNode();
                }

                // if we have a file node, create the contentNode
                if (fileNode != null) {
                    contentNode = fileNode.addNode("jcr:content", "nt:resource");
                }

                contentNode.setProperty("jcr:lastModified",
                    System.currentTimeMillis());
                contentNode.setProperty("jcr:data", new ByteArrayInputStream(
                    buf, 0, size()));
                contentNode.setProperty("jcr:mimeType",
                    "application/octet-stream");

                parentNode.save();
            } catch (RepositoryException re) {
                log.error("Cannot write file " + fileName, re);
                throw new IOException("Cannot write file " + fileName
                    + ", reason: " + re.toString());
            } finally {
                checkNode(parentNode, fileName);
            }
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     * In case it has to create the Node all non-existent intermediate path-elements
     * will be create with the given intermediate node type and the returned node
     * will be created with the given nodeType
     *
     * @param path to create
     * @param intermediateNodeType to use for creation of intermediate nodes
     * @param nodeType to use for creation of the final node
     * @param session to use
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    private static Node createPath(String path,
                                  String intermediateNodeType,
                                  String nodeType,
                                  Session session)
            throws RepositoryException {
        if (path == null || path.length() == 0 || "/".equals(path)) {
            return session.getRootNode();
        } else if (!session.itemExists(path)) {
            Node node = session.getRootNode();
            path = path.substring(1);
            int pos = path.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(path.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, intermediateNodeType);
                        } catch (RepositoryException re) {
                            // we ignore this as this folder might be created from a different task
                            node.refresh(false);
                        }
                    }
                    node = node.getNode(token);
                }
                path = path.substring(pos + 1);
            }
            if ( !node.hasNode(path) ) {
                node.addNode(path, nodeType);
            }
            return node.getNode(path);
        } else {
            return (Node) session.getItem(path);
        }
    }
}
