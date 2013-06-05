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
package org.apache.sling.servlets.post;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AbstractPostOperation</code> class is a base implementation of the
 * {@link PostOperation} service interface providing actual implementations with
 * useful tooling and common functionality like preparing the change logs or
 * saving or refreshing the JCR Session.
 */
public abstract class AbstractPostOperation implements PostOperation {

    /**
     * default log
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Prepares and finalizes the actual operation. Preparation encompasses
     * getting the absolute path of the item to operate on by calling the
     * {@link #getItemPath(SlingHttpServletRequest)} method and setting the
     * location and parent location on the response. After the operation has
     * been done in the {@link #doRun(SlingHttpServletRequest, PostResponse, List)}
     * method the session is saved if there are unsaved modifications. In case
     * of errorrs, the unsaved changes in the session are rolled back.
     *
     * @param request the request to operate on
     * @param response The <code>PostResponse</code> to record execution
     *            progress.
     */
    public void run(final SlingHttpServletRequest request,
                    final PostResponse response,
                    final SlingPostProcessor[] processors) {
        final Session session = request.getResourceResolver().adaptTo(Session.class);

        final VersioningConfiguration versionableConfiguration = getVersioningConfiguration(request);

        try {
            // calculate the paths
            String path = getItemPath(request);
            path = removeAndValidateWorkspace(path, session);
            response.setPath(path);

            // location
            response.setLocation(externalizePath(request, path));

            // parent location
            path = ResourceUtil.getParent(path);
            if (path != null) {
                response.setParentLocation(externalizePath(request, path));
            }

            final List<Modification> changes = new ArrayList<Modification>();

            doRun(request, response, changes);

            // invoke processors
            if (processors != null) {
                for (SlingPostProcessor processor : processors) {
                    processor.process(request, changes);
                }
            }

            final Set<String> nodesToCheckin = new LinkedHashSet<String>();

            // set changes on html response
            for(Modification change : changes) {
                switch ( change.getType() ) {
                    case MODIFY : response.onModified(change.getSource()); break;
                    case DELETE : response.onDeleted(change.getSource()); break;
                    case MOVE   : response.onMoved(change.getSource(), change.getDestination()); break;
                    case COPY   : response.onCopied(change.getSource(), change.getDestination()); break;
                    case CREATE :
                        response.onCreated(change.getSource());
                        if (versionableConfiguration.isCheckinOnNewVersionableNode()) {
                            nodesToCheckin.add(change.getSource());
                        }
                        break;
                    case ORDER  : response.onChange("ordered", change.getSource(), change.getDestination()); break;
                    case CHECKOUT :
                        response.onChange("checkout", change.getSource());
                        nodesToCheckin.add(change.getSource());
                        break;
                    case CHECKIN :
                        response.onChange("checkin", change.getSource());
                        nodesToCheckin.remove(change.getSource());
                        break;
                }
            }

            if (isSessionSaveRequired(session, request)) {
                request.getResourceResolver().commit();
            }

            if (!isSkipCheckin(request)) {
                // now do the checkins
                for(String checkinPath : nodesToCheckin) {
                    if (checkin(request.getResourceResolver(), checkinPath)) {
                        response.onChange("checkin", checkinPath);
                    }
                }
            }

        } catch (Exception e) {

            log.error("Exception during response processing.", e);
            response.setError(e);

        } finally {
            try {
                if (isSessionSaveRequired(session, request)) {
                    request.getResourceResolver().revert();
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException in finally block: {}",
                    e.getMessage(), e);
            }
        }

    }

    /**
     * Actually performs the desired operation filling progress into the
     * <code>changes</code> list and preparing and further information in the
     * <code>response</code>.
     * <p>
     * The <code>response</code> comes prepared with the path, location and
     * parent location set. Other properties are expected to be set by this
     * implementation.
     *
     * @param request The <code>SlingHttpServletRequest</code> providing the
     *            input, mostly in terms of request parameters, to the
     *            operation.
     * @param response The {@link PostResponse} to fill with response
     *            information
     * @param changes A container to add {@link Modification} instances
     *            representing the operations done.
     * @throws RepositoryException Maybe thrown if any error occurrs while
     *             accessing the repository.
     */
    protected abstract void doRun(SlingHttpServletRequest request,
            PostResponse response,
            List<Modification> changes) throws RepositoryException;

