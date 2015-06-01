/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to hold a list of items.
 */
public class ItemList<T extends Comparable>
    extends Commentable
    implements Iterable<T> {

    /** The list holding the items. */
    protected final List<T> items = new ArrayList<T>();

    /**
     * Add a new item
     * @param item The new item
     */
    public void add(final T item) {
        this.items.add(item);
        Collections.sort(this.items);
    }

    /**
     * Remove an item.
     * @param item The item to remove.
     */
    public void remove(final T item) {
        this.items.remove(item);
    }

    @Override
    public Iterator<T> iterator() {
        return this.items.iterator();
    }

    /**
     * Check if the list is empty.
     * @return {@code true} if the list is empty.
     */
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
