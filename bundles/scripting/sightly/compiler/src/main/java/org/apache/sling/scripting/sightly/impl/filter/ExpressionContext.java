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
 * Defines a context for the {@link Expression} that will be processed by a {@link Filter}. The context can then be used by filters to
 * further enhance the decision mechanism for their processing.
 */
public enum ExpressionContext {

    // Plugin contexts
    PLUGIN_DATA_SLY_USE,
    PLUGIN_DATA_SLY_TEXT,
    PLUGIN_DATA_SLY_ATTRIBUTE,
    PLUGIN_DATA_SLY_ELEMENT,
    PLUGIN_DATA_SLY_TEST,
    PLUGIN_DATA_SLY_LIST,
    PLUGIN_DATA_SLY_REPEAT,
    PLUGIN_DATA_SLY_INCLUDE,
    PLUGIN_DATA_SLY_RESOURCE,
    PLUGIN_DATA_SLY_TEMPLATE,
    PLUGIN_DATA_SLY_CALL,
    PLUGIN_DATA_SLY_UNWRAP,

    // Markup contexts
    ELEMENT,
    TEXT,
    ATTRIBUTE;

    private static final String PLUGIN_PREFIX = "PLUGIN_DATA_SLY_";

    /**
     * Retrieves the context for the plugin specified by {@code pluginName}.
     *
     * @param pluginName the name of the plugin for which to retrieve the context
     * @return the context
     * @throws IllegalArgumentException if the plugin identified by {@code pluginName} doesn't have a context associated
     */
    public static ExpressionContext getContextForPlugin(String pluginName) {
        return valueOf(PLUGIN_PREFIX + pluginName.toUpperCase());
    }

}
