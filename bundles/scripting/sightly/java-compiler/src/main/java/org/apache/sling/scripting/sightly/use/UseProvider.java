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

package org.apache.sling.scripting.sightly.use;

import javax.script.Bindings;

import org.apache.sling.scripting.sightly.render.RenderContext;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 * A {@code UseProvider} instantiates objects for the Use-API.
 * </p>
 */
@ConsumerType
public interface UseProvider {

    /**
     * Provide an instance based on the given identifier
     *
     * @param identifier    the identifier of the dependency
     * @param renderContext the current rendering context
     * @param arguments     specific arguments provided by the use plugin
     * @return a container with the instance that corresponds to the identifier; if the identifier cannot be
     * handled by this provider, a failed outcome is returned.
     */
    ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments);
}
