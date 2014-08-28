/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.api.scripting;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ScriptResolver</code> interface defines the API for a service
 * capable of locating scripts. Where the script is actually located is an
 * implementation detail of the service implementation.
 *
 * @deprecated The SlingScriptResolver interface is intended to be implemented
 * and also used by project specific code. To keep the API as clean as possible
 * this interface was deprecated
 */
@ProviderType
public interface SlingScriptResolver {

    /**
     * Finds the {@link SlingScript} for the given name.
     * <p>
     * The semantic meaning of the name is implementation specific: It may be an
     * absolute path to a <code>Resource</code> providing the script source or
     * it may be a relative path resolved according to some path settings.
     * Finally, the name may also just be used as an identifier to find the
     * script in some registry.
     *
     * @param resourceResolver The <code>ResourceResolver</code> used to
     *            access the script.
     * @param name The script name. Must not be <code>null</code>.
     * @return The {@link SlingScript} to which the name resolved or
     *         <code>null</code> otherwise.
     * @throws org.apache.sling.api.SlingException If an error occurrs trying to resolve the name.
     */
    SlingScript findScript(ResourceResolver resourceResolver, String name);
}
