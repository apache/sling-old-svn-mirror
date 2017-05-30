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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Edit.
 * 
 * <p>Use our request to get the post parameter and
 * a resourceResolver to get our post resource</p>
 */
@Model(adaptables=SlingHttpServletRequest.class)
public class Edit {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Edit.class);

    /** The post. */
    private Post post;

    /**
     * Instantiates a new Edit model.
     *
     * @param request the request
     */
    public Edit(SlingHttpServletRequest request) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            String path = request.getParameter("post");
            if (path != null) {
                Resource resource = resourceResolver.getResource(path);
                this.post = resource.adaptTo(Post.class);
            }
            LOGGER.info("Creating new post.");
        } catch (Exception e) {
            LOGGER.info("Couldn't get the post to edit.", e);
        }
    }

    /**
     * Gets the post.
     *
     * @return the post
     */
    public Post getPost() {
        return post;
    }
}