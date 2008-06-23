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
package org.apache.sling.jcr.contentloader.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * The <code>ContentLoader</code> creates the nodes and properties.
 */
public class ContentLoader implements ContentCreator {

    private PathEntry configuration;

    private final Stack<Node> parentNodeStack = new Stack<Node>();

    /** The list of versionables. */
    private final List<Node> versionables = new ArrayList<Node>();

    /** Delayed references during content loading for the reference property. */
    private final Map<String, List<String>> delayedReferences = new HashMap<String, List<String>>();

    private String defaultRootName;

    private Node rootNode;

    private boolean isRootNodeImport;

    // default content type for createFile()
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ContentLoaderService jcrContentHelper;

    public ContentLoader(ContentLoaderService jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;
    }

    /**
     * Initialize this component.
     * If the defaultRootName is null, we are in ROOT_NODE import mode.
     * @param pathEntry
     * @param parentNode
     * @param defaultRootName
     */
    public void init(final PathEntry pathEntry,
                     final Node parentNode,
                     final String defaultRootName) {
        this.configuration = pathEntry;
        this.parentNodeStack.clear();
        this.parentNodeStack.push(parentNode);
        this.defaultRootName = defaultRootName;
        this.rootNode = null;
        isRootNodeImport = defaultRootName == null;
    }

    public List<Node> getVersionables() {
        return this.versionables;
    }

    public void clear() {
        this.versionables.clear();
    }

