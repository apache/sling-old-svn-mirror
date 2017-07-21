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
package org.apache.sling.distribution.serialization.impl.vlt;

import static org.junit.Assert.assertEquals;

import org.apache.sling.distribution.serialization.impl.vlt.RegexpPathMapping;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for {@link RegexpPathMappingTest}
 */
public class RegexpPathMappingTest {

    private RegexpPathMapping pathMapping;

    @Before
    public void setUp() {
        pathMapping = new RegexpPathMapping();
    }

    @After
    public void tearDown() {
        pathMapping = null;
    }

    @Test
    public void testIdentityMapping() {
        assertEquals("/etc/my/fake/data", pathMapping.map("/etc/my/fake/data"));
    }

    @Test
    public void testCorrectMapping() {
        pathMapping.addMapping("/etc/(.*)", "/dummy/$1/custom");

        assertEquals("/dummy/my/fake/data/custom", pathMapping.map("/etc/my/fake/data"));
    }

}
