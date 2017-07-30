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
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.servlets.post.impl.helper.SlingFileUploadHandler;
import org.apache.sling.servlets.post.impl.helper.SlingPropertyValueHandler;

/**
 * The <code>ModifyOperation</code> class implements the default operation
 * called by the Sling default POST servlet if no operation is requested by the
 * client. This operation is able to create and/or modify content.
 */
public class ModifyOperation extends AbstractCreateOperation {

    private DateParser dateParser;

    /**
     * handler that deals with file upload
     */
    private final SlingFileUploadHandler uploadHandler;

    public ModifyOperation() {
        this.dateParser = new DateParser();
        this.uploadHandler = new SlingFileUploadHandler();
    }

    public void setServletContext(final ServletContext servletContext) {
        this.uploadHandler.setServletContext(servletContext);
    }

    public void setDateParser(final DateParser dateParser) {
        this.dateParser = dateParser;
    }

    @Override
    protected void doRun(final SlingHttpServletRequest request,
                    final PostResponse response,
                    final List<Modification> changes)
    throws PersistenceException {
        final Map<String, RequestProperty> reqProperties = collectContent(request, response);

        final VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

        // do not change order unless you have a very good reason.

        // ensure root of new content
        processCreate(request.getResourceResolver(), reqProperties, response, changes, versioningConfiguration);

        // write content from existing content (@Move/CopyFrom parameters)
        processMoves(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);
        processCopies(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);

        // cleanup any old content (@Delete parameters)
        processDeletes(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);

        // write content from form
        writeContent(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);

        // order content
        final Resource newResource = request.getResourceResolver().getResource(response.getPath());
        this.jcrSsupport.orderNode(request, newResource, changes);
    }

    @Override
    protected String getResourcePath(SlingHttpServletRequest request) {

        // calculate the paths
        StringBuilder rootPathBuf = new StringBuilder();
        String suffix;
        Resource currentResource = request.getResource();
        if (ResourceUtil.isSyntheticResource(currentResource)) {

            // no resource, treat the missing resource path as suffix
            suffix = currentResource.getPath();

        } else {

            // resource for part of the path, use request suffix
            suffix = request.getRequestPathInfo().getSuffix();

            if (suffix != null) {
                // cut off any selectors/extension from the suffix
                int dotPos = suffix.indexOf('.');
                if (dotPos > 0) {
                    suffix = suffix.substring(0, dotPos);
                }
            }

            // and preset the path buffer with the resource path
            rootPathBuf.append(currentResource.getPath());

        }

        // check for extensions or create suffix in the suffix
        boolean doGenerateName = false;
        if (suffix != null) {

            // check whether it is a create request (trailing /)
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
            } catch (PersistenceException re) {
                throw new SlingException("Failed to generate name", re);
            }
        }

