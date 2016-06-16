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
 * Defines a numeric constant expression (e.g. "42.1").
 */
public final class NumericConstant implements Atom {

    public static final NumericConstant ZERO = new NumericConstant(0);
    public static final NumericConstant ONE = new NumericConstant(1);
    public static final NumericConstant TWO = new NumericConstant(2);

    private final String text;
    private final Number value;

    /**
     * Creates a numeric constant.
     *
     * @param text the text representation
     * @throws java.lang.NumberFormatException if the text is not in a numeric format
     */
    public NumericConstant(String text) {
        this.text = text;
        this.value = parseNumber(text);
    }

    /**
     * Creates a numeric constant based on a {@link Number} representation.
     *
     * @param value the number representation
     */
    public NumericConstant(Number value) {
        this.value = value.longValue();
        this.text = value.toString();
    }

    /**
     * Returns the number representation of this constant.
     *
     * @return the number representation of this constant
     */
    public Number getValue() {
        return value;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    @Override
    public String toString() {
        return "NumericConstant{" +
                "text='" + text + '\'' +
                '}';
    }

    private Number parseNumber(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return Double.parseDouble(s);
        }
    }

}
