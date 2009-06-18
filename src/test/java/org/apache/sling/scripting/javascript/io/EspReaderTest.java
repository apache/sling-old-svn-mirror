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
package org.apache.sling.scripting.javascript.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.ScriptException;

import junit.framework.TestCase;

import org.apache.sling.scripting.javascript.internal.ScriptEngineHelper;

/**
 * The <code>EspReaderTest</code> contains some simple test cases for the
 * <code>EspReader</code> class which processes ESP (ECMA Server Page) templated
 * JavaScript and produces plain JavaScript.
 */
public class EspReaderTest extends TestCase {

    /** Test read() method */
    public void testReadSingle() throws IOException {
        String src = "<%var%>"; // expect var on reader

        Reader reader = new EspReader(new StringReader(src));

        assertTrue("Character 1 must be 'v'", 'v' == reader.read());
        assertTrue("Character 2 must be 'a'", 'a' == reader.read());
        assertTrue("Character 3 must be 'r'", 'r' == reader.read());
        assertTrue("Character 4 must be -1", -1 == reader.read());
    }

    /** Test read(char[], int, int) method */
    public void testReadArrayAll() throws IOException {
        String src = "<%var%>"; // expect var on reader

        Reader reader = new EspReader(new StringReader(src));
        char[] buf = new char[3];
        int rd = reader.read(buf, 0, buf.length);

        assertEquals(3, rd);
        assertEquals("var", new String(buf, 0, rd));

        // nothing more to read, expect EOF
        rd = reader.read(buf, 0, buf.length);
        assertEquals(-1, rd);
    }

    /** Test read(char[], int, int) method */
    public void testReadArrayOffset() throws IOException {
        String jsSrc = "var x = 0;";
        String src = "<%" + jsSrc + "%>";

        Reader reader = new EspReader(new StringReader(src));
        char[] buf = new char[10];
        int off = 2;
        int len = 3;
        int rd = reader.read(buf, off, len);
        assertEquals(len, rd);
        assertEquals("var", new String(buf, off, rd));

        off = 2;
        len = 7;
        rd = reader.read(buf, off, len);
        assertEquals(len, rd);
        assertEquals(" x = 0;", new String(buf, off, rd));

        // nothing more to read, expect EOF
        rd = reader.read(buf, 0, buf.length);
        assertEquals(-1, rd);
    }

    /** Test standard template text */
    public void testTemplate() throws IOException {
        assertEquals("out=response.writer;out.write(\"test\");", parse("test"));
        assertEquals("out=response.writer;out.write(\"test\\n\");\nout.write(\"test2\");", parse("test\ntest2"));
    }
    
    /** Test with a custom "out" initialization */
    public void testOutInit() throws IOException {
        final String input = "test";
        final String expected = "out=getOut();out.write(\"test\");";
            
        StringBuffer buf = new StringBuffer();

        EspReader r = new EspReader(new StringReader(input));
        r.setOutInitStatement("out=getOut();");
        int c;
        while ( (c=r.read()) >= 0) {
            buf.append( (char) c);
        }

        assertEquals(expected, buf.toString());
    }

    /** Test plain JavaScript code */
    public void testCode() throws IOException {
        assertEquals(" test(); ", parse("<% test(); %>"));
        assertEquals(" \ntest();\ntest2(); ", parse("<% \ntest();\ntest2(); %>"));
    }

    /** Test JavaScript expressions */
    public void testExpr() throws IOException {
        assertEquals("out=response.writer;out.write( x + 1 );", parse("<%= x + 1 %>"));
        assertEquals("out=response.writer;out.write(\"<!-- \");out.write( x + 1 );out.write(\" -->\");", parse("<!-- <%= x + 1 %> -->"));
    }

    /** Test JavaScript comment */
    public void testComment() throws IOException {
        assertEquals("", parse("<%-- test(); --%>"));
    }
    
    public void testCompactExpressionsDouble() throws IOException {
    	final String input = "<html version=\"${1+1}\">\n";
    	final String expected = "out=response.writer;out.write(\"<html version=\\\"\");out.write(1+1);out.write(\"\\\">\\n\");\n";
    	final String actual = parse(input);
        assertEquals(flatten(expected), flatten(actual));
    }
    
    public void testCompactExpressionsDoubleNegative() throws IOException {
    	final String input = "<html version=\"{1+1}\">\n";
    	final String expected = "out=response.writer;out.write(\"<html version=\\\"{1+1}\\\">\\n\");\n";
    	final String actual = parse(input);
        assertEquals(flatten(expected), flatten(actual));
    }
    
    public void testCompactExpressionsSingle() throws IOException {
    	final String input = "<html version='${1+1}'>\n";
    	final String expected = "out=response.writer;out.write(\"<html version='\");out.write(1+1);out.write(\"'>\\n\");\n";
    	final String actual = parse(input);
        assertEquals(flatten(expected), flatten(actual));
    }
    
    public void testCompactExpressionsSingleNegative() throws IOException {
    	final String input = "<html version='{1+1}'>\n";
    	final String expected = "out=response.writer;out.write(\"<html version='{1+1}'>\\n\");\n";
    	final String actual = parse(input);
        assertEquals(flatten(expected), flatten(actual));
    }
    
