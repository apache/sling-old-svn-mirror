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
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.junit.Test;

/** Read and merge our test models, write and read them again
 *  and verify the result at various stages.
 */
public class CustomResolverTest {

    public static class CustomVariableResolver implements VariableResolver {
        @Override
        public String resolve(Feature feature, String name) {
            return "#" + feature.getName() + "#" + name + "#";
        }
    }
    
    @Test
    public void testCustomVariableResolverNoFilter() throws Exception {
        final Model m = U.readCompleteTestModel();
        final VariableResolver r = new CustomVariableResolver();
        final Model effective = ModelUtility.getEffectiveModel(m, new ResolverOptions().variableResolver(r));
        final ArtifactGroup g = U.getGroup(effective, "example", "jackrabbit", 15);
        U.assertArtifact(g, "mvn:org.apache.sling/org.apache.sling.jcr.jackrabbit.server/#example#jackrabbit.version#/jar");
    }

    public static class CustomArtifactVersionResolver implements ArtifactVersionResolver {
        @Override
        public String resolve(Artifact artifact) {
            return "9.9.9";
        }
    }
    
    @Test
    public void testCustomArtifactVersionResolver() throws Exception {
        final Model m = U.readCompleteTestModel();
        final ArtifactVersionResolver r = new CustomArtifactVersionResolver();
        final Model effective = ModelUtility.getEffectiveModel(m, new ResolverOptions().artifactVersionResolver(r));
        final ArtifactGroup g = U.getGroup(effective, "example", null, 0);
        U.assertArtifact(g, "mvn:org.example/jar-without-version/9.9.9");
    }

}
