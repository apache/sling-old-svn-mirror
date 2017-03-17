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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.fsprovider.internal.ContentFileExtensions;
import org.apache.sling.fsprovider.internal.FsResourceMapper;
import org.apache.sling.fsprovider.internal.parser.ContentElement;
import org.apache.sling.fsprovider.internal.parser.ContentFileCache;

public final class ContentFileResourceMapper implements FsResourceMapper {
    
    // providerRoot + "/" to be used for prefix matching of paths
    private final String providerRootPrefix;

    // The "root" file or folder in the file system
    private final File providerFile;
    
    private final ContentFileExtensions contentFileExtensions;
    private final ContentFileCache contentFileCache;
    
    public ContentFileResourceMapper(String providerRoot, File providerFile,
            ContentFileExtensions contentFileExtensions, ContentFileCache contentFileCache) {
        this.providerRootPrefix = providerRoot.concat("/");
        this.providerFile = providerFile;
        this.contentFileExtensions = contentFileExtensions;
        this.contentFileCache = contentFileCache;
    }
    
    @Override
    public Resource getResource(final ResourceResolver resolver, final String resourcePath) {
        if (contentFileExtensions.isEmpty()) {
            return null;
        }
        ContentFile contentFile = getFile(resourcePath, null);
        if (contentFile != null && contentFile.hasContent()) {
            return new ContentFileResource(resolver, contentFile);
        }
        else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> getChildren(final ResourceResolver resolver, final Resource parent) {
        if (contentFileExtensions.isEmpty()) {
            return null;
        }
        final String parentPath = parent.getPath();
        ContentFile parentContentFile = parent.adaptTo(ContentFile.class);

        // not a FsResource, try to create one from the resource
        if (parentContentFile == null) {
            parentContentFile = getFile(parentPath, null);
            if (parentContentFile == null) {
                
                // check if parent is a file resource that contains a file content resource
                File parentFile = parent.adaptTo(File.class);
                if (parentFile != null && parentFile.isDirectory()) {
                    List<Resource> childResources = new ArrayList<>();
                    for (File file : parentFile.listFiles()) {
                        String filenameSuffix = contentFileExtensions.getSuffix(file);
                        if (filenameSuffix != null) {
                            String path = parentPath + "/" + StringUtils.substringBeforeLast(file.getName(), filenameSuffix);
                            ContentFile contentFile = new ContentFile(file, path, null, contentFileCache);
                            childResources.add(new ContentFileResource(resolver, contentFile));
                        }
                    }
                    if (!childResources.isEmpty()) {
                        return childResources.iterator();
                    }
                }
                
                // no children here
                return null;
            }
        }

        // get child resources from content fragments in content file
        List<ContentFile> children = new ArrayList<>();
        if (parentContentFile.hasContent()) {
            Iterator<Map.Entry<String,ContentElement>> childMaps = parentContentFile.getChildren();
            while (childMaps.hasNext()) {
                Map.Entry<String,ContentElement> entry = childMaps.next();
                children.add(parentContentFile.navigateToRelative(entry.getKey()));
            }
        }
        if (children.isEmpty()) {
            return null;
        }
        else {
            return IteratorUtils.transformedIterator(children.iterator(), new Transformer() {
                @Override
                public Object transform(Object input) {
                    ContentFile contentFile = (ContentFile)input;
                    return new ContentFileResource(resolver, contentFile);
                }
            });
        }
    }
    
    private ContentFile getFile(String path, String subPath) {
        if (!StringUtils.startsWith(path, providerRootPrefix)) {
            return null;
        }
        String relPath = path.substring(providerRootPrefix.length());
        for (String filenameSuffix : contentFileExtensions.getSuffixes()) {
            File file = new File(providerFile, relPath + filenameSuffix);
            if (file.exists()) {
                return new ContentFile(file, path, subPath, contentFileCache);
            }
        }
        // try to find in parent path which contains content fragment
        String parentPath = ResourceUtil.getParent(path);
        String nextSubPath = path.substring(parentPath.length() + 1)
                + (subPath != null ? "/" + subPath : "");
        return getFile(parentPath, nextSubPath);
    }

}
