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

import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * Defines a simple string constant (e.g. {@code 'hello world'}).
 */
public final class StringConstant implements Atom {

    private String text;

    /**
     * The empty string constant.
     */
    public static final StringConstant EMPTY = new StringConstant("");

    /**
     * Create a string constant node.
     *
     * @param text the string content (without its original quotes)
     */
    public StringConstant(String text) {
        this.text = text;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    /**
     * Gets the string content
     *
     * @return the string content
     */
    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "StringConstant{" +
                "text='" + text + '\'' +
                '}';
    }

}
