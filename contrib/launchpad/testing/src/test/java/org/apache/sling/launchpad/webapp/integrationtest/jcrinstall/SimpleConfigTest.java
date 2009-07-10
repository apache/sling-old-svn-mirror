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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Simple test of the jcrinstall configuration feature: create 
 * 	sling:OsgiConfig nodes, or config files, and check that the corresponding configs
 *  are available at /system/console/config
 */
public class SimpleConfigTest extends JcrinstallTestBase {
	
	static final String CONFIG_URL = HTTP_BASE_URL + "/system/console/config";
	static final int TIMEOUT_SECONDS = 4;

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
	
	protected String createConfig(String configPid, Map<String, String> properties) throws IOException {
		return createConfig("/apps", configPid, properties);
	}
	
	protected String createConfig(String basePath, String configPid, Map<String, String> properties) throws IOException {
		final String configPath = basePath + "/" + getClass().getSimpleName() + "/install";
		testClient.mkdirs(HTTP_BASE_URL, configPath);
		properties.put("jcr:primaryType", "sling:OsgiConfig");
		return testClient.createNode(HTTP_BASE_URL + configPath + "/" + configPid, properties);
	}
	
	public void testConfigNodes() throws IOException {
		final String uniqueId = getClass().getName() + ".A." + System.currentTimeMillis(); 
		final String key = getClass().getName() + ".key";
		final String value = getClass().getName() + "." + uniqueId;
		final String keyValue = key + "=" + value;
		
		final String contentType = CONTENT_TYPE_HTML;
		
		assertContentWithTimeout("Before test, config must not exist", CONFIG_URL, 
				contentType, new ConfigCondition(keyValue, false), TIMEOUT_SECONDS);
		
		// Create an OSGi config using a sling:OsgiConfig node
		final Map<String, String> props = new HashMap<String, String>();
		props.put(key, value);
		final String toDelete = createConfig(uniqueId, props);
		assertContentWithTimeout("Config must be present after creating config node", CONFIG_URL, 
				contentType, new ConfigCondition(keyValue, true), TIMEOUT_SECONDS);
		
		// Update config node, verify that config is updated
		final String newValue = getClass().getName() + ".NEW." + System.currentTimeMillis();
		final String newKeyValue = key + "=" + newValue;
		props.put(key, newValue);
		createConfig(uniqueId, props);
		assertContentWithTimeout("Config must be modified after node update", CONFIG_URL, 
				contentType, new ConfigCondition(newKeyValue, true), TIMEOUT_SECONDS);
		assertContentWithTimeout("Old value must be gone after update", CONFIG_URL, 
				contentType, new ConfigCondition(keyValue, false), TIMEOUT_SECONDS);
		
		// Delete and verify that the config is gone
		testClient.delete(toDelete);
		assertContentWithTimeout("Old config must be gone after removing config node", CONFIG_URL, 
				contentType, new ConfigCondition(keyValue, false), TIMEOUT_SECONDS);
		assertContentWithTimeout("New config must be gone after removing config node", CONFIG_URL, 
				contentType, new ConfigCondition(newKeyValue, false), TIMEOUT_SECONDS);
	}
	
	public void testAppsOverridesLibsNodes() throws IOException {
		final String uniqueId = getClass().getName() + ".B." + System.currentTimeMillis(); 
		final Map<String, String> props = new HashMap<String, String>();
		props.put("foo", "barONE");
		final String toDeleteA = createConfig("/libs", uniqueId, props);
		assertContentWithTimeout("Config must be present after creating /libs config node", CONFIG_URL, 
				CONTENT_TYPE_HTML, new ConfigCondition("barONE", true), TIMEOUT_SECONDS);
		
		props.put("foo", "barTWO");
		final String toDeleteB = createConfig("/apps", uniqueId, props);
		assertContentWithTimeout("Config must be updated after creating /apps config node", CONFIG_URL, 
				CONTENT_TYPE_HTML, new ConfigCondition("barTWO", true), TIMEOUT_SECONDS);
		
		props.put("foo", "barTHREE");
		createConfig("/libs", uniqueId, props);
		assertContentWithTimeout("Config must NOT be updated after updating /libs config node", CONFIG_URL, 
				CONTENT_TYPE_HTML, new ConfigCondition("barTWO", true), TIMEOUT_SECONDS);
		
		props.put("foo", "barFOUR");
		createConfig("/apps", uniqueId, props);
		assertContentWithTimeout("Config must be updated after updating /apps config node", CONFIG_URL, 
				CONTENT_TYPE_HTML, new ConfigCondition("barFOUR", true), TIMEOUT_SECONDS);
		
		testClient.delete(toDeleteA);
		testClient.delete(toDeleteB);
	}
	
	   /** Test creating configs from files */
    public void testConfigFiles() throws IOException {
        final Set<String> toDelete = new HashSet<String>();
        final String CFG_PATH = "/apps/" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + "/install";
        testClient.mkdirs(WEBDAV_BASE_URL,CFG_PATH);
        toDelete.add(uploadTestScript(CFG_PATH, "jcrinstall/cfg1.cfg", "cfg1.cfg"));
        assertContentWithTimeout("cfg1 must be installed", CONFIG_URL, 
                CONTENT_TYPE_HTML, new ConfigCondition("cfg1value=from.cfg1.cfg", true), TIMEOUT_SECONDS);
        
        toDelete.add(uploadTestScript(CFG_PATH, "jcrinstall/cfg1.properties", "cfg1.properties"));
        assertContentWithTimeout("cfg1 must be updated", CONFIG_URL, 
                CONTENT_TYPE_HTML, new ConfigCondition("cfg1value=from.cfg1.properties", true), TIMEOUT_SECONDS);
        assertContentWithTimeout("old cfg1 value must be gone", CONFIG_URL, 
                CONTENT_TYPE_HTML, new ConfigCondition("cfg1value=from.cfg1.cfg", false), TIMEOUT_SECONDS);
        
        for(String url : toDelete) {
            testClient.delete(url);
        }
        
        assertContentWithTimeout("cfg1 must be gone", CONFIG_URL, 
                CONTENT_TYPE_HTML, new ConfigCondition("cfg1value=from.cfg1.properties", false), TIMEOUT_SECONDS);
    }

}
