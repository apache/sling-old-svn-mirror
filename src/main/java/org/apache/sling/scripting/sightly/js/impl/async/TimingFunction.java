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
package org.apache.sling.scripting.sightly.js.impl.async;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.apache.sling.scripting.sightly.js.impl.loop.EventLoopInterop;
import org.apache.sling.scripting.sightly.js.impl.rhino.JsUtils;

/**
 * Timing function for JS scripts that use async constructs
 */
public final class TimingFunction extends BaseFunction {

    public static final TimingFunction INSTANCE = new TimingFunction();

    private TimingFunction() {
    }

    @Override
    public Object call(final Context cx, final Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            return Context.getUndefinedValue();
        }
        if (!(args[0] instanceof Function)) {
            throw new IllegalArgumentException("Timing function must receive a function as the first argument");
        }
        final Function function = (Function) args[0];
        return EventLoopInterop.schedule(cx, new Runnable() {
            @Override
            public void run() {
                JsUtils.callFn(function, cx, scope, null, new Object[0]);
            }
        });
    }
}