    /**
     * Get the versioning configuration.
     */
    protected VersioningConfiguration getVersioningConfiguration(SlingHttpServletRequest request) {
        VersioningConfiguration versionableConfiguration =
            (VersioningConfiguration) request.getAttribute(VersioningConfiguration.class.getName());
        return versionableConfiguration != null ? versionableConfiguration : new VersioningConfiguration();
    }

    /**
     * Check if checkin should be skipped
     */
    protected boolean isSkipCheckin(SlingHttpServletRequest request) {
        return !getVersioningConfiguration(request).isAutoCheckin();
    }

    /**
     * Check whether changes should be written back
     */
    protected boolean isSkipSessionHandling(SlingHttpServletRequest request) {
        return Boolean.parseBoolean((String) request.getAttribute(SlingPostConstants.ATTR_SKIP_SESSION_HANDLING)) == true;
    }

    /**
     * Check whether commit to the resource resolver should be called.
     */
    protected boolean isSessionSaveRequired(Session session, SlingHttpServletRequest request)
            throws RepositoryException {
        return !isSkipSessionHandling(request) && request.getResourceResolver().hasChanges();
    }

    /**
     * Remove the workspace name, if any, from the start of the path and validate that the
     * session's workspace name matches the path workspace name.
     */
    protected String removeAndValidateWorkspace(String path, Session session) throws RepositoryException {
        final int wsSepPos = path.indexOf(":/");
        if (wsSepPos != -1) {
            final String workspaceName = path.substring(0, wsSepPos);
            if (!workspaceName.equals(session.getWorkspace().getName())) {
                throw new RepositoryException("Incorrect workspace. Expecting " + workspaceName + ". Received "
                        + session.getWorkspace().getName());
            }
            return path.substring(wsSepPos + 1);
        }
        return path;
    }


    /**
     * Returns the path of the resource of the request as the item path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     */
    protected String getItemPath(SlingHttpServletRequest request) {
        return request.getResource().getPath();
    }

    /**
     * Returns an iterator on <code>Resource</code> instances addressed in the
     * {@link SlingPostConstants#RP_APPLY_TO} request parameter. If the request
     * parameter is not set, <code>null</code> is returned. If the parameter
     * is set with valid resources an empty iterator is returned. Any resources
     * addressed in the {@link SlingPostConstants#RP_APPLY_TO} parameter is
     * ignored.
     *
     * @param request The <code>SlingHttpServletRequest</code> object used to
     *            get the {@link SlingPostConstants#RP_APPLY_TO} parameter.
     * @return The iterator of resources listed in the parameter or
     *         <code>null</code> if the parameter is not set in the request.
     */
    protected Iterator<Resource> getApplyToResources(
            SlingHttpServletRequest request) {

        final String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
        if (applyTo == null) {
            return null;
        }

        return new ApplyToIterator(request, applyTo);
    }

    /**
     * Returns an external form of the given path prepending the context path
     * and appending a display extension.
     *
     * @param path the path to externalize
     * @return the url
     */
    protected final String externalizePath(SlingHttpServletRequest request,
            String path) {
        StringBuilder ret = new StringBuilder();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(SlingPostConstants.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }

    /**
     * Resolves the given path with respect to the current root path.
     *
     * @param relPath the path to resolve
     * @return the given path if it starts with a '/'; a resolved path
     *         otherwise.
     */
    protected final String resolvePath(String absPath, String relPath) {
        if (relPath.startsWith("/")) {
            return relPath;
        }
        return absPath + "/" + relPath;
    }

    /**
     * Returns true if any of the request parameters starts with
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>}.
     * In this case only parameters starting with either of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>} are
     * considered as providing content to be stored. Otherwise all parameters
     * not starting with the command prefix <code>:</code> are considered as
     * parameters to be stored.
     */
    protected final boolean requireItemPathPrefix(
            SlingHttpServletRequest request) {

        boolean requirePrefix = false;

        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements() && !requirePrefix) {
            String name = (String) names.nextElement();
            requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
        }

        return requirePrefix;
    }

