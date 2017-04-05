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

import org.apache.sling.provisioning.model.ModelUtility.ArtifactVersionResolver;
import org.junit.Before;
import org.junit.Test;

public class ModelUtilityApplyArtifactVersionsTest {

    private Model testModel;
    private ArtifactVersionResolver testArtifactVersionResolver;
    
    @Before
    public void setUp() {
        testModel = new Model();
        
        Feature feature = testModel.getOrCreateFeature("feature1");
        RunMode runMode = feature.getOrCreateRunMode(new String[] { "rm1"});
        ArtifactGroup group = runMode.getOrCreateArtifactGroup(10);
        
        group.add(new Artifact("g1", "a1", "v1", "c1", "t1"));
        group.add(new Artifact("g2", "a2", "LATEST", null, null));
        group.add(new Artifact("g3", "a3", "LATEST", null, null));
        
        // dummy variable resolver that simulates external resolving of some variables
        testArtifactVersionResolver = new ArtifactVersionResolver() {
            @Override
            public String resolve(Artifact artifact) {
                if ("g2".equals(artifact.getGroupId()) && "a2".equals(artifact.getArtifactId())) {
                    return "v2";
                }
                return null;
            }
        };
    }

    @Test
    public void testApplyArtifactVersions() {
        Model model = ModelUtility.applyArtifactVersions(testModel, testArtifactVersionResolver);

        Feature feature = model.getFeature("feature1");
        RunMode runMode = feature.getRunMode("rm1");
        ArtifactGroup group = runMode.getArtifactGroup(10);
        
        U.assertArtifactsInGroup(group, 3);
        U.assertArtifact(group, "mvn:g1/a1/v1/t1/c1");
        U.assertArtifact(group, "mvn:g2/a2/v2/jar");
        U.assertArtifact(group, "mvn:g3/a3//jar");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testApplyVariablesInvalidVariable() {
        ModelUtility.applyArtifactVersions(testModel, new ArtifactVersionResolver() {
            @Override
            public String resolve(Artifact artifact) {
                throw new IllegalArgumentException("Unable to resolve.");
            }
        });
    }
    
}
