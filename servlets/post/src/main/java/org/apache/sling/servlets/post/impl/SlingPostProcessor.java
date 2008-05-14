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
package org.apache.sling.servlets.post.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds various states and encapsulates methods that are needed to handle a
 * post request.
 */
public class SlingPostProcessor {

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(SlingPostProcessor.class);

    public static final String ORDER_FIRST = "first";
    public static final String ORDER_BEFORE = "before ";
    public static final String ORDER_AFTER = "after ";
    public static final String ORDER_LAST = "last";

    /**
     * handler that deals with properties
     */
    private final SlingPropertyValueHandler propHandler;

    /**
     * handler that deals with file upload
     */
    private final SlingFileUploadHandler uploadHandler;

    /**
     * utility class for generating node names
     */
    private final NodeNameGenerator nodeNameGenerator;

    // TODO
    private final String rootPath;

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
     * prefix of which the names of request properties must start with
     * in order to be regarded as input values.
     */
    private String savePrefix;

    private HtmlResponse htmlResponse;

    /**
     * map of properties that form the content
     */
    private Map<String, RequestProperty> reqProperties = new LinkedHashMap<String, RequestProperty>();


    /**
     * Creates a new post processor
     * @param request the request to operate on
     * @param session jcr session to operate on
     * @param nodeNameGenerator the node name generator. use a servlet scoped one,
     *        so that it can hold states.
     * @param dateParser helper for parsing date strings
     * @param servletContext The ServletContext to use for file upload
     */
    public SlingPostProcessor(SlingHttpServletRequest request, Session session,
                             NodeNameGenerator nodeNameGenerator,
                             DateParser dateParser,
                             ServletContext servletContext) {
        this.request = request;
        this.session = session;
        this.htmlResponse = new HtmlResponse();

        htmlResponse.setReferer(request.getHeader("referer"));

        // default to non-creating request (trailing DEFAULT_CREATE_SUFFIX)
        htmlResponse.setCreateRequest(false);

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
        if (suffix != null) {

            // cut off any selectors/extension from the suffix
            int dotPos = suffix.indexOf('.');
            if (dotPos > 0) {
                suffix = suffix.substring(0, dotPos);
            }

            // and check whether it is a create request (trailing /*)
            if (suffix.endsWith(SlingPostServlet.DEFAULT_CREATE_SUFFIX)) {
                suffix = suffix.substring(0, suffix.length()
                    - SlingPostServlet.DEFAULT_CREATE_SUFFIX.length());
                htmlResponse.setCreateRequest(true);
            }

            // append the remains of the suffix to the path buffer
            rootPathBuf.append(suffix);

        }

        this.rootPath = rootPathBuf.toString();
        this.nodeNameGenerator = nodeNameGenerator;
        this.dateParser = dateParser;
        propHandler = new SlingPropertyValueHandler(this);
        uploadHandler = new SlingFileUploadHandler(this, servletContext);
    }

    /**
     * Returns the jcr session
     * @return the jcr session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Returns the date parser
     * @return date parser
     */
    public DateParser getDateParser() {
        return dateParser;
    }

