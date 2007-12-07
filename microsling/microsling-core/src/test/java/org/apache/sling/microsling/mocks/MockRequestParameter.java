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
package org.apache.sling.microsling.mocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.sling.api.request.RequestParameter;

public class MockRequestParameter implements RequestParameter {

    private final String value;
    
    public MockRequestParameter(String value) {
        this.value = value;
    }
    
    public byte[] get() {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public String getContentType() {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public String getFileName() {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public InputStream getInputStream() throws IOException {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public long getSize() {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public String getString() {
        return value;
    }

    public String getString(String encoding)
            throws UnsupportedEncodingException {
        throw new Error("Not implemented in MockRequestParameter");
    }

    public boolean isFormField() {
        throw new Error("Not implemented in MockRequestParameter");
    }

}
