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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.LinkedList;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrItemResourceFactory {

    /** Default logger */
    private static final Logger log = LoggerFactory.getLogger(JcrItemResourceFactory.class);

    private final Session session;

    private final HelperData helper;

    public JcrItemResourceFactory(Session session, HelperData helper) {
        this.helper = helper;
        this.session = session;
    }

    /**
     * Creates a <code>Resource</code> instance for the item found at the
     * given path. If no item exists at that path or the item does not have
     * read-access for the session of this resolver, <code>null</code> is
     * returned.
     *
     * @param resourcePath The absolute path
     * @return The <code>Resource</code> for the item at the given path.
     * @throws RepositoryException If an error occurrs accessing checking the
     *             item in the repository.
     */
    public JcrItemResource<?> createResource(final ResourceResolver resourceResolver, final String resourcePath,
            final Resource parent, final Map<String, String> parameters) throws RepositoryException {
        final String jcrPath = helper.pathMapper.mapResourcePathToJCRPath(resourcePath);
        if (jcrPath == null) {
            log.debug("createResource: {} maps to an empty JCR path", resourcePath);
            return null;
        }

        final String version;
        if (parameters != null && parameters.containsKey("v")) {
            version = parameters.get("v");
        } else {
            version = null;
        }

        Node parentNode = null;
        if (parent != null) {
            parentNode = parent.adaptTo(Node.class);
        }

        Item item = null;
        if (parentNode != null && jcrPath.startsWith(parentNode.getPath())) {
            final String parentJcrPath = parentNode.getPath();
            String subPath = jcrPath.substring(parentJcrPath.length());
            if (!subPath.isEmpty() && subPath.charAt(0) == '/') {
                subPath = subPath.substring(1);
            }
            item = getSubitem(parentNode, subPath);
        } else if (itemExists(jcrPath)) {
            item = session.getItem(jcrPath);
        }

        if (item != null && version != null) {
            item = getHistoricItem(item, version);
        }

        if (item == null) {
            log.debug("createResource: No JCR Item exists at path '{}'", jcrPath);
            return null;
        } else {
            final JcrItemResource<?> resource;
            if (item.isNode()) {
                log.debug("createResource: Found JCR Node Resource at path '{}'", resourcePath);
                resource = new JcrNodeResource(resourceResolver, resourcePath, version, (Node) item, helper);
            } else {
                log.debug("createResource: Found JCR Property Resource at path '{}'", resourcePath);
                resource = new JcrPropertyResource(resourceResolver, resourcePath, version, (Property) item);
            }
            resource.getResourceMetadata().setParameterMap(parameters);
            return resource;
        }
    }

    private Item getHistoricItem(Item item, String versionSpecifier) throws RepositoryException {
        Item currentItem = item;
        LinkedList<String> relPath = new LinkedList<String>();
        Node version = null;
        while (!"/".equals(currentItem.getPath())) {
            if (isVersionable(currentItem)) {
                version = getFrozenNode((Node) currentItem, versionSpecifier);
                break;
            } else {
                relPath.addFirst(currentItem.getName());
                currentItem = currentItem.getParent();
            }
        }
        if (version != null) {
            return getSubitem(version, StringUtils.join(relPath.iterator(), '/'));
        }
        return null;
    }

    private static Item getSubitem(Node node, String relPath) throws RepositoryException {
        try {
            if (relPath.length() == 0) { // not using isEmpty() due to 1.5 compatibility
                return node;
            } else if (node.hasNode(relPath)) {
                return node.getNode(relPath);
            } else if (node.hasProperty(relPath)) {
                return node.getProperty(relPath);
            } else {
                return null;
            }
        } catch(RepositoryException e) {
            log.debug("getSubitem: Can't get subitem {} of {}: {}",
                    new Object[] { relPath, node.toString(), e.toString() });
            return null;
        }
    }

    private Node getFrozenNode(Node node, String versionSpecifier) throws RepositoryException {
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final VersionHistory history = versionManager.getVersionHistory(node.getPath());
        if (history.hasVersionLabel(versionSpecifier)) {
            return history.getVersionByLabel(versionSpecifier).getFrozenNode();
        } else if (history.hasNode(versionSpecifier)) {
            return history.getVersion(versionSpecifier).getFrozenNode();
        } else {
            return null;
        }
    }

    private static boolean isVersionable(Item item) throws RepositoryException {
        return item.isNode() && ((Node) item).isNodeType(JcrConstants.MIX_VERSIONABLE);
    }
    

    /**
     * Checks whether the item exists and this content manager's session has
     * read access to the item. If the item does not exist, access control is
     * ignored by this method and <code>false</code> is returned.
     *
     * @param path The path to the item to check
     * @return <code>true</code> if the item exists and this content manager's
     *         session has read access. If the item does not exist,
     *         <code>false</code> is returned ignoring access control.
     */
    private boolean itemExists(final String path) {
        try {
            return session.itemExists(path);
        } catch (RepositoryException re) {
            log.debug("itemExists: Error checking for existence of {}: {}",
                path, re.toString());
            return false;
        }
    }
}