    /** Test a complete template, using all features */
    public void testCompleteTemplate() throws IOException {
        final String input =
            "<html>\n"
            + "<head><title><%= someExpr %></title></head>\n"
            + "<!-- some HTML comment -->\n"
            + "<-- some ESP comment -->\n"
            + "// some javascript comment\n"
            + "/* another javascript comment /*\n"
            + "<%\n"
            + "expr on\n"
            + "two lines\n"
            + "%>\n"
            + "<verbatim stuff=\"quoted\">xyz</verbatim>\n"
            + "<moreverbatim stuff=\'single\'>xx</moreverbatim>\n"
            + "<!-- HTML comment with <% expr.here; %> and EOL\n-->\n"
            + "</html>"
        ;
        
        final String expected = 
            "out=response.writer;out.write(\"<html>\\n\");\n"
            + "out.write(\"<head><title>\");out.write( someExpr );out.write(\"</title></head>\\n\");\n"
            + "out.write(\"<!-- some HTML comment -->\\n\");\n"
            + "out.write(\"<-- some ESP comment -->\\n\");\n"
            + "out.write(\"// some javascript comment\\n\");\n"
            + "out.write(\"/* another javascript comment /*\\n\");\n"
            + "\n"
            + "expr on\n"
            + "two lines\n"
            + "out.write(\"\\n\");\n"
            + "out.write(\"<verbatim stuff=\\\"quoted\\\">xyz</verbatim>\\n\");\n"
            + "out.write(\"<moreverbatim stuff='single'>xx</moreverbatim>\\n\");\n"
            + "out.write(\"<!-- HTML comment with \"); expr.here; out.write(\" and EOL\\n\");\n"
            + "out.write(\"-->\\n\");\n"
            + "out.write(\"</html>\");"
        ;
        
        final String actual = parse(input);
        assertEquals(flatten(expected), flatten(actual));
    }

    /** Test a complete template, using all features */
    public void testNumericExpression() throws IOException {
        String input = "<%= 1 %>";
        String expected = "out=response.writer;out.write( 1 );";
        String actual = parse(input);
        assertEquals(expected, actual);
        
        input = "<%= \"1\" %>";
        expected = "out=response.writer;out.write( \"1\" );";
        actual = parse(input);
        assertEquals(expected, actual);
        
        input = "<%= '1' %>";
        expected = "out=response.writer;out.write( '1' );";
        actual = parse(input);
        assertEquals(expected, actual);
    }
    
    /** Test a complete template, using all features */
    public void testNumericExpressionOutput() throws ScriptException {
        ScriptEngineHelper script = new ScriptEngineHelper();
        
        String input = "out.write( 1 );";
        String actual = script.evalToString(input);
        String expected = "1";
        assertEquals(expected, actual);

        input = "out.write( \"1\" );";
        actual = script.evalToString(input);
        expected = "1";
        assertEquals(expected, actual);

        input = "out.write( '1' );";
        actual = script.evalToString(input);
        expected = "1";
        assertEquals(expected, actual);
    }
    
    public void testColon() throws IOException {
        final String input = "currentNode.text:<%= currentNode.text %>";
        final String expected = 
            "out=response.writer;" 
            + "out.write(\"currentNode.text:\");"
            + "out.write( currentNode.text );"
            ;
        final String actual = parse(input);
        assertEquals(expected, actual);
    }
    
    public void testEqualSigns() throws IOException {
        final String input = "currentNode.text=<%= currentNode.text %>";
        final String expected = 
            "out=response.writer;" 
            + "out.write(\"currentNode.text=\");"
            + "out.write( currentNode.text );"
            ;
        final String actual = parse(input);
        assertEquals(expected, actual);
    }
    
    public void testSingleQuoted() throws IOException {
        final String input = "currentNode.text='<%= currentNode.text %>'";
        final String expected = 
            "out=response.writer;" 
            + "out.write(\"currentNode.text='\");"
            + "out.write( currentNode.text );"
            + "out.write(\"'\");"
            ;
        final String actual = parse(input);
        assertEquals(expected, actual);
    }
    
    public void testDoubleQuoted() throws IOException {
        final String input = "currentNode.text=\"<%= currentNode.text %>\"";
        final String expected = 
            "out=response.writer;" 
            + "out.write(\"currentNode.text=\\\"\");"
            + "out.write( currentNode.text );"
            + "out.write(\"\\\"\");"
            ;
        final String actual = parse(input);
        assertEquals(expected, actual);
    }
    
    /** Helper to pass an ESP text through the EspReader and return the result */
    private String parse(String text) throws IOException {
        StringBuffer buf = new StringBuffer();

        Reader r = new EspReader(new StringReader(text));
        int c;
        while ( (c=r.read()) >= 0) {
            buf.append( (char) c);
        }

        return buf.toString();
    }
    
    /** Replace \n with . in strings to make it easier to compare visually for testing */
    private static String flatten(String str) {
        return str.replace('\n', '.');
    }
}
