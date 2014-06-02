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
package org.apache.sling.ide.eclipse.ui.views;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.PropertyType;

import org.apache.sling.ide.eclipse.ui.internal.Activator;

public class PropertyTypeSupport {

    static final String[] PROPERTY_TYPES = new String[] {
        "Binary",
        "Boolean", 
        "Date", 
        "Decimal", 
        "Double", 
        "Long", 
        "Name", 
        "Path", 
        "Reference", 
        "String", 
        "URI", 
        "WeakReference"};
    
    static Map<String,Integer> propertyTypeIndices;
    
    static{
        propertyTypeIndices = new HashMap<String,Integer>();
        for (int i = 0; i < PROPERTY_TYPES.length; i++) {
            String aPropertyType = PROPERTY_TYPES[i];
            propertyTypeIndices.put(aPropertyType, i);
        }
    }
    
    static int indexOfPropertyType(int propertyType) {
        String name = PropertyType.nameFromValue(propertyType);
        return propertyTypeIndices.get(name);
    }

    static int propertyTypeOfIndex(int index) {
        try{
            String name = PROPERTY_TYPES[index];
            int value = PropertyType.valueFromName(name);
            return value;
        } catch(Exception e) {
            return PropertyType.STRING;
        }
    }

    public static int propertyTypeOfString(String rawValue) {
        if (!rawValue.startsWith("{")) {
            return PropertyType.STRING;
        }
        for(int i=0; i<PROPERTY_TYPES.length; i++) {
            if (rawValue.startsWith("{"+PROPERTY_TYPES[i]+"}")) {
                return propertyTypeOfIndex(i);
            }
        }
        //TODO: hardcoded type here
        Activator.getDefault().getPluginLogger().warn("Unsupported property type: "+rawValue);
        return PropertyType.STRING;
    }
    
}
