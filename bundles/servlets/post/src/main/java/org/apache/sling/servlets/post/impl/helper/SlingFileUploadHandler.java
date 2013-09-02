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
package org.apache.sling.servlets.post.impl.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;

/**
 * Handles file uploads.
 * <p/>
 *
 * Simple example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./portrait" />
 *   </form>
 * </xmp>
 *
 * this will create a nt:file node below "/home/admin" if the node type of
 * "admin" is (derived from) nt:folder, a nt:resource node otherwise.
 * <p/>
 *
 * Filename example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./*" />
 *   </form>
 * </xmp>
 *
 * same as above, but uses the filename of the uploaded file as name for the
 * new node.
 * <p/>
 *
 * Type hint example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./portrait" />
 *     <input type="hidden" name="./portrait@TypeHint" value="my:file" />
 *   </form>
 * </xmp>
 *
 * this will create a new node with the type my:file below admin. if the hinted
 * type extends from nt:file an intermediate file node is created otherwise
 * directly a resource node.
 */
public class SlingFileUploadHandler {

    // nodetype name string constants
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";
    public static final String NT_UNSTRUCTURED = "nt:unstructured";

    // item name string constants
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_DATA = "jcr:data";

    private static final String CHUNK_NODE_NAME = "chunk";

    /**
     * The servlet context.
     */
    private ServletContext servletContext;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Uses the file(s) in the request parameter for creation of new nodes.
     * if the parent node is a nt:folder a new nt:file is created. otherwise
     * just a nt:resource. if the <code>name</code> is '*', the filename of
     * the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    private void setFile(final Resource parentResource, final Node parent, final RequestProperty prop, RequestParameter value, final List<Modification> changes, String name, final String contentType)
            throws RepositoryException, PersistenceException {
        // check type hint. if the type is ok and extends from nt:file,
        // create an nt:file with that type. if it's invalid, drop it and let
        // the parent node type decide.
        boolean createNtFile = parent.isNodeType(NT_FOLDER);
        String typeHint = prop.getTypeHint();
        if (typeHint != null) {
            try {
                NodeTypeManager ntMgr = parent.getSession().getWorkspace().getNodeTypeManager();
                NodeType nt = ntMgr.getNodeType(typeHint);
                createNtFile = nt.isNodeType(NT_FILE);
            } catch (RepositoryException e) {
                // assuming type not valid.
                typeHint = null;
            }
        }

        // also create an nt:file if the name contains an extension
        // the rationale is that if the file name is "important" we want
        // an nt:file, and an image name with an extension is probably "important"
        if(!createNtFile && name.indexOf('.') > 0) {
            createNtFile = true;
        }

        // set empty type
        if (typeHint == null) {
            typeHint = createNtFile ? NT_FILE : NT_RESOURCE;
        }

        // create nt:file node if needed
        Resource resParent;
        if (createNtFile) {
            // create nt:file
            resParent = getOrCreateChildResource(parentResource, name, typeHint, changes);
            name = JCR_CONTENT;
            typeHint = NT_RESOURCE;
        } else {
            resParent = parentResource;
        }

        // create resource node
        Resource newResource = getOrCreateChildResource(resParent, name, typeHint, changes);
        Node res = newResource.adaptTo(Node.class);

        // set properties
        changes.add(Modification.onModified(
                res.setProperty(JCR_LASTMODIFIED, Calendar.getInstance()).getPath()
                ));
        changes.add(Modification.onModified(
                res.setProperty(JCR_MIMETYPE, contentType).getPath()
                ));
        try {
            // process chunk upload request separately
            if (prop.isChunkUpload()) {
                processChunk(resParent, res, prop, value, changes);
            } else {
                changes.add(Modification.onModified(res.setProperty(JCR_DATA,
                        value.getInputStream()).getPath()));
            }
        } catch (IOException e) {
            throw new RepositoryException("Error while retrieving inputstream from parameter value.", e);
        }
    }

