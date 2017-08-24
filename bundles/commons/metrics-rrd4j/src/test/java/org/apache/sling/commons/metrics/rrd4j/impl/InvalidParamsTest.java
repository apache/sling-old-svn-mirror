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
package org.apache.sling.commons.metrics.rrd4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.codahale.metrics.MetricRegistry;

public class InvalidParamsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("target"));

    private MetricRegistry registry = new MetricRegistry();

    @Test
    public void badStep() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        int step = -1;
        String[] datasources = { "DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U" };
        String[] archives = { "RRA:AVERAGE:0.5:12:360" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).withStep(step).build();
        assertNotNull(reporter);
        assertTrue(rrd.exists());
        assertTrue(reporter.getStep() == 5);

        Set<String> ds = reporter.getDatasources();
        assertEquals(1, ds.size());
        assertEquals("oak_SESSION_LOGIN_COUNTER", ds.iterator().next());
    }

    @Test
    public void noDatasource() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = null;
        String[] archives = { "RRA:AVERAGE:0.5:12:360" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNull(reporter);
        assertFalse(rrd.exists());
    }

    @Test
    public void badDatasource() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = { "malformed:input" };
        String[] archives = { "RRA:AVERAGE:0.5:12:360" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNull(reporter);
        assertFalse(rrd.exists());
    }

    @Test
    public void badDatasourceFilter() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = { "DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U", "malformed:input" };
        String[] archives = { "RRA:AVERAGE:0.5:12:360" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNotNull(reporter);
        assertTrue(rrd.exists());

        Set<String> ds = reporter.getDatasources();
        assertEquals(1, ds.size());
        assertEquals("oak_SESSION_LOGIN_COUNTER", ds.iterator().next());
    }

    @Test
    public void noArchive() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = { "DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U" };
        String[] archives = null;

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNull(reporter);
        assertFalse(rrd.exists());
    }

    @Test
    public void badArchive() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = { "DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U" };
        String[] archives = { "malformed:input" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNull(reporter);
        assertFalse(rrd.exists());
    }

    @Test
    public void badArchiveFilter() throws Exception {
        File rrd = new File(folder.newFolder("InvalidParamsTest"), "metrics.rrd");
        assertFalse(rrd.exists());

        String[] datasources = { "DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U" };
        String[] archives = { "RRA:AVERAGE:0.5:12:360", "malformed:input" };

        RRD4JReporter reporter = RRD4JReporter.forRegistry(registry).withPath(rrd).withDatasources(datasources)
                .withArchives(archives).build();
        assertNotNull(reporter);
        assertTrue(rrd.exists());
        Set<String> archs = reporter.getArchives();
        assertEquals(1, archs.size());
    }
}
