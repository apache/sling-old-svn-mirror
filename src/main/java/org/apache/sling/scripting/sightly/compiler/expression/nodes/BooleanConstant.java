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
 * Defines a Boolean constant (e.g. "true" or "false").
 */
public final class BooleanConstant implements Atom {

    private String rawText;

    public static final BooleanConstant FALSE = new BooleanConstant(Boolean.toString(false));
    public static final BooleanConstant TRUE = new BooleanConstant(Boolean.toString(true));

    /**
     * Creates a boolean constant from a raw string.
     *
     * @param text the raw string
     */
    public BooleanConstant(String text) {
        this.rawText = text;
    }

    /**
     * Creates a boolean constant from a boolean value.
     *
     * @param value the value from which this constant is created
     */
    public BooleanConstant(boolean value) {
        this(Boolean.toString(value));
    }

    /**
     * Returns the boolean value of the constant.
     *
     * @return the boolean value of the constant
     */
    public boolean getValue() {
        return Boolean.parseBoolean(rawText);
    }

    @Override
    public String getText() {
        return rawText;
    }


    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    @Override
    public String toString() {
        return "BooleanConstant{" +
                "rawText='" + rawText + '\'' +
                '}';
    }

}
