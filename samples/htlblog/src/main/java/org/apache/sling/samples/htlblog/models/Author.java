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

import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;

/**
 * The Class Author.
 * 
 * <p>A simple model to get a userId and check if an author is logged in</p>
 */
@Model(adaptables=SlingHttpServletRequest.class)
public class Author {

    private String userId;

    /**
     * Instantiates a new author model.
     *
     * @param request the request
     */
    public Author(SlingHttpServletRequest request) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        userId = resourceResolver.getUserID();
    }

    /**
     * Gets the user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Check if an author is logged in.
     *
     * @return true, if an author is logged in
     */
    public boolean isLoggedIn() {
        boolean isLoggedIn = false;
        if(!userId.equals("anonymous")) {
            isLoggedIn = true;
        }
        return isLoggedIn;
    }
}