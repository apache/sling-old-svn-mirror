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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException; // required due to AbstractPostOperation signature.
import javax.servlet.ServletContext;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public static final String NT_RESOURCE = "nt:resource";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_DATA = "jcr:data";
    private static final String MT_APP_OCTET = "application/octet-stream";
    private static final String JCR_CONTENT = "jcr:content";
    private ServletContext servletContext;

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    public boolean isRequestStreamed(SlingHttpServletRequest request) {
        return request.getAttribute("request-parts-iterator") != null;
    }

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) throws RepositoryException {
        try {
            Iterator<Part> partsIterator = (Iterator<Part>) request.getAttribute("request-parts-iterator");
            Map<String, List<String>> formFields = new HashMap<String, List<String>>();
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
        } catch ( final PersistenceException pe) {
            if ( pe.getCause() instanceof RepositoryException ) {
                throw (RepositoryException)pe.getCause();
            }
            throw new RepositoryException(pe);
        }

    }

    private void addField(Map<String, List<String>> formFields, String name, Part part) {
        List<String> values = formFields.get(name);
        if ( values == null ) {
            values = new ArrayList<String>();
            formFields.put(name, values);
        }
        try {
            values.add(IOUtils.toString(part.getInputStream(),"UTF-8"));
        } catch (IOException e) {
            LOG.error("Failed to read form field "+name,e);
        }
    }


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
        Map<String, Object> fileProps = new HashMap<String, Object>();
        if (fileResource == null) {
            fileProps.put("jcr:primaryType", NT_FILE);
            fileResource = parentResource.getResourceResolver().create(parentResource, name, fileProps);
        }



        Map<String, Object> resourceProps = new HashMap<String, Object>();
        resourceProps.put("jcr:primaryType", NT_RESOURCE);
        resourceProps.put(JCR_LASTMODIFIED, Calendar.getInstance());
        // TODO: Should all the formFields be added to the prop map ?
        resourceProps.put(JCR_MIMETYPE, getContentType(part));
        try {
            resourceProps.put(JCR_DATA, part.getInputStream());
        } catch (IOException e) {
            throw new PersistenceException("Error while retrieving inputstream from request part.", e);
        }
        Resource result = fileResource.getChild(JCR_CONTENT);
        if ( result != null ) {
            final ModifiableValueMap vm = result.adaptTo(ModifiableValueMap.class);
            if ( vm == null ) {
                throw new PersistenceException("Resource at " + fileResource.getPath() + '/' + JCR_CONTENT + " is not modifiable.");
            }
            vm.putAll(resourceProps);
        } else {
            result = parentResource.getResourceResolver().create(fileResource, JCR_CONTENT, resourceProps);
        }
        // Commit must be called to perform to cause streaming so the next part can be found.
        result.getResourceResolver().commit();

        for( String key : resourceProps.keySet()) {
            changes.add(Modification.onModified(result.getPath() + '/' + key));
        }
    }

    private boolean isFormField(Part part) {
        return (part.getSubmittedFileName() == null);
    }

    private String getUploadName(Part part) {
        String name = part.getSubmittedFileName();
        // strip of possible path (some browsers include the entire path)
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.substring(name.lastIndexOf('\\') + 1);
        return Text.escapeIllegalJcrChars(name);
    }

    private boolean resourceExists(final Resource resource) {
        return  (resource != null && !ResourceUtil.isSyntheticResource(resource));
    }



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



}
