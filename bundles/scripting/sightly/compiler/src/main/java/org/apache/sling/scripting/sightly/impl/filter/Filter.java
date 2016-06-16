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
package org.apache.sling.scripting.sightly.impl.filter;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;

/**
 * A filter is a transformation which performs modifications on expressions. Unlike plugins, filters are always applied on an expression.
 * Whether the filter transformation is actually necessary is decided by the filter. The application order of filters is given by filter
 * priority.
 */
public interface Filter extends Comparable<Filter> {

    /**
     * Transform the given expression
     *
     * @param expression        the original expression
     * @param expressionContext the expression's context
     * @return a transformed expression. If the filter is not applicable
     * to the given expression, then the original expression shall be returned
     */
    Expression apply(Expression expression, ExpressionContext expressionContext);

    /**
     * The priority with which filters are applied. This establishes order between filters. Filters with
     * lower priority are applied first.
     *
     * @return an integer representing the filter's priority
     */
    int priority();

}
