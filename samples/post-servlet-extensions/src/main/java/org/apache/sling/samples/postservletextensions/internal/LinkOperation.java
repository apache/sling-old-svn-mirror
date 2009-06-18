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
package org.apache.sling.samples.postservletextensions.internal;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;

/**
 * This operation allows to create links between nodes.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostOperation"
 * @scr.property name="sling.post.operation" value="link"
 */
public class LinkOperation extends AbstractSlingPostOperation {

    public final LinkHelper linkHelper = new LinkHelper();

    @Override
    protected void doRun(SlingHttpServletRequest request,
            HtmlResponse response, List<Modification> changes)
            throws RepositoryException {

        Session session = request.getResourceResolver().adaptTo(Session.class);
        String resourcePath = request.getResource().getPath();
        if (session.itemExists(resourcePath)) {
            Node source = (Node) session.getItem(resourcePath);

            // create a symetric link
            RequestParameter linkParam = request.getRequestParameter("target");
            if (linkParam != null) {
                String linkPath = linkParam.getString();
                if (session.itemExists(linkPath)) {
                    Item targetItem = session.getItem(linkPath);
                    if (targetItem.isNode()) {
                        linkHelper.createSymetricLink(source,
                            (Node) targetItem, "link");
                    }
                }
            }
        }

    }
}
