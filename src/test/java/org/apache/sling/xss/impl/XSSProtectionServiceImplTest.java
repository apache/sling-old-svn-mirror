/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import org.apache.sling.xss.ProtectionContext;

/** Test the XSSProtectionServiceImpl with various strings.
 *
 *  Notes on testing with Japanese text:
 *
 *  1) To encode Japanese and other text, you can write that
 *  text to an UTF-8 encoded xml file (with an editor
 *  that supports full Unicode, Eclipse works for example)
 *  and use native2ascii -encoding UTF-8 < thatFile
 *
 *  2) Make sure your terminal's and JVM encoding is
 *  set correctly, otherwise strings displayed in error
 *  messages will be wrong. Eclipse's Java debugger
 *  displays everything right, I assume others do as well.
 */
public class XSSProtectionServiceImplTest {

    private XSSFilterImpl xssFilter;

    @Before
    public void setup() {
        xssFilter = new XSSFilterImpl();
    }

    private void assertNoChange(String input) throws Exception {
        assertXssProtection(input, input);
    }

    private void assertXssProtection(String input, String expected) throws Exception {
        final String output = xssFilter.filter(ProtectionContext.PLAIN_HTML_CONTENT, input);
        if(!expected.equals(output)) {
            fail("For input [" + input + "], expected [" + expected + "] but got [" + output + "]");
        }
    }

    @Test
    public void simpleStringsTest() throws Exception {
        assertNoChange("");
        assertNoChange("FOO");
        assertNoChange("The Quick Brown Fox");
    }

    @Test
    public void testStringsThatNeedChanges() throws Exception {
        assertXssProtection("Some <tag> in the text", "Some &lt;tag&gt; in the text");
        assertXssProtection("And a <script src='foo'/> here", "And a &lt;script src='foo'/&gt; here");
        assertXssProtection("And Bonnie & Clyde", "And Bonnie &amp; Clyde");
    }

    @Test
    public void testWithAccents() throws Exception {
        assertNoChange("Accents here \u00e9 \u00e0 \u00e8 \u00f6 \u00e4 \u00fc \u00e2 \u00ea \u00ee \u00f4 \u00fb and done");
    }

    @Test
    public void test21umlaut() throws Exception {
        assertNoChange("The 21 here is followed by u umlaut: 21\u00fcfile");
    }

    @Test
    public void testJapaneseStringOne() throws Exception {
        assertNoChange("Japanese mark:  \u3001\u3002\uff1f\uff01\uffe5\u30fb\uff20\uff03\uff04\uff05\uff3e\uff06\uff0a\uff08\uff09\u300c\u300d etc.");
    }

    @Test
    public void testJapaneseStringTwo() throws Exception {
        assertNoChange(
                "\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f"
        );
    }
}
