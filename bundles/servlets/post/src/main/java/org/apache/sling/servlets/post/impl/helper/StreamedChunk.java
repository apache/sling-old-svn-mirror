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

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supports streamed uploads including where the stream is made up of partial body parts.
 * The behaviour is documented here https://cwiki.apache.org/confluence/display/SLING/Chunked+File+Upload+Support, adding the ability
 * to define a body part with a Content-Range header on the part. Since the body parts are streamed there are some restrictions. If the
 * length of a body part is missing from either the Content-Range header or a Content-Length header in the Part, then the length of the part is
 * assumed to be the rest of the body to make the total length of the upload that specified in earlier Content-Range headers or a @Length property.
 *
 * When using only Content-Range headers (see the HTTP 1.1 spec) the Content-Range header must be complete and applied to the Part of the body.
 * The length of the full file must be specified and be the same on all body parts, and the body parts must be sent in order. This is a restriction
 * of the Sling Chunked File Upload protocol. When the total uploaded equals the file length the chunked uploads are processed to generate the final upload.
 *
 * When using request parameters, the most recent request parameters are used for @Completed, @Offset and @Length. When using request parameters if the
 * Content-Length header is missing from the body Part, then the Body part is assumed to be the final body part. Then the total uploaded equals the value of
 * the @Length parameter or a @Completed parameter is present, then the body parts are joined into a single body part.
 *
 * Consolidating body parts will cause all body parts to be read from the DS, which will incure 3x the IO of a non body part or chunked upload. For FS DS the IO may be from
 * OS level disk cache. For other styles of DS the IO may consume more resources. Chunked or Body part uploads are not as efficient as whole body uploads and should
 * be avoided wherever possible. This could be avoided if Oak would expose a seekable OutputStream, or allow writes to Binaries to specify and offset.
 *
 *
 *
 */
