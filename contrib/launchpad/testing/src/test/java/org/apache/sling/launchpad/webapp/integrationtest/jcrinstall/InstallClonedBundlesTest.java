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

/** Try installing some cloned bundles */
public class InstallClonedBundlesTest extends JcrinstallTestBase {
	
	public void testInstallAndRemoveBundles() throws Exception {
		final int activeBeforeTest = getActiveBundlesCount();
		final List<String> installed = new LinkedList<String>();
		
		final int nBundles = 10 * scaleFactor;
		for(int i=0 ; i < nBundles; i++) {
			installed.add(installClonedBundle(null, null));
		}
		
		assertActiveBundleCount("after adding bundles", 
				activeBeforeTest + nBundles, defaultBundlesTimeout);
		
		for(String path : installed) {
			removeClonedBundle(path);
		}
		
		assertActiveBundleCount("after removing added bundles", 
				activeBeforeTest, defaultBundlesTimeout);
	}
}
