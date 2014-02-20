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
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;

/**
 * The <code>MultipartRequestParameter</code> represents a request parameter
 * from a multipart/form-data POST request.
 * <p>
 * To not add a dependency to Servlet API 3 this class does not implement the
 * Servlet API 3 {@code Part} interface. To support Servlet API 3 {@code Part}s
 * the {@link SlingPart} class wraps instances of this class.
 */
public class MultipartRequestParameter extends AbstractRequestParameter {

    private final FileItem delegatee;

    private String encodedFileName;

    private String cachedValue;

    public MultipartRequestParameter(FileItem delegatee) {
        super(delegatee.getFieldName(), null);
        this.delegatee = delegatee;
    }

    void dispose() {
        this.delegatee.delete();
    }

    FileItem getFileItem() {
        return this.delegatee;
    }

    @Override
    void setEncoding(String encoding) {
        super.setEncoding(encoding);
        cachedValue = null;
    }

    public byte[] get() {
        return this.delegatee.get();
    }

    public String getContentType() {
        return this.delegatee.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        return this.delegatee.getInputStream();
    }

    public String getFileName() {
        if (this.encodedFileName == null && this.delegatee.getName() != null) {
            String tmpFileName = this.delegatee.getName();
            if (this.getEncoding() != null) {
                try {
                    byte[] rawName = tmpFileName.getBytes(Util.ENCODING_DIRECT);
                    tmpFileName = new String(rawName, this.getEncoding());
                } catch (UnsupportedEncodingException uee) {
                    // might log, but actually don't care
                }
            }
            this.encodedFileName = tmpFileName;
        }

        return this.encodedFileName;
    }

    public long getSize() {
        return this.delegatee.getSize();
    }

    public String getString() {
        // only apply encoding in the case of a form field
        if (this.isFormField()) {
            if (this.cachedValue == null) {
                // try explicit encoding if available
                byte[] data = get();
                String encoding = getEncoding();
                if (encoding != null) {
                    try {
                        this.cachedValue = new String(data, encoding);
                    } catch (UnsupportedEncodingException uee) {
                        // don't care, fall back to platform default
                    }
                }

                // if there is no encoding, or an illegal encoding,
                // use platform default
                if (cachedValue == null) {
                    cachedValue = new String(data);
                }
            }

            return this.cachedValue;
        }

        return this.delegatee.getString();
    }

    public String getString(String enc) throws UnsupportedEncodingException {
        return this.delegatee.getString(enc);
    }

    public boolean isFormField() {
        return this.delegatee.isFormField();
    }

    public String toString() {
        if (this.isFormField()) {
            return this.getString();
        }

        return "File: " + this.getFileName() + " (" + this.getSize() + " bytes)";
    }
}
