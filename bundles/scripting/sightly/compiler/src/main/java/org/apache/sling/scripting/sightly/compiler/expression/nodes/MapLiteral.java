/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * Defines a syntactical construction representing a map.
 */
public final class MapLiteral implements ExpressionNode {

    private Map<String, ExpressionNode> map;

    /**
     * Creates a map representation.
     *
     * @param map the backing {@link ExpressionNode} map
     */
    public MapLiteral(Map<String, ExpressionNode> map) {
        this.map = new HashMap<>();
        this.map.putAll(map);
    }

    /**
     * Returns an unmodifiable view of the backing map.
     *
     * @return an unmodifiable view of the backing map
     */
    public Map<String, ExpressionNode> getMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns an {@link ExpressionNode} from the backing map.
     *
     * @param key the key under which the node is stored
     * @return the node, if one is stored under that key; {@code null} otherwise
     */
    public ExpressionNode getValue(String key) {
        return map.get(key);
    }

    /**
     * Checks if the map contains the property identified by the passed property name.
     *
     * @param name the property name
     * @return {@code true} if the map contains the property, {@code false} otherwise
     */
    public boolean containsKey(String name) {
        return map.containsKey(name);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    @Override
    public String toString() {
        return "MapLiteral{" +
                "map=" + map +
                '}';
    }
}
