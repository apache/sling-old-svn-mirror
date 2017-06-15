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
package org.apache.sling.pipes.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * creates or get given expression's path and returns corresponding resource
 */
public class PathPipe extends BasePipe {

    public static final String RESOURCE_TYPE = "slingPipes/path";
    public static final String PN_NODETYPE = "nodeType";
    public static final String PN_AUTOSAVE = "autosave";

    String nodeType;

    boolean autosave;

    private final Logger logger = LoggerFactory.getLogger(PathPipe.class);

    public PathPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        nodeType = properties.get(PN_NODETYPE, "sling:Folder");
        autosave = properties.get(PN_AUTOSAVE, true);
    }

    @Override
    public Iterator<Resource> getOutput() {
        Iterator<Resource> output = Collections.emptyIterator();
        String expression = getExpr();
        Node leaf = null;
        boolean transientChange = false;
        try {
            String relativePath = expression.substring(1);
            Node parentNode = resolver.adaptTo(Session.class).getRootNode();
            if (!parentNode.hasNode(relativePath)) {
                Node node = parentNode;
                int pos = relativePath.lastIndexOf('/');
                if (pos != -1) {
                    final StringTokenizer st = new StringTokenizer(relativePath.substring(0, pos), "/");
                    while (st.hasMoreTokens()) {
                        final String token = st.nextToken();
                        if (!node.hasNode(token)) {
                            try {
                                node.addNode(token, nodeType);
                                transientChange = true;
                            } catch (RepositoryException re) {
                                // we ignore this as this folder might be created from a different task
                                node.getSession().refresh(false);
                            }
                        }
                        node = node.getNode(token);
                    }
                    relativePath = relativePath.substring(pos + 1);
                }
                if (!node.hasNode(relativePath)) {
                    node.addNode(relativePath, nodeType);
                    transientChange = true;
                }
                leaf = node.getNode(relativePath);
            }
            if (leaf == null) {
                leaf = parentNode.getNode(relativePath);
            }
            if (transientChange && autosave){
                resolver.adaptTo(Session.class).save();
            }
            output =  Collections.singleton(resolver.getResource(leaf.getPath())).iterator();
        } catch (RepositoryException e){
            logger.error ("Not able to create path {}", expression, e);
        }
        return output;
    }
}
