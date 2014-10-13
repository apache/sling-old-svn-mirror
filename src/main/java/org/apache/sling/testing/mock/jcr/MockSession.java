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
package org.apache.sling.testing.mock.jcr;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RangeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;

import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.xml.sax.ContentHandler;

/**
 * Mock {@link Session} implementation. This instance holds the JCR data in a
 * simple ordered map.
 */
class MockSession implements Session {

    private final Repository repository;
    private final Workspace workspace;

    // Use linked hashmap to ensure ordering when adding items is preserved.
    private final Map<String, Item> items = new LinkedHashMap<String, Item>();

    public MockSession(final Repository repository) {
        this.repository = repository;
        this.workspace = new MockWorkspace(this);
        this.items.put("/", new MockNode("/", this, MockNodeTypes.NT_UNSTRUCTURED));
    }

    @Override
    public ValueFactory getValueFactory() {
        return ValueFactoryImpl.getInstance();
    }

    @Override
    public Item getItem(final String absPath) throws RepositoryException {
        Item item = this.items.get(absPath);
        if (item != null) {
            return item;
        } else {
            throw new PathNotFoundException(String.format("No item found at: %s.", absPath));
        }
    }

    @Override
    public Node getNode(final String absPath) throws RepositoryException {
        Item item = getItem(absPath);
        if (item instanceof Node) {
            return (Node) item;
        } else {
            throw new PathNotFoundException(String.format("No node found at: %s.", absPath));
        }
    }

    @Override
    public Node getNodeByIdentifier(final String id) throws RepositoryException {
        for (Item item : this.items.values()) {
            if (item instanceof Node) {
                Node node = (Node) item;
                if (node.getIdentifier().equals(id)) {
                    return node;
                }
            }
        }
        throw new ItemNotFoundException(String.format("No node found with id: %s.", id));
    }

    @Override
    public Property getProperty(final String absPath) throws RepositoryException {
        Item item = getItem(absPath);
        if (item instanceof Property) {
            return (Property) item;
        } else {
            throw new PathNotFoundException(String.format("No property found at: %s.", absPath));
        }
    }

    @Override
    public boolean nodeExists(final String absPath) throws RepositoryException {
        try {
            getNode(absPath);
            return true;
        } catch (PathNotFoundException ex) {
            return false;
        }
    }

    @Override
    public boolean propertyExists(final String absPath) throws RepositoryException {
        try {
            getProperty(absPath);
            return true;
        } catch (PathNotFoundException ex) {
            return false;
        }
    }

    @Override
    public void removeItem(final String absPath) {
        removeItemWithChildren(absPath);
    }

    @Override
    public Node getRootNode() {
        return (Node) this.items.get("/");
    }

    @Override
    public Node getNodeByUUID(final String uuid) throws RepositoryException {
        return getNodeByIdentifier(uuid);
    }

    /**
     * Add item
     * @param item item
     * @throws RepositoryException
     */
    void addItem(final Item item) throws RepositoryException {
        this.items.put(item.getPath(), item);
    }

    /**
     * Remove item incl. children
     * @param path Item path
     */
    void removeItemWithChildren(final String path) {
        List<String> pathsToRemove = new ArrayList<String>();

        // build regex pattern for node and all its children
        Pattern pattern = Pattern.compile("^" + Pattern.quote(path) + "(/.+)?$");

        for (String itemPath : this.items.keySet()) {
            if (pattern.matcher(itemPath).matches()) {
                pathsToRemove.add(itemPath);
            }
        }
        for (String pathToRemove : pathsToRemove) {
            this.items.remove(pathToRemove);
        }
    }

    RangeIterator listChildren(final String parentPath, final ItemFilter filter) throws RepositoryException {
        List<Item> children = new ArrayList<Item>();

        // build regex pattern for all child paths of parent
        Pattern pattern = Pattern.compile("^" + Pattern.quote(parentPath) + "/[^/]+$");

        // collect child resources
        for (Item item : this.items.values()) {
            if (pattern.matcher(item.getPath()).matches() && (filter == null || filter.accept(item))) {
                children.add(item);
            }
        }

        return new RangeIteratorAdapter(children.iterator(), children.size());
    }

    @Override
    public boolean hasPendingChanges() {
        return false;
    }

    @Override
    public boolean itemExists(final String absPath) {
        return this.items.get(absPath) != null;
    }

    @Override
    public Workspace getWorkspace() {
        return this.workspace;
    }

    @Override
    public String getUserID() {
        return "mockedUserId";
    }

    @Override
    public String getNamespacePrefix(final String uri) throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getPrefix(uri);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getPrefixes();
    }

    @Override
    public String getNamespaceURI(final String prefix) throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getURI(prefix);
    }

    @Override
    public void setNamespacePrefix(final String prefix, final String uri) throws RepositoryException {
        getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public void refresh(final boolean keepChanges) throws RepositoryException {
        // do nothing
    }

    @Override
    public void checkPermission(final String absPath, final String actions) {
        // always grant permission
    }

    // --- unsupported operations ---
    @Override
    public void addLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary,
            final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final OutputStream out, final boolean skipBinary,
            final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary,
            final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final OutputStream out, final boolean skipBinary,
            final boolean noRecurse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session impersonate(final Credentials credentials) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlManager getAccessControlManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RetentionManager getRetentionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCapability(final String methodName, final Object target, final Object[] arguments) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) {
        throw new UnsupportedOperationException();
    }

}
