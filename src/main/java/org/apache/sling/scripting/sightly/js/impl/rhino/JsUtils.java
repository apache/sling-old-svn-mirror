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
package org.apache.sling.scripting.sightly.js.impl.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Utilities when inter-operating with JS scripts
 */
public class JsUtils {

    public static Object callFn(Function function, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        boolean exitContext = false;
        if (Context.getCurrentContext() == null) {
            Context.enter();
            exitContext = true;
        }
        Context context = (cx == null) ? Context.getCurrentContext() : cx;
        Object result = function.call(context, scope, thisObj, args);
        if (exitContext) {
            Context.exit();
        }
        return result;
    }



}
