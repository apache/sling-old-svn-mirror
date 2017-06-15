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
package org.apache.sling.scripting.esx.services;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.event.EventHandler;

public interface ScriptModuleCache extends EventHandler{

    /**
     * put compiled script into script module cache
     * @param path
     * @param script
     */
    public void put(String module, ScriptObjectMirror script);

    /**
     * get compiled script or null if the script is not in the cache
     * @param path
     * @return
     */
    public ScriptObjectMirror get(String module);   

    /**
     * 
     * @param resource
     * @return 
     */
    public ScriptObjectMirror get(Resource resource);
   

    /**
     * removing module script from cache if existing otherwise
     * doesn't do anything (return always true)
     *
     * return false if for some reason cannot be flushed from cache
     *
     * @param path
     */
    public boolean flush(String module);        
    
}