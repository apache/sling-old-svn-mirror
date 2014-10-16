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
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;

/**
 * Mock {@link Node} implementation
 */
class MockNode extends AbstractItem implements Node {

    public MockNode(final ItemData itemData, final Session session) {
        super(itemData, session);
    }

    @Override
    public Node addNode(final String relPath) throws RepositoryException {
        return addNode(relPath, JcrConstants.NT_UNSTRUCTURED);
    }

    @Override
    public Node addNode(final String relPath, final String primaryNodeTypeName) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        ItemData itemData = ItemData.newNode(path, new MockNodeType(primaryNodeTypeName));
        Node node = new MockNode(itemData, getSession());
        getMockedSession().addItem(itemData);
        return node;
    }

    @Override
    public Node getNode(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().getNode(path);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) {
                return item.isNode();
            }
        });
        return new NodeIteratorAdapter(items, items.getSize());
    }

    @Override
    public NodeIterator getNodes(final String namePattern) throws RepositoryException {
        final Pattern pattern = Pattern.compile(namePattern);
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isNode() && pattern.matcher(item.getName()).matches();
            }
        });
        return new NodeIteratorAdapter(items, items.getSize());
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) {
                return item.isProperty();
            }
        });
        return new PropertyIteratorAdapter(items, items.getSize());
    }

    @Override
    public PropertyIterator getProperties(final String namePattern) throws RepositoryException {
        final Pattern pattern = Pattern.compile(namePattern);
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isProperty() && pattern.matcher(item.getName()).matches();
            }
        });
        return new PropertyIteratorAdapter(items, items.getSize());
    }

    @Override
    public Property getProperty(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().getProperty(path);
    }

    @Override
    public String getIdentifier() {
        return this.itemData.getUuid();
    }

    @Override
    public String getUUID() {
        return getIdentifier();
    }

    @Override
    public boolean hasNode(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().nodeExists(path);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return getProperties().hasNext();
    }

    @Override
    public boolean hasProperty(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().propertyExists(path);
    }

    @Override
    public Property setProperty(final String name, final Value value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Value[] values) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(values);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final String[] values) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(values);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final String value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Property setProperty(final String name, final InputStream value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final boolean value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final double value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final long value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Calendar value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Node value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Binary value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final BigDecimal value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isNodeType(final String nodeTypeName) throws RepositoryException {
        return this.itemData.getNodeType().isNodeType(nodeTypeName);
    }

    @Override
    public NodeType getPrimaryNodeType() {
        return this.itemData.getNodeType();
    }

    @Override
    public Item getPrimaryItem() throws RepositoryException {
        // support "jcr:content" node and "jcr:data" property as primary items
        if (hasProperty(JcrConstants.JCR_DATA)) {
            return getProperty(JcrConstants.JCR_DATA);
        } else if (hasNode(JcrConstants.JCR_CONTENT)) {
            return getNode(JcrConstants.JCR_CONTENT);
        } else {
            throw new ItemNotFoundException();
        }
    }
    
    @Override
    public int hashCode() {
        return itemData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockNode) {
            return itemData.equals(((MockNode)obj).itemData);
        }
        return false;
    }

    // --- unsupported operations ---
    @Override
    public Property setProperty(final String name, final Value value, final int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Value[] values, final int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final String[] values, final int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final String value, final int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMixin(final String pMixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddMixin(final String pMixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelMerge(final Version pVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version checkin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doneMerge(final Version pVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getBaseVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCorrespondingNodePath(final String workspaceName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType[] getMixinNodeTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionHistory getVersionHistory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean holdsLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(final boolean isDeep, final boolean isSessionScoped) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final String srcWorkspace, final boolean bestEffort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void orderBefore(final String srcChildRelPath, final String destChildRelPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMixin(final String mixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final String versionName, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final String relPath, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreByLabel(final String versionLabel, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(final String srcWorkspaceName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void followLifecycleTransition(final String transition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getNodes(final String[] nameGlobs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getProperties(final String[] nameGlobs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getSharedSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeShare() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSharedSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrimaryType(final String pNodeTypeName) {
        throw new UnsupportedOperationException();
    }

}
