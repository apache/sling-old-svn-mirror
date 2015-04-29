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
package org.apache.sling.discovery.commons;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.junit.Test;
import org.mockito.Mockito;

public class InstancesDiffTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicatedSlingIds() {
        List<Instance> old = Arrays.asList(new Instance("duplicated"), new Instance("one"), new Instance("duplicated"));
        new InstancesDiff(old, empty());
    }

    @Test
    public void testEmptyCollections() {
        InstancesDiff diff = new InstancesDiff(empty(), empty());
        TestCase.assertEquals(0, diff.all(true).get().size());
        TestCase.assertEquals(0, diff.added().get().size());
        TestCase.assertEquals(0, diff.removed().get().size());
        TestCase.assertEquals(0, diff.retained(true).get().size());
    }

    // added

    @Test
    public void testAddedFromEmpty() throws Exception {
        InstancesDiff diff = new InstancesDiff(empty(), Arrays.asList(new Instance("one"), new Instance("two")));
        TestCase.assertEquals(2, diff.added().get().size());
    }

    @Test
    public void testAddedWithEmpty() throws Exception {
        InstancesDiff diff = new InstancesDiff(Arrays.asList(new Instance("one"), new Instance("two")), empty());
        TestCase.assertEquals(0, diff.added().get().size());
    }

    @Test
    public void testAddedWithoutIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(Collections.singletonList(new Instance("one")),
                Collections.singletonList(new Instance("two")));
        TestCase.assertEquals(1, diff.added().get().size());
        TestCase.assertEquals("two", diff.added().get().iterator().next().getSlingId());
    }

    @Test
    public void testAddedWithIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(Arrays.asList(new Instance("one"), new Instance("two")),
                Arrays.asList(new Instance("two"), new Instance("three")));
        TestCase.assertEquals(1, diff.added().get().size());
        TestCase.assertEquals("three", diff.added().get().iterator().next().getSlingId());
    }

    // all

    @Test
    public void testAll() throws Exception {
        InstancesDiff diff = new InstancesDiff(Collections.singletonList(new Instance("one")),
                Arrays.asList(new Instance("two"), new Instance("three")));
        TestCase.assertEquals(3, diff.all(true).get().size());
    }

    @Test
    public void testAllRetainedCollection() throws Exception {
        Instance oldInstance = new Instance("one");
        Instance newInstance = new Instance("one");
        InstancesDiff diff = new InstancesDiff(
                Collections.singletonList(oldInstance),
                Collections.singletonList(newInstance));
        TestCase.assertEquals(1, diff.all(true).get().size());
        TestCase.assertEquals(newInstance, diff.all(true).get().iterator().next());
        TestCase.assertEquals(1, diff.all(false).get().size());
        TestCase.assertEquals(oldInstance, diff.all(false).get().iterator().next());
    }

    // removed

    @Test
    public void testRemovedFromEmpty() throws Exception {
        InstancesDiff diff = new InstancesDiff(empty(), Arrays.asList(new Instance("one"), new Instance("two")));
        TestCase.assertEquals(0, diff.removed().get().size());
    }

    @Test
    public void testRemovedWithEmpty() throws Exception {
        InstancesDiff diff = new InstancesDiff(Arrays.asList(new Instance("one"), new Instance("two")), empty());
        TestCase.assertEquals(2, diff.removed().get().size());
    }

    @Test
    public void testRemovedWithoutIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(Collections.singletonList(new Instance("one")),
                Collections.singletonList(new Instance("two")));
        TestCase.assertEquals(1, diff.removed().get().size());
        TestCase.assertEquals("one", diff.removed().get().iterator().next().getSlingId());
    }

    @Test
    public void testRemovedWithIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(Arrays.asList(new Instance("one"), new Instance("two")),
                Arrays.asList(new Instance("two"), new Instance("three")));
        TestCase.assertEquals(1, diff.removed().get().size());
        TestCase.assertEquals("one", diff.removed().get().iterator().next().getSlingId());
    }

    // retained

    @Test
    public void testRetainedWithoutIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(empty(), Arrays.asList(new Instance("one"), new Instance("two")));
        TestCase.assertEquals(0, diff.retained(true).get().size());
    }

    @Test
    public void testRetainedWithIntersection() throws Exception {
        InstancesDiff diff = new InstancesDiff(Arrays.asList(new Instance("one"), new Instance("two")),
                Arrays.asList(new Instance("two"), new Instance("three")));
        TestCase.assertEquals(1, diff.retained(true).get().size());
        TestCase.assertEquals("two", diff.retained(true).get().iterator().next().getSlingId());
    }

    @Test
    public void testRetainedCollection() throws Exception {
        Instance oldInstance = new Instance("one");
        Instance newInstance = new Instance("one");
        InstancesDiff diff = new InstancesDiff(Collections.singletonList(oldInstance),
                Collections.singletonList(newInstance));
        TestCase.assertEquals(1, diff.retained(true).get().size());
        TestCase.assertEquals(newInstance, diff.retained(true).get().iterator().next());
        TestCase.assertEquals(oldInstance, diff.retained(false).get().iterator().next());
    }

    @Test
    public void testRetainedByProperties() throws Exception {
        InstancesDiff diff = new InstancesDiff(
                Arrays.asList(new Instance("one", Collections.singletonMap("p1", "v1")), new Instance("two", Collections.singletonMap("p1", "v1"))),
                Arrays.asList(new Instance("one", Collections.singletonMap("p1", "v2")), new Instance("two", Collections.singletonMap("p1", "v1"))));
        TestCase.assertEquals(1, diff.retained(true, false).get().size());
        TestCase.assertEquals("two", diff.retained(true, false).get().iterator().next().getSlingId());
        TestCase.assertEquals(1, diff.retained(true, true).get().size());
        TestCase.assertEquals("one", diff.retained(true, true).get().iterator().next().getSlingId());
    }

    // filters

    @Test
    public void testEmptyResult() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one"), new Instance("two")), empty()).all(true).filterWith(new InstanceFilter() {
            public boolean accept(InstanceDescription instanceDescription) {
                return false;
            }
        }).get();
        TestCase.assertEquals(0, instances.size());
    }

    @Test
    public void testFilterWith() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one"), new Instance("two")), empty()).all(true).filterWith(new InstanceFilter() {
            public boolean accept(InstanceDescription instanceDescription) {
                return "one".equals(instanceDescription.getSlingId());
            }
        }).get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("one", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsLeader() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "viewId"),
                new Instance("two", false, false, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isLeader()
                .get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("one", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsNotLeader() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "viewId"),
                new Instance("two", false, false, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isNotLeader()
                .get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("two", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsLocal() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "viewId"),
                new Instance("two", false, true, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isLocal()
                .get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("two", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsNotLocal() throws Exception {
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "viewId"),
                new Instance("two", false, true, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isNotLocal()
                .get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("one", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsInClusterView() throws Exception {
        ClusterView clusterView = clusterView("viewId");
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "otherView"),
                new Instance("two", false, true, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isInClusterView(clusterView)
                .get();
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("two", instances.iterator().next().getSlingId());
    }

    @Test
    public void testIsNotInClusterView() throws Exception {
        ClusterView clusterView = clusterView("yet-another-view");
        Collection<InstanceDescription> instances = new InstancesDiff(Arrays.asList(
                new Instance("one", true, false, Collections.<String, String>emptyMap(), "otherView"),
                new Instance("two", false, true, Collections.<String, String>emptyMap(), "viewId")), empty())
                .all(true)
                .isInClusterView(clusterView)
                .get();
        TestCase.assertEquals(0, instances.size());
    }

    private List<Instance> empty() {
        return Collections.<Instance>emptyList();
    }

    private class Instance implements InstanceDescription {

        final String slingId;

        final boolean leader;

        final boolean local;

        final Map<String, String> properties;

        final ClusterView clusterView;

        Instance(String slingId) {
            this(slingId, false, false, Collections.<String, String>emptyMap(), "");
        }

        Instance(String slingId, Map<String, String> properties) {
            this(slingId, false, false, properties, "");
        }

        Instance(String slingId, boolean leader, boolean local, Map<String, String> properties, String clusterViewId) {
            this.slingId = slingId;
            this.leader = leader;
            this.local = local;
            this.properties = properties;
            clusterView = clusterView(clusterViewId);
        }

        public ClusterView getClusterView() {
            return clusterView;
        }

        public boolean isLeader() {
            return leader;
        }

        public boolean isLocal() {
            return local;
        }

        public String getSlingId() {
            return slingId;
        }

        public String getProperty(String name) {
            return properties.get(name);
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    private ClusterView clusterView(String clusterViewId) {
        ClusterView clusterView = Mockito.mock(ClusterView.class);
        Mockito.when(clusterView.getId()).thenReturn(clusterViewId);
        return clusterView;
    }
}