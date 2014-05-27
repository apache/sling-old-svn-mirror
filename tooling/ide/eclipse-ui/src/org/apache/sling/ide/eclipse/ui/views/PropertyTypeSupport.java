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

import javax.jcr.PropertyType;

public class PropertyTypeSupport {

    static final String[] SUPPORTED_PROPERTY_TYPES = new String[] 
            {"Boolean", "Date", "Decimal", "Double", "Long", "Name", "Path", "String"};
    
    static int indexOfPropertyType(int propertyType) {
        switch(propertyType) {
        case PropertyType.BOOLEAN: {
            return 0;
        }
        case PropertyType.DATE: {
            return 1;
        }
        case PropertyType.DECIMAL: {
            return 2;
        }
        case PropertyType.DOUBLE: {
            return 3;
        }
        case PropertyType.LONG: {
            return 4;
        }
        case PropertyType.NAME: {
            return 5;
        }
        case PropertyType.PATH: {
            return 6;
        }
        case PropertyType.STRING: {
            return 7;
        }
        default: {
            //TODO: hardcode to STRING then
            return 7;
        }
        }
    }

    public static int propertyTypeOfIndex(int index) {
        switch(index) {
        case 0:
            return PropertyType.BOOLEAN;
        case 1:
            return PropertyType.DATE;
        case 2:
            return PropertyType.DECIMAL;
        case 3:
            return PropertyType.DOUBLE;
        case 4:
            return PropertyType.LONG;
        case 5:
            return PropertyType.NAME;
        case 6:
            return PropertyType.PATH;
        case 7:
            return PropertyType.STRING;
        default: {
            //TODO: hardcode to STRING then
            return PropertyType.STRING;
        }
        }
    }
    
}
