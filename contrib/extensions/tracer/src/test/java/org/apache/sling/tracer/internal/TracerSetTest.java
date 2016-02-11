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

package org.apache.sling.tracer.internal;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class TracerSetTest {
    
    @Test
    public void nullReporter() throws Exception{
        CallerStackReporter r = TracerSet.createReporter(createEntry("foo"));
        assertNull(r);
    }

    @Test
    public void completeStack() throws Exception{
        CallerStackReporter r = TracerSet.createReporter(createEntry("foo;caller=true"));
        assertNotNull(r);
        assertEquals(Integer.MAX_VALUE, r.getDepth());
    }

    @Test
    public void depthSpecified() throws Exception{
        CallerStackReporter r = TracerSet.createReporter(createEntry("foo;caller=28"));
        assertNotNull(r);
        assertEquals(28, r.getDepth());
    }

    @Test
    public void invalidDepth() throws Exception{
        CallerStackReporter r = TracerSet.createReporter(createEntry("foo;caller=abc"));
        assertNull(r);
    }

    @Test
    public void prefixFilter() throws Exception{
        CallerStackReporter r = TracerSet.createReporter(createEntry("foo;caller=28;callerPrefixFilter=\"a|b\""));
        assertNotNull(r);
        assertEquals(28, r.getDepth());
        assertTrue(r.getCallerFilter() instanceof PrefixExcludeFilter);
        PrefixExcludeFilter f = (PrefixExcludeFilter) r.getCallerFilter();
        assertEquals(asList("a", "b"), f.getPrefixesToExclude());
    }

    private static ManifestHeader.Entry createEntry(String config){
        ManifestHeader parsedConfig = ManifestHeader.parse(config);
        return parsedConfig.getEntries()[0];
    }
}
