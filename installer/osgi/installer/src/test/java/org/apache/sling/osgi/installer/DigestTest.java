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
package org.apache.sling.osgi.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

public class DigestTest {
	private void setTestData(Hashtable<String, Object> d) {
		d.put("str", "value");
		d.put("long", new Long(12));
		d.put("array", new String[] { "a", "b"});
	}
	
	private String testDigestChanged(Dictionary<String, Object> d, 
			String oldDigest, int step, boolean shouldChange) throws Exception {
		final String newDigest = DictionaryInstallableData.computeDigest(d);
		if(shouldChange) {
			assertTrue("Digest (" + newDigest + ") should have changed at step " + step, !newDigest.equals(oldDigest));
		} else {
			assertTrue("Digest (" + newDigest + ") should NOT have changed at step " + step, newDigest.equals(oldDigest));
		}
		return newDigest;
	}
	
	@org.junit.Test public void testDictionaryDigestSameData() throws Exception {
		final Hashtable<String, Object> d1 = new Hashtable<String, Object>();
		final Hashtable<String, Object> d2 = new Hashtable<String, Object>();
		
		setTestData(d1);
		setTestData(d2);
		
		assertEquals(
				"Two dictionary with same values have the same key", 
				DictionaryInstallableData.computeDigest(d1),
				DictionaryInstallableData.computeDigest(d2)
		);
	}
	
	@org.junit.Test public void testDictionaryDigestChanges() throws Exception {
		String digest = "";
		int step = 1;
		
		final Dictionary<String, Object> d = new Hashtable<String, Object>();
		digest = testDigestChanged(d, digest, step, true);
		digest = testDigestChanged(d, digest, step, false);
		
		d.put("key", "value");
		digest = testDigestChanged(d, digest, step, true);
		d.put("key", "value");
		digest = testDigestChanged(d, digest, step, false);
		
		d.put("int", new Integer(12));
		digest = testDigestChanged(d, digest, step, true);
		d.put("int", new Integer(12));
		digest = testDigestChanged(d, digest, step, false);
		d.put("int", new Integer(13));
		digest = testDigestChanged(d, digest, step, true);
		
		d.put("array", new String [] { "a", "b", "c"});
		digest = testDigestChanged(d, digest, step, true);
		d.put("array", new String [] { "a", "b", "c"});
		digest = testDigestChanged(d, digest, step, false);
		d.put("array", new String [] { "a", "b", "D"});
		digest = testDigestChanged(d, digest, step, true);
		d.put("another", new String [] { "a", "b", "D"});
		digest = testDigestChanged(d, digest, step, true);
	}
}
