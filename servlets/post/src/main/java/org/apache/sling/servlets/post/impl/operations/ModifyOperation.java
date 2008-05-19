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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.NodeNameGenerator;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.servlets.post.impl.helper.SlingFileUploadHandler;
import org.apache.sling.servlets.post.impl.helper.SlingPropertyValueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ModifyOperation</code> class implements the default operation
 * called by the Sling default POST servlet if no operation is requested by the
 * client. This operation is able to create and/or modify content.
 */
public class ModifyOperation extends AbstractSlingPostOperation {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * utility class for generating node names
     */
    private final NodeNameGenerator nodeNameGenerator;

    private final DateParser dateParser;

    /**
     * handler that deals with file upload
     */
    private final SlingFileUploadHandler uploadHandler;

    public ModifyOperation(NodeNameGenerator nodeNameGenerator,
            DateParser dateParser, ServletContext servletContext) {
        this.nodeNameGenerator = nodeNameGenerator;
        this.dateParser = dateParser;
        this.uploadHandler = new SlingFileUploadHandler(servletContext);
    }

    @Override
    protected void doRun(SlingHttpServletRequest request, HtmlResponse response)
            throws RepositoryException {

        Map<String, RequestProperty> reqProperties = collectContent(request,
            response);

        // do not change order unless you have a very good reason.
        Session session = request.getResourceResolver().adaptTo(Session.class);

        processCreate(session, reqProperties, response);
        writeContent(session, reqProperties, response);

        String path = response.getPath();
        orderNode(request, session.getItem(path));
    }

    @Override
    protected String getItemPath(SlingHttpServletRequest request) {

        // calculate the paths
        StringBuffer rootPathBuf = new StringBuffer();
        String suffix;
        Resource currentResource = request.getResource();
        if (ResourceUtil.isSyntheticResource(currentResource)) {

            // no resource, treat the missing resource path as suffix
            suffix = currentResource.getPath();

        } else {

            // resource for part of the path, use request suffix
            suffix = request.getRequestPathInfo().getSuffix();

            // and preset the path buffer with the resource path
            rootPathBuf.append(currentResource.getPath());

        }

        // check for extensions or create suffix in the suffix
        boolean doGenerateName = false;
        if (suffix != null) {

            // cut off any selectors/extension from the suffix
            int dotPos = suffix.indexOf('.');
            if (dotPos > 0) {
                suffix = suffix.substring(0, dotPos);
            }

            // and check whether it is a create request (trailing /)
            if (suffix.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
                suffix = suffix.substring(0, suffix.length()
                    - SlingPostConstants.DEFAULT_CREATE_SUFFIX.length());
                doGenerateName = true;

                // or with the star suffix /*
            } else if (suffix.endsWith(SlingPostConstants.STAR_CREATE_SUFFIX)) {
                suffix = suffix.substring(0, suffix.length()
                    - SlingPostConstants.STAR_CREATE_SUFFIX.length());
                doGenerateName = true;
            }

            // append the remains of the suffix to the path buffer
            rootPathBuf.append(suffix);

        }

        String path = rootPathBuf.toString();

        if (doGenerateName) {
            try {
                path = generateName(request, path);
            } catch (RepositoryException re) {
                throw new SlingException("Failed to generate name", re);
            }
        }

        return path;
    }

    private String generateName(SlingHttpServletRequest request, String basePath)
            throws RepositoryException {

        // If the path ends with a *, create a node under its parent, with
        // a generated node name
        basePath += "/"
            + nodeNameGenerator.getNodeName(request.getRequestParameterMap());

        // if resulting path exists, add a suffix until it's not the case
        // anymore
        Session session = request.getResourceResolver().adaptTo(Session.class);

        // if resulting path exists, add a suffix until it's not the case
        // anymore
        if (session.itemExists(basePath)) {
            for (int idx = 0; idx < 1000; idx++) {
                String newPath = basePath + "_" + idx;
                if (!session.itemExists(newPath)) {
                    basePath = newPath;
                    break;
                }
            }
        }

        // if it still exists there are more than 1000 nodes ?
        if (session.itemExists(basePath)) {
            throw new RepositoryException(
                "Collision in generated node names for path=" + basePath);
        }

        return basePath;
    }

