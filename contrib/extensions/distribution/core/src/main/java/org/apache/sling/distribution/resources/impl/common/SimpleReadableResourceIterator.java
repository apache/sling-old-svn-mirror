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

package org.apache.sling.distribution.resources.impl.common;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Iterator;
import java.util.Map;

/**
 * Read only resource collection.
 */
public class SimpleReadableResourceIterator  implements Iterator<Resource> {
    private final Iterator<Map<String, Object>> itemsIterator;
    private final ResourceResolver resourceResolver;
    private final String parentPath;

    SimpleReadableResourceIterator(Iterator<Map<String, Object>> itemsIterator, ResourceResolver resourceResolver, String parentPath) {

        this.itemsIterator = itemsIterator;
        this.resourceResolver = resourceResolver;
        this.parentPath = parentPath;
    }

    @Override
    public boolean hasNext() {
        return itemsIterator.hasNext();
    }

    @Override
    public Resource next() {
        Map<String, Object> itemProperties = itemsIterator.next();
        String itemName = (String) itemProperties.remove(AbstractReadableResourceProvider.INTERNAL_NAME);
        String resourcePath = parentPath + "/" + itemName;
        return new SimpleReadableResource(resourceResolver, resourcePath, itemProperties);
    }

    @Override
    public void remove() {
        itemsIterator.remove();
    }
}
