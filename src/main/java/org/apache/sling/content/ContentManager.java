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
package org.apache.sling.content;

import org.apache.sling.component.Content;

/**
 * The <code>ContentManager</code> interface is a generic interface to manage
 * <code>Content</code> objects through some OCM mapping layer.
 *
 * @see org.apache.sling.content.jcr.JcrContentManager
 */
public interface ContentManager {

    // Returns whether the content modification operations (create, update,
    // copy, move, delete) save the modifications immediately or not. This flag
    // has no influence on changes to the repository directly done through
    // the session of this ContentManager
    boolean isAutoSave();

    // Sets whether content modification operations (create, update, copy,
    // move, delete) save modifications immediately or not.
    void setAutoSave(boolean autoSave);

    // unconditionally saves updates in the ContentManager, which have not
    // been persisted yet
    void save();

    // Content related operations

    // store the content object at the location set in the path field
    void create(Content content);

    /**
     * Returns a <code>Content</code> object loaded from the repository node
     * to which <code>path</code> points. If no item exists at the given path
     * or if no <code>Content</code> object may be loaded from it,
     * <code>null</code> is returned.
     *
     * @param path The absolute path to the item to load
     * @return The <code>Content</code> object loaded from the path
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this content manager
     *             has no read access to the item.
     */
    Content load(String path);

    /**
     * Returns a <code>Content</code> object of the given concrete
     * <code>type</code> loaded from the repository node to which
     * <code>path</code> points. If no item exists at the given path or if no
     * <code>Content</code> object of the given concrete <code>type</code>
     * may be loaded from it, <code>null</code> is returned.
     *
     * @param path The absolute path to the item to load
     * @param type The required concrete type of the <code>Content</code>
     *            object.
     * @return The <code>Content</code> object loaded from the path
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this content manager
     *             has no read access to the item.
     */
    Content load(String path, Class type);

    void store(Content content);

    void copy(Content content, String destination, boolean deep);

    void move(Content content, String destination);

    void move(String source, String destination);

    void delete(Content content);

    void delete(String path);

    void orderBefore(Content content, String afterName);
}
