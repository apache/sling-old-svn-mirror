/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
/*
 * This class was taken from the ASF repo at http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/tags/HTTPCLIENT_3_1/src/test/org/apache/commons/httpclient/TestURI.java?revision=567248&view=co
 * to provide coverage for the URI class from the same source that is embedded in this bundle. 
 * 
 * It has minor modifications to eliminate dependencies on other parts of the HttpClient 3.1 package.
 */
package org.apache.sling.resourceresolver.impl.helper;



import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple tests for the URI class.
 * 
 * @author Michael Becke
 */
public class URITest extends TestCase {

    /**
     * Constructor for TestURI.
     * @param testName
     */
    public URITest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return new TestSuite(URITest.class);
    }
    
    public void testIPv4Address() throws URIException {

        URI base = new URI("http://10.0.1.10:8830", false);
        
        URI uri = base;        
        assertTrue("Should be an IPv4 address", uri.isIPv4address());
            
        uri = new URI(base, "/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());

        uri = new URI("/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());

        uri = new URI(base, "http://10.0.1.10:8830/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());

        uri = new URI("http://10.0.1.10:8830/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());

        uri = new URI(base, "http://host.org/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());

        uri = new URI("http://host.org/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());
        
    }
    
    public void testUrl() throws URIException {
        URI url = new URI("http://jakarta.apache.org", false);
        assertEquals(-1, url.getPort()); // URI itself has no knowledge of default ports.
        assertEquals("http", url.getScheme());
        
        url = new URI("https://jakarta.apache.org", false);
        assertEquals(-1, url.getPort()); // URI itself has no knowledge of default ports.
        assertEquals("https", url.getScheme());
    }
    
    /**
     * Tests the URI(URI, String) constructor.  This tests URIs ability to
     * resolve relative URIs.
     */
    public void testRelativeURIConstructor() {
        
        URI baseURI = null;
        
        try {
            baseURI = new URI("http://a/b/c/d;p?q", false);
        } catch ( URIException e ) {
            fail( "unable to create base URI: " + e );
        }
        
        // the following is an array of arrays in the following order
        // relative URI, scheme, host(authority), path, query, fragment, abs. URI
        //
        // these examples were taken from rfc 2396
        String[][] testRelativeURIs = {
            { "g:h", "g", null, "h", null, null, "g:h" },
            { "g", "http", "a", "/b/c/g", null, null, "http://a/b/c/g" },
            { "./g", "http", "a", "/b/c/g", null, null, "http://a/b/c/g" },
            { "g/", "http", "a", "/b/c/g/", null, null, "http://a/b/c/g/" },
            { "/g", "http", "a", "/g", null, null, "http://a/g" },
            { "//g", "http", "g", null, null, null, "http://g" },
            { "?y", "http", "a", "/b/c/d;p", "y", null, "http://a/b/c/d;p?y" },
            { "g?y", "http", "a", "/b/c/g", "y", null, "http://a/b/c/g?y" },
            { "#s", "http", "a", "/b/c/d;p", "q", "s", "http://a/b/c/d;p?q#s" },
            { "#", "http", "a", "/b/c/d;p", "q", "", "http://a/b/c/d;p?q#" },
            { "", "http", "a", "/b/c/d;p", "q", null, "http://a/b/c/d;p?q" },
            { "g#s", "http", "a", "/b/c/g", null, "s", "http://a/b/c/g#s" },
            { "g?y#s","http", "a", "/b/c/g", "y", "s", "http://a/b/c/g?y#s" },
            { ";x", "http", "a", "/b/c/;x", null, null, "http://a/b/c/;x" },
            { "g;x", "http", "a", "/b/c/g;x", null, null, "http://a/b/c/g;x" },
            { "g;x?y#s", "http", "a", "/b/c/g;x", "y", "s", "http://a/b/c/g;x?y#s" },
            { ".", "http", "a", "/b/c/", null, null, "http://a/b/c/" },
            { "./", "http", "a", "/b/c/", null, null, "http://a/b/c/" },
            { "..", "http", "a", "/b/", null, null, "http://a/b/" },
            { "../", "http", "a", "/b/", null, null, "http://a/b/" },
            { "../g", "http", "a", "/b/g", null, null, "http://a/b/g" },
            { "../..", "http", "a", "/", null, null, "http://a/" },
            { "../../", "http", "a", "/", null, null, "http://a/" },
            { "../../g", "http", "a", "/g", null, null, "http://a/g" },
            { "../../../g", "http", "a", "/g", null, null, "http://a/g" },
            { "../../../../g", "http", "a", "/g", null, null, "http://a/g" },
            { "/./g", "http", "a", "/g", null, null, "http://a/g" },
            { "/../g", "http", "a", "/g", null, null, "http://a/g" },
            { "g.", "http", "a", "/b/c/g.", null, null, "http://a/b/c/g." },
            { ".g", "http", "a", "/b/c/.g", null, null, "http://a/b/c/.g" },
            { "g..", "http", "a", "/b/c/g..", null, null, "http://a/b/c/g.." },
            { "..g", "http", "a", "/b/c/..g", null, null, "http://a/b/c/..g" },
            { "./../g", "http", "a", "/b/g", null, null, "http://a/b/g" },
            { "./g/.", "http", "a", "/b/c/g/", null, null, "http://a/b/c/g/" },
            { "g/./h", "http", "a", "/b/c/g/h", null, null, "http://a/b/c/g/h" },
            { "g/../h", "http", "a", "/b/c/h", null, null, "http://a/b/c/h" },
            { "g;x=1/./y", "http", "a", "/b/c/g;x=1/y", null, null, "http://a/b/c/g;x=1/y" },
            { "g;x=1/../y", "http", "a", "/b/c/y", null, null, "http://a/b/c/y" },
            { "g?y/./x", "http", "a", "/b/c/g", "y/./x", null, "http://a/b/c/g?y/./x" },
            { "g?y/../x", "http", "a", "/b/c/g", "y/../x", null, "http://a/b/c/g?y/../x" },
            { "g#s/./x", "http", "a", "/b/c/g", null, "s/./x", "http://a/b/c/g#s/./x" },
            { "g#s/../x", "http", "a", "/b/c/g", null, "s/../x", "http://a/b/c/g#s/../x" },
            { ":g", "http", "a", "/b/c/:g", null, null, "http://a/b/c/:g" }, // see issue #35148
            { "//a/b/c", "http", "a", "/b/c", null, null, "http://a/b/c" } // see HTTPCLIENT-580 
        };
        for (int i = 0; i < testRelativeURIs.length; i++) {
            URI testURI = null;
            
            try {
                testURI = new URI( baseURI, testRelativeURIs[i][0], false );
            } catch ( URIException e ) {
                e.printStackTrace();
                fail( 
                    "unable to create URI with relative value(" 
                    + testRelativeURIs[i][0] + "): " + e 
                );   
            }
            
            try {
                assertEquals("array index "+i, testRelativeURIs[i][1], testURI.getScheme());
                assertEquals("array index "+i, testRelativeURIs[i][2], testURI.getAuthority());
                assertEquals("array index "+i, testRelativeURIs[i][3], testURI.getPath());
                assertEquals("array index "+i, testRelativeURIs[i][4], testURI.getQuery());
                assertEquals("array index "+i, testRelativeURIs[i][5], testURI.getFragment());
                assertEquals("array index "+i, testRelativeURIs[i][6], testURI.getURIReference());
            } catch ( URIException e ) {
                fail( "error getting URI property: " + e );
            }            
        }
        
    }

    public void testTestURIAuthorityString() throws Exception {
        URI url = new URI("ftp", "user:password", "localhost", -1, "/");
        assertEquals("ftp://user:password@localhost/", url.toString());
        assertEquals("user:password@localhost", url.getAuthority());
    }
    
    

    
    public void testVariousCharacters() throws Exception {
        verifyInvalidURI("http://authority:123/path/path?query&name=val ue");
        verifyInvalidURI("http://authority:123/path/path?query&na me=value");
        verifyInvalidURI("http://authority:123/path/path?qu ery&name=value");
        verifyInvalidURI("http://authority:123/path/pa th?query&name=value");
        verifyInvalidURI("http://authority:123/pa th/path?query&name=value");
        verifyInvalidURI("http://authority:12 3/path/path?query&name=value");
        verifyInvalidURI("http://autho rity:123/path/path?query&name=value");
        verifyInvalidURI("htt p://authority:123/path/path?query&name=value");
    }
    
    private void verifyInvalidURI(String uri) {
        try {
            new URI(uri, true);
            fail("should have thrown URIException");
        } catch(URIException e) {
            /* expected */
        }
    }    
    
    /**
     * Verify proper handling of relative URIs which have a scheme. 
     * See bug http://issues.apache.org/jira/browse/HTTPCLIENT-587
     * 
     * @throws Exception
     */
    public void testRelativeWithScheme() throws Exception {
        URI base = new URI("http://www.example.com/some/path", true);
        URI rel1 = new URI("http:", true);
        URI rel2 = new URI("http:foo", true);
        URI rel3 = new URI("http:../../bar", true);
        URI derel1 = new URI(base, rel1);
        assertEquals("http://www.example.com/some/path",derel1.toString());
        URI derel2 = new URI(base, rel2);
        assertEquals("http://www.example.com/some/foo",derel2.toString());
        URI derel3 = new URI(base,rel3);
        assertEquals("http://www.example.com/bar",derel3.toString());
    }
    
    /**
     * Verify proper handling of relative URIs with embedded double-slashes,
     * like "foo//bar//baz". 
     * See bug http://issues.apache.org/jira/browse/HTTPCLIENT-588
     * 
     * @throws Exception
     */
    public void testRelativeWithDoubleSlash() throws Exception {
        URI rel = new URI("foo//bar//baz",true);
        assertEquals("foo//bar//baz",rel.toString());
    }
    
}