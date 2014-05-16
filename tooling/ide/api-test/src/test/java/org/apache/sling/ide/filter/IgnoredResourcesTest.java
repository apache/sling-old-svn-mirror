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
package org.apache.sling.ide.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class IgnoredResourcesTest {

    @Test
    public void nameMatches() {

        IgnoredResources r = new IgnoredResources();
        r.registerRegExpIgnoreRule("/content", "en");

        assertThat(r.isIgnored("/content/en"), equalTo(true));
    }

    @Test
    public void wildCardMatches() {

        IgnoredResources r = new IgnoredResources();
        r.registerRegExpIgnoreRule("/content", "*sync");

        assertThat(r.isIgnored("/content/contentsync"), equalTo(true));
        assertThat(r.isIgnored("/content/content"), equalTo(false));
    }

    @Test
    public void commentLinesAreIgnored() {

        IgnoredResources r = new IgnoredResources();
        r.registerRegExpIgnoreRule("/content", "#en");

        assertThat(r.isIgnored("/content/#en"), equalTo(false));
    }

    @Test
    public void dotsAreEscaped() {

        IgnoredResources r = new IgnoredResources();
        r.registerRegExpIgnoreRule("/content", "en.html");

        assertThat(r.isIgnored("/content/en.html"), equalTo(true));
        assertThat(r.isIgnored("/content/en-html"), equalTo(false));
    }

    @Test
    public void questionMarksAreEscaped() {

        IgnoredResources r = new IgnoredResources();
        r.registerRegExpIgnoreRule("/content", "?en");

        assertThat(r.isIgnored("/content/zen"), equalTo(true));
    }
}
