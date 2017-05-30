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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
abstract class FsItem implements Item {
    
    protected final ContentFile contentFile;
    protected final ResourceResolver resolver;
    protected final ValueMap props;
    
    public FsItem(ContentFile contentFile, ResourceResolver resolver) {
        this.contentFile = contentFile;
        this.resolver = resolver;
        this.props = contentFile.getValueMap();
    }

    @Override
    public String getPath() throws RepositoryException {
        if (contentFile.getSubPath() == null) {
            return contentFile.getPath();
        }
        else {
            return contentFile.getPath() + "/" + contentFile.getSubPath();
        }
    }

    @Override
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        String path;
        if (depth == 0) {
            path = "/";
        }
        else {
            String[] pathParts = StringUtils.splitPreserveAllTokens(getPath(), "/");
            path = StringUtils.join(pathParts, "/", 0, depth + 1);
        }
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            Node refNode = resource.adaptTo(Node.class);
            if (refNode != null) {
                return refNode;
            }
        }
        throw new ItemNotFoundException();
    }

    @Override
    public int getDepth() throws RepositoryException {
        if (StringUtils.equals("/", getPath())) {
            return 0;
        } else {
            return StringUtils.countMatches(getPath(), "/");
        }
    }

    @Override
    public Session getSession() throws RepositoryException {
        return resolver.adaptTo(Session.class);
    }

    @Override
    public boolean isNode() {
        return (this instanceof Node);
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isSame(Item otherItem) throws RepositoryException {
        return StringUtils.equals(getPath(), otherItem.getPath());
    }

    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // do nothing
    }
    
    @Override
    public String toString() {
        try {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("path", getPath())
                    .build();
        }
        catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    // --- unsupported methods ---

    @Override
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
            InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException,
            NoSuchNodeTypeException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, AccessDeniedException,
            RepositoryException {
        throw new UnsupportedOperationException();
    }

}
