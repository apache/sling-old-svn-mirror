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
package org.apache.sling.jcr.contentloader.internal.readers;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Specific <code>JsonReader</code>, <code>OrderedJsonReader</code> parse json document exactly the same,
 * but does specific look up for SLING:ordered : [{SLING:name: "first", ...},{SLING:name: "second", ...}]
 * type of structure that will force import of an orderable node with first and second
 * children, in that order.
 * Note that this is the reponsability of the json file to set appropriate node type / mixins.
 */
@Component(inherit = false)
@Service
@Properties({
        @Property(name = ContentReader.PROPERTY_EXTENSIONS, value = "ordered-json"),
        @Property(name = ContentReader.PROPERTY_TYPES, value = "application/json")
})
public class OrderedJsonReader extends JsonReader {

    private static final String PN_ORDEREDCHILDREN = "SLING:ordered";
    private static final String PN_ORDEREDCHILDNAME = "SLING:name";

    @Override
    protected void writeChildren(JsonObject obj, ContentCreator contentCreator) throws RepositoryException {
        if (! obj.containsKey(PN_ORDEREDCHILDREN)) {
            super.writeChildren(obj, contentCreator);
        } else {
            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                final String n = entry.getKey();
                // skip well known objects
                if (!ignoredNames.contains(n)) {
                    Object o = entry.getValue();
                    if (!handleSecurity(n, o, contentCreator)) {
                        if (n.equals(PN_ORDEREDCHILDREN)) {
                            if (o instanceof JsonArray) {
                                JsonArray children = (JsonArray) o;
                                for (int childIndex = 0; childIndex < children.size(); childIndex++) {
                                    Object oc = children.get(childIndex);
                                    if (oc instanceof JsonObject) {
                                        JsonObject child = (JsonObject) oc;
                                        String childName = child.getString(PN_ORDEREDCHILDNAME, null);
                                        if (StringUtils.isNotBlank(childName)) {
                                            JsonObjectBuilder builder = Json.createObjectBuilder();
                                            for (Map.Entry<String, JsonValue> e : child.entrySet())
                                            {
                                                if (!PN_ORDEREDCHILDNAME.equals(e.getKey()))
                                                {
                                                    builder.add(e.getKey(), e.getValue());
                                                }
                                            }
                                            child = builder.build();
                                            this.createNode(childName, child, contentCreator);
                                        } else {
                                            throw new JsonException(PN_ORDEREDCHILDREN + " children must have a name whose key is " + PN_ORDEREDCHILDNAME);
                                        }
                                    } else {
                                        throw new JsonException(PN_ORDEREDCHILDREN + " array must only have JSONObject items");
                                    }

                                }
                            } else {
                                throw new JsonException(PN_ORDEREDCHILDREN + " value must be a JSON array");
                            }
                        }
                    } else {
                        this.createProperty(n, o, contentCreator);
                    }
                }
            }
        }
    }
}
