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
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;

import javax.jcr.RepositoryException;

/**
 * Specific <code>JsonReader</code>, <code>OrderedJsonReader</code> parse json document exactly the same,
 * but does specific look up for SLING:ordered : [{SLING:name: "first", ...},{SLING:name: "second", ...}]
 * type of structure that will force import of an orderable node with first & second
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
    protected void writeChildren(JSONObject obj, ContentCreator contentCreator) throws JSONException, RepositoryException {
        if (! obj.has(PN_ORDEREDCHILDREN)) {
            super.writeChildren(obj, contentCreator);
        } else {
            JSONArray names = obj.names();
            for (int i = 0; names != null && i < names.length(); i++) {
                final String n = names.getString(i);
                // skip well known objects
                if (!ignoredNames.contains(n)) {
                    Object o = obj.get(n);
                    if (!handleSecurity(n, o, contentCreator)) {
                        if (n.equals(PN_ORDEREDCHILDREN)) {
                            if (o instanceof JSONArray) {
                                JSONArray children = (JSONArray) o;
                                for (int childIndex = 0; childIndex < children.length(); childIndex++) {
                                    Object oc = children.get(childIndex);
                                    if (oc instanceof JSONObject) {
                                        JSONObject child = (JSONObject) oc;
                                        String childName = child.optString(PN_ORDEREDCHILDNAME);
                                        if (StringUtils.isNotBlank(childName)) {
                                            child.remove(PN_ORDEREDCHILDNAME);
                                            this.createNode(childName, child, contentCreator);
                                        } else {
                                            throw new JSONException(PN_ORDEREDCHILDREN + " children must have a name whose key is " + PN_ORDEREDCHILDNAME);
                                        }
                                    } else {
                                        throw new JSONException(PN_ORDEREDCHILDREN + " array must only have JSONObject items");
                                    }

                                }
                            } else {
                                throw new JSONException(PN_ORDEREDCHILDREN + " value must be a JSON array");
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
