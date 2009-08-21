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
package org.apache.sling.osgi.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.installer.InstallableResource;
import org.osgi.framework.Constants;

public class RegisteredResourceTest {
	
    public static final String TEST_URL = "test:url";
    
    static File getTestBundle(String name) {
        return new File(System.getProperty("osgi.installer.base.dir"),
                "org.apache.sling.osgi.installer-" + System.getProperty("osgi.installer.pom.version") + "-" + name);
    }
    
	@org.junit.Test public void testStreamIsClosed() throws Exception {
		final String data = "some data";
		
		class TestInputStream extends FilterInputStream {
			int closeCount;
			
			TestInputStream(InputStream i) {
				super(i);
			}

			@Override
			public void close() throws IOException {
				super.close();
				closeCount++;
			}
			
		}
		
		final TestInputStream t = new TestInputStream(new ByteArrayInputStream(data.getBytes()));
		final InstallableResource ir = new InstallableResource(TEST_URL, t, "somedigest");
		assertEquals("TestInputStream must not be closed before test", 0, t.closeCount);
		new LocalFileRegisteredResource(ir);
		assertEquals("TestInputStream must be closed by RegisteredResource", 1, t.closeCount);
	}
	
    @org.junit.Test public void testResourceType() throws Exception {
        {
            final InputStream s = new ByteArrayInputStream("Some data".getBytes());
            final RegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1.jar", s, "some digest"));
            assertEquals(".jar URL creates a BUNDLE resource", 
                    RegisteredResource.ResourceType.BUNDLE, r.getResourceType());
            final InputStream rs = r.getInputStream();
            assertNotNull("BUNDLE resource provides an InputStream", rs);
            rs.close();
            assertNull("BUNDLE resource does not provide a Dictionary", r.getDictionary());
            assertEquals("RegisteredResource entity ID must match", "jar:test:1.jar", r.getEntityId());
        }
        
        {
            final InputStream s = new ByteArrayInputStream("foo=bar\nother=2".getBytes());
            final RegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1.properties", s, null));
            assertEquals(".properties URL creates a CONFIG resource", 
                    RegisteredResource.ResourceType.CONFIG, r.getResourceType());
            final InputStream rs = r.getInputStream();
            assertNull("CONFIG resource does not provide an InputStream", rs);
            final Dictionary<String, Object> d = r.getDictionary();
            assertNotNull("CONFIG resource provides a Dictionary", d);
            assertEquals("CONFIG resource dictionary has two properties", 2, d.size());
            assertNotNull("CONFIG resource has a pid attribute", r.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE));
        }
        
