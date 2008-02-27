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
package org.apache.sling.ujax;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.mime.MimeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds various states and encapsulates method that are neede to handle a
 * ujax post request.
 */
public class UjaxPostProcessor {

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(UjaxPostProcessor.class);


    /**
     * log that records the changes applied during the processing of the
     * post request.
     */
    private final ChangeLog changeLog = new ChangeLog();

    /**
     * handler that deals with properties
     */
    private final UjaxPropertyValueHandler propHandler;

    /**
     * handler that deals with file upload
     */
    private final UjaxFileUploadHandler uploadHandler;

    /**
     * utility class for generating node names
     */
    private final NodeNameGenerator nodeNameGenerator;

    /**
     * utility class for parsing date strings
     */
    private final DateParser dateParser;

    /**
     * the sling http servlet request
     */
    private final SlingHttpServletRequest request;

    /**
     * the jcr session to operate on
     */
    private final Session session;

    /**
     * the root path of this processor.
     */
    private final String rootPath;

    /**
     * path of the node that was targeted or created.
     */
    private String currentPath;

    /**
     * prefix of which the names of request properties must start with
     * in order to be regardes as input values.
     */
    private String savePrefix;

    /**
     * indicates if the request contains a 'star suffix'
     */
    private boolean isCreateRequest;

    /**
     * records any error
     */
    private Exception error;

    /**
     * Creates a new post processor
     * @param request the request to operate on
     * @param session jcr session to operate on
     * @param nodeNameGenerator the node name generator. use a servlet scoped one,
     *        so that it can hold states.
     * @param dateParser helper for parsing date strings
     */
    public UjaxPostProcessor(SlingHttpServletRequest request, Session session,
                             NodeNameGenerator nodeNameGenerator,
                             DateParser dateParser,
                             MimeTypeService mimeTypeService) {
        this.request = request;
        this.session = session;

        // default to non-creating request (trailing DEFAULT_CREATE_SUFFIX)
        isCreateRequest = false;

        // calculate the paths
        StringBuffer rootPathBuf = new StringBuffer();
        String suffix;
        Resource currentResource = request.getResource();
        if (Resource.RESOURCE_TYPE_NON_EXISTING.equals(currentResource.getResourceType())) {

            // no resource, treat the missing resource path as suffix
            suffix = currentResource.getPath();

        } else {

            // resource for part of the path, use request suffix
            suffix = request.getRequestPathInfo().getSuffix();

            // and preset the path buffer with the resource path
            rootPathBuf.append(currentResource.getPath());

        }

        // check for extensions or create suffix in the suffix
        if (suffix != null) {

            // cut off any selectors/extension from the suffix
            int dotPos = suffix.indexOf('.');
            if (dotPos > 0) {
                suffix = suffix.substring(0, dotPos);

            // otherwise check whether it is a create request (trailing /*)
            } else if (suffix.endsWith(UjaxPostServlet.DEFAULT_CREATE_SUFFIX)) {
                suffix = suffix.substring(0, suffix.length()
                    - UjaxPostServlet.DEFAULT_CREATE_SUFFIX.length());
                isCreateRequest = true;
            }

            // append the remains of the suffix to the path buffer
            rootPathBuf.append(suffix);

        }

        rootPath = rootPathBuf.toString();

        this.nodeNameGenerator = nodeNameGenerator;
        this.dateParser = dateParser;
        propHandler = new UjaxPropertyValueHandler(this);
        uploadHandler = new UjaxFileUploadHandler(this, mimeTypeService);
    }

    /**
     * Returns the change log.
     * @return the change log.
     */
    public ChangeLog getChangeLog() {
        return changeLog;
    }

    /**
     * Returns the jcr session
     * @return the jcr session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Returns the root path of this processor.
     * @return the root path of this processor.
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Returns the path of the node that was the parent of the property
     * modifications.
     * @return the path of the 'current' node.
     */
    public String getCurrentPath() {
        return currentPath;
    }

    /**
     * Returns <code>true</code> if this was a create request.
     * @return <code>true</code> if this was a create request.
     */
    public boolean isCreateRequest() {
        return isCreateRequest;
    }

    /**
     * Returns the location of the modification. this is the externalized form
     * of the current path.
     * @return the location of the modification.
     */
    public String getLocation() {
        if (currentPath == null) {
            return externalizePath(rootPath);
        }
        return externalizePath(currentPath);
    }

    /**
     * Returns the parent location of the modification. this is the externalized
     * form of the parent node of the current path.
     * @return the location of the modification.
     */
    public String getParentLocation() {
        String path = currentPath == null ? rootPath : currentPath;
        path = path.substring(0, path.lastIndexOf('/'));
        return externalizePath(path);
    }

