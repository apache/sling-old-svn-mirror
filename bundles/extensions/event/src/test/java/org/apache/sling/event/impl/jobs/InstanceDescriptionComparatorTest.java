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
package org.apache.sling.event.impl.jobs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

public class InstanceDescriptionComparatorTest {


    @org.junit.Test public void testSingleClusterThreeInstances() {
        final Instance cl1in1 = new Instance("1", "A", false, true);
        final Instance cl1in2 = new Instance("1", "B", true, false);
        final Instance cl1in3 = new Instance("1", "C", false, false);

        final List<InstanceDescription> desc = new ArrayList<InstanceDescription>();
        desc.add(cl1in2);
        desc.add(cl1in1);
        desc.add(cl1in3);
        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("1"));

        assertEquals("Instance0: ", cl1in2.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl1in1.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl1in3.getSlingId(), desc.get(2).getSlingId());
    }

    @org.junit.Test public void testTwoClustersThreeInstances() {
        final Instance cl1in1 = new Instance("1", "A", false, true);
        final Instance cl1in2 = new Instance("1", "B", true, false);
        final Instance cl1in3 = new Instance("1", "C", false, false);
        final Instance cl2in1 = new Instance("2", "D", false, false);
        final Instance cl2in2 = new Instance("2", "E", false, false);
        final Instance cl2in3 = new Instance("2", "F", true, false);

        final List<InstanceDescription> desc = new ArrayList<InstanceDescription>();
        desc.add(cl2in3);
        desc.add(cl1in2);
        desc.add(cl2in1);
        desc.add(cl1in3);
        desc.add(cl2in2);
        desc.add(cl1in1);
        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("1"));

        assertEquals("Instance0: ", cl1in2.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl1in1.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl1in3.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl2in1.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl2in2.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl2in3.getSlingId(), desc.get(5).getSlingId());

        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("2"));
        assertEquals("Instance0: ", cl2in3.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl2in1.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl2in2.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl1in1.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl1in2.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl1in3.getSlingId(), desc.get(5).getSlingId());
    }

    @org.junit.Test public void testThreeClustersThreeInstances() {
        final Instance cl1in1 = new Instance("1", "A", false, true);
        final Instance cl1in2 = new Instance("1", "B", true, false);
        final Instance cl1in3 = new Instance("1", "C", false, false);
        final Instance cl15in1 = new Instance("15", "Z", true, false);
        final Instance cl2in1 = new Instance("2", "D", true, false);
        final Instance cl2in2 = new Instance("2", "E", false, false);
        final Instance cl2in3 = new Instance("2", "F", false, false);

        final List<InstanceDescription> desc = new ArrayList<InstanceDescription>();
        desc.add(cl2in3);
        desc.add(cl1in2);
        desc.add(cl2in1);
        desc.add(cl15in1);
        desc.add(cl1in3);
        desc.add(cl2in2);
        desc.add(cl1in1);
        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("1"));

        assertEquals("Instance0: ", cl1in2.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl1in1.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl1in3.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl2in1.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl2in2.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl2in3.getSlingId(), desc.get(5).getSlingId());
        assertEquals("Instance6: ", cl15in1.getSlingId(), desc.get(6).getSlingId());

        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("2"));
        assertEquals("Instance0: ", cl2in1.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl2in2.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl2in3.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl1in1.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl1in2.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl1in3.getSlingId(), desc.get(5).getSlingId());
        assertEquals("Instance6: ", cl15in1.getSlingId(), desc.get(6).getSlingId());

        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("15"));
        assertEquals("Instance0: ", cl15in1.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl1in1.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl1in2.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl1in3.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl2in1.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl2in2.getSlingId(), desc.get(5).getSlingId());
        assertEquals("Instance6: ", cl2in3.getSlingId(), desc.get(6).getSlingId());

        Collections.sort(desc, new TopologyCapabilities.InstanceDescriptionComparator("4"));
        assertEquals("Instance0: ", cl1in1.getSlingId(), desc.get(0).getSlingId());
        assertEquals("Instance1: ", cl1in2.getSlingId(), desc.get(1).getSlingId());
        assertEquals("Instance2: ", cl1in3.getSlingId(), desc.get(2).getSlingId());
        assertEquals("Instance3: ", cl2in1.getSlingId(), desc.get(3).getSlingId());
        assertEquals("Instance4: ", cl2in2.getSlingId(), desc.get(4).getSlingId());
        assertEquals("Instance5: ", cl2in3.getSlingId(), desc.get(5).getSlingId());
        assertEquals("Instance6: ", cl15in1.getSlingId(), desc.get(6).getSlingId());
    }

    private static final class Instance implements InstanceDescription {

        private final String clusterId;
        private final String instanceId;
        private final boolean isLeader;
        private final boolean isLocal;

        public Instance(final String clusterId, final String instanceId, final boolean isLeader, final boolean isLocal) {
            this.clusterId = clusterId;
            this.instanceId = instanceId;
            this.isLeader = isLeader;
            this.isLocal = isLocal;
        }

        @Override
        public ClusterView getClusterView() {
            return new ClusterView() {

                @Override
                public InstanceDescription getLeader() {
                    return null;
                }

                @Override
                public List<InstanceDescription> getInstances() {
                    return null;
                }

                @Override
                public String getId() {
                    return clusterId;
                }
            };
        }

        @Override
        public boolean isLeader() {
            return this.isLeader;
        }

        @Override
        public boolean isLocal() {
            return this.isLocal;
        }

        @Override
        public String getSlingId() {
            return this.instanceId;
        }

        @Override
        public String getProperty(String name) {
            return null;
        }

        @Override
        public Map<String, String> getProperties() {
            return null;
        }
    }
}
