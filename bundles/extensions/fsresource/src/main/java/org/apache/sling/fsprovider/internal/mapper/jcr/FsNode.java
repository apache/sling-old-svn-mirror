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
package org.apache.sling.fsprovider.internal.mapper.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
public final class FsNode extends FsItem implements Node {
    
    public FsNode(ContentFile contentFile, ResourceResolver resolver) {
        super(contentFile, resolver);
    }
    
    private String getPrimaryTypeName() {
        return props.get("jcr:primaryType", String.class);
    }
    
    private String[] getMixinTypeNames() {
        return props.get("jcr:mixinTypes", new String[0]);
    }
    
    @Override
    public String getName() throws RepositoryException {
        if (contentFile.getSubPath() == null) {
            return ResourceUtil.getName(contentFile.getPath());
        }
        else {
            return ResourceUtil.getName(contentFile.getSubPath());
        }
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return getNode(ResourceUtil.getParent(getPath()));
    }
    
    @Override
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        if (relPath == null) {
            throw new PathNotFoundException();
        }
        
        // get absolute node path
        String path = relPath;
        if (!StringUtils.startsWith(path,  "/")) {
            path = ResourceUtil.normalize(getPath() + "/" + relPath);
        }

        if (StringUtils.equals(path, contentFile.getPath()) || StringUtils.startsWith(path, contentFile.getPath() + "/")) {
            // node is contained in content file
            String subPath;
            if (StringUtils.equals(path, contentFile.getPath())) {
                subPath = null;
            }
            else {
                subPath = path.substring(contentFile.getPath().length() + 1);
            }
            ContentFile referencedFile = contentFile.navigateToAbsolute(subPath);
            if (referencedFile.hasContent()) {
                return new FsNode(referencedFile, resolver);
            }
        }
        
        // check if node is outside content file
        Node refNode = null;
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            refNode = resource.adaptTo(Node.class);
            if (refNode != null) {
                return refNode;
            }
        }

        throw new PathNotFoundException(relPath);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return new FsNodeIterator(contentFile, resolver);
    }

    @Override
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        if (props.containsKey(relPath)) {
            return new FsProperty(contentFile, resolver, relPath, this);
        }
        throw new PathNotFoundException(relPath);
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return new FsPropertyIterator(props.keySet().iterator(), contentFile, resolver, this);
    }

    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        String uuid = props.get("jcr:uuid", String.class);
        if (uuid != null) {
            return uuid;
        }
        else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    @Override
    public boolean hasNode(String relPath) throws RepositoryException {
        try {
            getNode(relPath);
            return true;
        }
        catch (RepositoryException ex) {
            return false;
        }
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return props.containsKey(relPath);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return !props.isEmpty();
    }

    @Override
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return StringUtils.equals(nodeTypeName, getPrimaryTypeName());
    }

    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return false;
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return false;
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        return false;
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        return false;
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return new FsNodeType(getPrimaryTypeName(), false);
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        String[] mixinTypeNames = getMixinTypeNames();
        NodeType[] mixinTypes = new NodeType[mixinTypeNames.length];
        for (int i=0; i<mixinTypeNames.length; i++) {
            mixinTypes[i] = new FsNodeType(mixinTypeNames[i], true);
        }
        return mixinTypes;
    }
    

    // --- unsupported methods ---
    
    @Override
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node addNode(String relPath, String primaryNodeTypeName)
            throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, ActivityViolationException,
            RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String srcWorkspace) throws NoSuchWorkspaceException, AccessDeniedException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSharedSet()
            throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeShare()
            throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(Version version, String relPath, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLock()
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void followLifecycleTransition(String transition)
            throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
