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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Simple test of the jcrinstall configuration feature: create 
 * 	sling:OsgiConfig nodes and check that the corresponding configs
 *  are available at /system/console/config
 *
 */
public class SimpleConfigTest extends JcrinstallTestBase {
	
	static class ConfigCondition implements JcrinstallTestBase.StringCondition {
		private final String expectedValue;
		private final boolean expectValueInContent;
		
		ConfigCondition(String expectedValue, boolean expectValueInContent) {
			this.expectedValue = expectedValue;
			this.expectValueInContent = expectValueInContent;
		}
		public boolean eval(String input) {
			final boolean contains = input.contains(expectedValue);
			return expectValueInContent ? contains : !contains;
		}
	};
	
	public void testSimpleConfig() throws IOException {
		final String uniqueId = getClass().getName() + System.currentTimeMillis(); 
		final String key = getClass().getName() + ".key";
		final String value = getClass().getName() + "." + uniqueId;
		final String keyValue = key + "=" + value;
		
		final String configUrl = HTTP_BASE_URL + "/system/console/config";
		final String contentType = CONTENT_TYPE_HTML;
		final int timeoutSeconds = 4;
		
		assertContentWithTimeout("Before test, config must not exist", configUrl, 
				contentType, new ConfigCondition(keyValue, false), timeoutSeconds);
		
		// Create an OSGi config using a sling:OsgiConfig node
		final String configPath = "/apps/" + getClass().getSimpleName() + "/install";
		testClient.mkdirs(HTTP_BASE_URL, configPath);
		final Map<String, String> nodeProperties = new HashMap<String, String>();
		nodeProperties.put("jcr:primaryType", "sling:OsgiConfig");
		nodeProperties.put(key, value);
		final String toDelete = testClient.createNode(HTTP_BASE_URL + configPath + "/" + uniqueId, nodeProperties);
		assertContentWithTimeout("Config must be present after creating config node", configUrl, 
				contentType, new ConfigCondition(keyValue, true), timeoutSeconds);
		
		// Update config node, verify that config is updated
		final String newValue = getClass().getName() + ".NEW." + System.currentTimeMillis();
		final String newKeyValue = key + "=" + newValue;
		nodeProperties.put(key, newValue);
		testClient.createNode(HTTP_BASE_URL + configPath + "/" + uniqueId, nodeProperties);
		assertContentWithTimeout("Config must be modified after node update", configUrl, 
				contentType, new ConfigCondition(newKeyValue, true), timeoutSeconds);
		assertContentWithTimeout("Old value must be gone after update", configUrl, 
				contentType, new ConfigCondition(keyValue, false), timeoutSeconds);
		
		// Delete and verify that the config is gone
		testClient.delete(toDelete);
		assertContentWithTimeout("Old config must be gone after removing config node", configUrl, 
				contentType, new ConfigCondition(keyValue, false), timeoutSeconds);
		assertContentWithTimeout("New config must be gone after removing config node", configUrl, 
				contentType, new ConfigCondition(newKeyValue, false), timeoutSeconds);
	}
}