public class StreamedChunk {
    private static final String NT_RESOURCE = "nt:resource";
    private static final String JCR_LASTMODIFIED = "jcr:lastModified";
    private static final String JCR_MIMETYPE = "jcr:mimeType";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private static final String SLING_CHUNKS_LENGTH = "sling:length";
    private static final String SLING_FILE_LENGTH = "sling:fileLength";
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";
    private static final String SLING_CHUNK_MIXIN = "sling:chunks";
    private static final String SLING_CHUNK_NT = "sling:chunk";
    private static final String SLING_OFFSET = "sling:offset";
    private static final String MT_APP_OCTET = "application/octet-stream";
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamedChunk.class);

    private final long offset;
    private final long chunkLength;
    private final long fileLength;
    private final Part part;
    private final Map<String, List<String>> formFields;
    private ServletContext servletContext;
    private final boolean completed;
    private final boolean chunked;
    private final String chunkResourceName;

    /**
     * Construct a chunk from the part and form fields. Once constructed it is immutable exposing a store method to store the chunk.
     * If the part does not represent a chunk then the class behaves as if the chunk is a upload of 1 chunk (ie the whole file).
     * @param part the current part, not read other than headers.
     * @param formFields form fields encountered in teh request stream prior to this part.
     * @param servletContext the current servlet context needed to resolve mimetypes.
     */
    public StreamedChunk(Part part, Map<String, List<String>> formFields, ServletContext servletContext) {
        this.part = part;
        this.formFields = formFields;
        this.servletContext = servletContext;

        String contentRangeHeader = part.getHeader("Content-Range");
        String contentLengthHeader = part.getHeader("Content-Length");
        if ( contentRangeHeader != null ) {
            ContentRange contentRange = new ContentRange(contentRangeHeader);
            fileLength = contentRange.length;
            offset = contentRange.offset;
            chunkLength = contentRange.range;
            chunked = true;

        } else if ( formFields.containsKey(part.getName()+"@Length") && formFields.containsKey(part.getName()+"@Offset")) {
            fileLength = Long.parseLong(lastFrom(formFields.get(part.getName() + "@Length")));
            offset = Long.parseLong(lastFrom(formFields.get(part.getName() + "@Offset")));
            if (contentLengthHeader != null) {
                chunkLength = Long.parseLong(contentLengthHeader);
            } else if ( formFields.containsKey(part.getName() + "@PartLength")) {
                chunkLength = Long.parseLong(lastFrom(formFields.get(part.getName() + "@PartLength")));
            } else {
                // must assume the chunk contains all the data.
                LOGGER.info("No part length specified assuming this is the final part of the chunked upload.");
                chunkLength = fileLength - offset;
            }
            chunked = true;
        } else {
            offset = 0;
            if (contentLengthHeader != null) {
                fileLength =  Long.parseLong(contentLengthHeader);
                chunkLength = fileLength;
            } else {
                fileLength = -1;
                chunkLength = -1;
            }
            chunked = false;

        }
        chunkResourceName = "chunk_"+offset+"-"+(offset+chunkLength);
        completed = ((offset+chunkLength) == fileLength) || formFields.containsKey(part.getName()+"@Completed");
        LOGGER.debug(" chunkResourceName {},  chunked {},completed {},  fileLength {}, chunkLength {}, offset {} ",
                new Object[]{chunkResourceName, chunked, completed, fileLength, chunkLength, offset});
    }

    /**
     * Store the chunk in a file resource under a jcr:content sub node. The method does not commit the resource resolver. The caller
     * must perform the commit. If the stream is a stream of body parts and the parts are complete, the store operation will commit
     * the body part but leave the consolitation of all parts to be committed by the caller. ie, always call resourceResolver.commit() after
     * calling this method.
     * @param fileResource the file request.
     * @param changes changes that were made.
     * @return the jcr:content sub node.
     * @throws PersistenceException
     */
    public Resource store(Resource fileResource, List<Modification> changes) throws PersistenceException {
        Resource result = fileResource.getChild(JCR_CONTENT);
        if (result != null) {
            updateState(result, changes);
        } else {
            result = initState(fileResource, changes);
        }
        storeChunk(result, changes);
        return result;
    }


    /**
     * The last element of strings.
     * @param strings a non null non zero string array.
     * @return the last element.
     */
    private String lastFrom(List<String> strings) {
        return strings.get(strings.size()-1);
    }

    /**
     * Update the state of the content resource to reflect a new body part being streamd.
     * @param contentResource the content resource
     * @param changes changes made.
     * @throws IllegalStateException if the contentResource is not consistent with the part being streamed.
     * @throws PersistenceException if the part cant be streamed.
     */
    private void updateState(Resource contentResource, List<Modification> changes) throws IllegalStateException, PersistenceException {
        final ModifiableValueMap vm = contentResource.adaptTo(ModifiableValueMap.class);
        if ( vm == null ) {
            throw new PersistenceException("Resource at " + contentResource.getPath() + " is not modifiable.");
        }
        vm.put(JCR_LASTMODIFIED, Calendar.getInstance());
        vm.put(JCR_MIMETYPE, getContentType(part));
        if (chunked) {
            if ( vm.containsKey(SLING_FILE_LENGTH)) {
                long previousFileLength = (Long) vm.get(SLING_FILE_LENGTH, Long.class);
                if (previousFileLength != fileLength) {
                    throw new IllegalStateException("Chunk file length has changed while cunks were being uploaded expected " + previousFileLength + " chunk contained  " + fileLength);
                }
            }
            long previousChunksLength = 0;
            if ( vm.containsKey(SLING_CHUNKS_LENGTH)) {
                previousChunksLength = (Long) vm.get(SLING_CHUNKS_LENGTH, Long.class);
                if (previousChunksLength != offset) {
                    throw new IllegalStateException("Chunks recieved out of order, was expecting chunk starting at " + offset + " found last chunk ending at " + previousChunksLength);
                }
            }
            vm.put(SLING_CHUNKS_LENGTH, previousChunksLength + chunkLength);
            vm.put(JCR_MIXIN_TYPES, SLING_CHUNK_MIXIN);
        } else {
            try {
                vm.put(JCR_DATA, part.getInputStream());
            } catch (IOException e) {
                throw new PersistenceException("Error while retrieving inputstream from request part.", e);
            }
        }
    }

    /**
     * Initialise the state of the jcr:content sub resource.
     * @param fileResource the fileResource parent resource.
     * @param changes changes that were made.
     * @return the content resource.
     * @throws PersistenceException
     */
    private Resource initState(Resource fileResource, List<Modification> changes) throws PersistenceException {
        Map<String, Object> resourceProps = new HashMap<String, Object>();
        resourceProps.put(JCR_PRIMARY_TYPE, NT_RESOURCE);
        resourceProps.put(JCR_LASTMODIFIED, Calendar.getInstance());
        resourceProps.put(JCR_MIMETYPE, getContentType(part));

        if (chunked) {
            resourceProps.put(SLING_CHUNKS_LENGTH, chunkLength);
            resourceProps.put(SLING_FILE_LENGTH, fileLength);
            resourceProps.put(JCR_MIXIN_TYPES, SLING_CHUNK_MIXIN);
            // add a zero size file to satisfy JCR constraints.
            resourceProps.put(JCR_DATA, new ByteArrayInputStream(new byte[0]));
        } else {
            try {
                resourceProps.put(JCR_DATA, part.getInputStream());
            } catch (IOException e) {
                throw new PersistenceException("Error while retrieving inputstream from request part.", e);
            }
        }

        Resource result = fileResource.getResourceResolver().create(fileResource, JCR_CONTENT, resourceProps);
        for( String key : resourceProps.keySet()) {
            changes.add(Modification.onModified(result.getPath() + '/' + key));
        }
        return result;
    }

    /**
     * Store the chunk in a chunked resource. If not chunked does nothing.
     * @param contentResource
     * @param changes
     * @throws PersistenceException
     */
    private void storeChunk(Resource contentResource, List<Modification> changes) throws PersistenceException {
        if (chunked) {
            Map<String, Object> chunkProperties = new HashMap<String, Object>();
            chunkProperties.put(JCR_PRIMARY_TYPE, SLING_CHUNK_NT);
            chunkProperties.put(SLING_OFFSET, offset);
            try {
                chunkProperties.put(JCR_DATA, part.getInputStream());
            } catch (IOException e) {
                throw new PersistenceException("Error while retrieving inputstream from request part.", e);
            }
            LOGGER.debug("Creating chunk at {} with properties {}  ", chunkResourceName, chunkProperties);
            Resource chunkResource = contentResource.getResourceResolver().create(contentResource, chunkResourceName, chunkProperties);


            for (String key : chunkProperties.keySet()) {
                changes.add(Modification.onModified(chunkResource.getPath() + '/' + key));
            }


            processChunks(contentResource, changes);
        }

    }

    /**
     * process all chunks formed so far to create the final body.
     * @param contentResource
     * @param changes
     * @throws PersistenceException
     */
    private void processChunks(Resource contentResource, List<Modification> changes) throws PersistenceException {
        if (completed) {

            // have to commit before processing chunks.
            contentResource.getResourceResolver().commit();
            ModifiableValueMap vm = contentResource.adaptTo(ModifiableValueMap.class);
            vm.put("jcr:data", getChunksInputStream(contentResource));
            // might have to commit before removing chunk data, depending on if the InputStream still works.
            removeChunkData(contentResource, vm);
        }
    }

    /**
     * remove chunk data.
     * @param contentResource
     * @param vm
     * @throws PersistenceException
     */
    private void removeChunkData(Resource contentResource, ModifiableValueMap vm) throws PersistenceException {
        for ( Resource r : contentResource.getChildren()) {
            if (r.isResourceType(SLING_CHUNK_NT)) {
                r.getResourceResolver().delete(r);
            }
        }
        vm.remove(SLING_CHUNKS_LENGTH);
        vm.remove(SLING_FILE_LENGTH);
    }

    /**
     * Create an input stream that will read though the chunks in order.
     * @param contentResource
     * @return
     */
    private InputStream getChunksInputStream(Resource contentResource) {
        List<Resource> chunkResources = new ArrayList<Resource>();
        for ( Resource r : contentResource.getChildren()) {
            if (r.isResourceType(SLING_CHUNK_NT)) {
                chunkResources.add(r);
            }
        }
        Collections.sort(chunkResources, new Comparator<Resource>() {
            @Override
            public int compare(Resource o1, Resource o2) {
                long offset1 = o1.adaptTo(ValueMap.class).get(SLING_OFFSET, Long.class);
                long offset2 = o2.adaptTo(ValueMap.class).get(SLING_OFFSET, Long.class);
                return (int) (offset1 - offset2);
            }
        });
        if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug("Finishing Chunk upload at {} consolidating {} chunks into one file of  ",
                    new Object[]{
                            contentResource.getPath(),
                            chunkResources.size(),
                            contentResource.adaptTo(ValueMap.class).get(SLING_CHUNKS_LENGTH)
                    });
            LOGGER.debug("Content Resource Properties {} ", contentResource.adaptTo(ValueMap.class));
            for (Resource r : chunkResources) {
                LOGGER.debug("Chunk {} properties {} ", r.getPath(), r.adaptTo(ValueMap.class));
            }
        }
        return new ResourceIteratorInputStream(chunkResources.iterator());
    }

    /**
     * Get the content type of the part.
     * @param part
     * @return
     */
    private String getContentType(final Part part) {
        String contentType = part.getContentType();
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
                contentType = ctx.getMimeType(part.getSubmittedFileName());
            }
            if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                contentType = MT_APP_OCTET;
            }
        }
        return contentType;
    }

    /**
     * Parses Content-Range headers according to spec https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html section 14.16
     *
     *     Content-Range = "Content-Range" ":" content-range-spec
     * content-range-spec      = byte-content-range-spec
     * byte-content-range-spec = bytes-unit SP
     *                           byte-range-resp-spec "/"
     *                           ( instance-length | "*" )
     * byte-range-resp-spec = (first-byte-pos "-" last-byte-pos)
     *                          | "*"
     * instance-length = 1*DIGIT
     *
     * eg
     * bytes 0-1233/1234
     * bytes 500-1233/1234
     * bytes 500-1233/*
     *
     * According to https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.12 "bytes" is the only valid range unit.
     */
    public static class ContentRange {
        private static final Pattern rangePattern = Pattern.compile("bytes\\s([0-9]+)-([0-9]+)\\/([0-9]*)(\\**)");
        public long length;
        public long offset;
        public long range;


        public ContentRange(String contentRangeHeader) {
            Matcher m = rangePattern.matcher(contentRangeHeader);
            if ( m.find() ) {
                offset = Long.parseLong(m.group(1));
                long end = Long.parseLong(m.group(2));
                range = end-offset+1;
                if ("*".equals(m.group(4))) {
                    length = -1;
                } else {
                    length = Long.parseLong(m.group(3));
                    if ( offset > length ) {
                        throw new IllegalArgumentException("Range header "+contentRangeHeader+" is invalid, offset beyond end.");
                    }
                    if ( end > length ) {
                        throw new IllegalArgumentException("Range header "+contentRangeHeader+" is invalid, range end beyond end.");
                    }
                    if ( range > length ) {
                        throw new IllegalArgumentException("Range header "+contentRangeHeader+" is invalid, range greater than length.");
                    }
                }
                if ( offset > end ) {
                    throw new IllegalArgumentException("Range header "+contentRangeHeader+" is invalid, offset beyond end of range.");
                }
            } else {
                throw new IllegalArgumentException("Range header "+contentRangeHeader+" is invalid");
            }
        }
    }
}