    public Node getRootNode() {
        return this.rootNode;
    }


    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createNode(java.lang.String, java.lang.String, java.lang.String[])
     */
    public Node createNode(String name,
                           String primaryNodeType,
                           String[] mixinNodeTypes)
    throws RepositoryException {
        final Node parentNode = this.parentNodeStack.peek();
        if ( name == null ) {
            if ( this.parentNodeStack.size() > 1 ) {
                throw new RepositoryException("Node needs to have a name.");
            }
            name = this.defaultRootName;
        }

        // if we are in root node import mode, we don't create the root top level node!
        if ( !isRootNodeImport || this.parentNodeStack.size() > 1 ) {
            // if node already exists but should be overwritten, delete it
            if (this.configuration.isOverwrite() && parentNode.hasNode(name)) {
                parentNode.getNode(name).remove();
            }

            // ensure repository node
            Node node;
            if (parentNode.hasNode(name)) {

                // use existing node
                node = parentNode.getNode(name);

            } else if (primaryNodeType == null) {

                // node explicit node type, use repository default
                node = parentNode.addNode(name);

            } else {

                // explicit primary node type
                node = parentNode.addNode(name, primaryNodeType);
            }

            // ammend mixin node types
            if (mixinNodeTypes != null) {
                for (final String mixin : mixinNodeTypes) {
                    if (!node.isNodeType(mixin)) {
                        node.addMixin(mixin);
                    }
                }
            }

            // check if node is versionable
            final boolean addToVersionables = this.configuration.isCheckin()
                                        && node.isNodeType("mix:versionable");
            if ( addToVersionables ) {
                this.versionables.add(node);
            }

            this.parentNodeStack.push(node);
            if ( this.rootNode == null ) {
                this.rootNode = node;
            }
            return node;
        }
        return null;
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, int, java.lang.String)
     */
    public void createProperty(String name, int propertyType, String value)
    throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name)
            && !node.getProperty(name).isNew()) {
            return;
        }

        if ( propertyType == PropertyType.REFERENCE ) {
            // need to resolve the reference
            String propPath = node.getPath() + "/" + name;
            String uuid = getUUID(node.getSession(), propPath, value);
            if (uuid != null) {
                node.setProperty(name, uuid, propertyType);
            }

        } else if ("jcr:isCheckedOut".equals(name)) {

            // don't try to write the property but record its state
            // for later checkin if set to false
            final boolean checkedout = Boolean.valueOf(value);
            if (!checkedout) {
                if ( !this.versionables.contains(node) ) {
                    this.versionables.add(node);
                }
            }
        } else {
            node.setProperty(name, value, propertyType);
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, int, java.lang.String[])
     */
    public void createProperty(String name, int propertyType, String[] values)
    throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name)
            && !node.getProperty(name).isNew()) {
            return;
        }
        node.setProperty(name, values, propertyType);
    }

    protected Value createValue(final ValueFactory factory, Object value) {
        if ( value == null ) {
            return null;
        }
        if ( value instanceof Long ) {
            return factory.createValue((Long)value);
        } else if ( value instanceof Date ) {
            final Calendar c = Calendar.getInstance();
            c.setTime((Date)value);
            return factory.createValue(c);
        } else if ( value instanceof Calendar ) {
            return factory.createValue((Calendar)value);
        } else if ( value instanceof Double ) {
            return factory.createValue((Double)value);
        } else if ( value instanceof Boolean ) {
            return factory.createValue((Boolean)value);
        } else if ( value instanceof InputStream ) {
            return factory.createValue((InputStream)value);
        } else {
            return factory.createValue(value.toString());
        }

    }
    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, java.lang.Object)
     */
    public void createProperty(String name, Object value)
    throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name)
            && !node.getProperty(name).isNew()) {
            return;
        }
        if ( value == null ) {
            if ( node.hasProperty(name) ) {
                node.getProperty(name).remove();
            }
        } else {
            final Value jcrValue = this.createValue(node.getSession().getValueFactory(), value);
            node.setProperty(name, jcrValue);
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, java.lang.Object[])
     */
    public void createProperty(String name, Object[] values)
    throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name)
            && !node.getProperty(name).isNew()) {
            return;
        }
        if ( values == null || values.length == 0 ) {
            if ( node.hasProperty(name) ) {
                node.getProperty(name).remove();
            }
        } else {
            final Value[] jcrValues = new Value[values.length];
            for(int i = 0; i < values.length; i++) {
                jcrValues[i] = this.createValue(node.getSession().getValueFactory(), values[i]);
            }
            node.setProperty(name, jcrValues);
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#finishNode()
     */
    public void finishNode()
    throws RepositoryException {
        final Node node = this.parentNodeStack.pop();
        // resolve REFERENCE property values pointing to this node
        resolveReferences(node);
    }

    private String getUUID(Session session, String propPath,
                          String referencePath)
    throws RepositoryException {
        if (session.itemExists(referencePath)) {
            Item item = session.getItem(referencePath);
            if (item.isNode()) {
                Node refNode = (Node) item;
                if (refNode.isNodeType("mix:referenceable")) {
                    return refNode.getUUID();
                }
            }
        } else {
            // not existing yet, keep for delayed setting
            List<String> current = delayedReferences.get(referencePath);
            if (current == null) {
                current = new ArrayList<String>();
                delayedReferences.put(referencePath, current);
            }
            current.add(propPath);
        }

        // no UUID found
        return null;
    }

    private void resolveReferences(Node node) throws RepositoryException {
        List<String> props = delayedReferences.remove(node.getPath());
        if (props == null || props.size() == 0) {
            return;
        }

        // check whether we can set at all
        if (!node.isNodeType("mix:referenceable")) {
            return;
        }

        Session session = node.getSession();
        String uuid = node.getUUID();

        for (String property : props) {
            String name = getName(property);
            Node parentNode = getParentNode(session, property);
            if (parentNode != null) {
                parentNode.setProperty(name, uuid, PropertyType.REFERENCE);
            }
        }
    }

    /**
     * Gets the name part of the <code>path</code>. The name is
     * the part of the path after the last slash (or the complete path if no
     * slash is contained).
     *
     * @param path The path from which to extract the name part.
     * @return The name part.
     */
    private String getName(String path) {
        int lastSlash = path.lastIndexOf('/');
        String name = (lastSlash < 0) ? path : path.substring(lastSlash + 1);

        return name;
    }

    private Node getParentNode(Session session, String path)
            throws RepositoryException {
        int lastSlash = path.lastIndexOf('/');

        // not an absolute path, cannot find parent
        if (lastSlash < 0) {
            return null;
        }

        // node below root
        if (lastSlash == 0) {
            return session.getRootNode();
        }

        // item in the hierarchy
        path = path.substring(0, lastSlash);
        if (!session.itemExists(path)) {
            return null;
        }

        Item item = session.getItem(path);
        return (item.isNode()) ? (Node) item : null;
    }


    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createFileAndResourceNode(java.lang.String, java.io.InputStream, java.lang.String, long)
     */
    public void createFileAndResourceNode(String name,
                                          InputStream data,
                                          String mimeType,
                                          long lastModified)
    throws RepositoryException {
        int lastSlash = name.lastIndexOf('/');
        name = (lastSlash < 0) ? name : name.substring(lastSlash + 1);
        final Node parentNode = this.parentNodeStack.peek();

        // if node already exists but should be overwritten, delete it
        if (this.configuration.isOverwrite() && parentNode.hasNode(name)) {
            parentNode.getNode(name).remove();
        } else if (parentNode.hasNode(name)) {
            return;
        }

        // ensure content type
        if (mimeType == null) {
            mimeType = jcrContentHelper.getMimeType(name);
            if (mimeType == null) {
                jcrContentHelper.log.info(
                    "createFile: Cannot find content type for {}, using {}",
                    name, DEFAULT_CONTENT_TYPE);
                mimeType = DEFAULT_CONTENT_TYPE;
            }
        }

        // ensure sensible last modification date
        if (lastModified <= 0) {
            lastModified = System.currentTimeMillis();
        }

        this.createNode(name, "nt:file", null);
        this.createNode("jcr:content", "nt:resource", null);
        this.createProperty("jcr:mimeType", mimeType);
        this.createProperty("jcr:lastModified", lastModified);
        this.createProperty("jcr:data", data);
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#switchCurrentNode(java.lang.String, java.lang.String)
     */
    public boolean switchCurrentNode(String subPath, String newNodeType)
    throws RepositoryException {
        if ( this.parentNodeStack.size() > 1 ) {
            throw new RepositoryException("Switching the current node is not allowed.");
        }
        if ( subPath.startsWith("/") ) {
            subPath = subPath.substring(1);
        }
        final StringTokenizer st = new StringTokenizer(subPath, "/");
        Node node = this.parentNodeStack.peek();
        while ( st.hasMoreTokens() ) {
            final String token = st.nextToken();
            if ( !node.hasNode(token) ) {
                if ( newNodeType == null ) {
                    return false;
                }
                node = node.addNode(token, newNodeType);
            }
        }
        this.parentNodeStack.push(node);
        return true;
    }

}
