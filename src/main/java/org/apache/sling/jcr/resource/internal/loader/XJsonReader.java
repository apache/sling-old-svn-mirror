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

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class XJsonReader extends JsonReader {

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
        private XJsonReader xjsonReader;

        public NodeReader getReader() {
            if (xjsonReader == null) {
                xjsonReader = new XJsonReader();
            }
            return xjsonReader;
        }
    };
    
    protected Node createNode(String name, JSONObject obj) throws JSONException {
        Node node = new Node();
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
                    Node child = this.createNode(n, (JSONObject) o);
                    node.addChild(child);
                } else if (o instanceof JSONArray) {
                    Property prop = createProperty(n, o);
                    node.addProperty(prop);
                } else {
                    Property prop = createProperty(n, o);
                    node.addProperty(prop);
                }
            }
        }
        return node;
    }

    protected Property createProperty(String name, Object value)
            throws JSONException {
        Property property = new Property();
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

}