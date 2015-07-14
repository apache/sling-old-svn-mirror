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

import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.junit.Before;
import org.junit.Test;

public class ModelUtilityApplyVariablesTest {

    private Model testModel;
    private VariableResolver testVariableResolver;
    
    @Before
    public void setUp() {
        testModel = new Model();
        
        Feature feature = testModel.getOrCreateFeature("feature1");
        feature.getVariables().put("param1", "v1");
        feature.getVariables().put("extparam2", "v2");
        
        RunMode runMode = feature.getOrCreateRunMode(new String[] { "rm1"});
        ArtifactGroup group = runMode.getOrCreateArtifactGroup(10);
        
        group.add(new Artifact("g1", "a1", "${param1}", "c1", "t1"));
        group.add(new Artifact("g2", "a2", "${extparam2}", null, null));
        
        Configuration conf = runMode.getOrCreateConfiguration("pid1", null);
        conf.getProperties().put(ModelConstants.CFG_UNPROCESSED, "conf1=\"${extparam1}\"\n"
                + "conf2=\"${extparam2}\"");
        
        runMode.getSettings().put("set1", "${param1}");
        
        
        // dummy variable resolver that simulates external resolving of some variables
        testVariableResolver = new VariableResolver() {
            @Override
            public String resolve(Feature feature, String name) {
                if ("extparam1".equals(name)) {
                    return "extvalue1";
                }
                if ("extparam2".equals(name)) {
                    return "extvalue2";
                }
                return feature.getVariables().get(name);
            }
        };
    }

    @Test
    public void testApplyVariables() {
        Model model = ModelUtility.applyVariables(testModel, testVariableResolver);

        Feature feature = model.getFeature("feature1");
        assertEquals("v1", feature.getVariables().get("param1"));
        assertEquals("extvalue1", feature.getVariables().get("extparam1"));
        assertEquals("extvalue2", feature.getVariables().get("extparam2"));
        assertEquals(3, feature.getVariables().size());
        
        Model effectiveModel = ModelUtility.getEffectiveModel(model);
        Feature effectiveFeature = effectiveModel.getFeature("feature1");

        RunMode runMode = effectiveFeature.getRunMode("rm1");
        ArtifactGroup group = runMode.getArtifactGroup(10);
        
        group.add(new Artifact("g1", "a1", "${param1}", "c1", "t1"));
        group.add(new Artifact("g2", "a2", "${extparam2}", null, null));
        
        Configuration conf = runMode.getConfiguration("pid1", null);
        assertEquals("extvalue1", conf.getProperties().get("conf1"));
        assertEquals("extvalue2", conf.getProperties().get("conf2"));

        assertEquals("v1", runMode.getSettings().get("set1"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testApplyVariablesInvalidVariable() {
        ModelUtility.applyVariables(testModel, new VariableResolver() {
            @Override
            public String resolve(Feature feature, String name) {
                return feature.getVariables().get(name);
            }
        });
    }
    
}
