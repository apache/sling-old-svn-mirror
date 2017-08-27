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
package org.apache.sling.bundleresource.impl.url;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import org.apache.sling.api.resource.ResourceUtil;

public class ResourceURLStreamHandler extends URLStreamHandler {

    private static final Map<String, String> contents = new HashMap<>();

    private static final Map<String, List<String>> parentChild = new HashMap<>();

    public static void addContents(final String path, final String c) {
        contents.put(path, c);
        final String parent = ResourceUtil.getParent(path).concat("/");
        List<String> children = parentChild.get(parent);
        if ( children == null ) {
            children = new ArrayList<>();
            parentChild.put(parent, children);
        }
        children.add(path);
    }

    public static  Map<String, List<String>> getParentChildRelationship() {
        return parentChild;
    }

    public static void addJSON(final String path, final Map<String, Object> props) {
        final StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(build(props));
        addContents(path, writer.toString());
    }

    public static void reset() {
        contents.clear();
        parentChild.clear();
    }


    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        final ResourceURLConnection conn = new ResourceURLConnection(url, contents.get(url.getPath()));
        conn.connect();
        return conn;
    }

    private static JsonStructure build(final Object value) {
        if ( value instanceof List ) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>)value;
            final JsonArrayBuilder builder = Json.createArrayBuilder();
            for(final Object obj : list) {
                if ( obj instanceof String ) {
                    builder.add(obj.toString());
                } else if ( obj instanceof Long ) {
                    builder.add((Long)obj);
                } else if ( obj instanceof Double ) {
                    builder.add((Double)obj);
                } else if (obj instanceof Boolean ) {
                    builder.add((Boolean)obj);
                } else if ( obj instanceof Map ) {
                    builder.add(build(obj));
                } else if ( obj instanceof List ) {
                    builder.add(build(obj));
                }

            }
            return builder.build();
        } else if ( value instanceof Map ) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>)value;
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            for(final Map.Entry<String, Object> entry : map.entrySet()) {
                if ( entry.getValue() instanceof String ) {
                    builder.add(entry.getKey(), entry.getValue().toString());
                } else if ( entry.getValue() instanceof Long ) {
                    builder.add(entry.getKey(), (Long)entry.getValue());
                } else if ( entry.getValue() instanceof Double ) {
                    builder.add(entry.getKey(), (Double)entry.getValue());
                } else if ( entry.getValue() instanceof Boolean ) {
                    builder.add(entry.getKey(), (Boolean)entry.getValue());
                } else if ( entry.getValue() instanceof Map ) {
                    builder.add(entry.getKey(), build(entry.getValue()));
                } else if ( entry.getValue() instanceof List ) {
                    builder.add(entry.getKey(), build(entry.getValue()));
                }
            }
            return builder.build();
        }
        return null;
    }
}

