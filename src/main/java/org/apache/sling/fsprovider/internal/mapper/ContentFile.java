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
package org.apache.sling.fsprovider.internal.mapper;

import java.io.File;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.parser.ContentFileCache;

/**
 * Reference to a file that contains a content fragment (e.g. JSON, JCR XML).
 */
public final class ContentFile {
    
    private final File file;
    private final String path;
    private final String subPath;
    private final ContentFileCache contentFileCache;
    private boolean contentInitialized;
    private Object content;
    private ValueMap valueMap;
    
    /**
     * @param file File with content fragment
     * @param path Root path of the content file
     * @param subPath Relative path addressing content fragment inside file
     * @param contentFileCache Content file cache
     */
    public ContentFile(File file, String path, String subPath, ContentFileCache contentFileCache) {
        this.file = file;
        this.path = path;
        this.subPath = subPath;
        this.contentFileCache = contentFileCache;
    }

    /**
     * @param file File with content fragment
     * @param path Root path of the content file
     * @param subPath Relative path addressing content fragment inside file
     * @param contentFileCache Content file cache
     * @param content Content
     */
    public ContentFile(File file, String path, String subPath, ContentFileCache contentFileCache, Object content) {
        this(file, path, subPath, contentFileCache);
        this.contentInitialized = true;
        this.content = content;
    }

    /**
     * @return File with content fragment
     */
    public File getFile() {
        return file;
    }
    
    /**
     * @return Root path of content file
     */
    public String getPath() {
        return path;
    }

    /**
     * @return Relative path addressing content fragment inside file
     */
    public String getSubPath() {
        return subPath;
    }
    
    /**
     * Content object referenced by sub path.
     * @return Map if resource, property value if property.
     */
    public Object getContent() {
        if (!contentInitialized) {
            Map<String,Object> rootContent = contentFileCache.get(path, file);
            content = getDeepContent(rootContent, subPath);
            contentInitialized = true;
        }
        return content;
    }
    
    /**
     * @return true if any content was found.
     */
    public boolean hasContent() {
        return getContent() != null;
    }
    
    /**
     * @return true if content references resource map.
     */
    public boolean isResource() {
        return (getContent() instanceof Map);
    }
    
    /**
     * @return ValueMap for resource. Never null.
     */
    @SuppressWarnings("unchecked")
    public ValueMap getValueMap() {
        if (valueMap == null) {
            Object currentContent = getContent();
            if (currentContent instanceof Map) {
                valueMap = ValueMapUtil.toValueMap((Map<String,Object>)currentContent);
            }
            else {
                valueMap = ValueMap.EMPTY;
            }
        }
        return valueMap;
    }
    
    @SuppressWarnings("unchecked")
    private static Object getDeepContent(Object object, String subPath) {
        if (object == null) {
            return null;
        }
        if (subPath == null) {
            return object;
        }
        if (!(object instanceof Map)) {
            return null;
        }
        String name;
        String remainingSubPath;
        int slashIndex = subPath.indexOf('/');
        if (slashIndex >= 0) {
            name = subPath.substring(0, slashIndex);
            remainingSubPath = subPath.substring(slashIndex + 1);
        }
        else {
            name = subPath;
            remainingSubPath = null;
        }
        Object subObject = ((Map<String,Object>)object).get(name);
        return getDeepContent(subObject, remainingSubPath);
    }
    
}
