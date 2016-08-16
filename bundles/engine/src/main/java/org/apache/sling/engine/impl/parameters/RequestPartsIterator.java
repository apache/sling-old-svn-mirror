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

package org.apache.sling.engine.impl.parameters;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Contains a Lazy iterator of Parts from the request stream loaded as the request is streamed using the Commons FileUpload API.
 */
public class RequestPartsIterator implements Iterator<Part> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestPartsIterator.class);

    /** The CommonsFile Upload streaming API iterator */
    private final FileItemIterator itemIterator;

    /**
     * Create and initialse the iterator using the request. The request must be fresh. Headers can have been read but the stream
     * must not have been parsed.
     * @param servletRequest the request
     * @throws IOException when there is a problem reading the request.
     * @throws FileUploadException when there is a problem parsing the request.
     */
    public RequestPartsIterator(HttpServletRequest servletRequest) throws IOException, FileUploadException {
        ServletFileUpload upload = new ServletFileUpload();
        itemIterator = upload.getItemIterator(servletRequest);
    }

    @Override
    public boolean hasNext() {
        try {
            return itemIterator.hasNext();
        } catch (FileUploadException e) {
            LOG.error("hasNext Item failed cause:" + e.getMessage(), e);
        } catch (IOException e) {
            LOG.error("hasNext Item failed cause:" + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Part next() {
        try {
            return new StreamedRequestPart(itemIterator.next());
        } catch (IOException e) {
            LOG.error("next Item failed cause:" + e.getMessage(), e);
        } catch (FileUploadException e) {
            LOG.error("next Item failed cause:" + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported on a request stream.");
    }

    /**
     * Internal implementation of the Part API from Servlet 3 wrapping the Commons File Upload FIleItemStream object.
     */
    private static class StreamedRequestPart implements Part {
        private final FileItemStream fileItem;
        private final InputStream inputStream;

        public StreamedRequestPart(FileItemStream fileItem) throws IOException {
            this.fileItem = fileItem;
            inputStream = fileItem.openStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public String getContentType() {
            return fileItem.getContentType();
        }

        @Override
        public String getName() {
            return fileItem.getName();
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public void write(String s) throws IOException {
            throw new UnsupportedOperationException("Writing parts directly to disk is not supported by this implementation, use getInputStream instead");
        }

        @Override
        public void delete() throws IOException {
            // no underlying storage is used, so nothing to delete.
        }

        @Override
        public String getHeader(String headerName) {
            return fileItem.getHeaders().getHeader(headerName);
        }

        @Override
        public Collection<String> getHeaders(String headerName) {
            return toCollection(fileItem.getHeaders().getHeaders(headerName));
        }


        @Override
        public Collection<String> getHeaderNames() {
            return toCollection(fileItem.getHeaders().getHeaderNames());
        }

        @Override
        public String getSubmittedFileName() {
            return fileItem.getFieldName();
        }

        private <T> Collection<T> toCollection(Iterator<T> i) {
            if ( i == null ) {
                return Collections.emptyList();
            } else {
                List<T> c = new ArrayList<T>();
                while(i.hasNext()) {
                    c.add(i.next());
                }
                return c;
            }
        }

    }
}
