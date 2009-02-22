/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.jcrinstall;

import java.util.LinkedList;
import java.util.List;

/** Test replacing bundles while jcrinstall is disabled
 */
public class StopAndChangeBundlesTest extends JcrinstallTestBase {
	
	public void testStopAndRestart() throws Exception {
		final int activeBeforeTest = getActiveBundlesCount();
		final List<String> installed = new LinkedList<String>();
		
		final int nBundlesA = 7 * scaleFactor;
		final int nBundlesB = 13 * scaleFactor;
		
		assertActiveBundleCount("before adding bundles", 
				activeBeforeTest, defaultBundlesTimeout);
		
		// Add a first set of bundles A
		for(int i=0 ; i < nBundlesA; i++) {
			installed.add(installClonedBundle(null, null));
		}
		
		// And check that they all start
		assertActiveBundleCount("after adding bundles", 
				activeBeforeTest + nBundlesA, defaultBundlesTimeout);
		
		// Disable jcrinstall (to simulate repository going away),
		// delete set A and add another set of bundles B
		enableJcrinstallService(false);
		
		for(String path : installed) {
			removeClonedBundle(path);
		}
		installed.clear();
		
		for(int i=0 ; i < nBundlesB; i++) {
			installed.add(installClonedBundle(null, null));
		}
		
		// Before reactivating, bundles count must be the initial count,
		// as jcrinstall brings the start level down
		assertActiveBundleCount("after replacing bundles", 
				activeBeforeTest, defaultBundlesTimeout);
		
		// Re-enable and verify that only original bundles + set B are active
		enableJcrinstallService(true);
		assertActiveBundleCount("after re-enabling jcrinstall", 
				activeBeforeTest + nBundlesB, defaultBundlesTimeout);
		
		// Remove everything and check
		for(String path : installed) {
			removeClonedBundle(path);
		}
		
		assertActiveBundleCount("after removing all added bundles", 
				activeBeforeTest, defaultBundlesTimeout);
	}
}
