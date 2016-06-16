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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

public abstract class AbstractFilter implements Filter {

    protected int priority = 100;

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(Filter o) {
        if (this.priority < o.priority()) {
            return -1;
        } else if (this.priority == o.priority()) {
            return 0;
        }
        return 1;
    }

    /**
     * Collects the options passed in the {@code options} array into a new map while removing them from the original expression.
     *
     * @param expression the expression providing the options to be processed
     * @param options    the options of interest for the {@link Filter}
     * @return a map with the retrieved options; the map can be empty if none of the options were found
     */
    protected Map<String, ExpressionNode> getFilterOptions(Expression expression, String... options) {
        Map<String, ExpressionNode> collector = new HashMap<>();
        for (String option : options) {
            ExpressionNode optionNode = expression.removeOption(option);
            if (optionNode != null) {
                collector.put(option, optionNode);
            }
        }
        return collector;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }
}
