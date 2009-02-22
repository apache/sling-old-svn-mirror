/*
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
 */
package org.apache.sling.apt.parser.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.sink.Sink;
import org.apache.sling.apt.parser.SlingAptParser;

/** Test the SlingAptParserImpl.
 * 
 *  See http://maven.apache.org/doxia/references/apt-format.html for the 
 *  APT syntax reference
 *  
 *  We don't need to test all details as (I assume) everything's tested in
 *  the doxia module that we use, but it's nice to verify our understanding
 *  of the doxia APT syntax. 
 */
public class SlingAptParserImplTest extends TestCase implements MacroResolver {
    
    final SlingAptParserImpl parser = new SlingAptParserImpl(this);
    
    private final static Map<String, Object> DEFAULT_OPTIONS;
    private final static String EOL = System.getProperty( "line.separator" );
    
    static {
        DEFAULT_OPTIONS = new HashMap<String, Object> ();
        DEFAULT_OPTIONS.put(SlingAptParser.OPT_HTML_SKELETON, "false");
    }
    
    private static class SlingTestMacro implements Macro {
        public void execute(Sink sink, MacroRequest request) throws MacroExecutionException {
            sink.text("MACRO:" + getClass().getSimpleName());
            sink.text(", value=" + request.getParameter("value"));
        }
    }
    
    public Macro resolveMacro(String macroId) {
        if(SlingTestMacro.class.getSimpleName().equals(macroId)) {
            return new SlingTestMacro();
        }
        return null;
    }

    protected void parse(String input, String expected, Map<String, Object> options) throws Exception {
        final StringWriter out = new StringWriter();
        parser.parse(new StringReader(input), out, options);
        assertEquals(expected, out.toString().trim());
    }
    
    protected void parse(String input, String expected) throws Exception {
        parse(input, expected, DEFAULT_OPTIONS);
    }
    
    public void testNullReader() throws Exception {
        final StringWriter out = new StringWriter();
        try {
            parser.parse(null, out);
            fail("Expected NullPointerException for null input Reader");
        } catch(NullPointerException fineThatsWhatWeExpect) {
        }
    }
    
    public void testEmpty() throws Exception {
        parse("", "");
    }
    
    public void testParaA() throws Exception {
        parse(" para 1\n para 2","<p>para 1 para 2</p>");
    }

    public void testParaB() throws Exception {
        parse(" para 1\n\n para 2","<p>para 1</p><p>para 2</p>");
    }

    public void testLinkA() throws Exception {
        parse(
            " Para {{http://www.perdu.com}}", 
            "<p>Para <a href=\"http://www.perdu.com\">http://www.perdu.com</a></p>"
        );
    }

    public void testLinkB() throws Exception {
        parse(
            " Para {{{http://www.perdu.com}text here}}", 
            "<p>Para <a href=\"http://www.perdu.com\">text here</a></p>"
        );
    }

    public void testHeadA() throws Exception {
        parse("Heading here\n\n para 1","<h1>Heading here</h1><p>para 1</p>");
    }
    
    public void testMacro() throws Exception {
        parse("%{SlingTestMacro|value=foo}", "MACRO:SlingTestMacro, value\\=foo");
    }
    
    public void testSections() throws Exception {
        parse(
            "Top\n\n* s1\n\n** s2\n\n** s2b\n\n*** s3\n\n para",
            "<h1>Top</h1><h2>s1</h2><h3>s2</h3>" + EOL + "<h3>s2b</h3><h4>s3</h4><p>para</p>"
        );
    }

    public void testList() throws Exception {
        parse(
            " * 1\n para 1\n\n  * 2\n\n  * 3\n\n  para 2",
            "<ul><li><p>1 para 1</p><ul><li><p>2</p></li><li><p>3</p><p>para 2</p></li></ul></li></ul>"
        );
    }

    public void testMacroNotFound() throws Exception {
        final String badName = "MacroThatDoesNotExist"; 
        parse("%{" + badName + "}", "APT macro not found: '" + badName + "'");
    }
    
    public void testTitle() throws Exception {
        final String input = " ---- \n test title \n ---- \n\nH1 title now";
        final String expected = 
            "<html>" + EOL + "<head>" + EOL + "<title>test title</title>" + EOL + "</head>"
             + EOL + "<body><h1>H1 title now</h1>" + EOL + "</body>" + EOL + "</html>"
        ;
        parse(input, expected, null);
    }
}