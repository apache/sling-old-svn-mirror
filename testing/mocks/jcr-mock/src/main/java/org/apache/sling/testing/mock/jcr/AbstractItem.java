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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

/**
 * Mock {@link Item} implementation.
 */
abstract class AbstractItem implements Item {

    protected final ItemData itemData;
    private final Session session;

    public AbstractItem(final ItemData itemData, final Session session) {
        this.itemData = itemData;
        this.session = session;
    }

    @Override
    public String getName() throws RepositoryException {
        return this.itemData.getName();
    }

    @Override
    public String getPath() throws RepositoryException {
        return this.itemData.getPath();
    }

    @Override
    public Node getParent() throws RepositoryException {
        return (Node) getSession().getItem(ResourceUtil.getParent(getPath()));
    }

    @Override
    public Session getSession()throws RepositoryException {
        return this.session;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public Item getAncestor(final int depth) throws RepositoryException {
        if (depth < 0 || depth > getDepth()) {
            throw new ItemNotFoundException();
        }
        return this.session.getItem(ResourceUtil.getParent(getPath(), depth));
    }

    protected String makeAbsolutePath(final String relativePath) throws RepositoryException {
        String absolutePath = relativePath;
        // ensure the path is absolute and normalized
        if (!StringUtils.startsWith(absolutePath, "/")) {
            absolutePath = getPath() + "/" + absolutePath; // NOPMD
        }
        return ResourceUtil.normalize(absolutePath);
    }

    protected MockSession getMockedSession() {
        return (MockSession) this.session;
    }

    @Override
    public void remove() throws RepositoryException {
        getSession().removeItem(getPath());
    }

    @Override
    public int getDepth() throws RepositoryException {
        if (StringUtils.equals("/", getPath())) {
            return 0;
        } else {
            return StringUtils.countMatches(getPath(), "/");
        }
    }

    // --- unsupported operations ---
    @Override
    public void accept(final ItemVisitor visitor) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSame(final Item otherItem) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final boolean keepChanges) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
