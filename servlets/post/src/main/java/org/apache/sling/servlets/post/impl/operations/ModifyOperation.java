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
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.NodeNameGenerator;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.servlets.post.impl.helper.SlingFileUploadHandler;
import org.apache.sling.servlets.post.impl.helper.SlingPropertyValueHandler;

/**
 * The <code>ModifyOperation</code> class implements the default operation
 * called by the Sling default POST servlet if no operation is requested by the
 * client. This operation is able to create and/or modify content.
 */
public class ModifyOperation extends AbstractSlingPostOperation {

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
    protected void doRun(SlingHttpServletRequest request, HtmlResponse response, List<Modification> changes)
            throws RepositoryException {

        Map<String, RequestProperty> reqProperties = collectContent(request,
                response);

        // do not change order unless you have a very good reason.
        Session session = request.getResourceResolver().adaptTo(Session.class);

        // ensure root of new content
        processCreate(session, reqProperties, response, changes);

        // write content from existing content (@Move/CopyFrom parameters)
        processMoves(session, reqProperties, changes);
        processCopies(session, reqProperties, changes);

        // cleanup any old content (@Delete parameters)
        processDeletes(session, reqProperties, changes);

        // write content from form
        writeContent(session, reqProperties, changes);

        // order content
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
            if ((dotPos > 0)
                && (!(currentResource instanceof NonExistingResource))) {
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
            + nodeNameGenerator.getNodeName(request.getRequestParameterMap(),
                requireItemPathPrefix(request));

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
     */
    private void processCreate(Session session,
            Map<String, RequestProperty> reqProperties, HtmlResponse response, List<Modification> changes)
            throws RepositoryException {

        String path = response.getPath();
        if (!session.itemExists(path)) {
            deepGetOrCreateNode(session, path, reqProperties, changes);
            response.setCreateRequest(true);
        }

    }

    /**
     * Moves all repository content listed as repository move source in the
     * request properties to the locations indicated by the resource properties.
     */
    private void processMoves(Session session,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.hasRepositoryMoveSource()) {
                processMovesCopiesInternal(property, true, session,
                    reqProperties, changes);
            }
        }
    }

