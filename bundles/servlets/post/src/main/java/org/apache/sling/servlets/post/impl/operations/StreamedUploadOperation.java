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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.impl.helper.StreamedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs a streamed modification of the content.
 * Each File body encountered will result in a session save operation, to cause the underlying Resource implementation
 * to stream content from the request to the target.
 *
 * This implements PostOperation but does not touch the normal Sling Request processing which is not streamed.
 *
 * The map of available fields is built up as the request is streamed. It is advisable to submit the request with all the form
 * fields at the start of the request (normally based on DOM order) to ensure they are available before the streamed bodies are processed.
 *
 * The implementation does not implement the full Sling protocol aiming to keep it simple, and just deal with a streaming upload operation.
 * The implementation binds to the Sling Resource API rather than JCR to keep it independent of the type of persistence.
 */
public class StreamedUploadOperation extends AbstractPostOperation {
    private static final Logger LOG = LoggerFactory.getLogger(StreamedUploadOperation.class);
    public static final String NT_FILE = "nt:file";
    private ServletContext servletContext;

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    /**
     * Check the request and return true if there is a parts iterator attribute present. This attribute
     * will have been put there by the Sling Engine ParameterSupport class. If its not present, the request
     * is not streamed and cant be processed by this class. Check this first before using this class.
     * @param request the request.
     * @return true if the request can be streamed.
     */
    public boolean isRequestStreamed(SlingHttpServletRequest request) {
        return request.getAttribute("request-parts-iterator") != null;
    }

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes)
    throws PersistenceException {
        @SuppressWarnings("unchecked")
        Iterator<Part> partsIterator = (Iterator<Part>) request.getAttribute("request-parts-iterator");
        Map<String, List<String>> formFields = new HashMap<>();
        boolean streamingBodies = false;
        while (partsIterator.hasNext()) {
            Part part = partsIterator.next();
            String name = part.getName();

            if (isFormField(part)) {
                addField(formFields, name, part);
                if (streamingBodies) {
                    LOG.warn("Form field {} was sent after the bodies started to be streamed. " +
                            "Will not have been available to all streamed bodies. " +
                            "It is recommended to send all form fields before streamed bodies in the POST ", name);
                }
            } else {
                streamingBodies = true;
                // process the file body and commit.
                writeContent(request.getResourceResolver(), part, formFields, response, changes);

            }
        }
    }

    /**
     * Add a field to the store of formFields.
     * @param formFields the formFileds
     * @param name the name of the field.
     * @param part the part.
     */
    private void addField(Map<String, List<String>> formFields, String name, Part part) {
        List<String> values = formFields.get(name);
        if ( values == null ) {
            values = new ArrayList<>();
            formFields.put(name, values);
        }
        try {
            values.add(IOUtils.toString(part.getInputStream(),"UTF-8"));
        } catch (IOException e) {
            LOG.error("Failed to read form field "+name,e);
        }
    }


    /**
     * Write content to the resource API creating a standard JCR structure of nt:file - nt:resource - jcr:data.
     * This method will commit to the repository to force the repository to read from the input stream and write
     * to the target. How efficient that is depends on the repository implementation.
     * @param resolver the resource resolver.
     * @param part the part containing the file body.
     * @param formFields form fields collected so far.
     * @param response the response object, updated by the operation.
     * @param changes changes made to the repo.
     * @throws PersistenceException
     */
    private void writeContent(final ResourceResolver resolver,
                              final Part part,
                              final Map<String, List<String>> formFields,
                              final PostResponse response,
                              final List<Modification> changes)
            throws PersistenceException {

        final String path = response.getPath();
        final Resource parentResource = resolver.getResource(path);
        if ( !resourceExists(parentResource)) {
            throw new IllegalArgumentException("Parent resource must already exist to be able to stream upload content. Please create first ");
        }
        String name = getUploadName(part);
        Resource fileResource = parentResource.getChild(name);
        Map<String, Object> fileProps = new HashMap<>();
        if (fileResource == null) {
            fileProps.put("jcr:primaryType", NT_FILE);
            fileResource = parentResource.getResourceResolver().create(parentResource, name, fileProps);
        }


        StreamedChunk chunk = new StreamedChunk(part, formFields, servletContext);
        Resource result = chunk.store(fileResource, changes);
        result.getResourceResolver().commit();

    }

    /**
     * Is the part a form field ?
     * @param part
     * @return
     */
    private boolean isFormField(Part part) {
        return (part.getSubmittedFileName() == null);
    }

    /**
     * Get the upload file name from the part.
     * @param part
     * @return
     */
    private String getUploadName(Part part) {
        // only return non null if the submitted file name is non null.
        // the Sling API states that if the field name is '*' then the submitting file name is used,
        // otherwise the field name is used.
        String name = part.getName();
        String fileName = part.getSubmittedFileName();
        if ("*".equals(name)) {
            name = fileName;
        }
        // strip of possible path (some browsers include the entire path)
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.substring(name.lastIndexOf('\\') + 1);
        return Text.escapeIllegalJcrChars(name);
    }

    /**
     * Does the resource exist ?
     * @param resource
     * @return
     */
    private boolean resourceExists(final Resource resource) {
        return  (resource != null && !ResourceUtil.isSyntheticResource(resource));
    }





}
