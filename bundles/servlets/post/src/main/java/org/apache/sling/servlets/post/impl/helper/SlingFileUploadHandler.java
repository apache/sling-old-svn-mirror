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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The servlet context.
     */
    private volatile ServletContext servletContext;

    private final JCRSupport jcrSupport = JCRSupport.INSTANCE;

    public void setServletContext(final ServletContext servletContext) {
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
     * @throws PersistenceException if an error occurs
     */
    private void setFile(final Resource parentResource,
            final RequestProperty prop,
            final RequestParameter value,
            final List<Modification> changes, String name,
            final String contentType)
    throws PersistenceException {
        // check type hint. if the type is ok and extends from nt:file,
        // create an nt:file with that type. if it's invalid, drop it and let
        // the parent node type decide.
        boolean createNtFile = parentResource.isResourceType(JcrConstants.NT_FOLDER) || this.jcrSupport.isNodeType(parentResource, JcrConstants.NT_FOLDER);
        String typeHint = prop.getTypeHint();
        if (typeHint != null) {
            Boolean isFileNodeType = this.jcrSupport.isFileNodeType(parentResource.getResourceResolver(), typeHint);
            if ( isFileNodeType == null ) {
                // assuming type not valid.
                createNtFile = false;
                typeHint = null;
            } else {
                createNtFile = isFileNodeType;
            }
        }

        // also create an nt:file if the name contains an extension
        // the rationale is that if the file name is "important" we want
        // an nt:file, and an image name with an extension is probably "important"
        if (!createNtFile && name.indexOf('.') > 0) {
            createNtFile = true;
        }

        // set empty type
        if (typeHint == null) {
            typeHint = createNtFile ? JcrConstants.NT_FILE : JcrConstants.NT_RESOURCE;
        }

        // create nt:file resource if needed
        Resource resParent;
        if (createNtFile) {
            // create nt:file
            resParent = getOrCreateChildResource(parentResource, name, typeHint, changes);
            name = JcrConstants.JCR_CONTENT;
            typeHint = JcrConstants.NT_RESOURCE;
        } else {
            resParent = parentResource;
        }

        // create resource
        final Resource newResource = getOrCreateChildResource(resParent, name, typeHint, changes);
        final ModifiableValueMap mvm = newResource.adaptTo(ModifiableValueMap.class);
        // set properties
        mvm.put(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
        mvm.put(JcrConstants.JCR_MIMETYPE, contentType);
        changes.add(Modification.onModified(newResource.getPath() + "/" + JcrConstants.JCR_LASTMODIFIED));
        changes.add(Modification.onModified(newResource.getPath() + "/" + JcrConstants.JCR_MIMETYPE));

        try {
            // process chunk upload request separately
            if (prop.isChunkUpload()) {
                processChunk(resParent, newResource, prop, value, changes);
            } else {
                mvm.put(JcrConstants.JCR_DATA, value.getInputStream());
                changes.add(Modification.onModified(newResource.getPath() + "/" + JcrConstants.JCR_DATA));
            }
        } catch (IOException e) {
            throw new PersistenceException("Error while retrieving inputstream from parameter value.", e);
        }
    }

    /**
     * Process chunk upload. For first and intermediate chunks request persists
     * chunks at jcr:content/chunk_start_end/jcr:data or
     * nt:resource/chunk_start_end/jcr:data. For last last chunk,
     * merge all previous chunks and last chunk and replace binary at
     * destination.
     */
    private void processChunk(final Resource resParent,
            final Resource res,
            final RequestProperty prop,
            final RequestParameter value,
            final List<Modification> changes)
    throws PersistenceException {
        try {
            final ModifiableValueMap mvm = res.adaptTo(ModifiableValueMap.class);
            long chunkOffset = prop.getChunk().getOffset();
            if (chunkOffset == 0) {
                // first chunk
                // check if another chunk upload is already in progress. throw
                // exception
                final Iterator<Resource> itr = new FilteringResourceIterator(res.listChildren(), SlingPostConstants.CHUNK_NODE_NAME);
                if (itr.hasNext()) {
                    throw new PersistenceException(
                        "Chunk upload already in progress at {" + res.getPath()
                            + "}");
                }
                addChunkMixin(mvm);
                mvm.put(SlingPostConstants.NT_SLING_CHUNKS_LENGTH, 0);
                changes.add(Modification.onModified(res.getPath() + "/" + SlingPostConstants.NT_SLING_CHUNKS_LENGTH));
                if (mvm.get(JcrConstants.JCR_DATA) == null ) {
                    // create a empty jcr:data property
                    mvm.put(JcrConstants.JCR_DATA,
                        new ByteArrayInputStream("".getBytes()));
                }
            }
            if (mvm.get(SlingPostConstants.NT_SLING_CHUNKS_LENGTH) == null) {
                throw new PersistenceException("no chunk upload found at {"
                    + res.getPath() + "}");
            }
            long currentLength = mvm.get(SlingPostConstants.NT_SLING_CHUNKS_LENGTH, Long.class);
            long totalLength = prop.getChunk().getLength();
            if (chunkOffset != currentLength) {
                throw new PersistenceException("Chunk's offset {"
                    + chunkOffset
                    + "} doesn't match expected offset {"
                    + currentLength
                    + "}");
            }
            if (totalLength != 0) {
                if (mvm.get(SlingPostConstants.NT_SLING_FILE_LENGTH) != null ) {
                    long expectedLength = mvm.get(
                        SlingPostConstants.NT_SLING_FILE_LENGTH, Long.class);
                    if (totalLength != expectedLength) {
                        throw new PersistenceException("File length {"
                            + totalLength + "} doesn't match expected length {"
                            + expectedLength + "}");
                    }
                } else {
                    mvm.put(SlingPostConstants.NT_SLING_FILE_LENGTH,
                        totalLength);
                }
            }
            final Iterator<Resource> itr = new FilteringResourceIterator(res.listChildren(), SlingPostConstants.CHUNK_NODE_NAME + "_" + String.valueOf(chunkOffset));
            if (itr.hasNext()) {
                throw new PersistenceException("Chunk already present at {"
                    + itr.next().getPath() + "}");
            }
            String nodeName = SlingPostConstants.CHUNK_NODE_NAME + "_"
                + String.valueOf(chunkOffset) + "_"
                + String.valueOf(chunkOffset + value.getSize() - 1);
            if (totalLength == (currentLength + value.getSize())
                || prop.getChunk().isCompleted()) {
                File file = null;
                InputStream fileIns = null;
                try {
                    file = mergeChunks(res, value.getInputStream());
                    fileIns = new FileInputStream(file);
                    mvm.put(JcrConstants.JCR_DATA, fileIns);
                    changes.add(Modification.onModified(res.getPath() + "/" + JcrConstants.JCR_DATA));
                    final Iterator<Resource> rsrcItr = new FilteringResourceIterator(res.listChildren(), SlingPostConstants.CHUNK_NODE_NAME);
                    while (rsrcItr.hasNext()) {
                        Resource rsrcRange = rsrcItr.next();
                        changes.add(Modification.onDeleted(rsrcRange.getPath()));
                        rsrcRange.getResourceResolver().delete(rsrcRange);
                    }
                    if (mvm.get(SlingPostConstants.NT_SLING_FILE_LENGTH) != null) {
                        changes.add(Modification.onDeleted(res.getPath() + "/" + SlingPostConstants.NT_SLING_FILE_LENGTH));
                        mvm.remove(SlingPostConstants.NT_SLING_FILE_LENGTH);
                    }
                    if (mvm.get(SlingPostConstants.NT_SLING_CHUNKS_LENGTH) != null) {
                        changes.add(Modification.onDeleted(res.getPath() + "/" + SlingPostConstants.NT_SLING_CHUNKS_LENGTH));
                        mvm.remove(SlingPostConstants.NT_SLING_CHUNKS_LENGTH);
                    }
                    removeChunkMixin(mvm);
                } finally {
                    try {
                        fileIns.close();
                        file.delete();
                    } catch (IOException ign) {

                    }

                }
            } else {
                final Map<String,Object> props = new HashMap<>();
                props.put(JcrConstants.JCR_DATA, value.getInputStream());
                props.put(SlingPostConstants.NT_SLING_CHUNK_OFFSET, chunkOffset);
                props.put(SlingPostConstants.NT_SLING_CHUNKS_LENGTH, currentLength + value.getSize());
                for(final String key : props.keySet()) {
                    changes.add(Modification.onModified(res.getPath() + "/" + nodeName + "/" + key));
                }
                props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE,
                        SlingPostConstants.NT_SLING_CHUNK_NODETYPE);
                final Resource rangeRsrc = res.getResourceResolver().create(res, nodeName, props);

                changes.add(Modification.onCreated(rangeRsrc.getPath()));
            }
        } catch (IOException e) {
            throw new PersistenceException(
                "Error while retrieving inputstream from parameter value.", e);
        }
    }

    private static final class FilteringResourceIterator implements Iterator<Resource>, Iterable<Resource> {

        private final String prefix;

        private final Iterator<Resource> iter;

        private Resource next;

        public FilteringResourceIterator(final Iterator<Resource> iter, final String prefix) {
            this.prefix = prefix;
            this.iter = iter;
            this.next = seek();
        }

        private Resource seek() {
            Resource result = null;
            while ( iter.hasNext() && result == null ) {
                final Resource c = iter.next();
                if ( c.getName().startsWith(prefix) ) {
                    result = c;
                }
            }
            return result;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Resource next() {
            final Resource result = next;
            next = seek();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();

        }

        @Override
        public Iterator<Resource> iterator() {
            return this;
        }
    }

    /**
     * Merge all previous chunks with last chunk's stream into a temporary file
     * and return it.
     */
    private File mergeChunks(final Resource parentResource,
            final InputStream lastChunkStream)
    throws PersistenceException {
        OutputStream out = null;
        SequenceInputStream  mergeStrm = null;
        File file = null;
        try {
            file = File.createTempFile("tmp-", "-mergechunk");
            out = new FileOutputStream(file);
            String startPattern = SlingPostConstants.CHUNK_NODE_NAME + "_" + "0_";
            Iterator<Resource> itr = new FilteringResourceIterator(parentResource.listChildren(), startPattern);
            final Set<InputStream> inpStrmSet = new LinkedHashSet<>();
            while (itr.hasNext()) {
                final Resource rangeResource = itr.next();
                if (itr.hasNext() ) {
                    throw new PersistenceException(
                        "more than one resource found for pattern: " + startPattern + "*");
                }

                inpStrmSet.add(rangeResource.adaptTo(InputStream.class));
                log.debug("added chunk {} to merge stream", rangeResource.getName());
                String[] indexBounds = rangeResource.getName().substring(
                    (SlingPostConstants.CHUNK_NODE_NAME + "_").length()).split(
                    "_");
                startPattern = SlingPostConstants.CHUNK_NODE_NAME + "_"
                    + String.valueOf(Long.valueOf(indexBounds[1]) + 1) + "_";
                itr = new FilteringResourceIterator(parentResource.listChildren(), startPattern);
            }

            inpStrmSet.add(lastChunkStream);
            mergeStrm = new SequenceInputStream(Collections.enumeration(inpStrmSet));
            IOUtils.copyLarge(mergeStrm, out);
        } catch (final IOException e) {
            throw new PersistenceException("Exception during chunk merge occured: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(mergeStrm);

        }
        return file;
    }

    private Resource getChunkParent(final Resource rsrc) {
        // parent resource containing all chunks and has mixin sling:chunks applied
        // on it.
        Resource chunkParent = null;
        Resource jcrContentNode = null;
        if (hasChunks(rsrc)) {
            chunkParent = rsrc;
        } else {
            jcrContentNode = rsrc.getChild(JcrConstants.JCR_CONTENT);
            if ( hasChunks(jcrContentNode)) {
                chunkParent = jcrContentNode;
            }
        }
        return chunkParent;
    }

    /**
     * Delete all chunks saved within a resource. If no chunks exist, it is no-op.
     */
    public void deleteChunks(final Resource rsrc) throws PersistenceException {
        final Resource chunkParent = getChunkParent(rsrc);

        if (chunkParent != null) {
            for(final Resource c : new FilteringResourceIterator(rsrc.listChildren(), SlingPostConstants.CHUNK_NODE_NAME) ) {
                c.getResourceResolver().delete(c);
            }
            final ModifiableValueMap vm = chunkParent.adaptTo(ModifiableValueMap.class);
            vm.remove(SlingPostConstants.NT_SLING_FILE_LENGTH);
            vm.remove(SlingPostConstants.NT_SLING_CHUNKS_LENGTH);
            removeChunkMixin(vm);
        }
    }

    private final void addChunkMixin(final ModifiableValueMap vm) {
        final String[] mixins = vm.get(JcrConstants.JCR_MIXINTYPES, String[].class);
        if ( mixins == null ) {
            vm.put(JcrConstants.JCR_MIXINTYPES, new String[] {SlingPostConstants.NT_SLING_CHUNK_MIXIN});
        } else {
            final Set<String> types = new HashSet<>(Arrays.asList(mixins));
            if ( !types.contains(SlingPostConstants.NT_SLING_CHUNK_MIXIN) ) {
                types.add(SlingPostConstants.NT_SLING_CHUNK_MIXIN);
                vm.put(JcrConstants.JCR_MIXINTYPES, types.toArray(new String[types.size()]));
            }
        }
    }

    private final void removeChunkMixin(final ModifiableValueMap vm) {
        final String[] mixins = vm.get(JcrConstants.JCR_MIXINTYPES, String[].class);
        if ( mixins != null ) {
            final Set<String> types = new HashSet<>(Arrays.asList(mixins));
            if ( types.remove(SlingPostConstants.NT_SLING_CHUNK_MIXIN) ) {
                vm.put(JcrConstants.JCR_MIXINTYPES, types.toArray(new String[types.size()]));
            }
        }
    }

    /**
     * Get the last {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE}
     * {@link Resource}.
     *
     * @param rsrc {@link Resource} containing
     *            {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE}
     *            {@link Resource}s
     * @return the {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE} chunk
     *         resource.
     */
    public Resource getLastChunk(Resource rsrc)  {
        final Resource chunkParent = getChunkParent(rsrc);
        if (chunkParent == null) {
            return null;
        }
        Resource lastChunkRsrc = null;
        long lastChunkStartIndex = -1;
        for(final Resource chunkRsrc : new FilteringResourceIterator(rsrc.listChildren(), SlingPostConstants.CHUNK_NODE_NAME + "_") ) {
            final String[] indexBounds = chunkRsrc.getName().substring(
                     (SlingPostConstants.CHUNK_NODE_NAME + "_").length()).split("_");
            long chunkStartIndex = Long.valueOf(indexBounds[0]);
            if (chunkStartIndex > lastChunkStartIndex) {
                lastChunkRsrc = chunkRsrc;
                lastChunkStartIndex = chunkStartIndex;
            }
        }

        return lastChunkRsrc;
    }

    /**
     * Return true if resource has chunks stored in it, otherwise false.
     */
    private boolean hasChunks(final Resource rsrc) {
        final ValueMap vm = rsrc.getValueMap();
        final String[] mixinTypes = vm.get(JcrConstants.JCR_MIXINTYPES, String[].class);
        if ( mixinTypes != null ) {
            for (final String nodeType : mixinTypes) {
                if (nodeType.equals(SlingPostConstants.NT_SLING_CHUNK_MIXIN)) {
                    return true;
                }
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
     * @throws PersistenceException if an error occurs
     */
    public void setFile(final Resource parent, final RequestProperty prop, final List<Modification> changes)
    throws PersistenceException {
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
                final ServletContext ctx = this.servletContext;
                if (ctx != null) {
                    contentType = ctx.getMimeType(value.getFileName());
                }
                if ( contentType == null ) {
                    contentType = MT_APP_OCTET;
                }
            }

            this.setFile(parent, prop, value, changes, name, contentType);
        }
    }

    private Resource getOrCreateChildResource(final Resource parent,
            final String name,
            final String typeHint,
            final List<Modification> changes)
    throws PersistenceException {
        Resource result = parent.getChild(name);
        if ( result != null ) {
            if ( !result.isResourceType(typeHint) && jcrSupport.isNode(result) && !jcrSupport.isNodeType(result, typeHint) ) {
                parent.getResourceResolver().delete(result);
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
            // sling resource type not allowed for nt:file nor nt:resource
            if ( !jcrSupport.isNode(parent)
                 || (!JcrConstants.NT_FILE.equals(typeHint) && !JcrConstants.NT_RESOURCE.equals(typeHint)) ) {
                properties = Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)typeHint);
            } else {
                properties = Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, (Object)typeHint);
            }
        }
        final Resource result = parent.getResourceResolver().create(parent, name, properties);
        changes.add(Modification.onCreated(result.getPath()));
        return result;
    }

}