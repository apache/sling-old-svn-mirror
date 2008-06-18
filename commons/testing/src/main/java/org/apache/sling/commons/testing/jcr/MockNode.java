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
package org.apache.sling.commons.testing.jcr;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

// simple mock implementation of a node
public class MockNode implements Node {

    private String path;
    private Map <String, Property> properties = new HashMap <String, Property>();

    private NodeType nodeType;

    public MockNode(String path) {
        this(path, null);
    }

    public MockNode(String path, String type) {
        this.path = path;
        this.nodeType = new MockNodeType(type);
    }

    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public Node getParent() {
        return new MockNode(path.substring(0, path.lastIndexOf('/')));
    }

    public String getPath() {
        return path;
    }

    public NodeType getPrimaryNodeType() {
        return nodeType;
    }

    public boolean isSame(Item otherItem) {
        return equals(otherItem);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof MockNode)) {
            return false;
        }

        return ((MockNode) obj).getPath().equals(getPath());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "MockNode: path=" + getPath();
    }

    public void addMixin(String mixinName) {
    }

    public Node addNode(String relPath) {
        return null;
    }

    public Node addNode(String relPath, String primaryNodeTypeName) {
        return null;
    }

    public boolean canAddMixin(String mixinName) {
        return false;
    }

    public void cancelMerge(Version version) {

    }

    public Version checkin() {
        return null;
    }

    public void checkout() {
    }

    public void doneMerge(Version version) {
    }

    public Version getBaseVersion() {
        return null;
    }

    public String getCorrespondingNodePath(String workspaceName) {
        return null;
    }

    public NodeDefinition getDefinition() {
        return null;
    }

    public int getIndex() {
        return 0;
    }

    public Lock getLock() {
        return null;
    }

    public NodeType[] getMixinNodeTypes() {
        return null;
    }

    public Node getNode(String relPath) {
        return new MockNode(path + "/" + relPath);
    }

    public NodeIterator getNodes() {
        return new MockNodeIterator();
    }

    public NodeIterator getNodes(String namePattern) {
        return new MockNodeIterator();
    }

    public Item getPrimaryItem() {
        return null;
    }

    public PropertyIterator getProperties() {
        return new MockPropertyIterator(properties.values().iterator());
    }

    public PropertyIterator getProperties(String namePattern) {
        return null;
    }

    public Property getProperty(String relPath) {
        return properties.get(relPath);
    }

    public PropertyIterator getReferences() {
        return null;
    }

    public String getUUID() {
        return null;
    }

    public VersionHistory getVersionHistory() {
        return null;
    }

    public boolean hasNode(String relPath) {
        return false;
    }

    public boolean hasNodes() {
        return false;
    }

    public boolean hasProperties() {
        return false;
    }

    public boolean hasProperty(String relPath) {
        return properties.containsKey(relPath);
    }

    public boolean holdsLock() {
        return false;
    }

    public boolean isCheckedOut() {
        return false;
    }

    public boolean isLocked() {
        return false;
    }

    public boolean isNodeType(String nodeTypeName) {
        return false;
    }

    public Lock lock(boolean isDeep, boolean isSessionScoped) {
        return null;
    }

    public NodeIterator merge(String srcWorkspace, boolean bestEffort) {
        return null;
    }

    public void orderBefore(String srcChildRelPath, String destChildRelPath) {
    }

    public void removeMixin(String mixinName) {
    }

    public void restore(String versionName, boolean removeExisting) {
    }

    public void restore(Version version, boolean removeExisting) {
    }

    public void restore(Version version, String relPath, boolean removeExisting) {
    }

    public void restoreByLabel(String versionLabel, boolean removeExisting) {
    }

    public Property setProperty(String name, Value value) {
        return null;
    }

    public Property setProperty(String name, Value[] values) {
        return null;
    }

    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        MockProperty p = new MockProperty(name);
        p.setValue(values);
        properties.put(name, p);
        return p;
    }

    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        MockProperty p = new MockProperty(name);
        p.setValue(value);
        properties.put(name, p);
        return p;
    }

    public Property setProperty(String name, InputStream value) {
        return null;
    }

    public Property setProperty(String name, boolean value) {
        return null;
    }

    public Property setProperty(String name, double value) {
        return null;
    }

    public Property setProperty(String name, long value) {
        return null;
    }

    public Property setProperty(String name, Calendar value) {
        return null;
    }

    public Property setProperty(String name, Node value) {
        return null;
    }

    public Property setProperty(String name, Value value, int type) {
        return null;
    }

    public Property setProperty(String name, Value[] values, int type) {
        return null;
    }

    public Property setProperty(String name, String[] values, int type) {
        return null;
    }

    public Property setProperty(String name, String value, int type) {
        return null;
    }

    public void unlock() {
    }

    public void update(String srcWorkspaceName) {
    }

    public void accept(ItemVisitor visitor) {
    }

    public Item getAncestor(int depth) {
        return null;
    }

    public int getDepth() {
        return 0;
    }

    public Session getSession() {
        return null;
    }

    public boolean isModified() {
        return false;
    }

    public boolean isNew() {
        return false;
    }

    public boolean isNode() {
        return true;
    }

    public void refresh(boolean keepChanges) {
    }

    public void remove() {
    }

    public void save() {
    }

}
