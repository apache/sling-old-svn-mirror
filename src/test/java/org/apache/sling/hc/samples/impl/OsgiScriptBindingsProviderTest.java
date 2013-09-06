/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.samples.impl;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.hc.util.FormattingResultLog;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class OsgiScriptBindingsProviderTest {
    
    private Bundle mockBundle(boolean isFragment, boolean isActive) {
        final Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getState()).thenReturn(isActive ? Bundle.ACTIVE : Bundle.RESOLVED);
        
        final Dictionary<String, String> headers = new Hashtable<String, String>();
        if(isFragment) {
            headers.put(Constants.FRAGMENT_HOST, "FOO");
        }
        Mockito.when(b.getHeaders()).thenReturn(headers);
        
        return b;
    }
    
    @Test
    public void testInactiveBundles() throws Exception {
        final BundleContext ctx = Mockito.mock(BundleContext.class);
        final Bundle [] bundles = { 
                mockBundle(false, true), 
                mockBundle(false, false), 
                mockBundle(false, true),
                mockBundle(true, false)
        };
        Mockito.when(ctx.getBundles()).thenReturn(bundles);
        
        final FormattingResultLog resultLog = new FormattingResultLog();
        final OsgiScriptBindingsProvider.OsgiBinding b = new OsgiScriptBindingsProvider.OsgiBinding(ctx, resultLog);
        assertEquals(1, b.inactiveBundlesCount());
    }
}