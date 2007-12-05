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
package org.apache.sling.microsling.helpers.json;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/** Dumps JCR Items as JSON data. The dump methods
 *  are threadsafe.
 */
public class JsonItemWriter {
    final Set<String> propertyNamesToIgnore;

    /** Create a JsonItemWriter
     *  @param propertyNamesToIgnore if not null, a property having a name from this
     *  set of values is ignored.
     *  TODO we should use a filtering interface to make the selection of which Nodes
     *  and Properties to dump more flexible.
     */
    public JsonItemWriter(Set<String> propertyNamesToIgnore) {
        this.propertyNamesToIgnore = propertyNamesToIgnore;
    }

    /** Dump all Nodes of given NodeIterator in JSON
     * @throws JSONException */
    public void dump(NodeIterator it, Writer out) throws RepositoryException, JSONException {
        final JSONWriter w = new JSONWriter(out);
        w.array();
        while (it.hasNext()) {
            dumpSingleNode(it.nextNode(), w, 1, 0);
        }
        w.endArray();
    }

    /** Dump given node in JSON, optionally recursing into its child nodes */
    public void dump(Node node, Writer w, int maxRecursionLevels) throws RepositoryException, JSONException {
        dump(node, new JSONWriter(w), 0, maxRecursionLevels);
    }

    /** Dump given node in JSON, optionally recursing into its child nodes */
    protected void dump(Node node, JSONWriter w, int currentRecursionLevel, int maxRecursionLevels)
    throws RepositoryException, JSONException {

        w.object();
        PropertyIterator props = node.getProperties();

        // the node's actual properties
        while (props.hasNext()) {
            Property prop = props.nextProperty();

            if (propertyNamesToIgnore!=null && propertyNamesToIgnore.contains(prop.getName())) {
                continue;
            }

            if (!prop.getDefinition().isMultiple()) {
                writeProperty(w, currentRecursionLevel, prop);
            } else {
                w.array();
                for(Value v : prop.getValues()) {
                    w.value(convertValue(v));
                }
                w.endArray();
            }
        }

        // the child nodes
        if(recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            final NodeIterator children = node.getNodes();
            while(children.hasNext()) {
                final Node n = children.nextNode();
                dumpSingleNode(n, w, currentRecursionLevel, maxRecursionLevels);
            }
        }

        w.endObject();
    }

    /** Dump a single node */
    protected void dumpSingleNode(Node n, JSONWriter w, int currentRecursionLevel, int maxRecursionLevels)
    throws RepositoryException, JSONException {
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            w.key(n.getName());
            dump(n, w, currentRecursionLevel + 1, maxRecursionLevels);
        }
    }
    
    /** true if the current recursion level is active */
    protected boolean recursionLevelActive(int currentRecursionLevel, int maxRecursionLevels) {
        return currentRecursionLevel < maxRecursionLevels;
    }

    /**
     * Write a single property
     */
    protected void writeProperty(JSONWriter w, int indent, Property p)
    throws ValueFormatException, RepositoryException, JSONException {
        if(p.getType() == PropertyType.BINARY) {
            // TODO for now we mark binary properties with an initial star in their name
            // (star is not allowed as a JCR property name)
            // in the name, and the value should be the size of the binary data
            w.key("*" + p.getName());

        } else {
            w.key(p.getName());
        }

        w.value(convertValue(p.getValue()));
    }

    /** Convert a Value for JSON output */
    protected Object convertValue(Value v) throws ValueFormatException, IllegalStateException, RepositoryException {
        if(v.getType() == PropertyType.BINARY) {
            // TODO return the binary size
            return new Integer(0);

        } else if(v.getType() == PropertyType.DATE) {
            final DateFormat fmt = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
            return fmt.format(v.getDate().getTime());

        } else {
            return v.getString();
        }
    }
}