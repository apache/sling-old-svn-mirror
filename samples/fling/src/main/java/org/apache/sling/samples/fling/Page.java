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
package org.apache.sling.samples.fling;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import static org.apache.sling.query.SlingQuery.$;

@Model(adaptables = Resource.class)
public class Page {

    private final Resource resource;

    @Inject
    private String title;

    @Inject
    @Optional
    private String content;

    public Page(final Resource resource) {
        this.resource = resource;
    }

    public String getName() {
        return resource.getName();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Iterable<Page> getParents() {
        return $(resource).parents("fling/page").map(Page.class);
    }

    public Iterable<Page> getChildren() {
        return $(resource).children().map(Page.class);
    }

    public Iterable<Page> getSiblings() {
        return $(resource).siblings().not($(resource)).map(Page.class);
    }

}
