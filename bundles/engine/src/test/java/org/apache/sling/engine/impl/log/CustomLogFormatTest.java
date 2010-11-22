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
package org.apache.sling.engine.impl.log;

import junit.framework.TestCase;

/**
 * The <code>CustomLogFormatTest</code> class tests the
 * <code>CustomLogFormat</code> class.
 */
public class CustomLogFormatTest extends TestCase {

    public void testCase0() {
        this.testCase0Helper("%t [%R] -> %m %U%q %H");
        this.testCase0Helper("%{Content-Type}i");
        this.testCase0Helper("%400t");
        this.testCase0Helper("%!400t");
        this.testCase0Helper("%300,400t");
        this.testCase0Helper("%!300,400t");
        this.testCase0Helper("%!300,400{Content-Type}i");
        this.testCase0Helper("xyz %Dms");
        this.testCase0Helper("xyz %{foo}M");
    }

    private void testCase0Helper(String format) {
        CustomLogFormat clf = new CustomLogFormat(format);
        String format2 = clf.toString();
        assertEquals(format, format2);
    }

    public void testHeaderEscape() {

        // single whitespace character
        assertEquals("\\n", CustomLogFormat.HeaderParameter.escape("\n"));
        assertEquals("\\r", CustomLogFormat.HeaderParameter.escape("\r"));
        assertEquals("\\t", CustomLogFormat.HeaderParameter.escape("\t"));
        assertEquals("\\f", CustomLogFormat.HeaderParameter.escape("\f"));
        assertEquals("\\b", CustomLogFormat.HeaderParameter.escape("\b"));

        // single special character
        assertEquals("\\\\", CustomLogFormat.HeaderParameter.escape("\\"));
        assertEquals("\\\"", CustomLogFormat.HeaderParameter.escape("\""));

        // plain word
        assertEquals("This is a plain word", CustomLogFormat.HeaderParameter.escape("This is a plain word"));

        // embedded whitespace special
        assertEquals("This is a\\nplain word", CustomLogFormat.HeaderParameter.escape("This is a\nplain word"));

        // embedded non-printable
        assertEquals("Das isch \\u00e4n Umlut", CustomLogFormat.HeaderParameter.escape("Das isch \u00e4n Umlut"));
        assertEquals("This is a special character \\u1234", CustomLogFormat.HeaderParameter.escape("This is a special character \u1234"));
    }

}
