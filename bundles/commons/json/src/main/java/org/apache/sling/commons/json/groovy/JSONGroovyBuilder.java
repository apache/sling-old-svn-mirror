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
package org.apache.sling.commons.json.groovy;

import groovy.lang.Closure;
import groovy.util.BuilderSupport;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Groovy builder for JSON values.
 *
 * Based on code written by Andres Almiray <aalmiray@users.sourceforge.net> as
 * part of the json-lib project - http://json-lib.sourceforge.net/
 */
public class JSONGroovyBuilder extends BuilderSupport {

    /**
     * The string 'json' which indicates that the root node should be used
     * as-is. Otherwise, the root node is wrapped in a JSON object.
     */
    private static final String JSON = "json";

    /**
     * A logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(JSONGroovyBuilder.class);

    /**
     * A stack containing the names of created nodes.
     */
    protected Stack<String> nodeNames = new Stack<String>();

    private void addFromMap(JSONObject obj, Map attributes) {
        for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            Object value = resolveValue(attributes.get(key));

            try {
                obj.put(key, value);
            } catch (JSONException e) {
            }

        }
    }

    private Object resolveValue(Object value) {
        if (value instanceof Map) {
            JSONObject sub = new JSONObject();
            addFromMap(sub, (Map) value);
            value = sub;
        } else if (value instanceof Closure) {
            Object oldCurrent = getCurrent();
            JSONObject sub = new JSONObject();
            setCurrent(sub);
            Closure c = (Closure) value;
            c.setDelegate(this);
            c.call();
            setCurrent(oldCurrent);
            value = sub;
        } else if (value instanceof List) {
            value = new JSONArray((List) value);
        }

        return value;
    }

    /**
     * Create a node with the specified name.
     */
    @Override
    protected Object createNode(Object name) {
        nodeNames.push((String) name);
        return new JSONObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createNode(Object name, Map attributes) {
        nodeNames.push((String) name);
        JSONObject obj = new JSONObject();
        addFromMap(obj, attributes);
        return obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        nodeNames.push((String) name);
        if (value instanceof Map) {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject(attributes));
            arr.put(new JSONObject((Map) value));
            return arr;
        } else {
            nodeNames.pop();
            logger.warn("Unhandled createNode(O,M,O). Only able to handle cases where value Object is a Map. name={}, value={}",
                    new Object[] { name, value });
            return null;
        }
    }

    @Override
    protected Object createNode(Object name, Object value) {
        nodeNames.push((String) name);
        if (value instanceof List) {
            JSONArray arr = new JSONArray();
            for (Object obj : (List) value) {
                arr.put(resolveValue(obj));
            }
            return arr;
        } else if (value instanceof Closure) {
            Object oldCurrent = getCurrent();
            JSONObject obj = new JSONObject();
            setCurrent(obj);
            Closure c = (Closure) value;
            c.setDelegate(this);
            c.call();
            setCurrent(oldCurrent);
            return obj;
        } else if (value instanceof String) {
            return value;
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof Boolean) {
            return value;
        } else {
            System.err.println("Unhandlable class: " + value.getClass());
            nodeNames.pop();
            return null;
        }
    }

    /**
     * Add the child node to the parent, using the name at the top of the node
     * names stack.
     *
     * @param parent the parent node
     * @param child the child node
     */
    @Override
    protected void setParent(Object parent, Object child) {
        if (!nodeNames.isEmpty()) {
            try {
                ((JSONObject) parent).accumulate(nodeNames.pop(), child);
            } catch (JSONException e) {
            }
        }
    }

    /**
     * On the completion of the top-level node, if the node name isn't 'json',
     * create a container object.
     *
     * @param parent the parent node
     * @param child the node which was just created
     */
    @Override
    protected Object postNodeCompletion(Object parent, Object node) {
        if (parent == null && !nodeNames.empty()) {
            String rootName = nodeNames.pop();
            if (!JSON.equals(rootName)) {
                JSONObject obj = new JSONObject();
                try {
                    return obj.put(rootName, node);
                } catch (JSONException e) {
                    logger.error("Unable to create container JSON Object", e);
                    return super.postNodeCompletion(parent, node);
                }
            } else {
                return super.postNodeCompletion(parent, node);
            }
        } else {
            return super.postNodeCompletion(parent, node);
        }
    }

    protected boolean log = false;

}
