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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.ReferenceParser;
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
    throws RepositoryException {

        try {
            final Map<String, RequestProperty> reqProperties = collectContent(request, response);

            final VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

            String chunkNumberStr = request.getParameter(SlingPostConstants.CHUNK_NUMBER);
            String chunkUploadId = request.getParameter(SlingPostConstants.CHUNK_UPLOADID);
            boolean islastChunk = Boolean.parseBoolean(request.getParameter(SlingPostConstants.LAST_CHUNK));
            Integer chunkNumber = null;
            if (chunkNumberStr != null) {
                chunkNumber = Integer.parseInt(chunkNumberStr);
                storeChunk(request.getResourceResolver(), chunkUploadId, chunkNumber, islastChunk, reqProperties, response);
                if (!islastChunk) {
                    // do not process further unless it is last chunk
                    return;
                }
            }

            // do not change order unless you have a very good reason.

            // ensure root of new content
            processCreate(request.getResourceResolver(), reqProperties, response, changes, versioningConfiguration);

            // write content from existing content (@Move/CopyFrom parameters)
            processMoves(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);
            processCopies(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);

            // cleanup any old content (@Delete parameters)
            processDeletes(request.getResourceResolver(), reqProperties, changes, versioningConfiguration);

            // write content from form
            writeContent(request.getResourceResolver(), reqProperties, chunkUploadId, chunkNumber, changes, versioningConfiguration);

            // order content
            final Resource newResource = request.getResourceResolver().getResource(response.getPath());
            final Node newNode = newResource.adaptTo(Node.class);
            if ( newNode != null ) {
                orderNode(request, newNode, changes);
            }
        } catch ( final PersistenceException pe) {
            if ( pe.getCause() instanceof RepositoryException ) {
                throw (RepositoryException)pe.getCause();
            }
            throw new RepositoryException(pe);
        }
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
            } catch (RepositoryException re) {
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
            throws RepositoryException, PersistenceException {

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
            throws RepositoryException, PersistenceException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.hasRepositoryCopySource()) {
                processMovesCopiesInternal(property, false, resolver,
                    reqProperties, changes, versioningConfiguration);
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
    private void processMovesCopiesInternal(
                    RequestProperty property,
            boolean isMove, final ResourceResolver resolver,
            Map<String, RequestProperty> reqProperties, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws RepositoryException, PersistenceException {

        final Session session = resolver.adaptTo(Session.class);
        String propPath = property.getPath();
        String source = property.getRepositorySource();

        // only continue here, if the source really exists
        if (session.itemExists(source)) {

            // if the destination item already exists, remove it
            // first, otherwise ensure the parent location
            if (session.itemExists(propPath)) {
                Node parent = session.getItem(propPath).getParent();
                checkoutIfNecessary(parent, changes, versioningConfiguration);

                session.getItem(propPath).remove();
                changes.add(Modification.onDeleted(propPath));
            } else {
                Resource parent = deepGetOrCreateNode(resolver, property.getParentPath(),
                    reqProperties, changes, versioningConfiguration);
                final Node node = parent.adaptTo(Node.class);
                if ( node != null ) {
                    checkoutIfNecessary(node, changes, versioningConfiguration);
                }
            }

            // move through the session and record operation
            Item sourceItem = session.getItem(source);
            if (sourceItem.isNode()) {

                // node move/copy through session
                if (isMove) {
                    checkoutIfNecessary(sourceItem.getParent(), changes, versioningConfiguration);
                    session.move(source, propPath);
                } else {
                    Node sourceNode = (Node) sourceItem;
                    Node destParent = (Node) session.getItem(property.getParentPath());
                    checkoutIfNecessary(destParent, changes, versioningConfiguration);
                    CopyOperation.copy(sourceNode, destParent,
                        property.getName());
                }

            } else {

                // property move manually
                Property sourceProperty = (Property) sourceItem;

                // create destination property
                Node destParent = (Node) session.getItem(property.getParentPath());
                checkoutIfNecessary(destParent, changes, versioningConfiguration);
                CopyOperation.copy(sourceProperty, destParent, null);

                // remove source property (if not just copying)
                if (isMove) {
                    checkoutIfNecessary(sourceProperty.getParent(), changes, versioningConfiguration);
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
     * the resource.
     *
     * @param resolver The <code>ResourceResolver</code> used to access the
     *            resources to delete the properties.
     * @param reqProperties The map of request properties to check for
     *            properties to be removed.
     * @param response The <code>HtmlResponse</code> to be updated with
     *            information on deleted properties.
     * @throws RepositoryException Is thrown if an error occurrs checking or
     *             removing properties.
     */
    private void processDeletes(final ResourceResolver resolver,
            final Map<String, RequestProperty> reqProperties,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws RepositoryException, PersistenceException {

        for (final RequestProperty property : reqProperties.values()) {

            if (property.isDelete()) {
                final Resource parent = resolver.getResource(property.getParentPath());
                if ( parent == null ) {
                    continue;
                }
                final Node parentNode = parent.adaptTo(Node.class);

                if ( parentNode != null ) {
                    checkoutIfNecessary(parentNode, changes, versioningConfiguration);

                    if (property.getName().equals("jcr:mixinTypes")) {

                        // clear all mixins
                        for (NodeType mixin : parentNode.getMixinNodeTypes()) {
                            parentNode.removeMixin(mixin.getName());
                        }

                    } else {
                        if ( parentNode.hasProperty(property.getName())) {
                            parentNode.getProperty(property.getName()).remove();
                        } else if ( parentNode.hasNode(property.getName())) {
                            parentNode.getNode(property.getName()).remove();
                        }
                    }

                } else {
                    final ValueMap vm = parent.adaptTo(ModifiableValueMap.class);
                    if ( vm == null ) {
                        throw new PersistenceException("Resource '" + parent.getPath() + "' is not modifiable.");
                    }
                    vm.remove(property.getName());
                }

                changes.add(Modification.onDeleted(property.getPath()));
            }
        }

    }

    /**
     * Writes back the content
     *
     * @throws RepositoryException if a repository error occurs
     * @throws PersistenceException if a persistence error occurs
     */
    private void writeContent(final ResourceResolver resolver,
            final Map<String, RequestProperty> reqProperties,
            final String chunkUploadId, final Integer lastChunkNumber,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws RepositoryException, PersistenceException {

        final SlingPropertyValueHandler propHandler = new SlingPropertyValueHandler(
            dateParser, new ReferenceParser(resolver.adaptTo(Session.class)), changes);

        for (final RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                final Resource parent = deepGetOrCreateNode(resolver,
                    prop.getParentPath(), reqProperties, changes, versioningConfiguration);

                final Node parentNode = parent.adaptTo(Node.class);
                if ( parentNode != null ) {
                    checkoutIfNecessary(parentNode, changes, versioningConfiguration);
                }

                // skip jcr special properties
                if (prop.getName().equals("jcr:primaryType")
                    || prop.getName().equals("jcr:mixinTypes")) {
                    continue;
                }

                if (prop.isFileUpload()) {
                    if (chunkUploadId != null) {
                        InputStream ins = null;
                        File file = null;
                        try {
                            Resource uploadIdRes = resolver.getResource(chunkUploadId);
                            file = mergeChunks(uploadIdRes.adaptTo(Node.class), lastChunkNumber);
                            ins = new FileInputStream(file);
                            uploadHandler.setFile(parent, prop, ins, changes);
                            Node uploadIdNode = uploadIdRes.adaptTo(Node.class);
                            uploadIdNode.remove();
                        } catch (IOException e) {
                            throw new PersistenceException("fail to process file chunks", e);
                        } finally {
                            if (ins != null) {
                                try {
                                    ins.close();
                                } catch (IOException ignore) {
                                }
                            }
                            if (file != null) {
                                file.delete();
                            }
                        }

                    } else {
                        uploadHandler.setFile(parent, prop, changes);
                    }
                } else {
                    propHandler.setProperty(parent, prop);
                }
            }
        }
    }

    /**
     * Store chunks in repository. All chunks would be merge to create node structure in jcr.
     */
    private void storeChunk(ResourceResolver resourceResolver, String chunkUploadId, Integer chunkNumber, boolean islastChunk,
            Map<String, RequestProperty> reqProperties, PostResponse response) throws PersistenceException, RepositoryException {
        RequestParameter fileParam = null;
        try {

            if (chunkNumber == null || (chunkNumber == 1 && islastChunk)) {
                // chunkNumber is null.
                // upload contains only one chunk. let it be created directly
                return;
            }
            Resource chunkUploadRes = null;
            if (1 == chunkNumber) {
                Resource parentResource = resourceResolver.getResource(SlingPostConstants.CHUNK_UPLOAD_ROOT);
                Map<String, Object> props = new HashMap<String, Object>(5);
                props.put("jcr:primaryType", JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
                if (parentResource == null) {
                    String[] tokens = SlingPostConstants.CHUNK_UPLOAD_ROOT.substring(1).split("/");
                    parentResource = resourceResolver.getResource("/");
                    for (String token : tokens) {
                        Resource tokResource = resourceResolver.getResource(parentResource, token);
                        if (tokResource == null) {
                            tokResource = resourceResolver.create(parentResource, token, props);
                        }
                        parentResource = tokResource;
                    }
                }
                String uploadId = generateUploadId();
                props = new HashMap<String, Object>(5);
                props.put("jcr:primaryType", JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
                props.put(SlingPostConstants.BYTES_UPLOADED, 0L);
                props.put(SlingPostConstants.CHUNKS_UPLOADED, 0L);
                chunkUploadRes = resourceResolver.create(parentResource, uploadId, props);
                // TODO acl to uploadRes so that other users don't see it.
            } else {
                chunkUploadRes = resourceResolver.getResource(chunkUploadId);
            }
            Resource chunkNumRes = chunkUploadRes.getChild(Integer.toString(chunkNumber));
            if (chunkNumRes != null) {
                resourceResolver.delete(chunkNumRes);
            }
            Map<String, Object> props = new HashMap<String, Object>(5);
            fileParam = getFileReqParameter(reqProperties);
            props.put(JcrConstants.JCR_DATA, fileParam.getInputStream());
            props.put(SlingPostConstants.SIZE, fileParam.getSize());
            props.put("jcr:primaryType", NodeType.NT_UNSTRUCTURED);
            chunkNumRes = resourceResolver.create(chunkUploadRes, Integer.toString(chunkNumber), props);
            updateChunkUploadMetadata(chunkUploadRes.adaptTo(Node.class), chunkNumber, fileParam.getSize());
            if (!islastChunk) {
                response.setParentLocation(chunkUploadRes.getParent().getPath());
                response.setLocation(chunkUploadRes.getPath());
                response.setPath(chunkUploadRes.getParent().getPath());
            }
        } catch (IOException ioe) {
            if (ioe instanceof PersistenceException) {
                throw (PersistenceException) ioe;
            } else {
                throw new PersistenceException("failed to read file parameter: " + fileParam.getFileName(), ioe);
            }
        }

    }

    /**
     * Update chunk node with continuous bytes uploaded size and chunk number.
     */

    private void updateChunkUploadMetadata(Node chunkUploadNode, Integer chunkNumber, Long chunkSize) throws RepositoryException {
        long prevChunkNumber = chunkUploadNode.getProperty(SlingPostConstants.CHUNKS_UPLOADED).getLong();
        if (prevChunkNumber + 1 == chunkNumber) {
            long size = chunkUploadNode.getProperty(SlingPostConstants.BYTES_UPLOADED).getLong();
            chunkUploadNode.setProperty(SlingPostConstants.BYTES_UPLOADED, size + chunkSize);
            chunkUploadNode.setProperty(SlingPostConstants.CHUNKS_UPLOADED, chunkNumber);
        } else {
            int count = 1;
            Long size = new Long(0);
            while (chunkUploadNode.hasNode(Integer.toString(count))) {
                Node chunkNumNode = chunkUploadNode.getNode(Integer.toString(count));
                size += chunkNumNode.getProperty(SlingPostConstants.SIZE).getLong();
                count++;
            }
            chunkUploadNode.setProperty(SlingPostConstants.BYTES_UPLOADED, size);
            chunkUploadNode.setProperty(SlingPostConstants.CHUNKS_UPLOADED, --count);
        }
    }

    /**
     * Generate an unique uploadId.
     */
    private String generateUploadId() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid;
    }

    /**
     * Return file parameter from request properties.
     */

    private RequestParameter getFileReqParameter(final Map<String, RequestProperty> reqProperties) {
        RequestParameter result = null;
        for (final RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                if (!prop.isFileUpload()) {
                    continue;
                }

                for (final RequestParameter value : prop.getValues()) {

                    // ignore if a plain form field or empty
                    if (value.isFormField() || value.getSize() <= 0) {
                        continue;
                    }
                    result = value;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Merge chunks from one to lastChunkNumber into a tmp file.
     *
     * @param chunkUploadNode parent node where all chunks are stored.
     * @param lastChunkNumber
     * @return merged file
     * @throws PersistenceException
     * @throws RepositoryException
     */
    private File mergeChunks(final Node chunkUploadNode, final Integer lastChunkNumber) throws PersistenceException, RepositoryException {
        OutputStream out = null;
        File file = null;
        try {
            file = File.createTempFile("tmp", "mergechunk");
            out = new BufferedOutputStream(new FileOutputStream(file), 16 * 1024);
            int count = 1;
            while (count <= lastChunkNumber) {
                Node chunkNode = chunkUploadNode.getNode(Integer.toString(count));
                InputStream ins = null;
                int i = 0;
                try {
                    InputStream in = chunkNode.getProperty(javax.jcr.Property.JCR_DATA).getBinary().getStream();
                    ins = new BufferedInputStream(in, 16 * 1024);
                    byte[] buf = new byte[16 * 1024];
                    while ((i = in.read(buf)) != -1) {
                        out.write(buf, 0, i);
                        out.flush();
                    }

                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException ignore) {

                        }
                    }
                }
                count++;
            }
        } catch (IOException e) {
            throw new PersistenceException("excepiton occured", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {

                }
            }

        }
        return file;
    }
}
