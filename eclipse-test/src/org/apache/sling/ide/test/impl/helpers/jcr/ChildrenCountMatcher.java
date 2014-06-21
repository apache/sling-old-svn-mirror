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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * The <tt>ChildrenCountMatcher</tt> matches the node's children count
 *
 */
public class ChildrenCountMatcher extends TypeSafeMatcher<Node> {

    private final int childrenCount;

    public ChildrenCountMatcher(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("node with children count " + childrenCount);
    }

    @Override
    protected void describeMismatchSafely(Node item, Description mismatchDescription) {
        try {
            mismatchDescription.appendText("was node with children count ").appendValue(count(item.getNodes()));
        } catch (RepositoryException e) {
            super.describeMismatchSafely(item, mismatchDescription);
        }
    }

    @Override
    public boolean matchesSafely(Node item) {
        try {

            return item != null && count(item.getNodes()) == childrenCount;
        } catch (RepositoryException e) {
            return false;
        }
    }

    private int count(NodeIterator nodes) {
        int count = 0;
        while (nodes.hasNext()) {
            nodes.next();
            count++;
        }

        return count;
    }

}
