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
package org.apache.sling.api.request;

import java.util.HashMap;

import org.apache.sling.api.SlingHttpServletRequest;

/** <code>RequestDispatcherOptions</code> are used in the
 *  {@link SlingHttpServletRequest#getRequestDispatcher SlingHttpServletRequest.getRequestDispatcher} 
 *  method, to give more control on some aspects of the include/forward  
 *  mechanism.
 *  
 *  Typical use cases include:
 *  <ul>
 *      <li>
 *          Forcing a resource type, to render a Resource in a specific way, 
 *          like for example <em>render myself in a suitable way for a navigation box</em>.
 *      </li>
 *      <li>
 *          Adding selectors when including a Resource, like for example <em>add
 *          a "teaser" selector to the request that I'm including here</em>.
 *      </li>
 *  </ul>
 *  
 *  This class currently only inherits from Map, and defines some constants 
 *  for well-known options.
 */

public class RequestDispatcherOptions extends HashMap <String,String> {
    
    /** When dispatching, use the value provided by this option as the resource type, 
     *  instead of the one defined by the {@link Resource}.
     */
    public static final String OPT_FORCE_RESOURCE_TYPE = "forceResourceType";
    
    /** When dispatching, replace {@link RequestPathInfo} selectors by the 
     *  value provided by this option.
     */
    public static final String OPT_REPLACE_SELECTORS = "replaceSelectors";
    
    /** When dispatching, add the value provided by this option to the
     *  {@link RequestPathInfo} selectors.
     */
    public static final String OPT_ADD_SELECTORS = "addSelectors"; 

    /** When dispatching, replace the {@link RequestPathInfo} suffix by
     *  the value provided by this option
     */
    public static final String REPLACE_SUFFIX = "replaceSuffix"; 

}
