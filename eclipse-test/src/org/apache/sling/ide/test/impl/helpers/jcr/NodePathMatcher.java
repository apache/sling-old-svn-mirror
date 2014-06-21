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
package org.apache.sling.ide.test.impl.helpers.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * The <tt>NodePathMatcher</tt> matches the node's path
 *
 */
public class NodePathMatcher extends TypeSafeMatcher<Node> {

    private final String path;

    public NodePathMatcher(String path) {
        this.path = path;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("node at path " + path);
    }

    @Override
    public boolean matchesSafely(Node item) {
        try {
            return item != null && item.getPath().equals(path);
        } catch (RepositoryException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hamcrest.TypeSafeMatcher#describeMismatchSafely(java.lang.Object, org.hamcrest.Description)
     */
    @Override
    protected void describeMismatchSafely(Node item, Description mismatchDescription) {
        try {
            mismatchDescription.appendText("was node at path ").appendValue(item.getPath());
        } catch (RepositoryException e) {
            super.describeMismatchSafely(item, mismatchDescription);
        }
    }
}