        {
            final Hashtable<String, Object> data = new Hashtable<String, Object>();
            data.put("foo", "bar");
            data.put("other", 2);
            final RegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1", data));
            assertEquals("No-extension URL with Dictionary creates a CONFIG resource", 
                    RegisteredResource.ResourceType.CONFIG, r.getResourceType());
            final InputStream rs = r.getInputStream();
            assertNull("CONFIG resource does not provide an InputStream", rs);
            final Dictionary<String, Object> d = r.getDictionary();
            assertNotNull("CONFIG resource provides a Dictionary", d);
            assertEquals("CONFIG resource dictionary has two properties", 2, d.size());
            assertNotNull("CONFIG resource has a pid attribute", r.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE));
        }
    }
    
	@org.junit.Test public void testLocalFileCopy() throws Exception {
		final String data = "This is some data";
		final InputStream in = new ByteArrayInputStream(data.getBytes());
		final LocalFileRegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1.jar", in, "somedigest"));
		assertTrue("Local file exists", r.getDataFile(null).exists());
		assertEquals("Local file length matches our data", data.getBytes().length, r.getDataFile(null).length());
	}
	
    @org.junit.Test public void testMissingDigest() throws Exception {
        final String data = "This is some data";
        final InputStream in = new ByteArrayInputStream(data.getBytes());
        
        try {
            new LocalFileRegisteredResource(new InstallableResource("test:1.jar", in, null));
            fail("With jar extension, expected an IllegalArgumentException as digest is null");
        } catch(IllegalArgumentException asExpected) {
        }
        
        try {
            new LocalFileRegisteredResource(new InstallableResource("test:1.foo", in, null));
        } catch(IllegalArgumentException asExpected) {
            fail("With non-jar extension, did not expect an IllegalArgumentException if digest is null");
        }
    }
    
    @org.junit.Test public void testDictionaryDigestFromDictionaries() throws Exception {
        final Hashtable<String, Object> d1 = new Hashtable<String, Object>();
        final Hashtable<String, Object> d2 = new Hashtable<String, Object>();
        
        final String [] keys = { "foo", "bar", "something" };
        for(int i=0 ; i < keys.length; i++) {
            d1.put(keys[i], keys[i] + "." + keys[i]);
        }
        for(int i=keys.length - 1 ; i >= 0; i--) {
            d2.put(keys[i], keys[i] + "." + keys[i]);
        }
        
        final RegisteredResource r1 = new RegisteredResourceImpl(null, new InstallableResource("test:url1", d1));
        final RegisteredResource r2 = new RegisteredResourceImpl(null, new InstallableResource("test:url1", d2));
        
        assertEquals(
                "Two RegisteredResource (Dictionary) with same values but different key orderings must have the same key", 
                r1.getDigest(),
                r2.getDigest()
        );
    }
    
    @org.junit.Test public void testDictionaryDigestFromInputStreams() throws Exception {
        final String d1 = "foo=bar\nsomething=another\n";
        final String d2 = "something=another\nfoo=bar\n";
        
        final ByteArrayInputStream s1 = new ByteArrayInputStream(d1.getBytes());
        final ByteArrayInputStream s2 = new ByteArrayInputStream(d2.getBytes());
        
        final RegisteredResource r1 = new RegisteredResourceImpl(null, new InstallableResource("test:url1.properties", s1, null));
        final RegisteredResource r2 = new RegisteredResourceImpl(null, new InstallableResource("test:url2.properties", s2, null));
        
        assertEquals(
                "Two RegisteredResource (InputStream) with same values but different key orderings must have the same key", 
                r1.getDigest(),
                r2.getDigest()
        );
    }
    
    @org.junit.Test public void testBundleManifest() throws Exception {
        final File f = getTestBundle("testbundle-1.0.jar");
        final InstallableResource i = new InstallableResource("test:" + f.getAbsolutePath(), new FileInputStream(f), f.getName());
        final RegisteredResource r = new LocalFileRegisteredResource(i);
        assertNotNull("RegisteredResource must have bundle symbolic name", r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "bundle:osgi-installer-testbundle", r.getEntityId());
    }
    
    @org.junit.Test public void testConfigEntity() throws Exception {
        final InstallableResource i = new InstallableResource("test:/foo/someconfig", new Hashtable<String, Object>());
        final RegisteredResource r = new LocalFileRegisteredResource(i);
        assertNull("RegisteredResource must not have bundle symbolic name", r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "config:someconfig", r.getEntityId());
    }
    
    @org.junit.Test public void testUrlScheme() throws Exception {
        final ByteArrayInputStream s = new ByteArrayInputStream("foo".getBytes());
        

        final String [] badOnes = {
                "",
                ":colonTooEarly",
                ":colonTooEarlyAgain:",
                "noColon"
        };
        for(String url : badOnes) {
            try {
                new RegisteredResourceImpl(null, new InstallableResource(url, s, null));
                fail("Expected bad URL '" + url + "' to throw IllegalArgumentException");
            } catch(IllegalArgumentException asExpected) {
            }
        }
        
        final String [] goodOnes = {
            "foo:bar",
            "foo:bar:",
            "foo::bar",
            "foo://bar",
        };
        
        for(String url : goodOnes) {
            final RegisteredResource r = new RegisteredResourceImpl(null, new InstallableResource(url, s, null));
            assertEquals("Expected scheme 'foo' for URL " + url, "foo", r.getUrlScheme());
        }
    }
}