    /**
     * Create node(s) according to current request
     * 
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void processCreate(Session session,
            Map<String, RequestProperty> reqProperties, HtmlResponse response)
            throws RepositoryException {

        String path = response.getPath();
        if (!session.itemExists(path)) {
            deepGetOrCreateNode(session, path, reqProperties, response);
            response.setCreateRequest(true);
        }

    }

    /**
     * Writes back the content
     * 
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void writeContent(Session session,
            Map<String, RequestProperty> reqProperties, HtmlResponse response)
            throws RepositoryException {

        SlingPropertyValueHandler propHandler = new SlingPropertyValueHandler(
            dateParser, response);

        for (RequestProperty prop : reqProperties.values()) {
            Node parent = deepGetOrCreateNode(session, prop.getParentPath(),
                reqProperties, response);
            // skip jcr special properties
            if (prop.getName().equals("jcr:primaryType")
                || prop.getName().equals("jcr:mixinTypes")) {
                continue;
            }
            if (prop.isFileUpload()) {
                uploadHandler.setFile(parent, prop, response);
            } else {
                propHandler.setProperty(parent, prop);
            }
        }
    }

    /**
     * Collects the properties that form the content to be written back to the
     * repository.
     * 
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private Map<String, RequestProperty> collectContent(
            SlingHttpServletRequest request, HtmlResponse response) {

        boolean requireItemPrefix = requireItemPathPrefix(request);

        // walk the request parameters and collect the properties
        Map<String, RequestProperty> reqProperties = new HashMap<String, RequestProperty>();
        for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with sling:post
            if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
                continue;
            }
            // ignore field with a '@TypeHint' suffix. this is dealt with later
            if (paramName.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                continue;
            }
            // ignore field with a '@DefaultValue' suffix. this is dealt with
            // later
            if (paramName.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
                continue;
            }
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }
            String propertyName = paramName;
            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            RequestParameter[] values = e.getValue();
            final int vfIndex = propertyName.indexOf(SlingPostConstants.VALUE_FROM_SUFFIX);
            if (vfIndex >= 0) {
                // @ValueFrom example:
                // <input name="./Text@ValueFrom" type="hidden" value="fulltext"
                // />
                // causes the JCR Text property to be set to the value of the
                // fulltext form field.
                propertyName = propertyName.substring(0, vfIndex);
                final RequestParameter[] rp = request.getRequestParameterMap().get(
                    paramName);
                if (rp == null || rp.length > 1) {
                    // @ValueFrom params must have exactly one value, else
                    // ignored
                    continue;
                }
                String mappedName = rp[0].getString();
                values = request.getRequestParameterMap().get(mappedName);
                if (values == null) {
                    // no value for "fulltext" in our example, ignore parameter
                    continue;
                }
            }
            // create property helper and add it to the list
            String propPath = propertyName;
            if (!propPath.startsWith("/")) {
                propPath = response.getPath() + "/" + propertyName;
            }
            RequestProperty prop = new RequestProperty(propPath, values);

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            final String thName = propertyName
                + SlingPostConstants.TYPE_HINT_SUFFIX;
            final RequestParameter rp = request.getRequestParameter(thName);
            if (rp != null) {
                prop.setTypeHint(rp.getString());
            }

            // @DefaultValue
            final String dvName = propertyName
                + SlingPostConstants.DEFAULT_VALUE_SUFFIX;
            prop.setDefaultValues(request.getRequestParameters(dvName));

            reqProperties.put(propPath, prop);
        }

        return reqProperties;
    }

    /**
     * Checks the collected content for a jcr:primaryType property at the
     * specified path.
     * 
     * @param path path to check
     * @return the primary type or <code>null</code>
     */
    private String getPrimaryType(Map<String, RequestProperty> reqProperties,
            String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:primaryType");
        return prop == null ? null : prop.getStringValues()[0];
    }

    /**
     * Checks the collected content for a jcr:mixinTypes property at the
     * specified path.
     * 
     * @param path path to check
     * @return the mixin types or <code>null</code>
     */
    private String[] getMixinTypes(Map<String, RequestProperty> reqProperties,
            String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:mixinTypes");
        return prop == null ? null : prop.getStringValues();
    }

    /**
     * Deep gets or creates a node, parent-padding with default nodes nodes. If
     * the path is empty, the given parent node is returned.
     * 
     * @param path path to node that needs to be deep-created
     * @return node at path
     * @throws RepositoryException if an error occurs
     * @throws IllegalArgumentException if the path is relative and parent is
     *             <code>null</code>
     */
    private Node deepGetOrCreateNode(Session session, String path,
            Map<String, RequestProperty> reqProperties, HtmlResponse response)
            throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("Deep-creating Node '{}'", path);
        }
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must be an absolute path.");
        }
        // get the starting node
        String startingNodePath = path;
        Node startingNode = null;
        while (startingNode == null) {
            if (startingNodePath.equals("/")) {
                startingNode = session.getRootNode();
            } else if (session.itemExists(startingNodePath)) {
                startingNode = (Node) session.getItem(startingNodePath);
            } else {
                int pos = startingNodePath.lastIndexOf('/');
                if (pos > 0) {
                    startingNodePath = startingNodePath.substring(0, pos);
                } else {
                    startingNodePath = "/";
                }
            }
        }
        // is the searched node already existing?
        if (startingNodePath.length() == path.length()) {
            return startingNode;
        }
        // create nodes
        int from = (startingNodePath.length() == 1
                ? 1
                : startingNodePath.length() + 1);
        Node node = startingNode;
        while (from > 0) {
            final int to = path.indexOf('/', from);
            final String name = to < 0 ? path.substring(from) : path.substring(
                from, to);
            // although the node should not exist (according to the first test
            // above)
            // we do a sanety check.
            if (node.hasNode(name)) {
                node = node.getNode(name);
            } else {
                final String tmpPath = to < 0 ? path : path.substring(0, to);
                // check for node type
                final String nodeType = getPrimaryType(reqProperties, tmpPath);
                if (nodeType != null) {
                    node = node.addNode(name, nodeType);
                } else {
                    node = node.addNode(name);
                }
                // check for mixin types
                final String[] mixinTypes = getMixinTypes(reqProperties,
                    tmpPath);
                if (mixinTypes != null) {
                    for (String mix : mixinTypes) {
                        node.addMixin(mix);
                    }
                }
                response.onCreated(node.getPath());
            }
            from = to + 1;
        }
        return node;
    }

}