    /**
     * Returns the date parser
     * @return date parser
     */
    public DateParser getDateParser() {
        return dateParser;
    }

    /**
     * Returns an external form of the given path prepeding the context path
     * and appending a display extension.
     * @param path the path to externalize
     * @return the url
     */
    private String externalizePath(String path) {
        StringBuffer ret = new StringBuffer();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(UjaxPostServlet.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }



    /**
     * Returns any recorded error or <code>null</code>
     * @return an error or null
     */
    public Exception getError() {
        return error;
    }

    /**
     * Returns the request of this processor
     * @return the sling servlet request
     */
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    /**
     * Processes the actions defined by the request
     */
    public void run()  {
        try {
            processDeletes();
            processContent();
            processMoves();
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (Exception e) {
            error = e;
        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException in finally block: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Resolves the given path with respect to the current root path.
     *
     * @param path the path to resolve
     * @return the given path if it starts with a '/';
     *         a resolved path otherwise.
     */
    public String resolvePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        if (currentPath == null) {
            return rootPath + "/" + path;
        }
        return currentPath + "/" + path;
    }

    /**
     * Delete Items at the provided paths
     *
     * @throws RepositoryException if a repository error occurs
     */
    private void processDeletes() throws RepositoryException {
        final String [] paths = request.getParameterValues(UjaxPostServlet.RP_DELETE_PATH);
        if (paths != null) {
            for (String path : paths) {
                if (!path.equals("")) {
                    path = resolvePath(path);
                    if (session.itemExists(path)) {
                        session.getItem(path).remove();
                        changeLog.onDeleted(path);
                        log.debug("Deleted item {}", path);
                    } else {
                        log.debug("Item at {} not found for deletion, ignored", path);
                    }
                }
            }
        }
    }

    /**
     * Move nodes at the provided paths
     *
     * @throws RepositoryException if a repository error occurs
     * @throws IllegalArgumentException if the move arguments are incorrect
     */
    private void processMoves() throws RepositoryException,
            IllegalArgumentException {
        final String [] moveSrc = request.getParameterValues(UjaxPostServlet.RP_MOVE_SRC);
        final String [] moveDest = request.getParameterValues(UjaxPostServlet.RP_MOVE_DEST);
        if (moveSrc == null || moveDest == null) {
            return;
        }
        if (moveSrc.length != moveDest.length) {
            throw new IllegalArgumentException("Unable to process move. there " +
                    "must be the same amount of source and destination parameters.");
        }
        for (int i=0; i<moveSrc.length; i++) {
            String src = moveSrc[i];
            String dest = moveDest[i];
            if (src.equals(dest)) {
                // ignore
                continue;
            }
            if (src.equals("")) {
                throw new IllegalArgumentException("Unable to process move. source argument is empty.");
            }
            if (dest.equals("")) {
                throw new IllegalArgumentException("Unable to process move. destination argument is empty.");
            }
            src = resolvePath(src);
            dest = resolvePath(dest);
            session.move(src, dest);
            changeLog.onMoved(src, dest);
            log.debug("moved {} to {}", src, dest);
        }
    }


    /**
     * Create or update node(s) according to current request
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void processContent() throws RepositoryException, ServletException {
        // get desired path.
        String nodePath = rootPath;
        // check for star suffix in request
        if (isCreateRequest) {
            // If the path ends with a *, create a node under its parent, with
            // a generated node name
            nodePath += "/" + nodeNameGenerator.getNodeName(request.getRequestParameterMap(), getSavePrefix());

            // if resulting path exists, add a suffix until it's not the case anymore
            if (session.itemExists(nodePath)) {
                for (int suffix = 0; suffix < 1000; suffix++) {
                    String newPath = nodePath + "_" + suffix;
                    if(!session.itemExists(newPath)) {
                        nodePath = newPath;
                        break;
                    }
                }
            }
            // if it still exists there are more than 1000 nodes ?
            if (session.itemExists(nodePath)) {
                throw new ServletException("Collision in generated node names for path=" + nodePath);
            }

        }
        currentPath = nodePath;

        // process the "order" command if any
        final String order = request.getParameter(UjaxPostServlet.RP_ORDER);
        if  (order!=null) {
            processNodeOrder(currentPath, order);
        }

        // walk the request parameters, create and save nodes and properties
        for (Map.Entry<String, RequestParameter[]>  e: request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with ujax:
            if(paramName.startsWith(UjaxPostServlet.RP_PREFIX)) {
                continue;
            }
            // ignore field with a '@TypeHint' suffix. this is dealt in RequestProperty
            if (paramName.endsWith(UjaxPostServlet.TYPE_HINT_SUFFIX)) {
                continue;
            }
            // ignore field with a '@DefaultValue' suffix. this is dealt in RequestProperty
            if (paramName.endsWith(UjaxPostServlet.DEFAULT_VALUE_SUFFIX)) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if(!paramName.startsWith(getSavePrefix())) {
                continue;
            }
            String propertyName = paramName.substring(getSavePrefix().length());
            if (propertyName.length() == 0) {
                continue;
            }
            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            RequestParameter[] values = e.getValue();
            final int vfIndex = propertyName.indexOf(UjaxPostServlet.VALUE_FROM_SUFFIX);
            if (vfIndex >= 0) {
                // @ValueFrom example:
                // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
                // causes the JCR Text property to be set to the value of the fulltext form field.
                propertyName = propertyName.substring(0, vfIndex);
                final RequestParameter[] rp = request.getRequestParameterMap().get(paramName);
                if(rp == null || rp.length > 1) {
                    // @ValueFrom params must have exactly one value, else ignored
                    continue;
                }
                String mappedName = rp[0].getString();
                values = request.getRequestParameterMap().get(mappedName);
                if(values==null) {
                    // no value for "fulltext" in our example, ignore parameter
                    continue;
                }
            }
            // create property helper and get parent node
            RequestProperty prop = new RequestProperty(this, propertyName, values);
            Node parent = deepGetOrCreateNode(currentPath, prop.getParentPath());

            // call handler
            if (prop.isFileUpload()) {
                uploadHandler.setFile(parent, prop);
            } else {
                propHandler.setProperty(parent, prop);
            }
        }
    }

    /**
     * Deep gets or creates a node, parent-padding with default nodes nodes.
     * If the path is empty, the given parent node is returned.
     *
     * @param parent path to the parent node, may be null if path is absolute
     * @param path path to node that needs to be deep-created
     * @return node at path
     * @throws RepositoryException if an error occurs
     * @throws IllegalArgumentException if the path is relative and parent
     *         is <code>null</code>
     */
    private Node deepGetOrCreateNode(String parent, String path)
            throws RepositoryException {
        if(log.isDebugEnabled()) {
            log.debug("Deep-creating Node '{}/{}'", parent, path);
        }
        if (path.equals("")) {
            if (parent == null || !parent.startsWith("/")) {
                throw new IllegalArgumentException("parent must be an absolute path for relative paths.");
            }
            path = parent;
        } else if (path.charAt(0) != '/') {
            // prepend parent path if path is relative
            if (parent == null || !parent.startsWith("/")) {
                throw new IllegalArgumentException("parent must be an absolute path for relative paths.");
            }
            if (parent.endsWith("/")) {
                path = parent + path;
            } else {
                path = parent + "/" + path;
            }
        }

        String[] pathelems = path.substring(1).split("/");
        Node node = session.getRootNode();

        for (String name: pathelems) {
            if (node.hasNode(name)) {
                node = node.getNode(name);
            } else {
                node = node.addNode(name);
                changeLog.onCreated(node.getPath());
            }
        }
        return node;
    }

    /**
     * Return the "save prefix" to use. the names of request properties must
     * start with that prefix in order to be regarded as input values.
     *
     * @return the save prefix
     */
    public String getSavePrefix() {
        if (savePrefix == null) {
            savePrefix = request.getParameter(UjaxPostServlet.RP_SAVE_PARAM_PREFIX);
            if (savePrefix == null) {
                savePrefix = UjaxPostServlet.DEFAULT_SAVE_PARAM_PREFIX;
            }
            if (savePrefix.length() > 0) {
                String prefix = "";
                // if no parameters start with this prefix, it is not used
                for (String name: request.getRequestParameterMap().keySet()) {
                    if (name.startsWith(savePrefix)) {
                        prefix = savePrefix;
                        break;
                    }
                }
                savePrefix = prefix;
            }
        }
        return savePrefix;
    }

    /**
     * If orderCode is ORDER_ZERO, move n so that it is the first child of its
     * parent
     * @param nodePath path to the node to order
     * @param orderCode code that specifies how to order
     * @throws RepositoryException if a repository error occurs
     */
    private void processNodeOrder(String nodePath, String orderCode)
            throws RepositoryException {
        if (UjaxPostServlet.ORDER_ZERO.equals(orderCode)) {
            final Node n = deepGetOrCreateNode((String) null, nodePath);
            final Node parent = n.getParent();
            final String beforename=parent.getNodes().nextNode().getName();
            parent.orderBefore(n.getName(), beforename);
            if(log.isDebugEnabled()) {
                log.debug("Node {} moved to be first child of its parent, " +
                        "due to orderCode=" + orderCode, n.getPath());
            }
        } else {
            if(log.isDebugEnabled()) {
                log.debug("orderCode '{}' invalid, ignored", orderCode);
            }
        }
    }

}