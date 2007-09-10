/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.components.extensions;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>GenericExtension</code> TODO
 */
public class GenericExtension extends AbstractExtension {

    private Map properties;
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public Map getProperties() {
        return new HashMap(properties);
    }
    
    public void setProperties(Map properties) {
        this.properties = new HashMap(properties);
    }
}