        return path;
    }

    /**
     * Moves all repository content listed as repository move source in the
     * request properties to the locations indicated by the resource properties.
     * @param checkedOutNodes
     */
    private void processMoves(final ResourceResolver resolver,
            Map<String, RequestProperty> reqProperties, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws PersistenceException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.hasRepositoryMoveSource()) {
                processMovesCopiesInternal(property, true, resolver,
                    reqProperties, changes, versioningConfiguration);
            }
        }
    }

    /**
     * Copies all repository content listed as repository copy source in the
     * request properties to the locations indicated by the resource properties.
     * @param checkedOutNodes
     */
    private void processCopies(final ResourceResolver resolver,
            Map<String, RequestProperty> reqProperties, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws PersistenceException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.hasRepositoryCopySource()) {
                processMovesCopiesInternal(property, false, resolver,
                    reqProperties, changes, versioningConfiguration);
            }
        }
    }

    /**
     * Internal implementation of the
     * {@link #processCopies(ResourceResolver, Map, HtmlResponse)} and
     * {@link #processMoves(ResourceResolver, Map, HtmlResponse)} methods taking into
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
     * @param resolver The resource resolver to use to access the content
     * @param reqProperties All accepted request properties. This is used to
     *            create intermediary nodes along the property path.
     * @param response The <code>HtmlResponse</code> into which successful
     *            copies and moves as well as intermediary node creations are
     *            recorded.
     * @throws PersistenceException May be thrown if an error occurs.
     */
    private void processMovesCopiesInternal(
                    RequestProperty property,
            boolean isMove, final ResourceResolver resolver,
            Map<String, RequestProperty> reqProperties, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws PersistenceException {

        String propPath = property.getPath();
        String source = property.getRepositorySource();

        // only continue here, if the source really exists
        if (resolver.getResource(source) != null ) {

            // if the destination item already exists, remove it
            // first, otherwise ensure the parent location
            if (resolver.getResource(propPath) != null) {
                final Resource parent = resolver.getResource(propPath).getParent();
                this.jcrSsupport.checkoutIfNecessary(parent, changes, versioningConfiguration);

                resolver.delete(resolver.getResource(propPath));
                changes.add(Modification.onDeleted(propPath));
            } else {
                Resource parent = deepGetOrCreateResource(resolver, property.getParentPath(),
                    reqProperties, changes, versioningConfiguration);
                this.jcrSsupport.checkoutIfNecessary(parent, changes, versioningConfiguration);
            }

            // move through the session and record operation
            // check if the item is backed by JCR
            Resource sourceRsrc = resolver.getResource(source);
            final Object sourceItem = this.jcrSsupport.getItem(sourceRsrc);
            final Object destItem = this.jcrSsupport.getItem(resolver.getResource(property.getParentPath()));
            if ( sourceItem != null && destItem != null ) {
                if ( this.jcrSsupport.isNode(sourceRsrc) ) {
                    if ( isMove ) {
                        this.jcrSsupport.checkoutIfNecessary(sourceRsrc.getParent(), changes, versioningConfiguration);
                        this.jcrSsupport.move(sourceItem, destItem, ResourceUtil.getName(propPath));
                    } else {
                        this.jcrSsupport.checkoutIfNecessary(resolver.getResource(property.getParentPath()), changes, versioningConfiguration);
                        this.jcrSsupport.copy(sourceItem, destItem, property.getName());
                    }
                } else {
                    // property: move manually
                    this.jcrSsupport.checkoutIfNecessary(resolver.getResource(property.getParentPath()), changes, versioningConfiguration);
                    // create destination property
                    this.jcrSsupport.copy(sourceItem, destItem, ResourceUtil.getName(source));

                    // remove source property (if not just copying)
                    if ( isMove ) {
                        this.jcrSsupport.checkoutIfNecessary(sourceRsrc.getParent(), changes, versioningConfiguration);
                        resolver.delete(sourceRsrc);
                    }
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
     * the resource.
     *
     * @param resolver The <code>ResourceResolver</code> used to access the
     *            resources to delete the properties.
     * @param reqProperties The map of request properties to check for
     *            properties to be removed.
     * @param response The <code>HtmlResponse</code> to be updated with
     *            information on deleted properties.
     * @throws PersistenceException Is thrown if an error occurs checking or
     *             removing properties.
     */
    private void processDeletes(final ResourceResolver resolver,
            final Map<String, RequestProperty> reqProperties,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {

        for (final RequestProperty property : reqProperties.values()) {

            if (property.isDelete()) {
                final Resource parent = resolver.getResource(property.getParentPath());
                if ( parent == null ) {
                    continue;
                }
                this.jcrSsupport.checkoutIfNecessary(parent, changes, versioningConfiguration);

                final ValueMap vm = parent.adaptTo(ModifiableValueMap.class);
                if ( vm == null ) {
                    throw new PersistenceException("Resource '" + parent.getPath() + "' is not modifiable.");
                }
                if ( vm.containsKey(property.getName()) ) {
                    if ( JcrConstants.JCR_MIXINTYPES.equals(property.getName()) ) {
                        vm.put(JcrConstants.JCR_MIXINTYPES, new String[0]);
                    } else {
                        vm.remove(property.getName());
                    }
                } else {
                    final Resource childRsrc = resolver.getResource(parent.getPath() + '/' + property.getName());
                    if ( childRsrc != null ) {
                        resolver.delete(childRsrc);
                    }
                }

                changes.add(Modification.onDeleted(property.getPath()));
            }
        }

    }

    /**
     * Writes back the content
     *
     * @throws PersistenceException if a persistence error occurs
     */
    private void writeContent(final ResourceResolver resolver,
            final Map<String, RequestProperty> reqProperties,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {

        final SlingPropertyValueHandler propHandler = new SlingPropertyValueHandler(
            dateParser, this.jcrSsupport, changes);

        for (final RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                final Resource parent = deepGetOrCreateResource(resolver,
                    prop.getParentPath(), reqProperties, changes, versioningConfiguration);

                this.jcrSsupport.checkoutIfNecessary(parent, changes, versioningConfiguration);

                // skip jcr special properties
                if (prop.getName().equals(JcrConstants.JCR_PRIMARYTYPE)
                    || prop.getName().equals(JcrConstants.JCR_MIXINTYPES)) {
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
}
