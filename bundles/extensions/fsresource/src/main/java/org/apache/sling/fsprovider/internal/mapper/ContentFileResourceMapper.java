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

public final class ContentFileResourceMapper implements FsResourceMapper {
    
    // providerRoot + "/" to be used for prefix matching of paths
    private final String providerRootPrefix;

    // The "root" file or folder in the file system
    private final File providerFile;
    
    private final ContentFileExtensions contentFileExtensions;
    
    public ContentFileResourceMapper(String providerRoot, File providerFile, ContentFileExtensions contentFileExtensions) {
        this.providerRootPrefix = providerRoot.concat("/");
        this.providerFile = providerFile;
        this.contentFileExtensions = contentFileExtensions;
    }
    
    @Override
    public Resource getResource(final ResourceResolver resolver, final String resourcePath) {
        ContentFile contentFile = getFile(resourcePath, null);
        if (contentFile != null && contentFile.hasContent()) {
            return new ContentFileResource(resolver, resourcePath, contentFile);
        }
        else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> getChildren(final ResourceResolver resolver, final Resource parent) {
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
                            ContentFile contentFile = new ContentFile(file, null);
                            String path = parentPath + "/" + StringUtils.substringBeforeLast(file.getName(), filenameSuffix);
                            childResources.add(new ContentFileResource(resolver, path, contentFile));
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
        if (parentContentFile.hasContent() && parentContentFile.isResource()) {
            Map<String,Object> content = (Map<String,Object>)parentContentFile.getContent();
            for (Map.Entry<String, Object> entry: content.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    String subPath;
                    if (parentContentFile.getSubPath() == null) {
                        subPath = entry.getKey();
                    }
                    else {
                        subPath = parentContentFile.getSubPath() + "/" + entry.getKey();
                    }
                    children.add(new ContentFile(parentContentFile.getFile(), subPath, entry.getValue()));
                }
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
                    String path = parentPath + "/" + ResourceUtil.getName(contentFile.getSubPath());
                    return new ContentFileResource(resolver, path, contentFile);
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
                return new ContentFile(file, subPath);
            }
        }
        // try to find in parent path which contains content fragment
        String parentPath = ResourceUtil.getParent(path);
        String nextSubPath = path.substring(parentPath.length() + 1)
                + (subPath != null ? "/" + subPath : "");
        return getFile(parentPath, nextSubPath);
    }

}
