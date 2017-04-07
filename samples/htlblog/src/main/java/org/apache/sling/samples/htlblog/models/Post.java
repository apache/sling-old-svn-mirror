/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.samples.htlblog.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

/**
 * The Class Post.
 * 
 * <p>The post model to inject, set, or get our properties for a post resource</p>
 */
@Model(adaptables=Resource.class)
public class Post {

    /** The resource. */
    private final Resource resource;

    /** The title. */
    @Inject
    private String title;

    /** The body. */
    @Inject
    private String body;

    /** The created date */
    @Inject @Optional
    private Calendar created;

    /**
     * Instantiates a new post.
     *
     * @param resource the resource
     */
    public Post(final Resource resource) {
        this.resource = resource;
    }

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the body.
     *
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the created date.
     * 
     * <p>Format the date using a simple string</p>
     * 
     * <p>HTL does not support showing the raw JCR date due to XSS concerns.</p>
     *
     * @return the created
     * 
     * TODO: When Sling 9 is released, this should use HTL's native date formating feature.
     */
    public String getCreated() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM, d yyyy");
        return formatter.format(created.getTime());
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return resource.getPath();
    }

    /**
     * Gets the url.
     * 
     * <p>A simple util to add the extension for the post.</p>
     *
     * @return the url
     */
    public String getUrl() {
        return getPath() + ".html";
    }

    /**
     * Gets the featured image path.
     * 
     * <p>We don't know what the image name will be and we may end up
     * having comments inside the post, so we grab the first featuredImage child.</p>
     *
     * @return the featured image path
     */
    public String getFeaturedImagePath() {
        Iterator<Resource> featuredChildren = resource.getChild("featuredImage").listChildren();
        return featuredChildren.next().getPath();
    }
}
