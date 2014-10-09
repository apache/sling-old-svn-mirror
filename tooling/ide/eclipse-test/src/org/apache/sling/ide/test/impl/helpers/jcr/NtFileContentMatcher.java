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
 * The <tt>NtFileContentMatcher</tt> verifies the content of an <tt>nt:file</tt> node.
 * 
 * <p>
 * However, this matcher does not verify the actual type of the node, just the value of the jcr:content/jcr:data
 * property.
 * </p>
 *
 */
public class NtFileContentMatcher extends TypeSafeMatcher<Node> {

    private final String value;

    public NtFileContentMatcher(String value) {
        this.value = value;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("node[jcr:content][jcr:data] = ").appendValue(value);
    }

    @Override
    public boolean matchesSafely(Node item) {
        try {
            if (item == null || !item.hasNode("jcr:content")) {
                return false;
            }

            Node jcrContent = item.getNode("jcr:content");

            return jcrContent.hasProperty("jcr:data") && jcrContent.getProperty("jcr:data").getString().equals(value);
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    protected void describeMismatchSafely(Node item, Description mismatchDescription) {
        try {
            if (!item.hasNode("jcr:content")) {
                mismatchDescription.appendValue("was node with a jcr:content child node");
                return;
            }

            Node jcrContent = item.getNode("jcr:content");

            if (jcrContent.hasProperty("jcr:data")) {
                mismatchDescription.appendText("was node [jcr:content] = ").appendValue(
                        item.getProperty("jcr:data").getString());
            } else {
                mismatchDescription.appendText("was node without a property named jcr:data");
            }
        } catch (RepositoryException e) {
            super.describeMismatchSafely(item, mismatchDescription);
        }
    }
}
