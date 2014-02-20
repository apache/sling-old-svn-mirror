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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

public class SlingPart implements Part {

    private final MultipartRequestParameter param;

    public SlingPart(final MultipartRequestParameter param) {
        this.param = param;
    }

    public InputStream getInputStream() throws IOException {
        return this.param.getInputStream();
    }

    public String getContentType() {
        return this.param.getContentType();
    }

    public String getName() {
        return this.param.getFileItem().getFieldName();
    }

    public long getSize() {
        return this.param.getSize();
    }

    public void write(String fileName) throws IOException {
        throw new IOException("Unsupported yet");
    }

    public void delete() {
        this.param.getFileItem().delete();
    }

    public String getHeader(String name) {
        return this.param.getFileItem().getHeaders().getHeader(name);
    }

    public Collection<String> getHeaders(String name) {
        final ArrayList<String> headers = new ArrayList<String>();
        final Iterator<String> itemHeaders = this.param.getFileItem().getHeaders().getHeaders(name);
        while (itemHeaders.hasNext()) {
            headers.add(itemHeaders.next());
        }
        return headers;
    }

    public Collection<String> getHeaderNames() {
        final ArrayList<String> headers = new ArrayList<String>();
        final Iterator<String> itemHeaders = this.param.getFileItem().getHeaders().getHeaderNames();
        while (itemHeaders.hasNext()) {
            headers.add(itemHeaders.next());
        }
        return headers;
    }
}
