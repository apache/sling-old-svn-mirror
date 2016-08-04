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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        assertEquals(FeatureTypes.SUBSYSTEM_COMPOSITE, model.getFeature("f").getType());
    }

    @Test public void removeTest() throws Exception {
        final Model model = U.readCompleteTestModel(new String[] {"merge/remove-base.txt",
                                                   "merge/remove-merge.txt"});

        assertNotNull(model.getFeature("f"));
        assertNotNull(model.getFeature("f").getRunMode());
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(5));
        assertNotNull(model.getFeature("f").getRunMode().getArtifactGroup(7));
        assertEquals(FeatureTypes.PLAIN, model.getFeature("f").getType());

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

        assertNotNull(model.getFeature("f").getRunMode("myrunmode"));
        final List<Configuration> cfgs2 = U.assertConfigurationsInRunMode(model.getFeature("f").getRunMode("myrunmode"), 2);
        assertEquals("org.sling.service.runmode.A", cfgs2.get(0).getPid());
        assertEquals("org.sling.service.runmode.C", cfgs2.get(1).getPid());
    }

    @Test public void mergeRawTest() throws Exception {
        final Model baseRaw = U.readCompleteTestModel(new String[] {"merge/config-base.txt"});
        final Model mergeRaw = U.readCompleteTestModel(new String[] {"merge/config-merge.txt"});

        ModelUtility.merge(baseRaw, mergeRaw);

        final List<Configuration> cfgs = U.assertConfigurationsInRunMode(baseRaw.getFeature("configadmin").getRunMode(), 4);

        final Configuration cfgBoot = cfgs.get(0);
        assertEquals(1, cfgBoot.getProperties().size());
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.a"));
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.b"));
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.c"));

        final Configuration cfgA = cfgs.get(1);
        assertEquals("org.apache.test.A", cfgA.getPid());
        assertNull(cfgA.getFactoryPid());
        assertEquals(1, cfgA.getProperties().size());
        assertEquals("AA", cfgA.getProperties().get("name"));

        final Configuration cfgB = cfgs.get(2);
        assertEquals("org.apache.test.B", cfgB.getPid());
        assertNull(cfgB.getFactoryPid());
        assertEquals(3, cfgB.getProperties().size());
        assertEquals("BB", cfgB.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));
        assertArrayEquals(new String[] {"one", "two", "three"}, (String[])cfgB.getProperties().get("array"));

        final Configuration cfgC = cfgs.get(3);
        assertEquals("org.apache.test.C", cfgC.getPid());
        assertNull(cfgC.getFactoryPid());
        assertEquals(3, cfgC.getProperties().size());
        assertEquals("C", cfgC.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));
        assertArrayEquals(new Integer[] {1,2,3}, (Integer[])cfgC.getProperties().get("array"));
    }

    @Test public void mergeEffectiveTest() throws Exception {
        final Model baseRaw = U.readCompleteTestModel(new String[] {"merge/config-base.txt"});
        final Model mergeRaw = U.readCompleteTestModel(new String[] {"merge/config-merge.txt"});

        final Model baseEffective = ModelUtility.getEffectiveModel(baseRaw);
        final Model mergeEffective = ModelUtility.getEffectiveModel(mergeRaw);

        ModelUtility.merge(baseEffective, mergeEffective);

        final List<Configuration> cfgs = U.assertConfigurationsInRunMode(baseEffective.getFeature("configadmin").getRunMode(), 4);

        final Configuration cfgBoot = cfgs.get(0);
        assertEquals(1, cfgBoot.getProperties().size());
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.c"));

        final Configuration cfgA = cfgs.get(1);
        assertEquals("org.apache.test.A", cfgA.getPid());
        assertNull(cfgA.getFactoryPid());
        assertEquals(1, cfgA.getProperties().size());
        assertEquals("AA", cfgA.getProperties().get("name"));

        final Configuration cfgB = cfgs.get(2);
        assertEquals("org.apache.test.B", cfgB.getPid());
        assertNull(cfgB.getFactoryPid());
        assertEquals(2, cfgB.getProperties().size());
        assertEquals("BB", cfgB.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));

        final Configuration cfgC = cfgs.get(3);
        assertEquals("org.apache.test.C", cfgC.getPid());
        assertNull(cfgC.getFactoryPid());
        assertEquals(1, cfgC.getProperties().size());
        assertEquals("bar", cfgB.getProperties().get("foo"));
    }

    @Test public void mergeBaseRawTest() throws Exception {
        final Model baseRaw = U.readCompleteTestModel(new String[] {"merge/config-base.txt"});
        final Model mergeRaw = U.readCompleteTestModel(new String[] {"merge/config-merge.txt"});
        final Model mergeEffective = ModelUtility.getEffectiveModel(mergeRaw);

        ModelUtility.merge(baseRaw, mergeEffective);

        final List<Configuration> cfgs = U.assertConfigurationsInRunMode(baseRaw.getFeature("configadmin").getRunMode(), 4);

        final Configuration cfgBoot = cfgs.get(0);
        assertEquals(1, cfgBoot.getProperties().size());
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.c"));

        final Configuration cfgA = cfgs.get(1);
        assertEquals("org.apache.test.A", cfgA.getPid());
        assertNull(cfgA.getFactoryPid());
        assertEquals(1, cfgA.getProperties().size());
        assertEquals("AA", cfgA.getProperties().get("name"));

        final Configuration cfgB = cfgs.get(2);
        assertEquals("org.apache.test.B", cfgB.getPid());
        assertNull(cfgB.getFactoryPid());
        assertEquals(2, cfgB.getProperties().size());
        assertEquals("BB", cfgB.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));

        final Configuration cfgC = cfgs.get(3);
        assertEquals("org.apache.test.C", cfgC.getPid());
        assertNull(cfgC.getFactoryPid());
        assertEquals(1, cfgC.getProperties().size());
        assertEquals("bar", cfgB.getProperties().get("foo"));
    }

    @Test public void mergeBaseEffectiveTest() throws Exception {
        final Model baseRaw = U.readCompleteTestModel(new String[] {"merge/config-base.txt"});
        final Model mergeRaw = U.readCompleteTestModel(new String[] {"merge/config-merge.txt"});
        final Model baseEffective = ModelUtility.getEffectiveModel(baseRaw);

        ModelUtility.merge(baseEffective, mergeRaw);

        final List<Configuration> cfgs = U.assertConfigurationsInRunMode(baseEffective.getFeature("configadmin").getRunMode(), 4);

        final Configuration cfgBoot = cfgs.get(0);
        assertEquals(1, cfgBoot.getProperties().size());
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.a"));
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.b"));
        assertTrue(cfgBoot.getProperties().get(":bootstrap").toString().contains("uninstall bundle.c"));

        final Configuration cfgA = cfgs.get(1);
        assertEquals("org.apache.test.A", cfgA.getPid());
        assertNull(cfgA.getFactoryPid());
        assertEquals(1, cfgA.getProperties().size());
        assertEquals("AA", cfgA.getProperties().get("name"));

        final Configuration cfgB = cfgs.get(2);
        assertEquals("org.apache.test.B", cfgB.getPid());
        assertNull(cfgB.getFactoryPid());
        assertEquals(3, cfgB.getProperties().size());
        assertEquals("BB", cfgB.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));
        assertArrayEquals(new String[] {"one", "two", "three"}, (String[])cfgB.getProperties().get("array"));

        final Configuration cfgC = cfgs.get(3);
        assertEquals("org.apache.test.C", cfgC.getPid());
        assertNull(cfgC.getFactoryPid());
        assertEquals(3, cfgC.getProperties().size());
        assertEquals("C", cfgC.getProperties().get("name"));
        assertEquals("bar", cfgB.getProperties().get("foo"));
        assertArrayEquals(new Integer[] {1,2,3}, (Integer[])cfgC.getProperties().get("array"));
    }

    @Test public void testRemoveFromSpecialRunmode() throws Exception {
        final Model base = U.readTestModel("merge/remove-special-base.txt");
        final Model merge = U.readTestModel("merge/remove-special-merge.txt");
        final Model model = ModelUtility.getEffectiveModel(base);

        MergeUtility.merge(model, merge);

        final List<Artifact> group = U.assertArtifactsInGroup(model.getFeature("f").getRunMode(ModelConstants.RUN_MODE_STANDALONE).getArtifactGroup(0), 1);
        U.assertArtifact(group.get(0), "g", "b", "1.0.0", "jar", null);
    }

    @Test public void testValidateRunModes() throws Exception {
        final Model m = new Model();
        final Feature f = m.getOrCreateFeature("f");

        // single special run mode
        final RunMode rm1 = f.getOrCreateRunMode(new String[] {ModelConstants.RUN_MODE_STANDALONE});
        assertNull(ModelUtility.validate(m));
        f.getRunModes().remove(rm1);

        // two special run modes -> error
        final RunMode rmA = f.getOrCreateRunMode(new String[] {ModelConstants.RUN_MODE_STANDALONE, ModelConstants.RUN_MODE_WEBAPP});
        assertNotNull(ModelUtility.validate(m));
        f.getRunModes().remove(rmA);

        // two special run modes, one :remove -> ok
        final RunMode rmB = f.getOrCreateRunMode(new String[] {ModelConstants.RUN_MODE_STANDALONE, ModelConstants.RUN_MODE_REMOVE});
        assertNull(ModelUtility.validate(m));
        f.getRunModes().remove(rmB);

        // three special run modes, one :remove -> error
        final RunMode rmC = f.getOrCreateRunMode(new String[] {ModelConstants.RUN_MODE_STANDALONE, ModelConstants.RUN_MODE_WEBAPP, ModelConstants.RUN_MODE_REMOVE});
        assertNotNull(ModelUtility.validate(m));
        f.getRunModes().remove(rmC);
    }
}
