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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * The <code>EncodedRequestParameter</code> TODO
 */
public class EncodedRequestParameter extends AbstractEncodedParameter {

    private byte[] content;

    EncodedRequestParameter(String encoding) {
        super(encoding);
        this.content = Util.NO_CONTENT;
    }

    void setContent(byte[] content) {
        this.content = content;
        super.setEncoding(this.getEncoding());
    }

    /**
     * @see org.apache.sling.component.RequestParameter#get()
     */
    public byte[] get() {
        return this.content;
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getContentType()
     */
    public String getContentType() {
        // none known for www-form-encoded parameters
        return null;
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getInputStream()
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.get());
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getFileName()
     */
    public String getFileName() {
        // no original file name
        return null;
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getSize()
     */
    public long getSize() {
        return this.get().length;
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getString()
     */
    public String getString() {
        return this.getEncodedString();
    }

    /**
     * @see org.apache.sling.component.RequestParameter#getString(java.lang.String)
     */
    public String getString(String encoding) throws UnsupportedEncodingException {
        return new String(this.get(), encoding);
    }

    /**
     * @see org.apache.sling.component.RequestParameter#isFormField()
     */
    public boolean isFormField() {
        // www-form-encoded are always form fields
        return true;
    }

    public String toString() {
        return this.getString();
    }

    protected String decode(byte[] data, String encoding) {
        if (encoding != null) {
            try {
                String value = new String(data, Util.ENCODING_DIRECT);
                return URLDecoder.decode(value, encoding);
            } catch (UnsupportedEncodingException uue) {
                // not expected, use default encoding anyway ...
            } catch (IllegalArgumentException iae) {
                // due to illegal encoding in value, ignore for now
            }
        }

        // if still here, use platform default encoding
        return new String(data);
    }
}
