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

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class MockPart implements Part {

    private Map<String, Object> headers;
    private long size;
    private String submittedFileName;
    private String name;
    private String contentType;
    private InputStream inputStream;

    public MockPart(String name, String contentType, String submittedFileName, long size, InputStream inputStream, Map<String, Object> headers) {
        this.name = name;
        this.contentType = contentType;
        this.submittedFileName = submittedFileName;
        this.size = size;
        this.inputStream = inputStream;
        this.headers = headers;
    }

    @Override
    public InputStream getInputStream() throws IOException {

        return inputStream;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSubmittedFileName() {
        return submittedFileName;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void write(String s) throws IOException {
        throw new UnsupportedOperationException("Writing a part to disk is not supported.");

    }

    @Override
    public void delete() throws IOException {

    }

    @Override
    public String getHeader(String s) {
        return (String) headers.get(s);
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return (Collection<String>) headers.get(s);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }
}
