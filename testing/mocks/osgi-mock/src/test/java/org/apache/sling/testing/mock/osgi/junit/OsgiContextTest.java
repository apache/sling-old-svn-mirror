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
package org.apache.sling.testing.mock.osgi.junit;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OsgiContextTest {

    private final OsgiContextCallback contextBeforeSetup = mock(OsgiContextCallback.class);
    private final OsgiContextCallback contextAfterSetup = mock(OsgiContextCallback.class);
    private final OsgiContextCallback contextBeforeTeardown = mock(OsgiContextCallback.class);
    private final OsgiContextCallback contextAfterTeardown = mock(OsgiContextCallback.class);

    // Run all unit tests for each resource resolver types listed here
    @Rule
    public OsgiContext context = new OsgiContextBuilder()
        .beforeSetUp(contextBeforeSetup)
        .afterSetUp(contextAfterSetup)
        .beforeTearDown(contextBeforeTeardown)
        .afterTearDown(contextAfterTeardown)
        .build();

    @Before
    public void setUp() throws Exception {
        verify(contextBeforeSetup).execute(context);
        verify(contextAfterSetup).execute(context);
    }

    @Test
    public void testBundleContext() {
        assertNotNull(context.bundleContext());
    }

}
