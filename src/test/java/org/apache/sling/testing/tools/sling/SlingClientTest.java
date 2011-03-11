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
package org.apache.sling.testing.tools.sling;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SlingClientTest {
    public final static String SLING_SERVER_URL = "http://localhost:1234";
    private final String locationHeaderValue;
    private final String expectedPath;
    
    public SlingClientTest(String locationHeaderValue, String expectedPath) {
        this.locationHeaderValue = locationHeaderValue;
        this.expectedPath = expectedPath;
    }
    
    @Test
    public void testPath() {
        final SlingClient c = new SlingClient(SLING_SERVER_URL, null, null);
        assertEquals(expectedPath, c.locationToPath(locationHeaderValue));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadPrefix() {
        final SlingClient c = new SlingClient(SLING_SERVER_URL, null, null);
        c.locationToPath("http://example.com/foo");
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        final Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] { SLING_SERVER_URL + "/one", "/one" });
        data.add(new Object[] { SLING_SERVER_URL + "/one/foo", "/one/foo" });
        data.add(new Object[] { "/two", "/two" });
        data.add(new Object[] { "/two/bar", "/two/bar" });
        data.add(new Object[] { SLING_SERVER_URL, "" });
        return data;
    }
}
