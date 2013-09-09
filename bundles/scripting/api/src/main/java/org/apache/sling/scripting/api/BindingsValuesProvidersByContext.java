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
package org.apache.sling.scripting.api;

import java.util.Collection;

import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.BindingsValuesProvider;

/** Provides {@link BindingsValuesProvider} for specific contexts, based on
 *  their "context" service property.
 *  */
public interface BindingsValuesProvidersByContext {
    
    /** Retrieve the current {@link BindingsValuesProvider} for
     *  the supplied ScriptEngineFactory and context.
     *  
     * @param scriptEngineFactory metadata of the ScriptEngine that's being used
     * @param context Only BindingsValuesProviders that have this value in their CONTEXT
     *          service property are considered. For backwards compatibility, BindingsValuesProviders
     *          which do not have a CONTEXT service property are considered to have CONTEXT=request.  
     * @return The returned Collection of BindingsValuesProvider is sorted
     *          so as to give preference to more specific BindingsValuesProvider
     *          over those that match a compatible.javax.script.name ScriptEngineFactory property,
     *          for example, in the same way that the SlingScriptAdapterFactory did before
     *          this service was implemented. 
     */
    Collection<BindingsValuesProvider> getBindingsValuesProviders(
            ScriptEngineFactory scriptEngineFactory,
            String context);
}
