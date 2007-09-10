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
package org.apache.sling.core.parameters;

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
        delegatee.delete();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#get()
     */
    public byte[] get() {
        return delegatee.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getContentType()
     */
    public String getContentType() {
        return delegatee.getContentType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        return delegatee.getInputStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getName()
     */
    public String getFileName() {
        if (encodedFileName == null && delegatee.getName() != null) {
            String tmpFileName = delegatee.getName();
            if (getEncoding() != null) {
                try {
                    byte[] rawName = tmpFileName.getBytes(Util.ENCODING_DIRECT);
                    tmpFileName = new String(rawName, getEncoding());
                } catch (UnsupportedEncodingException uee) {
                    // might log, but actually don't care
                }
            }
            encodedFileName = tmpFileName;
        }
        
        return encodedFileName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getSize()
     */
    public long getSize() {
        return delegatee.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getString()
     */
    public String getString() {
        // only apply encoding in the case of a form field
        if (isFormField()) {
            return getEncodedString();
        }
        
        return delegatee.getString();
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

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#getString(java.lang.String)
     */
    public String getString(String enc) throws UnsupportedEncodingException {
        return delegatee.getString(enc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RequestParameter#isFormField()
     */
    public boolean isFormField() {
        return delegatee.isFormField();
    }
    
    public String toString() {
        if (isFormField()) {
            return getString();
        }
        
        return "File: " + getFileName() + " (" + getSize() + " bytes)";
    }
}
