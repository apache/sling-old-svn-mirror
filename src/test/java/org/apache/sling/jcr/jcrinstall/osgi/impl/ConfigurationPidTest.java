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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.junit.Assert.assertEquals;

public class ConfigurationPidTest {

    private void assertPid(String path, String expectedPid, String expectedFactoryPid) {
        final ConfigurationPid pid = new ConfigurationPid(path);
        assertEquals("For path " + path + ", pid matches", expectedPid, pid.getConfigPid());
        assertEquals("For path " + path + ", factory pid matches", expectedFactoryPid, pid.getFactoryPid());
    }
    
    @org.junit.Test public void testNonFactory() {
        assertPid("o.a.s.foo.bar.cfg", "o.a.s.foo.bar", null);
        assertPid("/somepath/o.a.s.foo.bar.cfg", "o.a.s.foo.bar", null);
    }

    @org.junit.Test public void testFactory() {
        assertPid("o.a.s.foo.bar-a.cfg", "o.a.s.foo.bar", "a");
        assertPid("/somepath/o.a.s.foo.bar-a.cfg", "o.a.s.foo.bar", "a");
    }
}
