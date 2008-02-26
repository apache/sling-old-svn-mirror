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
package org.apache.sling.jcr.resource.internal.loader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.jcr.PropertyType;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;


/**
 * The <code>JsonReader</code> TODO
 */
class JsonReader implements NodeReader {

    static final ImportProvider PROVIDER = new ImportProvider() {
        private JsonReader jsonReader;

        public NodeReader getReader() {
            if (jsonReader == null) {
                jsonReader = new JsonReader();
            }
            return jsonReader;
        }
    };
    
    public Node parse(InputStream ins) throws IOException {
        try {
            String jsonString = toString(ins).trim();
            if (!jsonString.startsWith("{")) {
                jsonString = "{" + jsonString + "}";
            }

            JSONObject json = new JSONObject(jsonString);
            String name = json.optString("name", null); // allow for no name !
            return this.createNode(name, json);

        } catch (JSONException je) {
            throw (IOException) new IOException(je.getMessage()).initCause(je);
        }
    }

    protected Node createNode(String name, JSONObject nodeDescriptor) throws JSONException {
        Node node = new Node();
        node.setName(name);

        Object primaryType = nodeDescriptor.opt("primaryNodeType");
        if (primaryType != null) {
            node.setPrimaryNodeType(String.valueOf(primaryType));
        }

        Object mixinsObject = nodeDescriptor.opt("mixinNodeTypes");
        if (mixinsObject instanceof JSONArray) {
            JSONArray mixins = (JSONArray) mixinsObject;
            for (int i=0; i < mixins.length(); i++) {
                node.addMixinNodeType(mixins.getString(i));
            }
        }

        Object propertiesObject = nodeDescriptor.opt("properties");
        if (propertiesObject instanceof JSONObject) {
            JSONObject properties = (JSONObject) propertiesObject;
            for (Iterator<String> pi=properties.keys(); pi.hasNext(); ) {
                String propName = pi.next();
                Property prop = this.createProperty(propName, properties.get(propName));
                node.addProperty(prop);
            }
        }

        Object nodesObject = nodeDescriptor.opt("nodes");
        if (nodesObject instanceof JSONArray) {
            JSONArray nodes = (JSONArray) nodesObject;
            for (int i=0; i < nodes.length(); i++) {
                Object entry = nodes.opt(i);
                if (entry instanceof JSONObject) {
                    JSONObject nodeObject = (JSONObject) entry;
                    String nodeName = nodeObject.optString("name", null);
                    if (nodeName == null) {
                        nodeName = "000000" + i;
                        nodeName = nodeName.substring(nodeName.length()-6);
                    }
                    Node child = this.createNode(nodeName, nodeObject);
                    node.addChild(child);
                }
            }
        }

        return node;
    }

    protected Property createProperty(String name, Object propDescriptorObject) throws JSONException {
        if (propDescriptorObject == null) {
            return null;
        }

        Property property = new Property();
        property.setName(name);

        Object value;
        String type;
        if (propDescriptorObject instanceof JSONObject) {
            JSONObject propDescriptor = (JSONObject) propDescriptorObject;

            value = propDescriptor.opt("value");
            if (value == null) {
                // check multivalue
                value = propDescriptor.opt("values");
                if (value == null) {
                    // missing value, ignore property
                    return null;
                }
            }

            Object typeObject = propDescriptor.opt("type");
            type = (typeObject != null)  ? String.valueOf(typeObject) : null;

        } else {
            value = propDescriptorObject;
            type = null;
        }

        // assume simple value
        if (value instanceof JSONArray) {
            // multivalue
            JSONArray array = (JSONArray) value;
            if (array.length() > 0) {
                for (int i=0; i < array.length(); i++) {
                    property.addValue(array.get(i));
                }
                value = array.opt(0);
            } else {
                property.addValue(null);
                value = null;
            }

        } else {
            // single value
            property.setValue(String.valueOf(value));
        }

        property.setType((type != null) ? type : this.getType(value));

        return property;
    }

    protected String getType(Object object) {
        if (object instanceof Double || object instanceof Float) {
            return PropertyType.TYPENAME_DOUBLE;
        } else if (object instanceof Number) {
            return PropertyType.TYPENAME_LONG;
        } else if (object instanceof Boolean) {
            return PropertyType.TYPENAME_BOOLEAN;
        }

        // fall back to default
        return PropertyType.TYPENAME_STRING;
    }
    
    private String toString(InputStream ins) throws IOException {
        if (!ins.markSupported()) {
            ins = new BufferedInputStream(ins);
        }
        
        String encoding;
        ins.mark(5);
        int c = ins.read();
        if (c == '#') {
            // character encoding following
            StringBuffer buf = new StringBuffer();
            for (c = ins.read(); !Character.isWhitespace((char) c); c = ins.read()) {
                buf.append((char) c);
            }
            encoding = buf.toString();
        } else {
            ins.reset();
            encoding = "UTF-8";
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int rd;
        while ( (rd = ins.read(buf)) >= 0) {
            bos.write(buf, 0, rd);
        }
        bos.close(); // just to comply with the contract
        
        return new String(bos.toByteArray(), encoding);
    }
}
