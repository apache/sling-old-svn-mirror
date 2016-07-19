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

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import junit.framework.TestCase;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class AuthenticationRequirementTest extends TestCase {

    @Rule
    public final OsgiContext context = new OsgiContext();

    private final SlingAuthenticator slingAuthenticator = new SlingAuthenticator();
    private final ServiceReference ref = new MockServiceReference();

    private Predicate<AuthenticationRequirementHolder> providerFilter = new Predicate<AuthenticationRequirementHolder>() {
        @Override
        public boolean apply(AuthenticationRequirementHolder input) {
            return input != null && PathBasedHolder.buildDescription(ref).equals(input.getProvider());
        }
    };

    Map<String, Boolean> initialRequirements = new HashMap<String, Boolean>();
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // add initial values for external auth requirements of the test service reference
        initialRequirements.put("/a", true);
        initialRequirements.put("/b", true);
        initialRequirements.put("/b/c", false);
        initialRequirements.put("/c", false);
        slingAuthenticator.setRequirements(ref, initialRequirements);
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

    public void test_VerifyInitialSetup() throws Exception {
        assertRequirements(initialRequirements, filterRequirements());
    }

    public void test_SetRequirements() throws Exception {
        Map<String, Boolean> toReplace =  ImmutableMap.of("/a", false, "/d", true);
        slingAuthenticator.setRequirements(ref, toReplace);

        // it's expected that all existing values have been replaced
        assertRequirements(toReplace, filterRequirements());
    }

    public void test_AppendRequirements() throws Exception {
        Map<String, Boolean> toAppend =  ImmutableMap.of("/d", true, "/e/f", false);
        slingAuthenticator.appendRequirements(ref, toAppend);

        // the expected result is the combination of the entries to append plus
        // the initial values.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        expected.putAll(toAppend);
        assertRequirements(expected, filterRequirements());
    }

    public void test_AppendRequirementsWithConflict() throws Exception {
        Map<String, Boolean> toAppend =  ImmutableMap.of("/a", false, "/d", true, "/e/f", false);
        slingAuthenticator.appendRequirements(ref, toAppend);

        // the expected result is the combination of the entries to append plus
        // the initial values; conflicting values (same path again) wont'be replaced
        // and the 'conflicting' entries are ignored.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(toAppend);
        expected.putAll(initialRequirements);
        assertRequirements(expected, filterRequirements());
    }

    public void test_RemoveRequirements() throws Exception {
        Map<String, Boolean> toRemove =  ImmutableMap.of("/a", true, "/b", true);
        slingAuthenticator.removeRequirements(ref, toRemove);

        // the expected result is initial set without the entries to be removed.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        for (String key : toRemove.keySet()) {
            expected.remove(key);
        }
        assertRequirements(expected, filterRequirements());
    }

    public void test_RemoveRequirements2() throws Exception {
        // paths to remove are contained but have a different value
        Map<String, Boolean> toRemove =  ImmutableMap.of("/a", false, "/b", false);
        slingAuthenticator.removeRequirements(ref, toRemove);

        // the expected result is the initial set without the entries to be removed.
        Map<String, Boolean> expected = new HashMap<String, Boolean>(initialRequirements);
        for (String key : toRemove.keySet()) {
            expected.remove(key);
        }
        assertRequirements(expected, filterRequirements());
    }

    public void test_RemoveNonExistingRequirements() throws Exception {
        // paths to remove are contained but have a different value
        Map<String, Boolean> toRemove =  ImmutableMap.of("/nonExisting", true);
        slingAuthenticator.removeRequirements(ref, toRemove);

        // the expected result is the initial set
        assertRequirements(initialRequirements, filterRequirements());
    }

    public void test_ClearRequirements() throws Exception {
        slingAuthenticator.clearRequirements(ref);

        assertRequirements(ImmutableMap.<String, Boolean>of(), filterRequirements());

    }

    private static final class MockServiceReference implements ServiceReference {

        private static final String ID = "id";
        private static final String DESC = "testingServiceDescription";

        @Override
        public Object getProperty(String s) {
            if (Constants.SERVICE_DESCRIPTION.equals(s)) {
                return DESC;
            } else if (Constants.SERVICE_ID.equals(s)) {
                return ID;
            }
            return null;
        }

        @Override
        public String[] getPropertyKeys() {
            return new String[] {Constants.SERVICE_DESCRIPTION};
        }

        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public Bundle[] getUsingBundles() {
            return new Bundle[0];
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String s) {
            return false;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }
}