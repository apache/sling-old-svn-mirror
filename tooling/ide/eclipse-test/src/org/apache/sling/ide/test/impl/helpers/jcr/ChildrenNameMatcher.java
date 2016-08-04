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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * The <tt>ChildrenCountMatcher</tt> matches the node's children count
 *
 */
public class ChildrenNameMatcher extends TypeSafeMatcher<Node> {

    private final String[] childrenNames;

    public ChildrenNameMatcher(String... childrenNames) {
        this.childrenNames = childrenNames;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("node with children named " + Arrays.toString(childrenNames));
    }

    @Override
    public boolean matchesSafely(Node item) {
        try {
            
            return item != null && childrenNames(item.getNodes()).equals(Arrays.asList(childrenNames));
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    protected void describeMismatchSafely(Node node, Description mismatchDescription) {

        try {
            List<String> actualChildrenNames = childrenNames(node.getNodes());
            mismatchDescription.appendText("was node with children named ").appendText(actualChildrenNames.toString());
        } catch (RepositoryException e) {
            super.describeMismatchSafely(node, mismatchDescription);
        }
    }

    private List<String> childrenNames(NodeIterator nodes) throws RepositoryException {

        List<String> names = new ArrayList<>();
        while (nodes.hasNext()) {
            names.add(nodes.nextNode().getName());
        }
        return names;
    }

}
