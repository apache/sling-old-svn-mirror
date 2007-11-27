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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.Resource;
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

        Property property;
        Value value;

        try {

            if (getScriptResource().getRawData() instanceof Node) {
                // SLING-72: Cannot use primary items due to WebDAV creating
                // nt:unstructured as jcr:content node. So we just assume
                // nt:file and try to use the well-known data path
                Node node = (Node) getScriptResource().getRawData();
                property = node.getProperty("jcr:content/jcr:data");
            } else {
                throw new IOException("Scriptresource " + getScriptResource()
                    + " must is not JCR Node based");
            }

            value = null;
            if (property.getDefinition().isMultiple()) {
                // for a multi-valued property, we take the first non-null
                // value (null values are possible in multi-valued
                // properties)
                // TODO: verify this claim ...
                Value[] values = property.getValues();
                for (Value candidateValue : values) {
                    if (candidateValue != null) {
                        value = candidateValue;
                        break;
                    }
                }

                // incase we could not find a non-null value, we bail out
                if (value == null) {
                    throw new IOException("Cannot access "
                        + getScriptResource().getURI());
                }
            } else {
                // for single-valued properties, we just take this value
                value = property.getValue();
            }
        } catch (RepositoryException re) {
            throw (IOException) new IOException("Cannot get script "
                + getScriptResource().getURI()).initCause(re);
        }

        // Now know how to get the input stream, we still have to decide
        // on the encoding of the stream's data. Primarily we assume it is
        // UTF-8, which is a default in many places in JCR. Secondarily
        // we try to get a jcr:encoding property besides the data property
        // to provide a possible encoding
        String encoding = "UTF-8";
        try {
            Node parent = property.getParent();
            if (parent.hasNode(DefaultSlingScriptResolver.JCR_ENCODING)) {
                encoding = parent.getProperty(
                    DefaultSlingScriptResolver.JCR_ENCODING).getString();
            }
        } catch (RepositoryException re) {
            // don't care if we fail for any reason here, just assume
            // default
        }

        // access the value as a stream and return a buffered reader
        // converting the stream data using UTF-8 encoding, which is
        // the default encoding used
        try {
            InputStream input = value.getStream();
            return new BufferedReader(new InputStreamReader(input, encoding));
        } catch (RepositoryException re) {
            throw (IOException) new IOException("Cannot get script "
                + getScriptResource().getURI()).initCause(re);
        }
    }
}