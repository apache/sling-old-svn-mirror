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
package org.apache.sling.scripting.sightly.js.impl;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;

/**
 * Utilities for script evaluation
 */
public class Utils {
    public static final String JS_EXTENSION = "js";

    public static final Bindings EMPTY_BINDINGS = new SimpleBindings(Collections.<String, Object>emptyMap());

    public static SlingScriptHelper getHelper(Bindings bindings) {
        return (SlingScriptHelper) bindings.get(SlingBindings.SLING);
    }

    public static boolean isJsScript(String identifier) {
        String extension = StringUtils.substringAfterLast(identifier, ".");
        return JS_EXTENSION.equalsIgnoreCase(extension);
    }

}
