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
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.mapper.valuemap.ValueMapUtil;
import org.apache.sling.fsprovider.internal.parser.ContentElement;
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
    private ContentElement content;
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
    public ContentElement getContent() {
        if (!contentInitialized) {
            ContentElement rootContent = contentFileCache.get(path, file);
            if (subPath == null) {
                content = rootContent;
            }
            else {
                content = rootContent.getChild(subPath);
            }
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
     * @return ValueMap for resource. Never null.
     */
    public ValueMap getValueMap() {
        if (valueMap == null) {
            ContentElement currentContent = getContent();
            if (currentContent != null) {
                valueMap = ValueMapUtil.toValueMap(currentContent.getProperties());
            }
            else {
                valueMap = ValueMap.EMPTY;
            }
        }
        return valueMap;
    }
    
    /**
     * @return Child maps.
     */
    public Iterator<Map.Entry<String,ContentElement>> getChildren() {
        return getContent().getChildren().entrySet().iterator();
    }
    
    /**
     * Navigate to another sub path position in content file.
     * @param newSubPath New sub path related to root path of content file
     * @return Content file
     */
    public ContentFile navigateToAbsolute(String newSubPath) {
        return new ContentFile(file, path, newSubPath, contentFileCache);
    }
        
    /**
     * Navigate to another sub path position in content file.
     * @param newSubPath New sub path relative to current sub path in content file
     * @return Content file
     */
    public ContentFile navigateToRelative(String newSubPath) {
        String absoluteSubPath;
        if (newSubPath == null) {
            absoluteSubPath = this.subPath;
        }
        else {
            absoluteSubPath = (this.subPath != null ? this.subPath + "/" : "") + newSubPath;
        }
        return new ContentFile(file, path, absoluteSubPath, contentFileCache);
    }
        
}
