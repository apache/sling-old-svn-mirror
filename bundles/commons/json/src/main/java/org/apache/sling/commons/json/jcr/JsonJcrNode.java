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
package org.apache.sling.commons.json.jcr;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * This class makes it easy to create a JSON object out of a JCR node. It is a shameless copy of {@link JsonItemWriter},
 * but instead of writing the resulting JSON directly to an output, you get a JSONObject that you can deal with.
 *
 * @author vidar@idium.no
 * @since Apr 17, 2009 6:55:30 PM
 */
public class JsonJcrNode extends JSONObject {

    private Node node;
    private Set<String> propertyNamesToIgnore;

    /**
     * Creates a JSONObject out of <code>node</code>. All <code>node</code>'s properties will be reflected in the JSON
     * object. In addition, properties <code>jcr:path</code> and <code>jcr:name</code> are added. Their values are
     * those returned by <code>node.getPath()</code> and <code>node.getName()</code>, respectively.
     * @param node The JCR node to use
     * @throws JSONException If there's a problem generating the JSON object
     * @throws RepositoryException If there's a problem reading data from the JCR repository
     */
    public JsonJcrNode(Node node) throws JSONException, RepositoryException {
        this(node, null);
    }

    /**
     * Creates a <code>JSONObject</code> out of <code>node</code>. All <code>node</code>'s properties will be reflected
     * in the JSON object, except those in <code>propertyNamesToIgnore</code>. In addition, properties
     * <code>jcr:path</code> and <code>jcr:name</code> are added. Their values are those returned by
     * <code>node.getPath()</code> and <code>node.getName()</code>, respectively.
     * @param node The JCR node to use
     * @param propertyNamesToIgnore A set of property names that should <em>not</em> be reflected in the resulting
     * JSON object.
     * @throws JSONException If there's a problem generating the JSON object
     * @throws RepositoryException If there's a problem reading data from the JCR repository
     */
    public JsonJcrNode(Node node, Set<String> propertyNamesToIgnore) throws JSONException, RepositoryException {
        this.node = node;
        this.propertyNamesToIgnore = propertyNamesToIgnore;
        this.populate();
    }

    private void populate() throws JSONException, RepositoryException {
        PropertyIterator properties = node.getProperties();
        addNative("jcr:path", node.getPath());
        addNative("jcr:name", node.getName());
        while (properties.hasNext()) {
            Property prop = properties.nextProperty();
            String name = prop.getName();
            if (propertyNamesToIgnore != null && propertyNamesToIgnore.contains(name)) {
                continue;
            }
            addProperty(prop);
        }
    }

    private void addNative(String key, String value) throws JSONException, RepositoryException {
        if (propertyNamesToIgnore == null || !propertyNamesToIgnore.contains(key)) {
            this.put(key, value);
        }
    }

    protected void addProperty(Property p)
            throws ValueFormatException, RepositoryException, JSONException {
        // special handling for binaries: we dump the length and not the length
        if (p.getType() == PropertyType.BINARY) {
            // TODO for now we mark binary properties with an initial colon in
            // their name
            // (colon is not allowed as a JCR property name)
            // in the name, and the value should be the size of the binary data
            String key = ":" + p.getName();
            if (!p.isMultiple()) {
                this.put(key, p.getLength());
            } else {
                final long[] sizes = p.getLengths();
                List<Long> list = new ArrayList<Long>();
                for (long value : sizes) {
                    list.add(value);
                }
                this.put(key, list);
            }
        } else {
            String key = p.getName();

            if (!p.isMultiple()) {
                addValue(key, p.getValue());
            } else {
                for (Value v : p.getValues()) {
                    addValue(key, v);
                }
            }
        }
    }

    protected void addValue(String key, Value v) throws IllegalStateException, RepositoryException, JSONException {

        switch (v.getType()) {
            case PropertyType.BINARY:
                this.accumulate(key, 0);
                break;

            case PropertyType.DATE:
                this.accumulate(key, JsonItemWriter.format(v.getDate()));
                break;

            case PropertyType.BOOLEAN:
                this.accumulate(key, v.getBoolean());
                break;

            case PropertyType.LONG:
                this.accumulate(key, v.getLong());
                break;

            case PropertyType.DOUBLE:
                this.accumulate(key, v.getDouble());
                break;
            default:
                this.accumulate(key, v.getString());
        }
    }


}
