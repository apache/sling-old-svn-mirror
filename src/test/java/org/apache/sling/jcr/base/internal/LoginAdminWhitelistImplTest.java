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
package org.apache.sling.jcr.base.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

public class LoginAdminWhitelistImplTest {
    private LoginAdminWhitelistImpl whitelist;
    private Map<String, Object> config;
    private static final String TYPICAL_DEFAULT_ALLOWED_BSN = "org.apache.sling.jcr.base";
    
    @Before
    public void setup() {
        whitelist = new LoginAdminWhitelistImpl();
        config = new HashMap<String, Object>();
    }
    
    private void assertAdminLogin(final String bundleSymbolicName, boolean expected) {
        final Bundle b = Mockito.mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(bundleSymbolicName);
        final boolean actual = whitelist.allowLoginAdministrative(b);
        assertEquals("For bundle " + bundleSymbolicName + ", expected admin login=" + expected, expected, actual);
    }
    
    private List<String> randomBsn() {
        final List<String> result = new ArrayList<String>();
        for(int i=0; i < 5; i++) {
            result.add("random.bsn." + UUID.randomUUID());
        }
        return result;
    }
 
    @Test
    public void testDefaultConfig() {
        whitelist.activate(config);
        
        for(String bsn : DefaultWhitelist.WHITELISTED_BSN) {
            assertAdminLogin(bsn, true);
        }
        
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, true);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testBypassWhitelist() {
        config.put(LoginAdminWhitelistImpl.PROP_BYPASS_WHITELIST, true);
        whitelist.activate(config);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, true);
        }
    }
    
    @Test
    public void testDefaultConfigOnly() {
        final String [] allowed = {
                "bundle1", "bundle2"
        };
        config.put(LoginAdminWhitelistImpl.PROP_DEFAULT_WHITELISTED_BSN, allowed);
        whitelist.activate(config);
        
        assertAdminLogin("bundle1", true);
        assertAdminLogin("bundle2", true);
        assertAdminLogin("foo.1.bar", false);
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, false);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testAdditionalConfigOnly() {
        final String [] allowed = {
                "bundle5", "bundle6"
        };
        config.put(LoginAdminWhitelistImpl.PROP_ADDITIONAL_WHITELISTED_BSN, allowed);
        whitelist.activate(config);
        
        assertAdminLogin("bundle5", true);
        assertAdminLogin("bundle6", true);
        assertAdminLogin("foo.1.bar", false);
        
        for(String bsn : DefaultWhitelist.WHITELISTED_BSN) {
            assertAdminLogin(bsn, true);
        }
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testDefaultAndAdditionalConfig() {
        config.put(LoginAdminWhitelistImpl.PROP_DEFAULT_WHITELISTED_BSN, new String [] { "defB"});
        config.put(LoginAdminWhitelistImpl.PROP_ADDITIONAL_WHITELISTED_BSN, new String [] { "addB"});
        whitelist.activate(config);
        
        assertAdminLogin("defB", true);
        assertAdminLogin("addB", true);
        assertAdminLogin("foo.1.bar", false);
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, false);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testRegexpWhitelist() {
        final String [] allowed = {
                "bundle3", "bundle4"
        };
        config.put(LoginAdminWhitelistImpl.PROP_DEFAULT_WHITELISTED_BSN, allowed);
        config.put(LoginAdminWhitelistImpl.PROP_WHITELIST_REGEXP, "foo.*bar");
        whitelist.activate(config);
        
        assertAdminLogin("bundle3", true);
        assertAdminLogin("bundle4", true);
        assertAdminLogin("foo.2.bar", true);
        assertAdminLogin("foo.somethingElse.bar", true);
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, false);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
}