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

import static org.apache.jackrabbit.vault.util.Constants.DOT_CONTENT_XML;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.fsprovider.internal.FsResourceMapper;
import org.apache.sling.fsprovider.internal.parser.ContentElement;
import org.apache.sling.fsprovider.internal.parser.ContentFileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileVaultResourceMapper implements FsResourceMapper {
    
    private static final String DOT_CONTENT_XML_SUFFIX = "/" + DOT_CONTENT_XML;
    private static final String DOT_DIR_SUFFIX = "/.dir";

    private final File providerFile;
    private final File filterXmlFile;
    private final ContentFileCache contentFileCache;
    private final WorkspaceFilter workspaceFilter;
    
    private static final Logger log = LoggerFactory.getLogger(FileVaultResourceMapper.class);
    
    public FileVaultResourceMapper(File providerFile, File filterXmlFile, ContentFileCache contentFileCache) {
        this.providerFile = providerFile;
        this.filterXmlFile = filterXmlFile;
        this.contentFileCache = contentFileCache;
        this.workspaceFilter = getWorkspaceFilter();
    }
    
    @Override
    public Resource getResource(final ResourceResolver resolver, final String resourcePath) {
        
        // direct file
        File file = getFile(resourcePath);
        if (file != null && file.isFile()) {
            return new FileResource(resolver, resourcePath, file);
        }
        
        // content file
        ContentFile contentFile = getContentFile(resourcePath, null);
        if (contentFile != null) {
            return new ContentFileResource(resolver, contentFile);
        }
        
        // fallback to directory resource if folder was found but nothing else
        if (file != null && file.isDirectory()) {
            return new FileResource(resolver, resourcePath, file);
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> getChildren(final ResourceResolver resolver, final Resource parent) {
        String parentPath = parent.getPath();
        
        Set<String> childPaths = new LinkedHashSet<>();
        
        // get children from content resource of parent
        ContentFile parentContentFile = getContentFile(parentPath, null);
        if (parentContentFile != null) {
            Iterator<Map.Entry<String,ContentElement>> childMaps = parentContentFile.getChildren();
            while (childMaps.hasNext()) {
                Map.Entry<String,ContentElement> entry = childMaps.next();
                String childPath = parentPath + "/" + entry.getKey();
                if (pathMatches(childPath)) {
                    childPaths.add(childPath);
                }
            }
        }
        
        // additional check for children in file system
        File parentFile = getFile(parentPath);
        if (parentFile != null && parentFile.isDirectory()) {
            for (File childFile : parentFile.listFiles()) {
                String childPath = parentPath + "/" + PlatformNameFormat.getRepositoryName(childFile.getName());
                if (pathMatches(childPath) && !childPaths.contains(childPath)) {
                    childPaths.add(childPath);
                }
            }
        }
        
        if (childPaths.isEmpty()) {
            return null;
        }
        else {
            return IteratorUtils.transformedIterator(childPaths.iterator(), new Transformer() {
                @Override
                public Object transform(Object input) {
                    String path = (String)input;
                    return getResource(resolver, path);
                }
            });
        }
    }

    /**
     * @return Workspace filter or null if none found.
     */
    private WorkspaceFilter getWorkspaceFilter() {
        if (filterXmlFile != null && filterXmlFile.exists()) {
            try {
                DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();
                workspaceFilter.load(filterXmlFile);
                return workspaceFilter;
            } catch (IOException | ConfigurationException ex) {
                log.error("Unable to parse workspace filter: " + filterXmlFile.getPath(), ex);
            }
        }
        else {
            log.debug("Workspace filter not found: " + filterXmlFile.getPath());
        }
        return null;
    }
    
    /**
     * Checks if the given path matches the workspace filter.
     * @param path Path
     * @return true if path matches
     */
    public boolean pathMatches(String path) {
        // ignore .dir folder
        if (StringUtils.endsWith(path, DOT_DIR_SUFFIX) || StringUtils.endsWith(path, DOT_CONTENT_XML_SUFFIX)) {
            return false;
        }
        if (workspaceFilter == null) {
            return true;
        }
        else {
            return workspaceFilter.contains(path);
        }
    }
    
    private File getFile(String path) {
        if (StringUtils.endsWith(path, DOT_CONTENT_XML_SUFFIX)) {
            return null;
        }
        File file = new File(providerFile, "." + PlatformNameFormat.getPlatformPath(path));
        if (file.exists()) {
            return file;
        }
        return null;
    }
    
    private ContentFile getContentFile(String path, String subPath) {
        File file = new File(providerFile, "." + PlatformNameFormat.getPlatformPath(path) + DOT_CONTENT_XML_SUFFIX);
        if (file.exists()) {
            ContentFile contentFile = new ContentFile(file, path, subPath, contentFileCache);
            if (contentFile.hasContent()) {
                return contentFile;
            }
        }
        
        // try to find in parent path which contains content fragment
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath == null) {
            return null;
        }
        String nextSubPath = path.substring(parentPath.length() + 1)
                + (subPath != null ? "/" + subPath : "");
        return getContentFile(parentPath, nextSubPath);
    }

}