    /**
     * Returns an external form of the given path prepending the context path
     * and appending a display extension.
     * @param path the path to externalize
     * @return the url
     */
    private String externalizePath(String path) {
        StringBuffer ret = new StringBuffer();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(SlingPostServlet.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }

    public HtmlResponse getHtmlResponse() {

        String path = htmlResponse.getPath();
        if (path == null) {
            path = rootPath;
        }

        // location
        htmlResponse.setLocation(externalizePath(path));

        // parent location
        path = path.substring(0, path.lastIndexOf('/'));
        htmlResponse.setParentLocation(externalizePath(path));

        return htmlResponse;
    }

    /**
     * Returns the request of this processor
     * @return the sling servlet request
     */
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    /**
     * Resolves the given path with respect to the current root path.
     *
     * @param relPath the path to resolve
     * @return the given path if it starts with a '/';
     *         a resolved path otherwise.
     */
    public String resolvePath(String relPath) {
        if (relPath.startsWith("/")) {
            return relPath;
        }
        String path = htmlResponse.getPath();
        if (path == null) {
            path = rootPath;
        }
        return path + "/" + relPath;
    }

    /**
     * Processes the actions defined by the request in the following order
     * <ol>
     * <li>calculate the 'currentPath' respecting a 'create suffix'
     * <li>collect all content properties included in the request
     * <li>create new node
     * <li>perform 'moves'
     * <li>perform 'deletes'
     * <li>write back content
     * <li>process node ordering
     * </ol>
     */
    public void run()  {
        try {
            // do not change order unless you have a very good reason.
            initCurrentPath();
            collectContent();
            processCreate();
            processMoves();
            processCopies();
            processDeletes();
            writeContent();
            processOrder();
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (Exception e) {
            log.error("Exception during response processing.", e);
            htmlResponse.setError(e);
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
     * Delete Items at the provided paths
     *
     * @throws RepositoryException if a repository error occurs
     */
    private void processDeletes() throws RepositoryException {
        final String [] paths = request.getParameterValues(SlingPostServlet.RP_DELETE_PATH);
        if (paths != null) {
            for (String path : paths) {
                if (!path.equals("")) {
                    path = resolvePath(path);
                    try {
                        if (session.itemExists(path)) {
                            session.getItem(path).remove();
                            htmlResponse.onDeleted(path);
                            log.debug("Deleted item {}", path);
                        } else {
                            log.debug("Item at {} not found for deletion, ignored", path);
                        }
                    } catch (NamespaceException e) {
                        // ignore since deleting an item that has a missing
                        // namespace is comparable to deleting an inexistent
                        // item.
                        log.warn("deleting {} caused {}", path, e.toString());
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
        final String [] moveSrc = request.getParameterValues(SlingPostServlet.RP_MOVE_SRC);
        final String [] moveDest = request.getParameterValues(SlingPostServlet.RP_MOVE_DEST);
        final String flags = request.getParameter(SlingPostServlet.RP_MOVE_FLAGS);
        final boolean isReplace = flags != null && flags.contains(SlingPostServlet.FLAG_REPLACE);

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
            // delete destination if already exists
            if (session.itemExists(dest)) {
                if (isReplace) {
                    session.getItem(dest).remove();
                } else {
                    throw new IllegalArgumentException(
                        "Unable to process move. destination item already exists "
                            + dest);
                }
            } else {
                // check if path to destination exists and create it, but only
                // if it's a descendant of the current node
                String dstParent = dest.substring(0, dest.lastIndexOf('/'));
                if (!dstParent.equals("") && !session.itemExists(dstParent)) {
                    if (dstParent.startsWith(htmlResponse.getPath() + "/")) {
                        deepGetOrCreateNode(dstParent);
                    } else {
                        throw new IllegalArgumentException(
                            "Unable to process move. destination's parent does not exist "
                                + dest);
                    }
                }
            }
            session.move(src, dest);
            htmlResponse.onMoved(src, dest);
            log.debug("moved {} to {}", src, dest);
        }
    }

    /**
     * Copy nodes at the provided paths
     *
     * @throws RepositoryException if a repository error occurs
     * @throws IllegalArgumentException if the move arguments are incorrect
     */
    private void processCopies() throws RepositoryException,
            IllegalArgumentException {
        final String [] copySrc = request.getParameterValues(SlingPostServlet.RP_COPY_SRC);
        final String [] copyDst = request.getParameterValues(SlingPostServlet.RP_COPY_DEST);
        final String flags = request.getParameter(SlingPostServlet.RP_COPY_FLAGS);
        boolean isReplace = flags != null && flags.contains(SlingPostServlet.FLAG_REPLACE);

        if (copySrc == null || copyDst == null) {
            return;
        }
        if (copySrc.length != copyDst.length) {
            throw new IllegalArgumentException("Unable to process copy. there " +
                    "must be the same amount of source and destination parameters.");
        }
        for (int i=0; i<copySrc.length; i++) {
            String src = copySrc[i];
            String dst = copyDst[i];
            if (src.equals(dst)) {
                // ignore
                continue;
            }
            if (src.equals("")) {
                throw new IllegalArgumentException("Unable to process copy. source argument is empty.");
            }
            if (dst.equals("")) {
                throw new IllegalArgumentException("Unable to process copy. destination argument is empty.");
            }
            src = resolvePath(src);
            // special handling for "*" destinations. use the calculated new path.
            if (dst.equals("*")) {
                dst = htmlResponse.getPath();
                // small hack, force replace
                isReplace = true;
            } else {
                dst = resolvePath(dst);
            }
            // delete destination if already exists
            String dstParent = dst.substring(0, dst.lastIndexOf('/'));
            if (session.itemExists(dst)) {
                if (isReplace) {
                    session.getItem(dst).remove();
                } else {
                    throw new IllegalArgumentException(
                        "Unable to process copy. destination item already exists "
                            + dst);
                }
            } else {
                // check if path to destination exists and create it, but only
                // if it's a descendant of the current node
                if (!dstParent.equals("") && !session.itemExists(dstParent)) {
                    if (dstParent.startsWith(htmlResponse.getPath() + "/")) {
                        deepGetOrCreateNode(dstParent);
                    } else {
                        throw new IllegalArgumentException(
                            "Unable to process copy. destination's parent does not exist "
                                + dst);
                    }
                }
            }
            copyNode((Node) session.getItem(src), (Node) session.getItem(dstParent), dst.substring(dst.lastIndexOf('/')+1));
            htmlResponse.onCopied(src, dst);
            log.debug("copy {} to {}", src, dst);
        }
    }

    private void copyNode(Node src, Node dstParent, String name)
            throws RepositoryException {
        // create new node
        Node dst = dstParent.addNode(name, src.getPrimaryNodeType().getName());
        for (NodeType mix: src.getMixinNodeTypes()) {
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

    /**
     * Initialize the current path. If this is a create request, a new node
     * name is generated.
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void initCurrentPath() throws RepositoryException, ServletException {
        // get desired path.
        String path = rootPath;

        // check for star suffix in request
        if (htmlResponse.isCreateRequest()) {
            // If the path ends with a *, create a node under its parent, with
            // a generated node name
            path += "/" + nodeNameGenerator.getNodeName(request.getRequestParameterMap(), getSavePrefix());

            // if resulting path exists, add a suffix until it's not the case anymore
            if (session.itemExists(path)) {
                for (int suffix = 0; suffix < 1000; suffix++) {
                    String newPath = path + "_" + suffix;
                    if(!session.itemExists(newPath)) {
                        path = newPath;
                        break;
                    }
                }
            }
            // if it still exists there are more than 1000 nodes ?
            if (session.itemExists(path)) {
                throw new ServletException("Collision in generated node names for path=" + path);
            }
        }

        htmlResponse.setPath(path);
    }

    /**
     * Create node(s) according to current request
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void processCreate() throws RepositoryException, ServletException {
        // create new node in any case
        deepGetOrCreateNode(htmlResponse.getPath());
    }

    /**
     * Writes back the content
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    private void writeContent() throws RepositoryException, ServletException {
        for (RequestProperty prop: reqProperties.values()) {
            Node parent = deepGetOrCreateNode(prop.getParentPath());
            // skip jcr special properties
            if (prop.getName().equals("jcr:primaryType") ||
                    prop.getName().equals("jcr:mixinTypes")) {
                continue;
            }
            if (prop.isFileUpload()) {
                uploadHandler.setFile(parent, prop);
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
    private void collectContent() throws RepositoryException, ServletException {
        // walk the request parameters and collect the properties
        for (Map.Entry<String, RequestParameter[]>  e: request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with sling:post
            if(paramName.startsWith(SlingPostServlet.RP_PREFIX)) {
                continue;
            }
            // ignore field with a '@TypeHint' suffix. this is dealt with later
            if (paramName.endsWith(SlingPostServlet.TYPE_HINT_SUFFIX)) {
                continue;
            }
            // ignore field with a '@DefaultValue' suffix. this is dealt with later
            if (paramName.endsWith(SlingPostServlet.DEFAULT_VALUE_SUFFIX)) {
                continue;
            }
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
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
            final int vfIndex = propertyName.indexOf(SlingPostServlet.VALUE_FROM_SUFFIX);
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
            // create property helper and add it to the list
            String propPath = propertyName;
            if (!propPath.startsWith("/")) {
                propPath = htmlResponse.getPath() + "/" + propertyName;
            }
            RequestProperty prop = new RequestProperty(propPath, values);

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            final String thName = getSavePrefix() + propertyName + SlingPostServlet.TYPE_HINT_SUFFIX;
            final RequestParameter rp = request.getRequestParameter(thName);
            if (rp != null) {
                prop.setTypeHint(rp.getString());
            }

            // @DefaultValue
            final String dvName = getSavePrefix() + propertyName + SlingPostServlet.DEFAULT_VALUE_SUFFIX;
            prop.setDefaultValues(request.getRequestParameters(dvName));

            reqProperties.put(propPath, prop);
        }
    }

    /**
     * Checks the collected content for a jcr:primaryType property at the
     * specified path.
     * @param path path to check
     * @return the primary type or <code>null</code>
     */
    private String getPrimaryType(String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:primaryType");
        return prop == null ? null : prop.getStringValues()[0];
    }

    /**
     * Checks the collected content for a jcr:mixinTypes property at the
     * specified path.
     * @param path path to check
     * @return the mixin types or <code>null</code>
     */
    private String[] getMixinTypes(String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:mixinTypes");
        return prop == null ? null : prop.getStringValues();
    }

    /**
     * Deep gets or creates a node, parent-padding with default nodes nodes.
     * If the path is empty, the given parent node is returned.
     *
     * @param path path to node that needs to be deep-created
     * @return node at path
     * @throws RepositoryException if an error occurs
     * @throws IllegalArgumentException if the path is relative and parent
     *         is <code>null</code>
     */
    private Node deepGetOrCreateNode(String path)
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
        while ( startingNode == null ) {
            if ( startingNodePath.equals("/") ) {
                startingNode = session.getRootNode();
            } else if ( session.itemExists(startingNodePath) ) {
                startingNode = (Node)session.getItem(startingNodePath);
            } else {
                int pos = startingNodePath.lastIndexOf('/');
                if (pos > 0 ) {
                    startingNodePath = startingNodePath.substring(0, pos);
                } else {
                    startingNodePath = "/";
                }
            }
        }
        // is the searched node already existing?
        if (startingNodePath.length() == path.length() ) {
            return startingNode;
        }
        // create nodes
        int from = (startingNodePath.length() == 1 ? 1 : startingNodePath.length() + 1);
        Node node = startingNode;
        while (from > 0) {
            final int to = path.indexOf('/', from);
            final String name = to < 0
                    ? path.substring(from)
                    : path.substring(from, to);
            // although the node should not exist (according to the first test above)
            // we do a sanety check.
            if (node.hasNode(name)) {
                node = node.getNode(name);
            } else {
                final String tmpPath = to < 0 ? path : path.substring(0, to);
                // check for node type
                final String nodeType = getPrimaryType(tmpPath);
                if (nodeType != null) {
                    node = node.addNode(name, nodeType);
                } else {
                    node = node.addNode(name);
                }
                // check for mixin types
                final String[] mixinTypes = getMixinTypes(tmpPath);
                if (mixinTypes != null) {
                    for (String mix: mixinTypes) {
                        node.addMixin(mix);
                    }
                }
                htmlResponse.onCreated(node.getPath());
            }
            from = to + 1;
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
            savePrefix = request.getParameter(SlingPostServlet.RP_SAVE_PARAM_PREFIX);
            if (savePrefix == null) {
                savePrefix = SlingPostServlet.DEFAULT_SAVE_PARAM_PREFIX;
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

    private void processOrder() throws RepositoryException {
        final String orderCode = request.getParameter(SlingPostServlet.RP_ORDER);
        if  (orderCode!=null) {
            final Node n = deepGetOrCreateNode(htmlResponse.getPath());
            orderNode(n, orderCode);
        }
    }

    /**
     * Orders the given node according to the specified command.
     *
     * The following syntax is supported:
     * <xmp>
     * | first    | before all child nodes
     * | before A | before child node A
     * | after A  | after child node A
     * | last     | after all nodes
     * | N        | at a specific position, N being an integer
     * </xmp>
     *
     * @param node node to order
     * @param command specifies the ordering type
     * @throws RepositoryException if an error occurs
     */
    private void orderNode(Node node, String command)
            throws RepositoryException {
        Node parent = node.getParent();
        String next = null;
        if (command.equals(ORDER_FIRST)) {
            next =  parent.getNodes().nextNode().getName();
        } else if (command.equals(ORDER_LAST)) {
            next = "";
        } else if (command.startsWith(ORDER_BEFORE)) {
            next = command.substring(ORDER_BEFORE.length());
        } else if (command.startsWith(ORDER_AFTER)) {
            String name = command.substring(ORDER_AFTER.length());
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
                // abcde  move a -> 2 (above 3)
                // bcade  move a -> 1 (above 1)
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
                throw new IllegalArgumentException("provided node ordering command is invalid: " + command);
            }
        }
        if (next != null) {
            if (next.equals("")) {
                next = null;
            }
            parent.orderBefore(node.getName(), next);
            if(log.isDebugEnabled()) {
                log.debug("Node {} moved '{}'", node.getPath(), command);
            }
        } else {
            throw new IllegalArgumentException("provided node ordering command is invalid: " + command);
        }
    }
}