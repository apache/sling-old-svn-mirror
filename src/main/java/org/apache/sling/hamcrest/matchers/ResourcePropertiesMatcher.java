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

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ResourcePropertiesMatcher extends TypeSafeMatcher<Resource> {

    private final Map<String, Object> properties;

    public ResourcePropertiesMatcher(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("properties is null or empty");
        }

        this.properties = properties;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Resource with properties ").appendValueList("[", ",", "]", properties.entrySet());
    }

    @Override
    protected boolean matchesSafely(Resource item) {

        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            Object value = item.adaptTo(ValueMap.class).get(prop.getKey());
            if ( value == null || !value.equals(prop.getValue())) {
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
        mismatchDescription.appendText("was Resource with properties ").appendValueList("[", ",", "]", actualProperties.entrySet()).appendText(" (resource: ").appendValue(item).appendText(")");
    }

}