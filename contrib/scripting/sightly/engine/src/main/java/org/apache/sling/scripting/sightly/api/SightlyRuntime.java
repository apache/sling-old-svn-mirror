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

package org.apache.sling.scripting.sightly.api;

/**
 * Runtime interface provided to Sightly scripts
 */
public interface SightlyRuntime {

    /**
     * Call the specified function name with the given arguments
     * @param functionName - the name of the called function
     * @param arguments - 0 or more arguments passed to the function
     * @return - the object returned by the function
     * @throws SightlyRenderException - if the function was
     * not defined
     */
    Object call(String functionName, Object ... arguments);

}
