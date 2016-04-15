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
package org.apache.sling.sample.slingshot.comments.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.apache.sling.sample.slingshot.SlingshotUtil;
import org.apache.sling.sample.slingshot.comments.Comment;
import org.apache.sling.sample.slingshot.comments.CommentsService;
import org.apache.sling.sample.slingshot.comments.CommentsUtil;
import org.apache.sling.sample.slingshot.impl.Util;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the comments service
 */
@Component(service = CommentsService.class)
public class CommentsServiceImpl implements CommentsService {

    /** The resource type for the comments holder. */
    public static final String RESOURCETYPE_COMMENTS = "sling:OrderedFolder";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * @see org.apache.sling.sample.slingshot.comments.CommentsService#getCommentsResourcePath(org.apache.sling.api.resource.Resource)
     */
    @Override
    public String getCommentsResourcePath(final Resource resource) {
        final String contentPath = SlingshotUtil.getContentPath(resource);
        if ( contentPath != null ) {
            final String fullPath = SlingshotConstants.APP_ROOT_PATH
                    + "/users/" + SlingshotUtil.getUserId(resource)
                    + "/ugc/comments" + contentPath;
            return fullPath;
        }
        return null;
    }

    /**
     * @see org.apache.sling.sample.slingshot.comments.CommentsService#addComment(org.apache.sling.api.resource.Resource, org.apache.sling.sample.slingshot.comments.Comment)
     */
    @Override
    public void addComment(final Resource resource, final Comment c)
    throws PersistenceException {
        final String commentsPath = this.getCommentsResourcePath(resource);
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, RESOURCETYPE_COMMENTS);
        final Resource ratingsResource = ResourceUtil.getOrCreateResource(resource.getResourceResolver(),
                commentsPath, props, null, true);

        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, CommentsUtil.RESOURCETYPE_COMMENT);
        properties.put(CommentsUtil.PROPERTY_TITLE, c.getTitle());
        properties.put(CommentsUtil.PROPERTY_TEXT, c.getText());
        properties.put(CommentsUtil.PROPERTY_USER, c.getCreatedBy());

        // we try it five times
        PersistenceException exception = null;
        Resource newResource = null;
        for(int i=0; i<5; i++) {
            try {
                exception = null;
                final String name = ResourceUtil.createUniqueChildName(ratingsResource, Util.filter(c.getTitle()));
                newResource = resource.getResourceResolver().create(ratingsResource, name, properties);

                resource.getResourceResolver().commit();
                break;
            } catch ( final PersistenceException pe) {
                resource.getResourceResolver().revert();
                resource.getResourceResolver().refresh();
                exception = pe;
            }
        }
        if ( exception != null ) {
            throw exception;
        }
        // order node at the top (if jcr based)
        final Node newNode = newResource.adaptTo(Node.class);
        if ( newNode != null ) {
            try {
                final Node parent = newNode.getParent();
                final Node firstNode = parent.getNodes().nextNode();
                if ( !firstNode.getName().equals(newNode.getName()) ) {
                    parent.orderBefore(newNode.getName(), firstNode.getName());
                    newNode.getSession().save();
                }
            } catch ( final RepositoryException re) {
                logger.error("Unable to order comment to the top", re);
            }
        }
    }

}
