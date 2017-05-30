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

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.xml.sax.ContentHandler;

/**
 * Mock {@link Session} implementation. This instance holds the JCR data in a
 * simple ordered map.
 */
class MockSession implements Session {

    private final MockRepository repository;
    private final Workspace workspace;
    private final Map<String, ItemData> items;
    private final String userId;
    private boolean isLive;
    private boolean hasKnownChanges;

    public MockSession(MockRepository repository, Map<String, ItemData> items,
            String userId, String workspaceName) throws RepositoryException {
        this.repository = repository;
        this.workspace = new MockWorkspace(repository, this, workspaceName);
        this.items = items;
        this.userId = userId;
        isLive = true;
        hasKnownChanges = false;
        this.save();
    }

    private void checkLive() throws RepositoryException {
        if (!isLive) {
            throw new RepositoryException("Session is logged out / not live.");
        }
    }

    @Override
    public ValueFactory getValueFactory() throws RepositoryException {
        checkLive();
        return ValueFactoryImpl.getInstance();
    }

    @Override
    public Item getItem(final String absPath) throws RepositoryException {
        checkLive();
        final ItemData itemData = getItemData(absPath);
        if (itemData != null) {
            if (itemData.isNode()) {
                return new MockNode(itemData, this);
            }
            else {
                return new MockProperty(itemData, this);
            }
        } else {
            throw new PathNotFoundException(String.format("No item found at: %s.", absPath));
        }
    }

    @Override
    public Node getNode(final String absPath) throws RepositoryException {
        checkLive();
        Item item = getItem(absPath);
        if (item instanceof Node) {
            return (Node) item;
        } else {
            throw new PathNotFoundException(String.format("No node found at: %s.", absPath));
        }
    }

    @Override
    public Node getNodeByIdentifier(final String id) throws RepositoryException {
        checkLive();
        for (ItemData item : this.items.values()) {
            if (item.isNode() && StringUtils.equals(item.getUuid(), id)) {
                return new MockNode(item, this);
            }
        }
        throw new ItemNotFoundException(String.format("No node found with id: %s.", id));
    }

    @Override
    public Property getProperty(final String absPath) throws RepositoryException {
        checkLive();
        Item item = getItem(absPath);
        if (item instanceof Property) {
            return (Property) item;
        } else {
            throw new PathNotFoundException(String.format("No property found at: %s.", absPath));
        }
    }

    @Override
    public boolean nodeExists(final String absPath) throws RepositoryException {
        checkLive();
        return itemExists(absPath) && getItemData(absPath).isNode();
    }

    @Override
    public boolean propertyExists(final String absPath) throws RepositoryException {
        checkLive();
        return itemExists(absPath) && getItemData(absPath).isProperty();
    }

    @Override
    public void removeItem(final String absPath) throws RepositoryException {
        checkLive();
        removeItemWithChildren(absPath);
    }

    @Override
    public Node getRootNode() throws RepositoryException {
        checkLive();
        return getNode("/");
    }

    @Override
    public Node getNodeByUUID(final String uuid) throws RepositoryException {
        checkLive();
        return getNodeByIdentifier(uuid);
    }

    /**
     * Add item
     * @param itemData item data
     */
    void addItem(final ItemData itemData) {
        this.items.put(itemData.getPath(), itemData);
    }

    private ItemData getItemData(final String absPath) {
        final String normalizedPath = ResourceUtil.normalize(absPath);
        return this.items.get(normalizedPath);
    }

    /**
     * Remove item incl. children
     * @param absPath Item path
     */
    private void removeItemWithChildren(final String absPath) throws RepositoryException {
        if (!itemExists(absPath)) {
            return;
        }

        final ItemData parent = getItemData(absPath);
        final String descendantPrefix = parent.getPath() + "/";

        final List<String> pathsToRemove = new ArrayList<String>();
        pathsToRemove.add(parent.getPath());
        for (String itemPath : this.items.keySet()) {
            if (itemPath.startsWith(descendantPrefix)) {
                pathsToRemove.add(itemPath);
            }
        }
        for (String pathToRemove : pathsToRemove) {
            this.items.remove(pathToRemove);
        }

        hasKnownChanges = true;
    }

    RangeIterator listChildren(final String parentPath, final ItemFilter filter) throws RepositoryException {
        List<Item> children = new ArrayList<Item>();

        //remove trailing slash or make root path / empty string
        final String path = parentPath.replaceFirst("/$", "");

        // build regex pattern for all child paths of parent
        Pattern pattern = Pattern.compile("^" + Pattern.quote(path) + "/[^/]+$");

        // collect child resources
        for (ItemData item : this.items.values()) {
            if (pattern.matcher(item.getPath()).matches() && (filter == null || filter.accept(item))) {
                children.add(item.getItem(this));
            }
        }

        return new RangeIteratorAdapter(children.iterator(), children.size());
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        checkLive();

        if (hasKnownChanges) {
            return true;
        }

        for (final ItemData item : this.items.values()) {
            if (item.isNew() || item.isChanged()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean itemExists(final String absPath) throws RepositoryException {
        checkLive();
        return getItemData(absPath) != null;
    }

    @Override
    public Workspace getWorkspace() {
        return this.workspace;
    }

    @Override
    public String getUserID() {
        return this.userId;
    }

    @Override
    public String getNamespacePrefix(final String uri) throws RepositoryException {
        checkLive();
        return getWorkspace().getNamespaceRegistry().getPrefix(uri);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        checkLive();
        return getWorkspace().getNamespaceRegistry().getPrefixes();
    }

    @Override
    public String getNamespaceURI(final String prefix) throws RepositoryException {
        checkLive();
        return getWorkspace().getNamespaceRegistry().getURI(prefix);
    }

    @Override
    public void setNamespacePrefix(final String prefix, final String uri) throws RepositoryException {
        checkLive();
        getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public void save() throws RepositoryException {
        checkLive();
        // reset new flags
        for (ItemData itemData : this.items.values()) {
            itemData.setIsNew(false);
            itemData.setIsChanged(false);
        }

        hasKnownChanges = false;
    }

    @Override
    public void refresh(final boolean keepChanges) throws RepositoryException {
        // do nothing
        checkLive();
    }

    @Override
    public void checkPermission(final String absPath, final String actions) throws RepositoryException {
        // always grant permission
        checkLive();
    }

    @Override
    public boolean isLive() {
        return isLive;
    }

    @Override
    public void logout() {
        isLive = false;
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public String[] getAttributeNames() {
        return new String[0];
    }

    
    // --- unsupported operations ---
    @Override
    public void addLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary,
            final boolean noRecurse) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDocumentView(final String absPath, final OutputStream out, final boolean skipBinary,
            final boolean noRecurse) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final ContentHandler contentHandler, final boolean skipBinary,
            final boolean noRecurse) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSystemView(final String absPath, final OutputStream out, final boolean skipBinary,
            final boolean noRecurse) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session impersonate(final Credentials credentials) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLockToken(final String lt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlManager getAccessControlManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RetentionManager getRetentionManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCapability(final String methodName, final Object target, final Object[] arguments) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
