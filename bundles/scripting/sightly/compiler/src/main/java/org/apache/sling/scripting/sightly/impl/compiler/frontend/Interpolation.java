/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler.frontend;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;

/**
 * A sequence with alternating string fragments and Sightly expressions. These result from parsing HTML attributes or string nodes. For
 * instance "Hello ${World}!" would result in 3 fragments: "Hello ", ${World} and "!"
 */
public class Interpolation {

    //todo: this should be immutable to maintain the design consistent

    private List<Fragment> fragments = new ArrayList<>();
    private String content;

    public void addFragment(Fragment fragment) {
        fragments.add(fragment);
    }

    public void addExpression(Expression expression) {
        fragments.add(new Fragment.Expr(expression));
    }

    public void addText(String text) {
        fragments.add(new Fragment.Text(text));
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get the number of fragments
     * @return the number of fragments
     */
    public int size() {
        return fragments.size();
    }

    /**
     * Return the fragment with the specified index
     * @param index - the index of the fragments. must be less than the size of the interpolation
     * @return - the specified fragment
     * @throws IndexOutOfBoundsException - if the index is negative or greater or equal than size
     */
    public Fragment getFragment(int index) {
        return fragments.get(index);
    }

    public Iterable<Fragment> getFragments() {
        return fragments;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Interpolation{" +
                "fragments=" + fragments +
                '}';
    }
}
