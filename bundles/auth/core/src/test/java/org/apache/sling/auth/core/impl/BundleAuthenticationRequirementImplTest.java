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
package org.apache.sling.auth.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import junitx.util.PrivateAccessor;

public class BundleAuthenticationRequirementImplTest {

    private final SlingAuthenticator slingAuthenticator = new SlingAuthenticator();

    private Predicate<AuthenticationRequirementHolder> providerFilter = new Predicate<AuthenticationRequirementHolder>() {
        @Override
        public boolean apply(AuthenticationRequirementHolder input) {
            return input != null && input.getProvider().startsWith(BundleAuthenticationRequirementImpl.PREFIX);
        }
    };

    Map<String, Boolean> initialRequirements = new HashMap<String, Boolean>();

    BundleAuthenticationRequirement requirement;

    @Before public void setUp() throws Throwable {
        final Bundle bundle = mock(Bundle.class);
        final ComponentContext cmpCtx = mock(ComponentContext.class);
        when(cmpCtx.getUsingBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(5L);
        when(bundle.getSymbolicName()).thenReturn("testbundle");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));

        this.requirement = new BundleAuthenticationRequirementImpl();
        PrivateAccessor.setField(this.requirement, "slingAuthenticator", slingAuthenticator);
        PrivateAccessor.invoke(requirement, "activate", new Class[] {ComponentContext.class}, new Object[] {cmpCtx});

        // add initial values for external auth requirements of the test service reference
        initialRequirements.put("/a", true);
        initialRequirements.put("/b", true);
        initialRequirements.put("/b/c", false);
        initialRequirements.put("/c", false);
        requirement.setRequirements(initialRequirements);
    }

    private Iterable<AuthenticationRequirementHolder> filterRequirements() {
        return Iterables.filter(slingAuthenticator.getAuthenticationRequirements(), providerFilter);
    }

    private void assertRequirements(Map<String, Boolean> expected, Iterable<AuthenticationRequirementHolder> requirements) {
        assertEquals(expected.size(), Iterables.size(requirements));

        for (AuthenticationRequirementHolder holder : requirements) {
            assertTrue(holder.fullPath, expected.containsKey(holder.fullPath));
            boolean b = expected.get(holder.fullPath);
            assertEquals(holder.fullPath, b, holder.requiresAuthentication());
        }
    }

    @Test public void test_VerifyInitialSetup() throws Exception {
        assertRequirements(initialRequirements, filterRequirements());
    }

    @Test public void test_SetRequirements() throws Exception {
        Map<String, Boolean> toReplace =  ImmutableMap.of("/a", false, "/d", true);
        requirement.setRequirements(toReplace);

        // it's expected that all existing values have been replaced
        assertRequirements(toReplace, filterRequirements());
    }

    @Test public void test_AppendRequirements() throws Exception {
        Map<String, Boolean> toAppend =  ImmutableMap.of("/d", true, "/e/f", false);
        requirement.appendRequirements(toAppend);

        // the expected result is the combination of the entries to append plus
        // the initial values.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        expected.putAll(toAppend);
        assertRequirements(expected, filterRequirements());
    }

    @Test public void test_AppendRequirementsWithConflict() throws Exception {
        Map<String, Boolean> toAppend =  ImmutableMap.of("/a", false, "/d", true, "/e/f", false);
        requirement.appendRequirements(toAppend);

        // the expected result is the combination of the entries to append plus
        // the initial values; conflicting values (same path again) wont'be replaced
        // and the 'conflicting' entries are ignored.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(toAppend);
        expected.putAll(initialRequirements);
        assertRequirements(expected, filterRequirements());
    }

    @Test public void test_RemoveRequirements() throws Exception {
        Map<String, Boolean> toRemove =  ImmutableMap.of("/a", true, "/b", true);
        requirement.removeRequirements(toRemove);

        // the expected result is initial set without the entries to be removed.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        for (String key : toRemove.keySet()) {
            expected.remove(key);
        }
        assertRequirements(expected, filterRequirements());
    }

    @Test public void test_RemoveRequirements2() throws Exception {
        // paths to remove are contained but have a different value
        Map<String, Boolean> toRemove =  ImmutableMap.of("/a", false, "/b", false);
        requirement.removeRequirements(toRemove);

        // the expected result is the initial set without the entries to be removed.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        for (String key : toRemove.keySet()) {
            expected.remove(key);
        }
        assertRequirements(expected, filterRequirements());
    }

    @Test public void test_RemoveNonExistingRequirements() throws Exception {
        // paths to remove are contained but have a different value
        Map<String, Boolean> toRemove =  ImmutableMap.of("/nonExisting", true);
        requirement.removeRequirements(toRemove);

        // the expected result is the initial set
        assertRequirements(initialRequirements, filterRequirements());
    }

    @Test public void test_ClearRequirements() throws Exception {
        requirement.clearRequirements();

        assertRequirements(ImmutableMap.<String, Boolean>of(), filterRequirements());

    }
}