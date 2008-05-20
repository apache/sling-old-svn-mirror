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

import javax.jcr.Item;
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
    protected void execute(HtmlResponse response, Item source,
            String destParent, String destName) throws RepositoryException {

        copy(source, (Node) source.getSession().getItem(destParent), destName);

        String dest = destParent + "/" + destName;
        response.onCopied(source.getPath(), dest);
        log.debug("copy {} to {}", source, dest);
    }

    /**
     * Copy the <code>src</code> item into the <code>dstParent</code> node.
     * The name of the newly created item is set to <code>name</code>.
     * 
     * @param src The item to copy to the new location
     * @param dstParent The node into which the <code>src</code> node is to be
     *            copied
     * @param name The name of the newly created item. If this is
     *            <code>null</code> the new item gets the same name as the
     *            <code>src</code> item.
     * @throws RepositoryException May be thrown in case of any problem copying
     *             the content.
     * @see #copy(Node, Node, String)
     * @see #copy(Property, Node, String)
     */
    static void copy(Item src, Node dstParent, String name)
            throws RepositoryException {
        if (src.isNode()) {
            copy((Node) src, dstParent, name);
        } else {
            copy((Property) src, dstParent, name);
        }
    }

    /**
     * Copy the <code>src</code> node into the <code>dstParent</code> node.
     * The name of the newly created node is set to <code>name</code>.
     * <p>
     * This method does a recursive (deep) copy of the subtree rooted at the
     * source node to the destination. Any protected child nodes and and
     * properties are not copied.
     * 
     * @param src The node to copy to the new location
     * @param dstParent The node into which the <code>src</code> node is to be
     *            copied
     * @param name The name of the newly created node. If this is
     *            <code>null</code> the new node gets the same name as the
     *            <code>src</code> node.
     * @throws RepositoryException May be thrown in case of any problem copying
     *             the content.
     */
    static void copy(Node src, Node dstParent, String name)
            throws RepositoryException {

        // ensure destination name
        if (name == null) {
            name = src.getName();
        }

        // ensure new node creation
        if (dstParent.hasNode(name)) {
            dstParent.getNode(name).remove();
        }

        // create new node
        Node dst = dstParent.addNode(name, src.getPrimaryNodeType().getName());
        for (NodeType mix : src.getMixinNodeTypes()) {
            dst.addMixin(mix.getName());
        }

        // copy the properties
        for (PropertyIterator iter = src.getProperties(); iter.hasNext();) {
            copy(iter.nextProperty(), dst, null);
        }

        // copy the child nodes
        for (NodeIterator iter = src.getNodes(); iter.hasNext();) {
            Node n = iter.nextNode();
            if (!n.getDefinition().isProtected()) {
                copy(n, dst, null);
            }
        }
    }

    /**
     * Copy the <code>src</code> property into the <code>dstParent</code>
     * node. The name of the newly created property is set to <code>name</code>.
     * <p>
     * If the source property is protected, this method does nothing.
     * 
     * @param src The property to copy to the new location
     * @param dstParent The node into which the <code>src</code> property is
     *            to be copied
     * @param name The name of the newly created property. If this is
     *            <code>null</code> the new property gets the same name as the
     *            <code>src</code> property.
     * @throws RepositoryException May be thrown in case of any problem copying
     *             the content.
     */
    static void copy(Property src, Node dstParent, String name)
            throws RepositoryException {
        if (!src.getDefinition().isProtected()) {
            if (name == null) {
                name = src.getName();
            }

            // ensure new property creation
            if (dstParent.hasProperty(name)) {
                dstParent.getProperty(name).remove();
            }

            if (src.getDefinition().isMultiple()) {
                dstParent.setProperty(name, src.getValues());
            } else {
                dstParent.setProperty(name, src.getValue());
            }
        }
    }

}
