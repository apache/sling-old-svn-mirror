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

import org.hamcrest.Matcher;

public final class JcrMatchers {

    public static Matcher<Node> hasPath(String nodePath) {
        return new NodePathMatcher(nodePath);
    }

    public static Matcher<Node> hasPrimaryType(String primaryType) {
        return new PrimaryTypeMatcher(primaryType);
    }

    public static Matcher<Node> hasMixinTypes(String mixinTypes) {
        return new MixinTypesMatcher(mixinTypes);
    }

    public static Matcher<Node> hasChildrenCount(int childrenCount) {
        return new ChildrenCountMatcher(childrenCount);
    }

    public static Matcher<Node> hasChildrenNames(String... childrenNames) {
        return new ChildrenNameMatcher(childrenNames);
    }

    private JcrMatchers() {

    }
}
