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
package org.apache.sling.scripting.thymeleaf.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.FilenameUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.path.PathBuilder;
import org.thymeleaf.templateresource.ITemplateResource;

public class SlingTemplateResource implements ITemplateResource {

    private final Resource resource;

    private Reader reader;

    public SlingTemplateResource(final Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getDescription() {
        return resource.getPath();
    }

    @Override
    public String getBaseName() {
        return FilenameUtils.getBaseName(resource.getName());
    }

    @Override
    public boolean exists() {
        return resource != null && !(ResourceUtil.isNonExistingResource(resource));
    }

    @Override
    public Reader reader() throws IOException {
        if (reader == null) {
            final InputStream inputStream = resource.adaptTo(InputStream.class);
            reader = new InputStreamReader(inputStream);
        }
        return reader;
    }

    @Override
    public ITemplateResource relative(final String relativeLocation) {
        final PathBuilder pathBuilder = new PathBuilder(resource.getPath());
        final String path = pathBuilder.append("..").append(relativeLocation).toString();
        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final Resource relative = resourceResolver.getResource(path);
        // final Resource relative = resource.getParent().getChild(relativeLocation);
        return new SlingTemplateResource(relative);
    }

}