    /**
     * Copies all repository content listed as repository copy source in the
     * request properties to the locations indicated by the resource properties.
     */
    private void processCopies(Session session,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.hasRepositoryCopySource()) {
                processMovesCopiesInternal(property, false, session,
                    reqProperties, changes);
            }
        }
    }

    /**
     * Internal implementation of the
     * {@link #processCopies(Session, Map, HtmlResponse)} and
     * {@link #processMoves(Session, Map, HtmlResponse)} methods taking into
     * account whether the source is actually a property or a node.
     * <p>
     * Any intermediary nodes to the destination as indicated by the
     * <code>property</code> path are created using the
     * <code>reqProperties</code> as indications for required node types.
     *
     * @param property The {@link RequestProperty} identifying the source
     *            content of the operation.
     * @param isMove <code>true</code> if the source item is to be moved.
     *            Otherwise the source item is just copied.
     * @param session The repository session to use to access the content
     * @param reqProperties All accepted request properties. This is used to
     *            create intermediary nodes along the property path.
     * @param response The <code>HtmlResponse</code> into which successfull
     *            copies and moves as well as intermediary node creations are
     *            recorded.
     * @throws RepositoryException May be thrown if an error occurrs.
     */
    private void processMovesCopiesInternal(RequestProperty property,
            boolean isMove, Session session,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        String propPath = property.getPath();
        String source = property.getRepositorySource();

        // only continue here, if the source really exists
        if (session.itemExists(source)) {

            // if the destination item already exists, remove it
            // first, otherwise ensure the parent location
            if (session.itemExists(propPath)) {
                session.getItem(propPath).remove();
                changes.add(Modification.onDeleted(propPath));
            } else {
                deepGetOrCreateNode(session, property.getParentPath(),
                    reqProperties, changes);
            }

            // move through the session and record operation
            Item sourceItem = session.getItem(source);
            if (sourceItem.isNode()) {

                // node move/copy through session
                if (isMove) {
                    session.move(source, propPath);
                } else {
                    Node sourceNode = (Node) sourceItem;
                    Node destParent = (Node) session.getItem(property.getParentPath());
                    CopyOperation.copy(sourceNode, destParent,
                        property.getName());
                }

            } else {

                // property move manually
                Property sourceProperty = (Property) sourceItem;

                // create destination property
                Node destParent = (Node) session.getItem(property.getParentPath());
                CopyOperation.copy(sourceProperty, destParent, null);

                // remove source property (if not just copying)
                if (isMove) {
                    sourceProperty.remove();
                }
            }

            // make sure the property is not deleted even in case for a given
            // property both @MoveFrom and @Delete is set
            property.setDelete(false);

            // record successful move
            if (isMove) {
                changes.add(Modification.onMoved(source, propPath));
            } else {
                changes.add(Modification.onCopied(source, propPath));
            }
        }
    }

    /**
     * Removes all properties listed as {@link RequestProperty#isDelete()} from
     * the repository.
     *
     * @param session The <code>javax.jcr.Session</code> used to access the
     *            repository to delete the properties.
     * @param reqProperties The map of request properties to check for
     *            properties to be removed.
     * @param response The <code>HtmlResponse</code> to be updated with
     *            information on deleted properties.
     * @throws RepositoryException Is thrown if an error occurrs checking or
     *             removing properties.
     */
    private void processDeletes(Session session,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.isDelete()) {
                String propPath = property.getPath();
                if (session.itemExists(propPath)) {
                    session.getItem(propPath).remove();
                    changes.add(Modification.onDeleted(propPath));
                }
            }
        }

    }

    /**
     * Writes back the content
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void writeContent(Session session,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        SlingPropertyValueHandler propHandler = new SlingPropertyValueHandler(
            dateParser, changes);

        for (RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                Node parent = deepGetOrCreateNode(session,
                    prop.getParentPath(), reqProperties, changes);
                // skip jcr special properties
                if (prop.getName().equals("jcr:primaryType")
                    || prop.getName().equals("jcr:mixinTypes")) {
                    continue;
                }
                if (prop.isFileUpload()) {
                    uploadHandler.setFile(parent, prop, changes);
                } else {
                    propHandler.setProperty(parent, prop);
                }
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
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }

            // ensure the paramName is an absolute property name
            String propPath = toPropertyPath(paramName, response);

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.TYPE_HINT_SUFFIX);

                final RequestParameter[] rp = e.getValue();
                if (rp.length > 0) {
                    prop.setTypeHintValue(rp[0].getString());
                }

                continue;
            }

            // @DefaultValue
            if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.DEFAULT_VALUE_SUFFIX);

                prop.setDefaultValues(e.getValue());

                continue;
            }

            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            // @ValueFrom example:
            // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
            // causes the JCR Text property to be set to the value of the
            // fulltext form field.
            if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.VALUE_FROM_SUFFIX);

                // @ValueFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String refName = e.getValue()[0].getString();
                    RequestParameter[] refValues = request.getRequestParameters(refName);
                    if (refValues != null) {
                        prop.setValues(refValues);
                    }
                }

                continue;
            }

            // SLING-458: Allow Removal of properties prior to update
            // @Delete example:
            // <input name="./Text@Delete" type="hidden" />
            // causes the JCR Text property to be deleted before update
            if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath, SlingPostConstants.SUFFIX_DELETE);

                prop.setDelete(true);

                continue;
            }

            // SLING-455: @MoveFrom means moving content to another location
            // @MoveFrom example:
            // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by moving the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_MOVE_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String sourcePath = e.getValue()[0].getString();
                    prop.setRepositorySource(sourcePath, true);
                }

                continue;
            }

            // SLING-455: @CopyFrom means moving content to another location
            // @CopyFrom example:
            // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_COPY_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String sourcePath = e.getValue()[0].getString();
                    prop.setRepositorySource(sourcePath, false);
                }

                continue;
            }

            // plain property, create from values
            RequestProperty prop = getOrCreateRequestProperty(reqProperties,
                propPath, null);
            prop.setValues(e.getValue());
        }

        return reqProperties;
    }

    /**
     * Returns the <code>paramName</code> as an absolute (unnormalized)
     * property path by prepending the response path (<code>response.getPath</code>)
     * to the parameter name if not already absolute.
     */
    private String toPropertyPath(String paramName, HtmlResponse response) {
        if (!paramName.startsWith("/")) {
            paramName = ResourceUtil.normalize(response.getPath() + '/' + paramName);
        }

        return paramName;
    }

    /**
     * Returns the request property for the given property path. If such a
     * request property does not exist yet it is created and stored in the
     * <code>props</code>.
     *
     * @param props The map of already seen request properties.
     * @param paramName The absolute path of the property including the
     *            <code>suffix</code> to be looked up.
     * @param suffix The (optional) suffix to remove from the
     *            <code>paramName</code> before looking it up.
     * @return The {@link RequestProperty} for the <code>paramName</code>.
     */
    private RequestProperty getOrCreateRequestProperty(
            Map<String, RequestProperty> props, String paramName, String suffix) {
        if (suffix != null && paramName.endsWith(suffix)) {
            paramName = paramName.substring(0, paramName.length()
                - suffix.length());
        }

        RequestProperty prop = props.get(paramName);
        if (prop == null) {
            prop = new RequestProperty(paramName);
            props.put(paramName, prop);
        }

        return prop;
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
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
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
                changes.add(Modification.onCreated(node.getPath()));
            }
            from = to + 1;
        }
        return node;
    }

}
