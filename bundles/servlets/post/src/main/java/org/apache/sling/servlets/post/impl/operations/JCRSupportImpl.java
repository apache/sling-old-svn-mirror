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
package org.apache.sling.servlets.post.impl.operations;

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRSupportImpl {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Orders the given node according to the specified command. The following
     * syntax is supported: &lt;xmp&gt; | first | before all child nodes | before A |
     * before child node A | after A | after child node A | last | after all
     * nodes | N | at a specific position, N being an integer &lt;/xmp&gt;
     *
     * @param request The http request
     * @param item node to order
     * @param changes The list of modifications
     * @throws RepositoryException if an error occurs
     */
    public void orderNode(final SlingHttpServletRequest request,
            final Resource resource,
            final List<Modification> changes) throws PersistenceException {

        final String command = request.getParameter(SlingPostConstants.RP_ORDER);
        if (command == null || command.length() == 0) {
            // nothing to do
            return;
        }

        final Node node = resource.adaptTo(Node.class);
        if (node == null) {
            return;
        }

        try {
            final Node parent = node.getParent();

            String next = null;
            if (command.equals(SlingPostConstants.ORDER_FIRST)) {

                next = parent.getNodes().nextNode().getName();

            } else if (command.equals(SlingPostConstants.ORDER_LAST)) {

                next = "";

            } else if (command.startsWith(SlingPostConstants.ORDER_BEFORE)) {

                next = command.substring(SlingPostConstants.ORDER_BEFORE.length());

            } else if (command.startsWith(SlingPostConstants.ORDER_AFTER)) {

                String name = command.substring(SlingPostConstants.ORDER_AFTER.length());
                NodeIterator iter = parent.getNodes();
                while (iter.hasNext()) {
                    Node n = iter.nextNode();
                    if (n.getName().equals(name)) {
                        if (iter.hasNext()) {
                            next = iter.nextNode().getName();
                        } else {
                            next = "";
                        }
                    }
                }

            } else {
                // check for integer
                try {
                    // 01234
                    // abcde move a -> 2 (above 3)
                    // bcade move a -> 1 (above 1)
                    // bacde
                    int newPos = Integer.parseInt(command);
                    next = "";
                    NodeIterator iter = parent.getNodes();
                    while (iter.hasNext() && newPos >= 0) {
                        Node n = iter.nextNode();
                        if (n.getName().equals(node.getName())) {
                            // if old node is found before index, need to
                            // inc index
                            newPos++;
                        }
                        if (newPos == 0) {
                            next = n.getName();
                            break;
                        }
                        newPos--;
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "provided node ordering command is invalid: " + command);
                }
            }

            if (next != null) {
                if (next.equals("")) {
                    next = null;
                }
                parent.orderBefore(node.getName(), next);
                changes.add(Modification.onOrder(node.getPath(), next));
                if (logger.isDebugEnabled()) {
                    logger.debug("Node {} moved '{}'", node.getPath(), command);
                }
            } else {
                throw new IllegalArgumentException(
                    "provided node ordering command is invalid: " + command);
            }
        } catch ( final RepositoryException re) {
            throw new PersistenceException("Unable to order resource", re, resource.getPath(), null);
        }
    }

    private boolean isVersionable(final Node node) throws RepositoryException {
        return node.isNodeType(JcrConstants.MIX_VERSIONABLE);
    }

    public boolean isVersionable(final Resource rsrc) throws PersistenceException {
        try {
            final Node node = rsrc.adaptTo(Node.class);
            return node != null && isVersionable(node);
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re, rsrc.getPath(), null);
        }
    }

    public boolean checkin(final Resource rsrc)
    throws PersistenceException {
        final Node node = rsrc.adaptTo(Node.class);
        if (node != null) {
            try {
                if (node.isCheckedOut() && isVersionable(node)) {
                    node.getSession().getWorkspace().getVersionManager().checkin(node.getPath());
                    return true;
                }
            } catch ( final RepositoryException re) {
                throw new PersistenceException(re.getMessage(), re, rsrc.getPath(), null);
            }
        }
        return false;
    }

    private Node findVersionableAncestor(Node node) throws RepositoryException {
        if (isVersionable(node)) {
            return node;
        }
        try {
            node = node.getParent();
            return findVersionableAncestor(node);
        } catch (ItemNotFoundException e) {
            // top-level
            return null;
        }
    }

    public void checkoutIfNecessary(final Resource resource,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        if (resource != null && versioningConfiguration.isAutoCheckout()) {
            final Node node = resource.adaptTo(Node.class);
            if ( node != null ) {
                try {
                    Node versionableNode = findVersionableAncestor(node);
                    if (versionableNode != null) {
                        if (!versionableNode.isCheckedOut()) {
                            versionableNode.getSession().getWorkspace().getVersionManager().checkout(versionableNode.getPath());
                            changes.add(Modification.onCheckout(versionableNode.getPath()));
                        }
                    }
                } catch ( final RepositoryException re) {
                    throw new PersistenceException(re.getMessage(), re);
                }
            }
        }
    }

    public boolean isNode(final Resource rsrc) {
        return rsrc.adaptTo(Node.class) != null;
    }

    public boolean isNodeType(final Resource rsrc, final String typeHint) {
        final Node node = rsrc.adaptTo(Node.class);
        if ( node != null ) {
            try {
                return node.isNodeType(typeHint);
            } catch ( final RepositoryException re) {
                // ignore
            }
        }
        return false;
    }

    public Boolean isFileNodeType(final ResourceResolver resolver, final String nodeType) {
        final Session session = resolver.adaptTo(Session.class);
        if ( session != null ) {
            try {
                final NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                final NodeType nt = ntMgr.getNodeType(nodeType);
                return nt.isNodeType(JcrConstants.NT_FILE);
            } catch (RepositoryException e) {
                // assuming type not valid.
                return null;
            }
        }
        return false;
    }
}
