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
package org.apache.sling.scripting.sightly.impl.compiler;

import java.util.Arrays;

import org.apache.sling.scripting.sightly.impl.plugin.PluginCallInfo;

/**
 * HTL Syntax specific elements.
 */
public class Syntax {

    public static final String SLY_COMMENT_PREFIX = "<!--/*";

    public static final String SLY_COMMENT_SUFFIX = "*/-->";

    public static final String PLUGIN_ATTRIBUTE_PREFIX = "data-sly-";

    public static final String DEFAULT_LIST_ITEM_VAR_NAME = "item";

    public static final String ITEM_LOOP_STATUS_SUFFIX = "List";

    public static final String CONTEXT_OPTION = "context";

    /**
     * Checks whether a piece of text represents a HTL comment
     * @param text - the text
     * @return - true if it is a HTL comment, false otherwise
     */
    public static boolean isSightlyComment(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith(SLY_COMMENT_PREFIX) && trimmed.endsWith(SLY_COMMENT_SUFFIX);
    }

    public static boolean isPluginAttribute(String attributeName) {
        return attributeName.startsWith(PLUGIN_ATTRIBUTE_PREFIX);
    }

    public static PluginCallInfo parsePluginAttribute(String attributeName) {
        if (!isPluginAttribute(attributeName)) {
            return null;
        }
        String fragment = attributeName.substring(PLUGIN_ATTRIBUTE_PREFIX.length());
        String[] parts = fragment.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        return new PluginCallInfo(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }

    public static String itemLoopStatusVariable(String itemVariable) {
        return itemVariable + ITEM_LOOP_STATUS_SUFFIX;
    }

}
