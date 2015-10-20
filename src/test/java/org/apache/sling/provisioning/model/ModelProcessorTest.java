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
package org.apache.sling.provisioning.model;

import java.util.Enumeration;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ModelProcessorTest {

    private Model testModel;
    private ModelProcessor underTest;

    @Before
    public void setUp() {
        testModel = new Model();
        testModel.setLocation("LocM1");

        Feature feature1 = testModel.getOrCreateFeature("feature1");
        feature1.setLocation("LocF1");
        feature1.setComment("ComF1");
        feature1.setType(Feature.Type.SUBSYSTEM_COMPOSITE);
        feature1.getVariables().setLocation("LocFV1");
        feature1.getVariables().setComment("ComFV1");
        feature1.getVariables().put("k1", "v1");
        feature1.getVariables().put("k2", "v2");

        RunMode runMode11 = feature1.getOrCreateRunMode(new String[] { "rm1", "rm2"});
        runMode11.setLocation("LocRM11");

        RunMode runMode12 = feature1.getOrCreateRunMode(new String[] { "rm2"});
        ArtifactGroup group12 = runMode12.getOrCreateArtifactGroup(10);
        group12.setLocation("LocRMG11");
        group12.setComment("ComRMG11");

        group12.add(new Artifact("g1", "a1", "v1", "c1", "t1"));
        group12.add(new Artifact("g2", "a2", "v2", null, null));

        runMode12.getConfigurations().setLocation("LocConf12");
        runMode12.getConfigurations().setComment("ComConf12");

        Configuration conf121 = runMode12.getOrCreateConfiguration("pid1", null);
        conf121.setLocation("LocConf121");
        conf121.setComment("ComConf121");
        conf121.getProperties().put("conf1", "v1");
        conf121.getProperties().put("conf2", "v2");

        Configuration conf122 = runMode12.getOrCreateConfiguration("pid2", "fac2");
        conf122.setLocation("LocConf122");
        conf122.setComment("ComConf122");
        conf122.getProperties().put("conf3", "v3");

        runMode12.getSettings().setLocation("LocSet12");
        runMode12.getSettings().setComment("ComSet12");

        runMode12.getSettings().put("set1", "v1");
        runMode12.getSettings().put("set2", "v2");

        Feature feature2 = testModel.getOrCreateFeature("feature1");

        RunMode runMode21 = feature2.getOrCreateRunMode(new String[0]);
        ArtifactGroup group21 = runMode21.getOrCreateArtifactGroup(20);
        group21.add(new Artifact("g3", "a3", null, null, null));

        underTest = new TestModelProcessor();
    }

    @Test
    public void testProcess() {
        Model model = underTest.process(testModel);

        assertEquals("LocM1", model.getLocation());

        Feature feature1 = model.getFeature("feature1");
        assertNotNull(feature1);
        assertEquals("LocF1", feature1.getLocation());
        assertEquals("ComF1", feature1.getComment());
        assertEquals(Feature.Type.SUBSYSTEM_COMPOSITE, feature1.getType());
        assertEquals("LocFV1", feature1.getVariables().getLocation());
        assertEquals("ComFV1", feature1.getVariables().getComment());
        assertEquals("#v1", feature1.getVariables().get("k1"));
        assertEquals("#v2", feature1.getVariables().get("k2"));

        RunMode runMode11 = feature1.getRunMode("rm1", "rm2");
        assertNotNull(runMode11);
        assertEquals("LocRM11", runMode11.getLocation());

        RunMode runMode12 = feature1.getRunMode(new String[] { "rm2"});
        assertNotNull(runMode12);

        ArtifactGroup group12 = runMode12.getArtifactGroup(10);
        assertNotNull(group12);
        assertEquals("LocRMG11", group12.getLocation());
        assertEquals("ComRMG11", group12.getComment());

        U.assertArtifactsInGroup(group12, 2);
        U.assertArtifact(group12, "mvn:#g1/#a1/#v1/#t1/#c1");
        U.assertArtifact(group12, "mvn:#g2/#a2/#v2/#jar");

        assertEquals("LocConf12", runMode12.getConfigurations().getLocation());
        assertEquals("ComConf12", runMode12.getConfigurations().getComment());

        Configuration conf121 = runMode12.getConfiguration("pid1", null);
        assertEquals("LocConf121", conf121.getLocation());
        assertEquals("ComConf121", conf121.getComment());
        assertEquals("#v1", conf121.getProperties().get("conf1"));
        assertEquals("#v2", conf121.getProperties().get("conf2"));

        Configuration conf122 = runMode12.getConfiguration("pid2", "fac2");
        assertEquals("LocConf122", conf122.getLocation());
        assertEquals("ComConf122", conf122.getComment());
        assertEquals("#v3", conf122.getProperties().get("conf3"));

        assertEquals("LocSet12", runMode12.getSettings().getLocation());
        assertEquals("ComSet12", runMode12.getSettings().getComment());
        assertEquals("#v1", runMode12.getSettings().get("set1"));
        assertEquals("#v2", runMode12.getSettings().get("set2"));

        Feature feature2 = model.getFeature("feature1");
        assertNotNull(feature2);

        RunMode runMode21 = feature2.getRunMode();
        assertNotNull(runMode21);

        ArtifactGroup group21 = runMode21.getArtifactGroup(20);
        assertNotNull(group21);

        U.assertArtifactsInGroup(group21, 1);
        U.assertArtifact(group21, "mvn:#g3/#a3/#LATEST/#jar");
    }


    static final class TestModelProcessor extends ModelProcessor {

        @Override
        protected KeyValueMap<String> processVariables(KeyValueMap<String> variables, Feature feature) {
            KeyValueMap<String> newVars = new KeyValueMap<String>();
            for (Entry<String, String> entry : variables) {
                newVars.put(entry.getKey(), "#" + entry.getValue());
            }
            return newVars;
        }

        @Override
        protected Artifact processArtifact(Artifact artifact, Feature feature, RunMode runMode) {
            Artifact newArtifact = new Artifact(
                    artifact.getGroupId()!=null ? "#" + artifact.getGroupId() : null,
                    artifact.getArtifactId()!=null ? "#" + artifact.getArtifactId() : null,
                    artifact.getVersion()!=null ? "#" + artifact.getVersion() : "#LATEST",
                    artifact.getClassifier()!=null ? "#" + artifact.getClassifier() : null,
                    artifact.getType()!=null ? "#" + artifact.getType() : null);
            return newArtifact;
        }

        @Override
        protected Configuration processConfiguration(Configuration configuration, Feature feature, RunMode runMode) {
            Configuration newConfig = new Configuration(configuration.getPid(), configuration.getFactoryPid());
            final Enumeration<String> i = configuration.getProperties().keys();
            while ( i.hasMoreElements() ) {
                final String key = i.nextElement();
                newConfig.getProperties().put(key, "#" + configuration.getProperties().get(key));
            }
            return newConfig;
        }

        @Override
        protected KeyValueMap<String> processSettings(KeyValueMap<String> settings, Feature feature, RunMode runMode) {
            KeyValueMap<String> newSettings = new KeyValueMap<String>();
            for (Entry<String, String> entry : settings) {
                newSettings.put(entry.getKey(), "#" + entry.getValue());
            }
            return newSettings;
        }

    }

}
