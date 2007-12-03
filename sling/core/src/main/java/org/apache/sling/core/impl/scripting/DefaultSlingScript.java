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
package org.apache.sling.core.impl.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;

class DefaultSlingScript implements SlingScript {

    private Resource scriptResource;

    private SlingScriptEngine scriptEngine;

    DefaultSlingScript(Resource scriptResource, SlingScriptEngine scriptEngine) {
        this.scriptResource = scriptResource;
        this.scriptEngine = scriptEngine;
    }

    public Resource getScriptResource() {
        return scriptResource;
    }

    public SlingScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public Reader getScriptReader() throws IOException {

        InputStream input = getScriptResource().adaptTo(InputStream.class);
        if (input == null) {
            throw new IOException("Cannot get a stream to the script resource "
                + getScriptResource());
        }

        // Now know how to get the input stream, we still have to decide
        // on the encoding of the stream's data. Primarily we assume it is
        // UTF-8, which is a default in many places in JCR. Secondarily
        // we try to get a jcr:encoding property besides the data property
        // to provide a possible encoding
        ResourceMetadata meta = getScriptResource().getResourceMetadata();
        String encoding = (String) meta.get(ResourceMetadata.CHARACTER_ENCODING);
        if (encoding == null) {
            encoding = "UTF-8";
        }

        // access the value as a stream and return a buffered reader
        // converting the stream data using UTF-8 encoding, which is
        // the default encoding used
        return new BufferedReader(new InputStreamReader(input, encoding));
    }
}