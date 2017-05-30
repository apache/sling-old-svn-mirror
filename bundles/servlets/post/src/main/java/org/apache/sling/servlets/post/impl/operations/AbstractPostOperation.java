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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.JCRSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AbstractPostOperation</code> class is a base implementation of the
 * {@link PostOperation} service interface providing actual implementations with
 * useful tooling and common functionality like preparing the change logs or
 * saving or refreshing.
 */
public abstract class AbstractPostOperation implements PostOperation {

    /**
     * Default logger
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** The JCR support provides additional functionality if the resources a backed up by JCR. */
    protected final JCRSupport jcrSsupport = JCRSupport.INSTANCE;

    /**
     * Prepares and finalizes the actual operation. Preparation encompasses
     * getting the absolute path of the item to operate on by calling the
     * {@link #getResourcePath(SlingHttpServletRequest)} method and setting the
     * location and parent location on the response. After the operation has
     * been done in the {@link #doRun(SlingHttpServletRequest, PostResponse, List)}
     * method the session is saved if there are unsaved modifications. In case
     * of errors, the unsaved changes in the session are rolled back.
     *
     * @param request the request to operate on
     * @param response The <code>PostResponse</code> to record execution
     *            progress.
     * @param processors The array of processors
     */
    @Override
    public void run(final SlingHttpServletRequest request,
                    final PostResponse response,
                    final SlingPostProcessor[] processors) {
        final VersioningConfiguration versionableConfiguration = getVersioningConfiguration(request);

        try {
            // calculate the paths
            String path = this.getResourcePath(request);
            response.setPath(path);

            // location
            response.setLocation(externalizePath(request, path));

            // parent location
            path = ResourceUtil.getParent(path);
            if (path != null) {
                response.setParentLocation(externalizePath(request, path));
            }

            final List<Modification> changes = new ArrayList<>();

            doRun(request, response, changes);

            // invoke processors
            if (processors != null) {
                for (SlingPostProcessor processor : processors) {
                    processor.process(request, changes);
                }
            }

            // check modifications for remaining postfix and store the base path
            final Map<String, String> modificationSourcesContainingPostfix = new HashMap<>();
            final Set<String> allModificationSources = new HashSet<>(changes.size());
            for (final Modification modification : changes) {
                final String source = modification.getSource();
                if (source != null) {
                    allModificationSources.add(source);
                    final int atIndex = source.indexOf('@');
                    if (atIndex > 0) {
                        modificationSourcesContainingPostfix.put(source.substring(0, atIndex), source);
                    }
                }
            }

            // fail if any of the base paths (before the postfix) which had a postfix are contained in the modification set
            if (modificationSourcesContainingPostfix.size() > 0) {
                for (final Map.Entry<String, String> sourceToCheck : modificationSourcesContainingPostfix.entrySet()) {
                    if (allModificationSources.contains(sourceToCheck.getKey())) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Postfix-containing path " + sourceToCheck.getValue() +
                                " contained in the modification list. Check configuration.");
                        return;
                    }
                }
            }

            final Set<String> nodesToCheckin = new LinkedHashSet<>();

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
                    case RESTORE : response.onChange("restore", change.getSource());
                        break;
                }
            }

            if (isResourceResolverCommitRequired(request)) {
                request.getResourceResolver().commit();
            }

            if (!isSkipCheckin(request)) {
                // now do the checkins
                for(String checkinPath : nodesToCheckin) {
                    if (this.jcrSsupport.checkin(request.getResourceResolver().getResource(checkinPath))) {
                        response.onChange("checkin", checkinPath);
                    }
                }
            }

        } catch (Exception e) {

            log.error("Exception during response processing.", e);
            response.setError(e);

        } finally {
            if (isResourceResolverCommitRequired(request)) {
                request.getResourceResolver().revert();
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
     * @throws PersistenceException Maybe thrown if any error occurs while
     *             accessing the repository.
     */
    protected abstract void doRun(SlingHttpServletRequest request,
            PostResponse response,
            List<Modification> changes) throws PersistenceException;

    /**
     * Get the versioning configuration.
     * @param request The http request
     * @return The versioning configuration
     */
    protected VersioningConfiguration getVersioningConfiguration(final SlingHttpServletRequest request) {
        VersioningConfiguration versionableConfiguration =
            (VersioningConfiguration) request.getAttribute(VersioningConfiguration.class.getName());
        return versionableConfiguration != null ? versionableConfiguration : new VersioningConfiguration();
    }

    /**
     * Check if checkin should be skipped
     * @param request The http request
     * @return {@code true} if checkin should be skipped
     */
    protected boolean isSkipCheckin(SlingHttpServletRequest request) {
        return !getVersioningConfiguration(request).isAutoCheckin();
    }

    /**
     * Check whether changes should be written back
     * @param request The http request
     * @return {@code true} If committing be skipped
     */
    private boolean isSkipSessionHandling(SlingHttpServletRequest request) {
        return Boolean.parseBoolean((String) request.getAttribute(SlingPostConstants.ATTR_SKIP_SESSION_HANDLING)) == true;
    }

    /**
     * Check whether commit to the resource resolver should be called.
     * @param request The http request
     * @return {@code true} if a commit is required.
     */
    private boolean isResourceResolverCommitRequired(SlingHttpServletRequest request) {
        return !isSkipSessionHandling(request) && request.getResourceResolver().hasChanges();
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
            final SlingHttpServletRequest request) {

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
     * @param request The http request
     * @param path the path to externalize
     * @return the url
     */
    protected final String externalizePath(final SlingHttpServletRequest request,
            final String path) {
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
     * Returns the path of the resource of the request as the resource path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     * @param request The http request
     * @return The resource path
     */
    protected String getResourcePath(SlingHttpServletRequest request) {
        return request.getResource().getPath();
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

        @Override
        public boolean hasNext() {
            return nextResource != null;
        }

        @Override
        public Resource next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Resource result = nextResource;
            nextResource = seek();

            return result;
        }

        @Override
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