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
package org.apache.sling.provisioning.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

public class ModelUtilityTest {

    @Test public void mergeArtifactsTest() throws Exception {
        final Model model = U.readCompleteTestModel(new String[] {"merge/artifact-base.txt",
                                                                 "merge/artifact-merge.txt"});

        // model should now have one artifact
        assertNotNull(model.getFeature("f"));
        assertNotNull(model.getFeature("f").getRunMode());
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(3));
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(5));
        U.assertArtifactsInGroup(model.getFeature("f").getRunMode().getArtifactGroup(5), 0);
        final List<Artifact> list = U.assertArtifactsInGroup(model.getFeature("f").getRunMode().getArtifactGroup(3), 1);

        U.assertArtifact(list.get(0), "g", "a", "2.0.0", "jar", null);
    }

    @Test public void removeTest() throws Exception {
        final Model model = U.readCompleteTestModel(new String[] {"merge/remove-base.txt",
                                                   "merge/remove-merge.txt"});

        assertNotNull(model.getFeature("f"));
        assertNotNull(model.getFeature("f").getRunMode());
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(5));
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(7));

        final List<Artifact> group5 = U.assertArtifactsInGroup(model.getFeature("f").getRunMode().getArtifactGroup(5), 1);
        U.assertArtifact(group5.get(0), "g", "a", "1.0.0", "jar", null);

        final List<Artifact> group7 = U.assertArtifactsInGroup(model.getFeature("f").getRunMode().getArtifactGroup(7), 1);
        U.assertArtifact(group7.get(0), "g", "c", "1.0.0", "jar", null);

        final List<Configuration> cfgs = U.assertConfigurationsInRunMode(model.getFeature("f").getRunMode(), 2);
        assertEquals("org.sling.service.A", cfgs.get(0).getPid());
        assertEquals("org.sling.service.C", cfgs.get(1).getPid());

        assertEquals(2, model.getFeature("f").getRunMode().getSettings().size());
        assertEquals("a", model.getFeature("f").getRunMode().getSettings().get("key.a"));
        assertEquals("c", model.getFeature("f").getRunMode().getSettings().get("key.c"));
    }
}
