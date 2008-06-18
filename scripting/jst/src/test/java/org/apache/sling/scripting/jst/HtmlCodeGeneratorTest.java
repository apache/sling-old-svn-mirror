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
package org.apache.sling.scripting.jst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

/** Test the JsCodeGenerator */
public class HtmlCodeGeneratorTest extends TestCase {

    public static final String ENCODING = "UTF-8";
    public static final String TEST_RESOURCES_PATH = "/htmlcode";
    private final HtmlCodeGenerator generator = new HtmlCodeGenerator();
    
    /** Pass a .input.jst test file through the HtmlCodeGenerator,
     *  and verify that the result matches the corresponding
     *  .expected.html file. 
     */
    protected boolean runTest(int index) throws Exception {
        final String scriptPath = TEST_RESOURCES_PATH + "/htmlcode" + index + ".input.jst";
        final InputStream scriptStream = getClass().getResourceAsStream(scriptPath);
        if(scriptStream == null) {
            // no more test files
            return false;
        }
        assertNotNull(scriptPath + " must be found", scriptStream);
        
        final String expectedCode = StringUtil.readClassResource(
                getClass(), TEST_RESOURCES_PATH + "/htmlcode" + index + ".expected.html", ENCODING);
        final StringWriter result = new StringWriter();
        final SlingHttpServletRequest request = getMockRequest();
        generator.generateHtml(request, scriptPath, scriptStream, new PrintWriter(result));
        compareLineByLine(expectedCode.toString(), result.toString());
        
        return true;
    }
    
    /** Read until a non-blank line is found */
    protected String readLine(BufferedReader br) throws IOException {
        String result = null;
        while(true) {
            result = br.readLine();
            if(result == null) {
                break;
            }
            result = result.trim();
            if(result.indexOf("<!-- TODO TEST") >= 0) {
                continue;
            }
            if(result.length() > 0) {
                break;
            }
        }
        return result;
    }
    
    protected void compareLineByLine(String expected, String actual) throws IOException {
        final BufferedReader ex = new BufferedReader(new StringReader(expected.trim()));
        final BufferedReader ac = new BufferedReader(new StringReader(actual.trim()));
        boolean ok = false;
        
        try {
            for(int line=1; true; line++) {
                final String exLine = readLine(ex);
                final String acLine = readLine(ac);
                
                if(exLine == null && acLine==null) {
                    ok = true;
                    break;
                } else if(exLine == null && acLine != null) {
                    fail("At line " + line + ", actual code has more lines than expected");
                } else {
                    assertEquals("Line " + line + " must match", exLine, acLine);
                }
            }
            
        } finally {
            if(!ok) {
                System.err.println("Expected code:\n" + expected);
                System.err.println("Actual code:\n" + actual);
            }
        }
    }
     
    protected SlingHttpServletRequest getMockRequest() {
        final String resourcePath = "foo";
        final MockSlingHttpServletRequest r = new MockSlingHttpServletRequest(resourcePath,null,null,null,null) {
            @Override
            public String getContextPath() {
                return "/CONTEXT";
            }

            @Override
            public String getServletPath() {
                return "/SERVLET";
            }
        };
        
        final String path = "/foo/node";
        
        final MockNode mn = new MockNode(path);
        try {
            mn.setProperty("title", "test.title");
            mn.setProperty("desc", "test.desc");
        } catch(RepositoryException ignored) {
            // ignore, cannot happen with this mock class
        }
        
        final MockResource mr = new MockResource(null, path, null) {
            @SuppressWarnings("unchecked")
            public <Type> Type adaptTo(Class<Type> type) {
                if(type.equals(Node.class)) {
                    return (Type)mn;
                } else {
                    return null;
                }
            }
        };
        
        r.setResource(mr);
        
        return r;
    }
    
    public void testHtmlcodeTestFiles() throws Exception {
        int maxTestFiles = 999;
        int expectedTests = 1;
        int counter = 0;
        
        for(int i=1 ; i <= maxTestFiles; i++) {
            if(!runTest(i)) {
                break;
            }
            counter++;
        }
        
        assertTrue("At least " + expectedTests + " have been run", counter >= expectedTests);
    }
    
    public void testTitleBuilding() throws RepositoryException {
        final String path = "/foo/title";
        final MockNode n = new MockNode(path);
        final MockResource r = new MockResource(null, path, null);
        
        assertEquals("foo/title", generator.getTitle(r, n));
        
        n.setProperty("description", "the description");
        assertEquals("the description", generator.getTitle(r, n));
        
        n.setProperty("name", "the name");
        assertEquals("the name", generator.getTitle(r, n));
        
        n.setProperty("title", "the title");
        assertEquals("the title", generator.getTitle(r, n));
    }
}