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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ResourceChildrenMatcher extends TypeSafeMatcher<Resource> {
    
    private final List<String> childNames;
    
    public ResourceChildrenMatcher(List<String> childNames) {
        if ( childNames == null || childNames.isEmpty() ) {
            throw new IllegalArgumentException("childNames is null or empty");
        }
        
        this.childNames = childNames;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Resource with children ").appendValueList("[", ",", "]", childNames);
    }

    @Override
    protected boolean matchesSafely(Resource item) {
        for ( String childName : childNames ) {
            if ( item.getChild(childName) == null ) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void describeMismatchSafely(Resource item, Description mismatchDescription) {
        List<String> actualChildNames = new ArrayList<String>();
        for (Resource child : item.getChildren()) {
            actualChildNames.add(child.getName());
        }
        mismatchDescription.appendText("was Resource with children ").appendValueList("[", ",", "]", actualChildNames).appendText(" (resource: ").appendValue(item).appendText(")");
    }

}