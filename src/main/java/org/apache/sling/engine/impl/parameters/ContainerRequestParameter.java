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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * The <code>ContainerRequestParameter</code> TODO
 */
public class ContainerRequestParameter extends AbstractRequestParameter {

    private String value;

    private byte[] content;

    public ContainerRequestParameter(String name, String value, String encoding) {
        super(name, encoding);
        this.value = value;
        this.content = null;
    }

    @Override
    public void setEncoding(String encoding) {
        // recode this parameter by encoding the string with the current
        // encoding and decode the bytes with the encoding
        try {
            this.value = getString(encoding);
        } catch (UnsupportedEncodingException uee) {
            throw new SlingUnsupportedEncodingException(uee);
        }

        super.setEncoding(encoding);
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#get()
     */
    public byte[] get() {
        if (content == null) {
            try {
                content = getString().getBytes(getEncoding());
            } catch (Exception e) {
                // UnsupportedEncodingException, IllegalArgumentException
                content = getString().getBytes();
            }
        }
        return content;
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getContentType()
     */
    public String getContentType() {
        // none known for www-form-encoded parameters
        return null;
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getInputStream()
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.get());
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getFileName()
     */
    public String getFileName() {
        // no original file name
        return null;
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getSize()
     */
    public long getSize() {
        return this.get().length;
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getString()
     */
    public String getString() {
        return value;
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#getString(java.lang.String)
     */
    public String getString(String encoding)
            throws UnsupportedEncodingException {
        return new String(this.get(), encoding);
    }

    /**
     * @see org.apache.sling.api.request.RequestParameter#isFormField()
     */
    public boolean isFormField() {
        // www-form-encoded are always form fields
        return true;
    }

    public String toString() {
        return this.getString();
    }

}
