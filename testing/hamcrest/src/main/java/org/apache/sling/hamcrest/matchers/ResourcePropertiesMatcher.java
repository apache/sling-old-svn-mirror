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
package org.apache.sling.hamcrest.matchers;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ResourcePropertiesMatcher extends TypeSafeMatcher<Resource> {

    private final Map<String, Object> expectedProps;

    public ResourcePropertiesMatcher(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("properties is null or empty");
        }

        this.expectedProps = properties;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Resource with properties ")
            .appendValueList("[", ",", "]", convertArraysToStrings(expectedProps).entrySet());
    }

    @Override
    protected boolean matchesSafely(Resource item) {
        ValueMap givenProps = item.adaptTo(ValueMap.class);
        for (Map.Entry<String, Object> prop : expectedProps.entrySet()) {
            Object givenValue = givenProps.get(prop.getKey());
            Object expectedValue = prop.getValue();
            if (givenValue != null && expectedValue != null
                    && givenValue.getClass().isArray() && expectedValue.getClass().isArray()) {
                if (!arrayEquals(expectedValue, givenValue)) {
                    return false;
                }
            }
            else {
                if (!objectEquals(expectedValue, givenValue)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean objectEquals(Object value1, Object value2) {
        if (value1 == null) {
            return (value2 == null);
        }
        else if (value2 == null) {
            return (value1 == null);
        }
        else {
            return value1.equals(value2);
        }
    }
    
    private boolean arrayEquals(Object array1, Object array2) {
        int length1 = Array.getLength(array1);
        int length2 = Array.getLength(array2);
        if (length1 != length2) {
            return false;
        }
        for (int i=0; i<length1; i++) {
            if (!objectEquals(Array.get(array1, i), Array.get(array2, i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void describeMismatchSafely(Resource item, Description mismatchDescription) {
        Map<String, Object> actualProperties = item.adaptTo(ValueMap.class);
        if (actualProperties == null) {
            mismatchDescription.appendText("was Resource which does not expose a value map via adaptTo(ValueMap.class)");
            return;
        }
        mismatchDescription.appendText("was Resource with properties ")
             .appendValueList("[", ",", "]", convertArraysToStrings(actualProperties).entrySet())
             .appendText(" (resource: ")
             .appendValue(item)
             .appendText(")");
    }
    
    /**
     * Convert arrays to string representation to get better message if comparison fails.
     * @param props Properties
     * @return Properties with array values converted to strings
     */
    private Map<String,Object> convertArraysToStrings(Map<String,Object> props) {
        SortedMap<String,Object> transformedProps = new TreeMap<String,Object>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            Object value = entry.getValue();
            if (value != null && value.getClass().isArray()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i=0; i<Array.getLength(value); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Object item = Array.get(value, i);
                    if (item == null) {
                        sb.append("null");
                    }
                    else {
                        sb.append(item.toString());
                    }
                }
                sb.append("]");
                value = sb.toString();
            }
            transformedProps.put(entry.getKey(), value);
        }
        return transformedProps;
    }

}