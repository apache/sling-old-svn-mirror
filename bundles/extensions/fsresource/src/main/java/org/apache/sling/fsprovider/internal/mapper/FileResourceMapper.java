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

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.fsprovider.internal.ContentFileExtensions;
import org.apache.sling.fsprovider.internal.FsResourceMapper;

public final class FileResourceMapper implements FsResourceMapper {

    // The location in the resource tree where the resources are mapped
    private final String providerRoot;

    // providerRoot + "/" to be used for prefix matching of paths
    private final String providerRootPrefix;

    // The "root" file or folder in the file system
    private final File providerFile;
    
    private final ContentFileExtensions contentFileExtensions;
    
    public FileResourceMapper(String providerRoot, File providerFile, ContentFileExtensions contentFileExtensions) {
        this.providerRoot = providerRoot;
        this.providerRootPrefix = providerRoot.concat("/");
        this.providerFile = providerFile;
        this.contentFileExtensions = contentFileExtensions;
    }
    
    @Override
    public Resource getResource(final ResourceResolver resolver, final String resourcePath) {
        File file = getFile(resourcePath);
        if (file != null) {
            return new FileResource(resolver, resourcePath, file);
        }
        else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> getChildren(final ResourceResolver resolver, final Resource parent) {
        final String parentPath = parent.getPath();
        File parentFile = parent.adaptTo(File.class);

        // not a FsResource, try to create one from the resource
        if (parentFile == null) {
            // if the parent path is at or below the provider root, get
            // the respective file
            parentFile = getFile(parentPath);

            // if the parent path is actually the parent of the provider
            // root, return a single element iterator just containing the
            // provider file, unless the provider file is a directory and
            // a repository item with the same path actually exists
            if (parentFile == null) {

                if (providerFile.exists() && !StringUtils.startsWith(parentPath, providerRoot)) {
                    String parentPathPrefix = parentPath.concat("/");
                    if (providerRoot.startsWith(parentPathPrefix)) {
                        String relPath = providerRoot.substring(parentPathPrefix.length());
                        if (relPath.indexOf('/') < 0) {
                            Resource res = new FileResource(resolver, providerRoot, providerFile);
                            return IteratorUtils.singletonIterator(res);
                        }
                    }
                }

                // no children here
                return null;
            }
        }
        
        // ensure parent is a directory
        if (!parentFile.isDirectory()) {
            return null;
        }

        Iterator<File> children = IteratorUtils.filteredIterator(IteratorUtils.arrayIterator(parentFile.listFiles()), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                File file = (File)object;
                return !contentFileExtensions.matchesSuffix(file);
            }
        });
        if (!children.hasNext()) {
            return null;
        }
        return IteratorUtils.transformedIterator(children, new Transformer() {
            @Override
            public Object transform(Object input) {
                File file = (File)input;
                String path = parentPath + "/" + file.getName();
                return new FileResource(resolver, path, file);
            }
        });
    }

    /**
     * Returns a file corresponding to the given absolute resource tree path. If
     * the path equals the configured provider root, the provider root file is
     * returned. If the path starts with the configured provider root, a file is
     * returned relative to the provider root file whose relative path is the
     * remains of the resource tree path without the provider root path.
     * Otherwise <code>null</code> is returned.
     */
    private File getFile(String path) {
        if (path.equals(providerRoot)) {
            return providerFile;
        }
        if (path.startsWith(providerRootPrefix)) {
            String relPath = path.substring(providerRootPrefix.length());
            File file = new File(providerFile, relPath);
            if (file.exists() && !contentFileExtensions.matchesSuffix(file)) {
                return file;
            }
        }
        return null;
    }
    
}
