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

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.powermock.reflect.Whitebox;

import junit.framework.TestCase;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XSSAPIImplTest {

    public static final String RUBBISH = "rubbish";

    private XSSAPI xssAPI;

    @Before
    public void setup() {
        try {
            InputStream policyStream = new FileInputStream("./src/main/resources/SLING-INF/content/config.xml");
            Policy policy = Policy.getInstance(policyStream);
            AntiSamy antiSamy = new AntiSamy(policy);

            PolicyHandler mockPolicyHandler = mock(PolicyHandler.class);
            when(mockPolicyHandler.getPolicy()).thenReturn(policy);
            when(mockPolicyHandler.getAntiSamy()).thenReturn(antiSamy);

            XSSFilterImpl xssFilter = new XSSFilterImpl();
            Whitebox.setInternalState(xssFilter, "defaultHandler", mockPolicyHandler);

            xssAPI = new XSSAPIImpl();
            Field filterField = XSSAPIImpl.class.getDeclaredField("xssFilter");
            filterField.setAccessible(true);
            filterField.set(xssAPI, xssFilter);

            ResourceResolver mockResolver = mock(ResourceResolver.class);
            when(mockResolver.map(anyString())).thenAnswer(new Answer() {
                public Object answer(InvocationOnMock invocation) {
                    Object[] args = invocation.getArguments();
                    String url = (String) args[0];
                    return url.replaceAll("jcr:", "_jcr_");
                }
            });

            SlingHttpServletRequest mockRequest = mock(SlingHttpServletRequest.class);
            when(mockRequest.getResourceResolver()).thenReturn(mockResolver);

            xssAPI = xssAPI.getRequestSpecificAPI(mockRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEncodeForHTML() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, null},
                {"simple", "simple"},

                {"<script>", "&lt;script&gt;"},
                {"<b>", "&lt;b&gt;"},

                {"günter", "günter"},
                {"\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f", "\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("HTML Encoding '" + source + "'", expected, xssAPI.encodeForHTML(source));
        }
    }

    @Test
    public void testEncodeForHTMLAttr() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, null},
                {"simple", "simple"},

                {"<script>", "&lt;script>"},
                {"\" <script>alert('pwned');</script>", "&#34; &lt;script>alert(&#39;pwned&#39;);&lt;/script>"},
                {"günter", "günter"},
                {"\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f", "\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("HTML Encoding '" + source + "'", expected, xssAPI.encodeForHTMLAttr(source));
        }
    }

    @Test
    public void testEncodeForXML() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, null},
                {"simple", "simple"},

                {"<script>", "&lt;script&gt;"},
                {"<b>", "&lt;b&gt;"},

                {"günter", "günter"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("XML Encoding '" + source + "'", expected, xssAPI.encodeForXML(source));
        }
    }

    @Test
    public void testEncodeForXMLAttr() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, null},
                {"simple", "simple"},

                {"<script>", "&lt;script>"},
                {"<b>", "&lt;b>"},

                {"günter", "günter"},
                {"\"xss:expression(alert('XSS'))", "&#34;xss:expression(alert(&#39;XSS&#39;))"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("XML Encoding '" + source + "'", expected, xssAPI.encodeForXMLAttr(source));
        }
    }

    @Test
    public void testFilterHTML() {
        String[][] testData = {
                //         Source                            Expected Result
                {null, ""},
                {"", ""},
                {"simple", "simple"},

                {"<script>ugly</script>", ""},
                {"<b>wow!</b>", "<b>wow!</b>"},

                {"<p onmouseover='ugly'>nice</p>", "<p>nice</p>"},

                {"<img src='javascript:ugly'/>", ""},
                {"<img src='nice.jpg'/>", "<img src=\"nice.jpg\" />"},

                {"<ul><li>1</li><li>2</li></ul>", "<ul><li>1</li><li>2</li></ul>"},

                {"günter", "günter"},


                {"<strike>strike</strike>", "<strike>strike</strike>"},
                {"<s>s</s>", "<s>s</s>"},

                {"<a href=\"\">empty href</a>", "<a href=\"\">empty href</a>"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("Filtering '" + source + "'", expected, xssAPI.filterHTML(source));
        }
    }

    @Test
    public void testGetValidHref() {
        String[][] testData = {
                //         Href                                        Expected Result
                //
                {null, ""},
                {"", ""},
                {"simple", "simple"},

                {"../parent", "../parent"},
                {"repo/günter", "repo/günter"},

                // JCR namespaces:
                {"my/page/jcr:content.feed", "my/page/_jcr_content.feed"},
                {"my/jcr:content/page/jcr:content", "my/_jcr_content/page/_jcr_content"},

                {"\" onClick=ugly", "%22%20onClick=ugly"},
                {"javascript:ugly", ""},
                {"http://localhost:4502", "http://localhost:4502"},
                {"http://localhost:4502/test", "http://localhost:4502/test"},
                {"http://localhost:4502/jcr:content/test", "http://localhost:4502/_jcr_content/test"},
                {"http://localhost:4502/test.html?a=b&b=c", "http://localhost:4502/test.html?a=b&b=c"},

                // space
                {"/test/ab cd", "/test/ab%20cd"},
                {"http://localhost:4502/test/ab cd", "http://localhost:4502/test/ab%20cd"},
                {"/test/ab attr=c", "/test/ab%20attr=c"},
                {"http://localhost:4502/test/ab attr=c", "http://localhost:4502/test/ab%20attr=c"},
                // "
                {"/test/ab\"cd", "/test/ab%22cd"},
                {"http://localhost:4502/test/ab\"cd", "http://localhost:4502/test/ab%22cd"},
                // '
                {"/test/ab'cd", "/test/ab%27cd"},
                {"http://localhost:4502/test/ab'cd", "http://localhost:4502/test/ab%27cd"},
                // =
                {"/test/ab=cd", "/test/ab=cd"},
                {"http://localhost:4502/test/ab=cd", "http://localhost:4502/test/ab=cd"},
                // >
                {"/test/ab>cd", "/test/ab%3Ecd"},
                {"http://localhost:4502/test/ab>cd", "http://localhost:4502/test/ab%3Ecd"},
                // <
                {"/test/ab<cd", "/test/ab%3Ccd"},
                {"http://localhost:4502/test/ab<cd", "http://localhost:4502/test/ab%3Ccd"},
                // `
                {"/test/ab`cd", "/test/ab%60cd"},
                {"http://localhost:4502/test/ab`cd", "http://localhost:4502/test/ab%60cd"},
                // colons in query string
                {"/test/search.html?0_tag:id=test", "/test/search.html?0_tag%3Aid=test"},
                { // JCR namespaces and colons in query string
                        "/test/jcr:content/search.html?0_tag:id=test",
                        "/test/_jcr_content/search.html?0_tag%3Aid=test"
                },
                { // ? in query string
                        "/test/search.html?0_tag:id=test?ing&1_tag:id=abc",
                        "/test/search.html?0_tag%3Aid=test?ing&1_tag%3Aid=abc",
                }
        };

        for (String[] aTestData : testData) {
            String href = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("Requested '" + href + "'", expected, xssAPI.getValidHref(href));
        }
    }

    @Test
    public void testGetValidInteger() {
        String[][] testData = {
                //         Source                                        Expected Result
                //
                {null, "123"},
                {"100", "100"},
                {"0", "0"},

                {"junk", "123"},
                {"100.5", "123"},
                {"", "123"},
                {"null", "123"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            Integer expected = (aTestData[1] != null) ? new Integer(aTestData[1]) : null;

            TestCase.assertEquals("Validating integer '" + source + "'", expected, xssAPI.getValidInteger(source, 123));
        }
    }

    @Test
    public void testGetValidLong() {
        String[][] testData = {
                //         Source                                        Expected Result
                //
                {null, "123"},
                {"100", "100"},
                {"0", "0"},

                {"junk", "123"},
                {"100.5", "123"},
                {"", "123"},
                {"null", "123"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            Long expected = (aTestData[1] != null) ? new Long(aTestData[1]) : null;

            TestCase.assertEquals("Validating long '" + source + "'", expected, xssAPI.getValidLong(source, 123));
        }
    }

    @Test
    public void testGetValidDimension() {
        String[][] testData = {
                //         Source                                        Expected Result
                //
                {null, "123"},
                {"", "123"},
                {"100", "100"},
                {"0", "0"},

                {"junk", "123"},
                {"100.5", "123"},
                {"", "123"},
                {"null", "123"},

                {"\"auto\"", "\"auto\""},
                {"'auto'", "\"auto\""},
                {"auto", "\"auto\""},

                {"autox", "123"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("Validating dimension '" + source + "'", expected, xssAPI.getValidDimension(source, "123"));
        }
    }

    @Test
    public void testEncodeForJSString() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, null},
                {"simple", "simple"},

                {"break\"out", "break\\x22out"},
                {"break'out", "break\\x27out"},

                {"</script>", "<\\/script>"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("Encoding '" + source + "'", expected, xssAPI.encodeForJSString(source));
        }
    }

    @Test
    public void testGetValidJSToken() {
        String[][] testData = {
                //         Source                            Expected Result
                //
                {null, RUBBISH},
                {"", RUBBISH},
                {"simple", "simple"},
                {"clickstreamcloud.thingy", "clickstreamcloud.thingy"},

                {"break out", RUBBISH},
                {"break,out", RUBBISH},

                {"\"literal string\"", "\"literal string\""},
                {"'literal string'", "'literal string'"},
                {"\"bad literal'", RUBBISH},
                {"'literal'); junk'", "'literal\\x27); junk'"},

                {"1200", "1200"},
                {"3.14", "3.14"},
                {"1,200", RUBBISH},
                {"1200 + 1", RUBBISH}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            TestCase.assertEquals("Validating Javascript token '" + source + "'", expected, xssAPI.getValidJSToken(source, RUBBISH));
        }
    }

    @Test
    public void testEncodeForCSSString() {
        String[][] testData = {
                // Source   Expected result
                {null, null},
                {"test"   , "test"},
                {"\\"     , "\\5c"},
                {"'"      , "\\27"},
                {"\""     , "\\22"}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            String result = xssAPI.encodeForCSSString(source);
            TestCase.assertEquals("Encoding '" + source + "'", expected, result);
        }
    }

    @Test
    public void testGetValidStyleToken() {
        String[][] testData = {
                // Source                           Expected result
                {null                               , RUBBISH},
                {""                                 , RUBBISH},

                // CSS close
                {"}"                                , RUBBISH},

                // line break
                {"br\neak"                          , RUBBISH},

                // no javascript:
                {"javascript:alert(1)"              , RUBBISH},
                {"'javascript:alert(1)'"            , RUBBISH},
                {"\"javascript:alert('XSS')\""      , RUBBISH},
                {"url(javascript:alert(1))"         , RUBBISH},
                {"url('javascript:alert(1)')"       , RUBBISH},
                {"url(\"javascript:alert('XSS')\")" , RUBBISH},

                // no expression
                {"expression(alert(1))"             , RUBBISH},
                {"expression  (alert(1))"           , RUBBISH},
                {"expression(this.location='a.co')" , RUBBISH},

                // html tags
                {"</style><script>alert(1)</script>", RUBBISH},

                // usual CSS stuff
                {"background-color"                 , "background-color"},
                {"-moz-box-sizing"                  , "-moz-box-sizing"},
                {".42%"                             , ".42%"},
                {"#fff"                             , "#fff"},

                // valid strings
                {"'literal string'"                 , "'literal string'"},
                {"\"literal string\""               , "\"literal string\""},
                {"'it\\'s here'"                    , "'it\\'s here'"},
                {"\"it\\\"s here\""                 , "\"it\\\"s here\""},

                // invalid strings
                {"\"bad string"                     , RUBBISH},
                {"'it's here'"                      , RUBBISH},
                {"\"it\"s here\""                   , RUBBISH},

                // valid parenthesis
                {"rgb(255, 255, 255)"               , "rgb(255, 255, 255)"},

                // invalid parenthesis
                {"rgb(255, 255, 255"               , RUBBISH},
                {"255, 255, 255)"                  , RUBBISH},

                // valid tokens
                {"url(http://example.com/test.png)", "url(http://example.com/test.png)"},
                {"url('image/test.png')"           , "url('image/test.png')"},

                // invalid tokens
                {"color: red"                      , RUBBISH}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            String result = xssAPI.getValidStyleToken(source, RUBBISH);
            if (!result.equals(expected)) {
                fail("Validating style token '" + source + "', expecting '" + expected + "', but got '" + result + "'");
            }
        }
    }

    @Test
    public void testGetValidCSSColor() {
        String[][] testData = {
                //      Source                          Expected Result
                //
                {null, RUBBISH},
                {"", RUBBISH},

                {"rgb(0,+0,-0)", "rgb(0,+0,-0)"},
                {"rgba ( 0\f%, 0%,\t0%,\n100%\r)", "rgba ( 0\f%, 0%,\t0%,\n100%\r)",},

                {"#ddd", "#ddd"},
                {"#eeeeee", "#eeeeee",},

                {"hsl(0,1,2)", "hsl(0,1,2)"},
                {"hsla(0,1,2,3)", "hsla(0,1,2,3)"},
                {"currentColor", "currentColor"},
                {"transparent", "transparent"},

                {"\f\r\n\t MenuText\f\r\n\t ", "MenuText"},
                {"expression(99,99,99)", RUBBISH},
                {"blue;", RUBBISH},
                {"url(99,99,99)", RUBBISH}
        };

        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            String result = xssAPI.getValidCSSColor(source, RUBBISH);
            if (!result.equals(expected)) {
                fail("Validating CSS Color '" + source + "', expecting '" + expected + "', but got '" + result + "'");
            }
        }
    }

    @Test
    public void testGetValidMultiLineComment() {
        String[][] testData = {
                //Source            Expected Result

                {null               , RUBBISH},
                {"blah */ hack"     , RUBBISH},

                {"Valid comment"    , "Valid comment"}
        };
        for (String[] aTestData : testData) {
            String source = aTestData[0];
            String expected = aTestData[1];

            String result = xssAPI.getValidMultiLineComment(source, RUBBISH);
            if (!result.equals(expected)) {
                fail("Validating multiline comment '" + source + "', expecting '" + expected + "', but got '" + result + "'");
            }
        }
    }
}
