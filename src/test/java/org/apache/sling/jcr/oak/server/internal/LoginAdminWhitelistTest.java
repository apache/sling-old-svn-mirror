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
package org.apache.sling.jcr.oak.server.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

public class LoginAdminWhitelistTest {

    private static final String TYPICAL_DEFAULT_ALLOWED_BSN = "org.apache.sling.jcr.base";

    private LoginAdminWhitelist whitelist;

    @Before
    public void setup() {
        whitelist = new LoginAdminWhitelist();
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
        final LoginAdminWhitelistConfiguration config = config(null, null, null, null);
        whitelist.activate(config);

        for(String bsn : config.whitelist_bundles_default()) {
            assertAdminLogin(bsn, true);
        }
        
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, true);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }

    @Test
    public void testBypassWhitelist() {
        whitelist.activate(config(true, null, null, null));
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, true);
        }
    }
    
    @Test
    public void testDefaultConfigOnly() {
        final String [] allowed = {
                "bundle1", "bundle2"
        };
        whitelist.activate(config(null, null, allowed, null));
        
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
        final LoginAdminWhitelistConfiguration config = config(null, null, null, allowed);
        whitelist.activate(config);
        
        assertAdminLogin("bundle5", true);
        assertAdminLogin("bundle6", true);
        assertAdminLogin("foo.1.bar", false);
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, true);
        
        for(String bsn : config.whitelist_bundles_default()) {
            assertAdminLogin(bsn, true);
        }
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testDefaultAndAdditionalConfig() {
        whitelist.activate(config(null, null, new String [] { "defB"}, new String [] { "addB"}));
        
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
        whitelist.activate(config(null, "foo.*bar", allowed, null));
        
        assertAdminLogin("bundle3", true);
        assertAdminLogin("bundle4", true);
        assertAdminLogin("foo.2.bar", true);
        assertAdminLogin("foo.somethingElse.bar", true);
        assertAdminLogin(TYPICAL_DEFAULT_ALLOWED_BSN, false);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }


    private LoginAdminWhitelistConfiguration config(final Boolean bypass, final String regexp, final String[] defaultBSNs, final String[] additionalBSNs) {
        return new LoginAdminWhitelistConfiguration() {
            @Override
            public boolean whitelist_bypass() {
                return defaultIfNull(bypass, "whitelist_bypass");
            }

            @Override
            public String whitelist_bundles_regexp() {
                return defaultIfNull(regexp, "whitelist_bundles_regexp");
            }

            @Override
            public String[] whitelist_bundles_default() {
                return defaultIfNull(defaultBSNs, "whitelist_bundles_default");
            }

            @Override
            public String[] whitelist_bundles_additional() {
                return defaultIfNull(additionalBSNs, "whitelist_bundles_additional");
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LoginAdminWhitelistConfiguration.class;
            }

            private <T> T defaultIfNull(final T value, final String methodName) {
                if (value != null) {
                    return value;
                }
                try {
                    return (T)this.annotationType().getMethod(methodName).getDefaultValue();
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        };
    }
}