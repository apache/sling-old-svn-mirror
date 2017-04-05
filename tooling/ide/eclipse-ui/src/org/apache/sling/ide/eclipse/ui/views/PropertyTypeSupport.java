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

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
        propertyTypeIndices = new HashMap<>();
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
        String name = null;
        try{
            name = PROPERTY_TYPES[index];
            int value = PropertyType.valueFromName(name);
            return value;
        } catch(Exception e) {
            Activator.getDefault().getPluginLogger().warn("Unsupported index ("+index+") and/or name ("+name+"): "+e, e);
            return PropertyType.STRING;
        }
    }

    public static int propertyTypeOfString(String rawValue) {
        if (rawValue==null) {
            // avoid NPE
            return PropertyType.STRING;
        }
        if (!rawValue.startsWith("{")) {
            return PropertyType.STRING;
        }
        int curlyEnd = rawValue.indexOf("}", 1);
        if (curlyEnd==-1) {
            return PropertyType.STRING;
        }
        String type = rawValue.substring(1, curlyEnd);
        int index = -2;
        try{
            index = propertyTypeIndices.get(type);
            return propertyTypeOfIndex(index);
        } catch(Exception e) {
            Activator.getDefault().getPluginLogger().warn("Unsupported type ("+type+") and/or index ("+index+"): "+e, e);
            return PropertyType.STRING;
        }
    }

    public static String encodeValueAsString(Object value, int propertyType) {
        switch(propertyType) {
        case PropertyType.BOOLEAN: {
            return Boolean.toString(value.equals((Integer)1));
        }
        case PropertyType.DECIMAL:
        case PropertyType.DOUBLE:
        case PropertyType.LONG:
        case PropertyType.NAME:
        case PropertyType.PATH:
        case PropertyType.STRING: {
            return String.valueOf(value);
        }
        case PropertyType.BINARY: {
            //TODO: how to handle binary here
            return "";
        }
        case PropertyType.DATE: {
            if (value instanceof Date) {
                Date date = (Date)value;
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                return DateTimeSupport.print(c);
            } else if (value instanceof GregorianCalendar) {
                GregorianCalendar date = (GregorianCalendar)value;
                return DateTimeSupport.print(date);
            } else {
                return String.valueOf(value);
            }
        }
        case PropertyType.URI: {
            if (value instanceof URI) {
                URI uri = (URI)value;
                return uri.toString();
            } else {
                return String.valueOf(value);
            }
        }
        case PropertyType.REFERENCE:
        case PropertyType.WEAKREFERENCE: {
            return String.valueOf(value);
        }
        default: {
            return String.valueOf(value);
        }
        }
    }
    
}
