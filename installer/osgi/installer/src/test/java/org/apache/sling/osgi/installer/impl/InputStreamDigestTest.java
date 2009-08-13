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

import java.io.ByteArrayInputStream;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.apache.sling.osgi.installer.InstallableResource;

public class InputStreamDigestTest {
	
	private InstallableResource getInstallableResource(String data) {
		return new InstallableResource(data, new ByteArrayInputStream(data.getBytes()));
	}
	
	@org.junit.Test public void testStreamDigest() throws Exception {
		final String data1 = "This is some data";
		final String data2 = "This is more data, which is different";
	
		final RegisteredResource r1 = new LocalFileRegisteredResource(getInstallableResource(data1));
		final RegisteredResource r2 = new LocalFileRegisteredResource(getInstallableResource(data1));
		final RegisteredResource r3 = new LocalFileRegisteredResource(getInstallableResource(data2));
		
		assertTrue("r1 has non-empty digest", r1.getDigest().length() > 5);
		assertTrue("r2 has non-empty digest", r2.getDigest().length() > 5);
		assertTrue("r3 has non-empty digest", r3.getDigest().length() > 5);
		
		assertEquals("r1 and r2 have same digest", r1.getDigest(), r2.getDigest());
		assertFalse("r1 and r3 have different digest", r1.getDigest().equals(r3.getDigest()));
	}

}
