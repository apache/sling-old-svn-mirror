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

import org.apache.sling.scripting.sightly.SightlyException;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.apache.sling.scripting.sightly.js.impl.loop.EventLoopInterop;

/**
 *
 */
public class AsyncExtractor {

    public static final String THEN_METHOD = "then";

    public void extract(Object jsObj, UnaryCallback unaryCallback) {
        if (!isPromise(jsObj)) {
            unaryCallback.invoke(jsObj);
        }
        if (jsObj instanceof AsyncContainer) {
            ((AsyncContainer) jsObj).addListener(unaryCallback);
        }
        if (jsObj instanceof ScriptableObject) {
            ScriptableObject scriptableObject = (ScriptableObject) jsObj;
            decodeJSPromise(scriptableObject, unaryCallback);
        }
    }

    private void decodeJSPromise(final Scriptable promise, final UnaryCallback callback) {
        try {
            Context context = Context.enter();
            final AsyncContainer errorContainer = new AsyncContainer();
            final Function errorHandler = createErrorHandler(errorContainer);
            final Function successHandler = convertCallback(callback);
            EventLoopInterop.schedule(context, new Runnable() {
                @Override
                public void run() {
                    ScriptableObject.callMethod(promise, THEN_METHOD,
                            new Object[] {successHandler, errorHandler});
                }
            });
            if (errorContainer.isCompleted()) {
                throw new SightlyException("Promise has completed with failure: " + Context.toString(errorContainer.getResult()));
            }
        } finally {
            Context.exit();
        }
    }

    private Function createErrorHandler(AsyncContainer asyncContainer) {
        return convertCallback(asyncContainer.createCompletionCallback());
    }

    public boolean isPromise(Object jsObj) {
        if (jsObj instanceof AsyncContainer) {
            return true;
        }
        if (jsObj instanceof ScriptableObject) {
            Scriptable scriptable = (Scriptable) jsObj;
            return ScriptableObject.hasProperty(scriptable, THEN_METHOD);
        }
        return false;
    }

    private static Function convertCallback(final UnaryCallback unaryCallback) {
        return new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                Object arg = (args.length == 0) ? Context.getUndefinedValue() : args[0];
                unaryCallback.invoke(arg);
                return Context.getUndefinedValue();
            }
        };
    }
}
