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
package org.apache.sling.fsprovider.internal.parser;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

/**
 * Cache for parsed content from content files (e.g. JSON, JCR XML).
 */
public final class ContentFileCache {

    private final Map<String,ContentElement> contentCache;
    private final ContentElement NULL_ELEMENT = new ContentElementImpl(null, Collections.<String,Object>emptyMap());
    
    /**
     * @param maxSize Cache size. 0 = caching disabled.
     */
    @SuppressWarnings("unchecked")
    public ContentFileCache(int maxSize) {
        if (maxSize > 0) {
            this.contentCache = Collections.synchronizedMap(new LRUMap(maxSize));
        }
        else {
            this.contentCache = null;
        }
    }
    
    /**
     * Get content.
     * @param path Path (used as cache key).
     * @param file File
     * @return Content or null
     */
    public ContentElement get(String path, File file) {
        ContentElement content = null;
        if (contentCache != null) {
            content = contentCache.get(path);
        }
        if (content == null) {
            content = ContentFileParserUtil.parse(file);
            if (content == null) {
                content = NULL_ELEMENT;
            }
            if (contentCache != null) {
                contentCache.put(path, content);
            }
        }
        if (content == NULL_ELEMENT) {
            return null;
        }
        else {
            return content;
        }
    }
    
    /**
     * Remove content from cache.
     * @param path Path (used as cache key)
     */
    public void remove(String path) {
        if (contentCache != null) {
            contentCache.remove(path);
        }
    }

    /**
     * Clear whole cache
     */
    public void clear() {
        if (contentCache != null) {
            contentCache.clear();
        }
    }
    
    /**
     * @return Current cache size
     */
    public int size() {
        if (contentCache != null) {
            return contentCache.size();
        }
        else {
            return 0;
        }
    }

}
