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

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * The <tt>NodePathMatcher</tt> matches the node's mixin types
 *
 */
public class MixinTypesMatcher extends TypeSafeMatcher<Node> {

    private final String[] mixinTypes;

    public MixinTypesMatcher(String... mixinTypes) {
        this.mixinTypes = mixinTypes;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("node with mixinTypes ").appendValue(mixinTypes);
    }

    @Override
    public boolean matchesSafely(Node item) {
        try {
            return Arrays.equals(this.mixinTypes, mixinTypes(item));
        } catch (RepositoryException e) {
            return false;
        }
    }

    private String[] mixinTypes(Node item) throws RepositoryException {

        NodeType[] mixinNodeTypes = item.getMixinNodeTypes();
        String[] mixinTypes = new String[mixinNodeTypes.length];
        for (int i = 0; i < mixinNodeTypes.length; i++) {
            mixinTypes[i] = mixinNodeTypes[i].getName();
        }
        return mixinTypes;
    }

    @Override
    protected void describeMismatchSafely(Node item, Description mismatchDescription) {
        try {
            mismatchDescription.appendText("was node with mixinTypes ").appendValue(mixinTypes(item));
        } catch (RepositoryException e) {
            super.describeMismatchSafely(item, mismatchDescription);
        }
    }

}
