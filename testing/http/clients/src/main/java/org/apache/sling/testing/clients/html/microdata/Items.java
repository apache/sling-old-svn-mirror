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
package org.apache.sling.testing.clients.html.microdata;

/**
 * A composite collection to represent zero or more {@link Item}. When calling operation exposed by {@link Item}
 * interface, the first item in the collection is used.
 */
public interface Items extends Iterable<Item>, Item {

    /**
     * Returns the item at given index.
     *
     * @param index the index
     * @return the item
     * @throws IndexOutOfBoundsException if the index is not found
     */
    Item at(int index) throws IndexOutOfBoundsException;

    /**
     * @return the amount of items contained in this collection.
     */
    int length();
}