    /**
     * Uses the file(s) in the request parameter for creation of new nodes.
     * if the parent node is a nt:folder a new nt:file is created. otherwise
     * just a nt:resource. if the <code>name</code> is '*', the filename of
     * the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    private void setFile(final Resource parentResource, final RequestProperty prop, final RequestParameter value, final List<Modification> changes, String name, final String contentType)
            throws PersistenceException, RepositoryException {
        String typeHint = prop.getTypeHint();
        if ( typeHint == null ) {
            typeHint = NT_FILE;
        }
        if(prop.isChunkUpload()){
            // cannot process chunk upload if parent node doesn't
            // exists. throw exception
            throw new RepositoryException(
                    "Cannot process chunk upload request. Parent resource ["
                            + parentResource.getPath() + "] doesn't exists");
        }
        // create properties
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling:resourceType", typeHint);
        props.put(JCR_LASTMODIFIED, Calendar.getInstance());
        props.put(JCR_MIMETYPE, contentType);
        try {
             props.put(JCR_DATA, value.getInputStream());
        } catch (final IOException e) {
             throw new PersistenceException("Error while retrieving inputstream from parameter value.", e);
        }

        // get or create resource
        Resource result = parentResource.getChild(name);
        if ( result != null ) {
            final ModifiableValueMap vm = result.adaptTo(ModifiableValueMap.class);
            if ( vm == null ) {
                throw new PersistenceException("Resource at " + parentResource.getPath() + '/' + name + " is not modifiable.");
            }
            vm.putAll(props);
        } else {
            result = parentResource.getResourceResolver().create(parentResource, name, props);
        }
        for(final String key : props.keySet()) {
            changes.add(Modification.onModified(result.getPath() + '/' + key));
        }
    }
    /**
     * Process chunk upload. For first and intermediate chunks request persists
     * chunks at jcr:content/chunk_start_end/jcr:data or
     * nt:resource/chunk_start_end/jcr:data. For last last chunk,
     * merge all previous chunks and current chunk and replace binary at
     * destination.
     */
    private void processChunk(final Resource resParent, final Node res,
            final RequestProperty prop, RequestParameter value,
            final List<Modification> changes) throws RepositoryException {
        try {
            long chunkOffset = prop.getOffset();
            if (chunkOffset == 0) {
                // first chunk
                // check if another chunk upload is already in progress. throw
                // exception
                NodeIterator itr = res.getNodes(CHUNK_NODE_NAME + "*");
                if (itr.hasNext()) {
                    throw new RepositoryException(
                        "Chunk upload already in progress at {" + res.getPath()
                            + "}");
                }
                res.addMixin(JcrResourceConstants.NT_SLING_CHUNK_MIXIN);
                changes.add(Modification.onModified(res.setProperty(
                    JcrResourceConstants.NT_SLING_CHUNKS_LENGTH, 0).getPath()));
                if (!res.hasProperty(JCR_DATA)) {
                    // create a empty jcr:data property
                    res.setProperty(JCR_DATA,
                        new ByteArrayInputStream("".getBytes()));
                }
            }
            if (!res.hasProperty(JcrResourceConstants.NT_SLING_CHUNKS_LENGTH)) {
                throw new RepositoryException("no chunk upload found at {"
                    + res.getPath() + "}");
            }
            long currentLength = res.getProperty(
                JcrResourceConstants.NT_SLING_CHUNKS_LENGTH).getLong();
            long totalLength = prop.getLength();
            if (chunkOffset != currentLength) {
                throw new RepositoryException("Chunk's offset {"
                    + chunkOffset
                    + "} doesn't match expected offset {"
                    + res.getProperty(
                        JcrResourceConstants.NT_SLING_CHUNKS_LENGTH).getLong()
                    + "}");
            }
            if (totalLength != 0) {
                if (res.hasProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH)) {
                    long expectedLength = res.getProperty(
                        JcrResourceConstants.NT_SLING_FILE_LENGTH).getLong();
                    if (totalLength != expectedLength) {
                        throw new RepositoryException("File length {"
                            + totalLength + "} doesn't match expected length {"
                            + expectedLength + "}");
                    }
                } else {
                    res.setProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH,
                        totalLength);
                }
            }
            NodeIterator itr = res.getNodes(CHUNK_NODE_NAME + "_"
                + String.valueOf(chunkOffset) + "*");
            if (itr.hasNext()) {
                throw new RepositoryException("Chunk already present at {"
                    + itr.nextNode().getPath() + "}");
            }
            String nodeName = CHUNK_NODE_NAME + "_"
                + String.valueOf(chunkOffset) + "_"
                + String.valueOf(chunkOffset + value.getSize() - 1);
            if (totalLength == (currentLength + value.getSize())
                || prop.isCompleted()) {
                File file = null;
                InputStream fileIns = null;
                try {
                    file = mergeChunks(res, value.getInputStream());
                    fileIns = new FileInputStream(file);
                    changes.add(Modification.onModified(res.setProperty(
                        JCR_DATA, fileIns).getPath()));
                    NodeIterator nodeItr = res.getNodes(CHUNK_NODE_NAME + "*");
                    while (nodeItr.hasNext()) {
                        Node nodeRange = nodeItr.nextNode();
                        changes.add(Modification.onDeleted(nodeRange.getPath()));
                        nodeRange.remove();
                    }
                    if (res.hasProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH)) {
                        javax.jcr.Property expLenProp = res.getProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH);
                        changes.add(Modification.onDeleted(expLenProp.getPath()));
                        expLenProp.remove();
                    }
                    if (res.hasProperty(JcrResourceConstants.NT_SLING_CHUNKS_LENGTH)) {
                        javax.jcr.Property currLenProp = res.getProperty(JcrResourceConstants.NT_SLING_CHUNKS_LENGTH);
                        changes.add(Modification.onDeleted(currLenProp.getPath()));
                        currLenProp.remove();
                    }
                    res.removeMixin(JcrResourceConstants.NT_SLING_CHUNK_MIXIN);
                } finally {
                    try {
                        fileIns.close();
                        file.delete();
                    } catch (IOException ign) {

                    }

                }
            } else {
                Node rangeNode = res.addNode(nodeName,
                    JcrResourceConstants.NT_SLING_CHUNK_NODETYPE);
                changes.add(Modification.onCreated(rangeNode.getPath()));
                changes.add(Modification.onModified(rangeNode.setProperty(
                    JCR_DATA, value.getInputStream()).getPath()));
                changes.add(Modification.onModified(rangeNode.setProperty(
                    JcrResourceConstants.NT_SLING_CHUNK_OFFSET, chunkOffset).getPath()));
                changes.add(Modification.onModified(res.setProperty(
                    JcrResourceConstants.NT_SLING_CHUNKS_LENGTH,
                    currentLength + value.getSize()).getPath()));
            }
        } catch (IOException e) {
            throw new RepositoryException(
                "Error while retrieving inputstream from parameter value.", e);
        }
    }

    /**
     * Merge all previous chunks with last chunk's stream into a temporary file
     * and return it.
     */
    private File mergeChunks(final Node parentNode,
            final InputStream lastChunkStream) throws PersistenceException,
            RepositoryException {
        OutputStream out = null;
        File file = null;
        try {
            file = File.createTempFile("tmp-", "-mergechunk");
            out = new BufferedOutputStream(new FileOutputStream(file),
                16 * 1024);
            String startPattern = CHUNK_NODE_NAME + "_" + "0_*";
            NodeIterator nodeItr = parentNode.getNodes(startPattern);
            InputStream ins = null;
            int i = 0;
            while (nodeItr.hasNext()) {
                if (nodeItr.getSize() > 1) {
                    throw new RepositoryException(
                        "more than one node found for pattern: " + startPattern);
                }
                Node rangeNode = nodeItr.nextNode();

                try {
                    InputStream in = rangeNode.getProperty(
                        javax.jcr.Property.JCR_DATA).getBinary().getStream();
                    ins = new BufferedInputStream(in, 16 * 1024);
                    byte[] buf = new byte[16 * 1024];
                    while ((i = ins.read(buf)) != -1) {
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
                String[] indexBounds = rangeNode.getName().substring(
                    (CHUNK_NODE_NAME + "_").length()).split("_");
                startPattern = CHUNK_NODE_NAME + "_"
                    + String.valueOf(Long.valueOf(indexBounds[1]) + 1) + "_*";
                nodeItr = parentNode.getNodes(startPattern);
            }

            ins = new BufferedInputStream(lastChunkStream, 16 * 1024);
            byte[] buf = new byte[16 * 1024];
            while ((i = ins.read(buf)) != -1) {
                out.write(buf, 0, i);
                out.flush();
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

    /**
     * Delete all chunks saved within a node. If no chunks exist, it is no-op.
     */
    public void deleteChunks(final Node node) throws RepositoryException {
        Node chunkNode = null;
        Node jcrContentNode = null;
        if (hasChunks(node)) {
            chunkNode = node;
        } else if (node.hasNode(JCR_CONTENT)
            && hasChunks((jcrContentNode = node.getNode(JCR_CONTENT)))) {
            chunkNode = jcrContentNode;

        }
        if (chunkNode != null) {
            NodeIterator nodeItr = chunkNode.getNodes(CHUNK_NODE_NAME + "*");
            while (nodeItr.hasNext()) {
                Node rangeNode = nodeItr.nextNode();
                rangeNode.remove();
            }
            if (chunkNode.hasProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH)) {
                chunkNode.getProperty(JcrResourceConstants.NT_SLING_FILE_LENGTH).remove();
            }
            if (chunkNode.hasProperty(JcrResourceConstants.NT_SLING_CHUNKS_LENGTH)) {
                chunkNode.getProperty(
                    JcrResourceConstants.NT_SLING_CHUNKS_LENGTH).remove();
            }
            chunkNode.removeMixin(JcrResourceConstants.NT_SLING_CHUNK_MIXIN);
        }
    }

    /**
     * Return true if node has chunks stored in it, otherwise false.
     */
    private boolean hasChunks(final Node node) throws RepositoryException {
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            if (nodeType.getName().equals(
                JcrResourceConstants.NT_SLING_CHUNK_MIXIN)) {
                return true;
            }
        }
        return false;
    }

    private static final String MT_APP_OCTET = "application/octet-stream";

    /**
     * Uses the file(s) in the request parameter for creation of new nodes.
     * if the parent node is a nt:folder a new nt:file is created. otherwise
     * just a nt:resource. if the <code>name</code> is '*', the filename of
     * the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    public void setFile(final Resource parent, final RequestProperty prop, final List<Modification> changes)
            throws RepositoryException, PersistenceException {
        for (final RequestParameter value : prop.getValues()) {

            // ignore if a plain form field or empty
            if (value.isFormField() || value.getSize() <= 0) {
                continue;
            }

            // get node name
            String name = prop.getName();
            if (name.equals("*")) {
                name = value.getFileName();
                // strip of possible path (some browsers include the entire path)
                name = name.substring(name.lastIndexOf('/') + 1);
                name = name.substring(name.lastIndexOf('\\') + 1);
            }
            name = Text.escapeIllegalJcrChars(name);

            // get content type
            String contentType = value.getContentType();
            if (contentType != null) {
                int idx = contentType.indexOf(';');
                if (idx > 0) {
                    contentType = contentType.substring(0, idx);
                }
            }
            if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                // try to find a better content type
                ServletContext ctx = this.servletContext;
                if (ctx != null) {
                    contentType = ctx.getMimeType(value.getFileName());
                }
                if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                    contentType = MT_APP_OCTET;
                }
            }

            final Node node = parent.adaptTo(Node.class);
            if ( node == null ) {
                this.setFile(parent, prop, value, changes, name, contentType);
            } else {
                this.setFile(parent, node, prop, value, changes, name, contentType);
            }
        }

    }

    private Resource getOrCreateChildResource(final Resource parent, final String name,
            final String typeHint,
            final List<Modification> changes)
                    throws PersistenceException, RepositoryException {
        Resource result = parent.getChild(name);
        if ( result != null ) {
            final Node existing = result.adaptTo(Node.class);
            if ( existing != null && !existing.isNodeType(typeHint)) {
                existing.remove();
                result = createWithChanges(parent, name, typeHint, changes);
            }
        } else {
            result = createWithChanges(parent, name, typeHint, changes);
        }
        return result;
    }

    private Resource createWithChanges(final Resource parent, final String name,
            final String typeHint,
            final List<Modification> changes)
                    throws PersistenceException {
        Map<String, Object> properties = null;
        if ( typeHint != null ) {
            properties = new HashMap<String, Object>();
            if ( parent.adaptTo(Node.class) != null ) {
                properties.put("jcr:primaryType", typeHint);
            } else {
                properties.put("sling:resourceType", typeHint);
            }
        }
        final Resource result = parent.getResourceResolver().create(parent, name, properties);
        changes.add(Modification.onCreated(result.getPath()));
        return result;
    }

}