/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.microsling.scripting.helpers;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.sling.microsling.scripting.helpers.EspReader;

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
}
