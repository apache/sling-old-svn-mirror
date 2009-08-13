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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.osgi.installer.InstallableResource;

public class RegisteredResourceTest {
	
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
		final InstallableResource ir = new InstallableResource(data, t);
		assertEquals("TestInputStream must not be closed before test", 0, t.closeCount);
		new LocalFileRegisteredResource(ir);
		assertEquals("TestInputStream must be closed by RegisteredResource", 1, t.closeCount);
	}
	
	@org.junit.Test public void testLocalFileCopy() throws Exception {
		final String data = "This is some data";
		final InputStream in = new ByteArrayInputStream(data.getBytes());
		final LocalFileRegisteredResource r = new LocalFileRegisteredResource(new InstallableResource(data, in));
		assertTrue("Local file exists", r.getDataFile(null).exists());
		assertEquals("Local file length matches our data", data.getBytes().length, r.getDataFile(null).length());
	}
	
}
