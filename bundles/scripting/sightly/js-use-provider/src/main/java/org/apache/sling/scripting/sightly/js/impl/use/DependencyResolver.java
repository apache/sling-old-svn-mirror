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

package org.apache.sling.scripting.sightly.js.impl.use;

import javax.script.Bindings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.js.impl.JsEnvironment;
import org.apache.sling.scripting.sightly.js.impl.Utils;
import org.apache.sling.scripting.sightly.js.impl.async.UnaryCallback;

/**
 * Resolves dependencies specified by the Use function
 */
public class DependencyResolver {

    private final Resource caller;
    private final JsEnvironment jsEnvironment;
    private final Bindings globalBindings;

    public DependencyResolver(Resource resource, JsEnvironment jsEnvironment, Bindings globalBindings) {
        this.caller = resource;
        this.jsEnvironment = jsEnvironment;
        this.globalBindings = globalBindings;
    }

    /**
     * Resolve a dependency
     * @param dependency the dependency identifier
     * @param callback the callback that will receive the resolved dependency
     */
    public void resolve(String dependency, UnaryCallback callback) {
        if (!Utils.isJsScript(dependency)) {
            throw new SightlyException("Only JS scripts are allowed as dependencies. Invalid dependency: " + dependency);
        }
        Resource scriptResource = Utils.getScriptResource(caller, dependency, globalBindings);
        jsEnvironment.runResource(scriptResource, globalBindings, Utils.EMPTY_BINDINGS, callback);
    }

}
