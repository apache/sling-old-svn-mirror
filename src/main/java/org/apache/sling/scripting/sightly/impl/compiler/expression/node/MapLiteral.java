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
package org.apache.sling.scripting.sightly.impl.compiler.expression.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.NodeVisitor;

/**
 * A syntactical construction representing a map
 */
public class MapLiteral implements ExpressionNode {

    private Map<String, ExpressionNode> map;

    public static final MapLiteral EMPTY = new MapLiteral(new HashMap<String, ExpressionNode>());

    public MapLiteral(Map<String, ExpressionNode> map) {
        this.map = new HashMap<String, ExpressionNode>();
        this.map.putAll(map);
    }

    public Map<String, ExpressionNode> getMap() {
        return Collections.unmodifiableMap(map);
    }

    public ExpressionNode getValue(String property) {
        return map.get(property);
    }

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
