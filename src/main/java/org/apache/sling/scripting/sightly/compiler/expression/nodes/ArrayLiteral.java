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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * Syntactical structure for an array of items.
 */
public final class ArrayLiteral implements ExpressionNode {

    private final List<ExpressionNode> items;

    /**
     * Creates an array from a list of {@link ExpressionNode} elements.
     *
     * @param items the list of {@link ExpressionNode} elements
     */
    public ArrayLiteral(List<ExpressionNode> items) {
        this.items = new ArrayList<>(items);
    }

    /**
     * Returns an unmodifiable {@link List} containing the array's elements.
     *
     * @return an unmodifiable {@link List}
     */
    public List<ExpressionNode> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }
}