    /**
     * Returns <code>true</code> if the <code>name</code> starts with either
     * of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>}.
     */
    protected boolean hasItemPathPrefix(String name) {
        return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
    }

    /**
     * Orders the given node according to the specified command. The following
     * syntax is supported: <xmp> | first | before all child nodes | before A |
     * before child node A | after A | after child node A | last | after all
     * nodes | N | at a specific position, N being an integer </xmp>
     *
     * @param item node to order
     * @throws RepositoryException if an error occurs
     */
    protected void orderNode(SlingHttpServletRequest request, Item item,
            List<Modification> changes) throws RepositoryException {

        String command = request.getParameter(SlingPostConstants.RP_ORDER);
        if (command == null || command.length() == 0) {
            // nothing to do
            return;
        }

        if (!item.isNode()) {
            return;
        }

        Node parent = item.getParent();

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
                    if (n.getName().equals(item.getName())) {
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
            parent.orderBefore(item.getName(), next);
            changes.add(Modification.onOrder(item.getPath(), next));
            if (log.isDebugEnabled()) {
                log.debug("Node {} moved '{}'", item.getPath(), command);
            }
        } else {
            throw new IllegalArgumentException(
                "provided node ordering command is invalid: " + command);
        }
    }

    protected Node findVersionableAncestor(Node node) throws RepositoryException {
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

    protected boolean isVersionable(Node node) throws RepositoryException {
        return node.isNodeType("mix:versionable");
    }

    protected void checkoutIfNecessary(Node node, List<Modification> changes,
            VersioningConfiguration versioningConfiguration) throws RepositoryException {
        if (versioningConfiguration.isAutoCheckout()) {
            Node versionableNode = findVersionableAncestor(node);
            if (versionableNode != null) {
                if (!versionableNode.isCheckedOut()) {
                    versionableNode.checkout();
                    changes.add(Modification.onCheckout(versionableNode.getPath()));
                }
            }
        }
    }

    private boolean checkin(final ResourceResolver resolver, final String path) throws RepositoryException {
        final Resource rsrc = resolver.getResource(path);
        final Node node = (rsrc == null ? null : rsrc.adaptTo(Node.class));
        if (node != null) {
            if (node.isCheckedOut() && isVersionable(node)) {
                node.checkin();
                return true;
            }
        }
        return false;
    }

    private static class ApplyToIterator implements Iterator<Resource> {

        private final ResourceResolver resolver;
        private final Resource baseResource;
        private final String[] paths;

        private int pathIndex;

        private Resource nextResource;

        private Iterator<Resource> resourceIterator = null;

        ApplyToIterator(SlingHttpServletRequest request, String[] paths) {
            this.resolver = request.getResourceResolver();
            this.baseResource = request.getResource();
            this.paths = paths;
            this.pathIndex = 0;

            nextResource = seek();
        }

        public boolean hasNext() {
            return nextResource != null;
        }

        public Resource next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Resource result = nextResource;
            nextResource = seek();

            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Resource seek() {
        	if (resourceIterator != null) {
        		if (resourceIterator.hasNext()) {
            		//return the next resource in the iterator
        			Resource res = resourceIterator.next();
        			return res;
                }
    			resourceIterator = null;
        	}
            while (pathIndex < paths.length) {
                String path = paths[pathIndex];
                pathIndex++;

                //SLING-2415 - support wildcard as the last segment of the applyTo path
                if (path.endsWith("*")) {
                	if (path.length() == 1) {
                		resourceIterator = baseResource.listChildren();
                	} else if (path.endsWith("/*")) {
                    	path = path.substring(0, path.length() - 2);
                    	if (path.length() == 0) {
                    		resourceIterator = baseResource.listChildren();
                    	} else {
                        	Resource res = resolver.getResource(baseResource, path);
                            if (res != null) {
                            	resourceIterator = res.listChildren();
                            }
                    	}
                    }
                	if (resourceIterator != null) {
                		//return the first resource in the iterator
                		if (resourceIterator.hasNext()) {
                			Resource res = resourceIterator.next();
                			return res;
                		}
                        resourceIterator = null;
                	}
                } else {
                    Resource res = resolver.getResource(baseResource, path);
                    if (res != null) {
                        return res;
                    }
                }
            }

            // no more elements in the array
            return null;
        }
    }
}