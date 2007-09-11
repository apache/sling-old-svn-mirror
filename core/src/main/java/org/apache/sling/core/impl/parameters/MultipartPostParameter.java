/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;

/**
 * The <code>MultipartRequestParameter</code> TODO
 */
class MultipartRequestParameter extends AbstractEncodedParameter {

    private final FileItem delegatee;
    private String encodedFileName;

    /**
     *
     */
    MultipartRequestParameter(FileItem delegatee) {
        super(null);
        this.delegatee = delegatee;
    }

    void dispose() {
        this.delegatee.delete();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#get()
     */
    public byte[] get() {
        return this.delegatee.get();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getContentType()
     */
    public String getContentType() {
        return this.delegatee.getContentType();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        return this.delegatee.getInputStream();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getFileName()
     */
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

    /**
     * @see org.apache.sling.component.RequestParameter#getSize()
     */
    public long getSize() {
        return this.delegatee.getSize();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getString()
     */
    public String getString() {
        // only apply encoding in the case of a form field
        if (this.isFormField()) {
            return this.getEncodedString();
        }

        return this.delegatee.getString();
    }

    protected String decode(byte[] data, String encoding) {
        if (encoding != null) {
            try {
                return new String(data, encoding);
            } catch (UnsupportedEncodingException uee) {
                // TODO: handle
            }
        }

        // if there is no encoding, or an illegal encoding, use platform default
        return new String(data);
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getString(java.lang.String)
     */
    public String getString(String enc) throws UnsupportedEncodingException {
        return this.delegatee.getString(enc);
    }

    /**
     * @see org.apache.sling.component.RequestParameter#isFormField()
     */
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
