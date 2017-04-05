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
 package org.apache.sling.validation.testservices.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = PostResponseCreator.class
)
public class ValidationPostResponseCreator implements PostResponseCreator {

    private final Logger logger = LoggerFactory.getLogger(ValidationPostResponseCreator.class);

    @Override
    public PostResponse createPostResponse(SlingHttpServletRequest request) {
        String operation = request.getParameter(SlingPostConstants.RP_OPERATION);
        if (operation != null && "validation".equals(operation)) {
            final ResourceBundle resourceBundle = request.getResourceBundle(Locale.US);
            logger.debug("resource bundle: {}", resourceBundle);
            return new ValidationPostResponse(resourceBundle);
        }
        return null;
    }
}
