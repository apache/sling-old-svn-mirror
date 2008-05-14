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
package org.apache.sling.servlets.post.impl.operations;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>CopyOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_COPY copy}
 * operation for the Sling default POST servlet.
 */
public class CopyOperation extends AbstractCopyMoveOperation {

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(CopyOperation.class);

    @Override
    protected String getOperationName() {
        return "copy";
    }

    @Override
    protected void execute(HtmlResponse response, Session session,
            String source, String dest) throws RepositoryException {
        copyNode((Node) session.getItem(source),
            (Node) session.getItem(ResourceUtil.getParent(dest)),
            ResourceUtil.getName(dest));
        response.onCopied(source, dest);
        log.debug("copy {} to {}", source, dest);
    }

    private void copyNode(Node src, Node dstParent, String name)
            throws RepositoryException {
        // create new node
        Node dst = dstParent.addNode(name, src.getPrimaryNodeType().getName());
        for (NodeType mix : src.getMixinNodeTypes()) {
            dst.addMixin(mix.getName());
        }
        // copy the properties
        for (PropertyIterator iter = src.getProperties(); iter.hasNext();) {
            Property p = iter.nextProperty();
            if (p.getDefinition().isProtected()) {
                // skip
            } else if (p.getDefinition().isMultiple()) {
                dst.setProperty(p.getName(), p.getValues());
            } else {
                dst.setProperty(p.getName(), p.getValue());
            }
        }
        // copy the child nodes
        for (NodeIterator iter = src.getNodes(); iter.hasNext();) {
            Node n = iter.nextNode();
            if (!n.getDefinition().isProtected()) {
                copyNode(n, dst, n.getName());
            }
        }
    }

}
