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
package org.apache.sling.commons.osgi.bundleversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Test the BundleBundleVersionInfo class - not extremely
 *  valid is we're testing with mock Bundles, but should
 *  at least catch regressions.
 */
public class BundleBundleVersionInfoTest {
    protected Mockery context;
    
    @Before
    public void setUp() {
        context = new JUnit4Mockery();
    }

    private Bundle getMockBundle(final String symbolicName, final Version v, final long lastModified) {
        final Dictionary<String, Object> h = new Hashtable<String, Object>();
        h.put(Constants.BUNDLE_VERSION, v);
        if(lastModified > 0) {
            h.put(BundleVersionInfo.BND_LAST_MODIFIED, String.valueOf(lastModified));
        }
        
        final Bundle b = context.mock(Bundle.class);
        context.checking(new Expectations() {{
            allowing(b).getHeaders();
            will(returnValue(h));
            allowing(b).getSymbolicName();
            will(returnValue(symbolicName));
            allowing(b).getLastModified();
            will(returnValue(lastModified));
        }});
        return b;
    }
    
    @Test
    public void testVersionInfo() {
        final String name = "some.bundle";
        final Version version = new Version("1.0.4");
        final long lastMod = 1234L;
        final Bundle b = getMockBundle(name, version, lastMod); 
        
        BundleVersionInfo<?> vi = new BundleBundleVersionInfo(b);
        assertEquals("Symbolic name matches", name, vi.getBundleSymbolicName());
        assertEquals("Version matches", version, vi.getVersion());
        assertTrue("isBundle", vi.isBundle());
        assertFalse("Not a snapshot", vi.isSnapshot());
        assertEquals("Last-Modified matches", lastMod, vi.getBundleLastModified());
        assertTrue("Bundle is stored as source", vi.getSource() == b);
    }
    
    @Test
    public void testSnapshot() {
        final String name = "some.bundle";
        final Version version = new Version("1.0.4.SNAPSHOT");
        final long lastMod = 0;
        final Bundle b = getMockBundle(name, version, lastMod); 
        
        BundleVersionInfo<?> vi = new BundleBundleVersionInfo(b);
        assertEquals("Symbolic name matches", name, vi.getBundleSymbolicName());
        assertEquals("Version matches", version, vi.getVersion());
        assertTrue("isBundle", vi.isBundle());
        assertTrue("Bundle is a snapshot", vi.isSnapshot());
        assertEquals("Last-Modified matches", BundleVersionInfo.BND_LAST_MODIFIED_MISSING, vi.getBundleLastModified());
        assertTrue("Bundle is stored as source", vi.getSource() == b);
   }
}