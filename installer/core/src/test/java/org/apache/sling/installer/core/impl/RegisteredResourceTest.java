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
package org.apache.sling.installer.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class RegisteredResourceTest {

    public static final String TEST_URL = "test:url";

    static File getTestBundle(String name) {
        return new File(System.getProperty("osgi.installer.base.dir"),
                "org.apache.sling.installer.core-" + System.getProperty("osgi.installer.pom.version") + "-" + name);
    }

    @org.junit.Test public void testResourceType() throws Exception {
        {
            final InputStream s = new FileInputStream(getTestBundle("testbundle-1.0.jar"));
            final RegisteredResource r = create(new InstallableResource("test:1.jar", s, null, "some digest", null, null));

            assertEquals(".jar URL creates a BUNDLE resource",
                    InstallableResource.TYPE_BUNDLE, r.getType());
            final InputStream rs = r.getInputStream();
            assertNotNull("BUNDLE resource provides an InputStream", rs);
            rs.close();
            assertNull("BUNDLE resource does not provide a Dictionary", r.getDictionary());
            assertEquals("RegisteredResource entity ID must match", "bundle:osgi-installer-testbundle", r.getEntityId());
        }

        {
            final Hashtable<String, Object> data = new Hashtable<String, Object>();
            data.put("foo", "bar");
            data.put("other", 2);
            final TaskResource r = create(new InstallableResource("configuration:1", null, data, null, null, null));
            assertEquals("No-extension URL with Dictionary creates a CONFIG resource",
                    InstallableResource.TYPE_CONFIG, r.getType());
            final InputStream rs = r.getInputStream();
            assertNull("CONFIG resource does not provide an InputStream", rs);
            final Dictionary<String, Object> d = r.getDictionary();
            assertNotNull("CONFIG resource provides a Dictionary", d);
            assertEquals("CONFIG resource dictionary has two properties", 2, d.size());
            assertNotNull("CONFIG resource has a pid attribute", r.getAttribute(Constants.SERVICE_PID));
        }
    }

	@org.junit.Test public void testLocalFileCopy() throws Exception {
	    final File localFile = File.createTempFile("testLocalFileCopy", ".data");
        localFile.deleteOnExit();
	    final BundleContext bc = new MockBundleContext();
	    final File f = getTestBundle("testbundle-1.0.jar");
        final InputStream s = new FileInputStream(f);
        FileDataStore.SHARED = new FileDataStore(bc) {

            @Override
            public File createNewDataFile(InputStream stream, String url,
                    String digest, String hint) throws IOException {
                this.copyToLocalStorage(stream, localFile);
                return localFile;
            }

        };

		InternalResource.create("test", new InstallableResource("test:1.jar", s, null, "somedigest", null, null));
		assertTrue("Local file exists", localFile.exists());

		assertEquals("Local file length matches our data", f.length(), localFile.length());
	}

    @org.junit.Test public void testMissingDigest() throws Exception {
        final String data = "This is some data";
        final InputStream in = new ByteArrayInputStream(data.getBytes());

        create(new InstallableResource("test:1.jar", in, null, null, null, null));
        assertNotNull(create(new InstallableResource("test:1.jar", in, null, null, null, null)).getDigest());
    }

    @org.junit.Test public void testBundleManifest() throws Exception {
        final File f = getTestBundle("testbundle-1.0.jar");
        final InstallableResource i = new InstallableResource("test:" + f.getAbsolutePath(), new FileInputStream(f), null, f.getName(), null, null);
        final TaskResource r = create(i);
        assertNotNull("RegisteredResource must have bundle symbolic name", r.getAttribute(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "bundle:osgi-installer-testbundle", r.getEntityId());
    }

//    @org.junit.Test
    public void testConfigEntity() throws Exception {
        final InstallableResource i = new InstallableResource("test:/foo/someconfig", null, new Hashtable<String, Object>(), null, null, null);
        final TaskResource r = create(i);
        assertNull("RegisteredResource must not have bundle symbolic name", r.getAttribute(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "config:someconfig", r.getEntityId());
    }

    @org.junit.Test public void testConfigDigestIncludesUrl() throws Exception {
        final Dictionary<String, Object> data = new Hashtable<String, Object>();
        final InstallableResource rA = new InstallableResource("test:urlA", null, data, null, null, null);
        final InstallableResource rB = new InstallableResource("test:urlB", null, data, null, null, null);
        assertTrue(
                "Expecting configs with same data but different URLs to have same digests",
                create(rA).getDigest().equals(create(rB).getDigest()));
    }

    @Test
    public void testDictionaryDigest() throws IOException {
        final Dictionary<String, Object> d = new Hashtable<String, Object>();
        final InstallableResource r = new InstallableResource("x:url", null, d, null, null, null);
        assertNotNull("Expected RegisteredResource to compute its own digest", create(r).getDigest());
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

        final InstallableResource r1 = new InstallableResource("test:url1", null, d1, null, null, null);
        final InstallableResource r2 = new InstallableResource("test:url1", null, d2, null, null, null);

        assertEquals(
                "Two InstallableResource (Dictionary) with same values but different key orderings must have the same key",
                create(r1).getDigest(),
                create(r2).getDigest()
        );
    }

    private TaskResource create(final InstallableResource is) throws IOException {
        new FileDataStore(new MockBundleContext());
        final InternalResource internal = InternalResource.create("test", is);
        final RegisteredResourceImpl rr = RegisteredResourceImpl.create(internal);
        final TransformationResult[] tr = new DefaultTransformer().transform(rr);
        if ( tr != null ) {
            return rr.clone(tr[0]);
        } else if ( is.getId().startsWith("configuration:") ) {
            final TransformationResult result = new TransformationResult();
            result.setId(is.getId().substring(14));
            result.setResourceType(InstallableResource.TYPE_CONFIG);
            result.setAttributes(Collections.singletonMap(Constants.SERVICE_PID, (Object)result.getId()));
            return rr.clone(result);
        }
        return rr;
    }
}