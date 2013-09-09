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

import javax.script.Bindings;

/**
 * Service interface which allows for the Bindings object.
 * @since 2.1
 */
public interface BindingsValuesProvider {

    /** The name of the multi-value service property that defines the context(s) to which 
     *  a BindingsValuesProvider applies. This service property is optional, if not set
     *  the default value is {@link @DEFAULT_CONTEXT}  
     */
    String CONTEXT = "context";
    
    /** The default value of the CONTEXT service property, used for compatibility with
     *  previous versions of this bundle that didn't require it. 
     */
    String DEFAULT_CONTEXT = "request";
    
    /**
     * Add objects to the Bindings object. The Bindings
     * object passed to this method does not support replacing
     * or removing entries provided by Sling core scripting supports
     * (request, response, resource, etc.). Entries created by
     * <i>other</i> implementations of SlingScriptBindingsValuesProvider
     * is permitted.
     *
     * @param bindings the Bindings object
     */
    void addBindings(Bindings bindings);

}
