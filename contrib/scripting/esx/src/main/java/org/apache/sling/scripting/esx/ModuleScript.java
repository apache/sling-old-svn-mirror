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
package org.apache.sling.scripting.esx;

import org.apache.sling.api.resource.Resource;

public class ModuleScript {
    
    /**
     * 
     */
    public static int JS_FILE = 1;
    
    /**
     * 
     */
    public static int JSON_FILE = 2;
    
    /**
     * 
     */
    public static int RESOURCE_FILE = 3;
    
    /**
     * 
     */
    public static int TEXT_FILE = 4;
    
    
    private int type;
    private Resource resource;
    
    /**
     * 
     * @param type
     * @param resource 
     */
    public ModuleScript(int type, Resource resource) {
        this.type = type;
        this.resource = resource;
    }
    
    /**
     * 
     * @return 
     */
    public boolean isJsFile() {
        return (type == JS_FILE);
    }

    /**
     * 
     * @return 
     */
    public boolean isJsonFile() {
        return (type == JSON_FILE);
    }
    
    /**
     * 
     * @return 
     */
    public boolean isResourceFile() {
        return (type == RESOURCE_FILE);
    }
    
    /**
     * 
     * @return 
     */
    public boolean isTextFile() {
        return (type == TEXT_FILE);
    }
    
    /**
     * 
     * @return 
     */
    public Resource getResource() {
        return resource;
    }
}