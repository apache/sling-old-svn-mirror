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
package org.apache.sling.resourcebuilder.impl;

import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;

/** ResourceBuilder service, only implements the for* methods to
 *  create new builders. This allows us to provide a single service
 *  interface which transparently becomes stateful, by switching from
 *  this object to the ResourceBuilderImpl.
 */
@Component
@Service(value=ResourceBuilder.class)
public class ResourceBuilderService implements ResourceBuilder {
    
    @Reference
    private MimeTypeService mimeTypeService;
    
    
    private ResourceBuilder notStarted() {
        throw new IllegalStateException(
                "This ResourceBuilder is not started, please use the"
                + "forParent or forResolver methods to start it."
         );
    }

    @Override
    public ResourceBuilder forParent(Resource parent) {
        return new ResourceBuilderImpl(parent, mimeTypeService);
    }

    @Override
    public ResourceBuilder forResolver(ResourceResolver r) {
        final Resource root = r.getResource("/");
        if(root == null) {
            throw new IllegalStateException("Cannot read root resource");
        }
        return forParent(root);
    }

    @Override
    public ResourceBuilder resource(String relativePath, Object... properties) {
        return notStarted();
    }

    @Override
    public ResourceBuilder file(String filename, InputStream data, String mimeType, long lastModified) {
        return notStarted();
    }

    @Override
    public ResourceBuilder file(String filename, InputStream data) {
        return notStarted();
    }

    @Override
    public ResourceBuilder commit() {
        return notStarted();
    }

    @Override
    public ResourceBuilder withIntermediatePrimaryType(String primaryType) {
        return notStarted();
    }

    @Override
    public ResourceBuilder siblingsMode() {
        return notStarted();
    }

    @Override
    public ResourceBuilder hierarchyMode() {
        return notStarted();
    }

    @Override
    public Resource getCurrentParent() {
        notStarted();
        return null;
    }

    @Override
    public ResourceBuilder atParent() {
        return notStarted();
    }
}