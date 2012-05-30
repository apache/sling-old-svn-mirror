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
package org.apache.sling.jcr.compiler.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.classloader.ClassLoaderWriter;

class JcrClassWriter implements ClassLoaderWriter {

    private final Node outputFolder;

    private static final String NT_FOLDER = "nt:folder";

    JcrClassWriter(Node outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(String path) {
        return new RepositoryOutputStream(this.outputFolder, path);
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
    private static boolean mkdirs(final Session session, String path) {
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

        } catch (RepositoryException re) {
            // discard changes
            try {
                session.refresh(false);
            } catch (RepositoryException e) {
                // we simply ignore this
            }
        }

        // false in case of error or no need to create
        return false;
    }

    private static final class RepositoryOutputStream extends ByteArrayOutputStream {

        private final String fileName;

        private final Node outputFolder;

        RepositoryOutputStream(final Node outputFolder,
                               final String fileName) {
            this.outputFolder = outputFolder;
            this.fileName = fileName;
        }

        /**
         * @see java.io.ByteArrayOutputStream#close()
         */
        public void close() throws IOException {
            super.close();

            try {
                String fullPath = this.outputFolder.getPath();
                if ( !fileName.startsWith("/") ) {
                    fullPath = fullPath + '/';
                }
                fullPath = fullPath + fileName;

                final int lastPos = fullPath.lastIndexOf('/');
                final String path = (lastPos == -1 ? null : fullPath.substring(0, lastPos));
                final String name = (lastPos == -1 ? fullPath : fullPath.substring(lastPos + 1));
                if ( lastPos != -1 ) {
                    if ( !JcrClassWriter.mkdirs(outputFolder.getSession(), path) ) {
                        throw new IOException("Unable to create path for " + path);
                    }
                }
                Node fileNode = null;
                Node contentNode = null;
                Node parentNode = null;
                if (outputFolder.getSession().itemExists(fullPath)) {
                    final Item item = outputFolder.getSession().getItem(fullPath);
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
                        outputFolder.getSession().save();
                        fileNode = parentNode.addNode(name, "nt:file");
                    }
                } else {
                    if (lastPos <= 0) {
                        parentNode = outputFolder.getSession().getRootNode();
                    } else {
                        Item parent = outputFolder.getSession().getItem(path);
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

                contentNode.setProperty("jcr:lastModified", System.currentTimeMillis());
                contentNode.setProperty("jcr:data", new ByteArrayInputStream(buf, 0, size()));
                contentNode.setProperty("jcr:mimeType", "application/octet-stream");

                outputFolder.getSession().save();
            } catch (RepositoryException re) {
                throw (IOException)new IOException("Cannot write file " + fileName + ", reason: " + re.toString()).initCause(re);
            }
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(String path) {
        // we don't need to implement this one
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String path) throws IOException {
        // we don't need to implement this one
        return null;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(String path) {
        // we don't need to implement this one
        return 0;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldPath, String newPath) {
        // we don't need to implement this one
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        // we don't need to implement this one
        return null;
    }
}
