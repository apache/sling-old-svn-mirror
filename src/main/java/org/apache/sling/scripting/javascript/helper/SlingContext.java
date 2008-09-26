/*
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
 */
package org.apache.sling.scripting.javascript.helper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;

/**
 * The <code>SlingContext</code> extends Context to overwrite the
 * {@link #initStandardObjects(ScriptableObject, boolean)} method to add more
 * standard objects.
 */
public class SlingContext extends Context {

    @Override
    public ScriptableObject initStandardObjects(ScriptableObject scope,
            boolean sealed) {
        ScriptableObject rootScope = super.initStandardObjects(scope, sealed);

        // prepare the ImporterToplevel host object because it will be
        // used as top level scope for the RhinoJavaScriptEngine but is
        // not initialized with the rest of the standard objects
        ImporterTopLevel.init(this, rootScope, sealed);
        
        // add Sling global objects
        SlingGlobal.init(rootScope, sealed);

        return rootScope;
    }
}
