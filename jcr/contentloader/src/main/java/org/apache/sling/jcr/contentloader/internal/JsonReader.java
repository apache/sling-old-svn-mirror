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
package org.apache.sling.jcr.contentloader.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;


/**
 * The <code>JsonReader</code> TODO
 */
class JsonReader implements NodeReader {

    private static final Set<String> ignoredNames = new HashSet<String>();
    static {
        ignoredNames.add("jcr:primaryType");
        ignoredNames.add("jcr:mixinTypes");
        ignoredNames.add("jcr:uuid");
        ignoredNames.add("jcr:baseVersion");
        ignoredNames.add("jcr:predecessors");
        ignoredNames.add("jcr:successors");
        ignoredNames.add("jcr:checkedOut");
        ignoredNames.add("jcr:created");
    }

    static final ImportProvider PROVIDER = new ImportProvider() {
        private JsonReader jsonReader;

        public NodeReader getReader() {
            if (jsonReader == null) {
                jsonReader = new JsonReader();
            }
            return jsonReader;
        }
    };

    public NodeDescription parse(InputStream ins) throws IOException {
        try {
            String jsonString = toString(ins).trim();
            if (!jsonString.startsWith("{")) {
                jsonString = "{" + jsonString + "}";
            }

            JSONObject json = new JSONObject(jsonString);
            return this.createNode(null, json);

        } catch (JSONException je) {
            throw (IOException) new IOException(je.getMessage()).initCause(je);
        }
    }

    protected NodeDescription createNode(String name, JSONObject obj) throws JSONException {
        NodeDescription node = new NodeDescription();
        node.setName(name);

        Object primaryType = obj.opt("jcr:primaryType");
        if (primaryType != null) {
            node.setPrimaryNodeType(String.valueOf(primaryType));
        }

        Object mixinsObject = obj.opt("jcr:mixinTypes");
        if (mixinsObject instanceof JSONArray) {
            JSONArray mixins = (JSONArray) mixinsObject;
            for (int i = 0; i < mixins.length(); i++) {
                node.addMixinNodeType(mixins.getString(i));
            }
        }

        // add properties and nodes
        JSONArray names = obj.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String n = names.getString(i);
            // skip well known objects
            if (!ignoredNames.contains(n)) {
                Object o = obj.get(n);
                if (o instanceof JSONObject) {
                    NodeDescription child = this.createNode(n, (JSONObject) o);
                    node.addChild(child);
                } else if (o instanceof JSONArray) {
                    PropertyDescription prop = createProperty(n, o);
                    node.addProperty(prop);
                } else {
                    PropertyDescription prop = createProperty(n, o);
                    node.addProperty(prop);
                }
            }
        }
        return node;
    }

    protected PropertyDescription createProperty(String name, Object value)
            throws JSONException {
        PropertyDescription property = new PropertyDescription();
        property.setName(name);

        // assume simple value
        if (value instanceof JSONArray) {
            // multivalue
            JSONArray array = (JSONArray) value;
            if (array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
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
        // set type
        property.setType(getType(value));

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
