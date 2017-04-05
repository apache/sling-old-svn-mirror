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
package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.sling.provisioning.model.Artifact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NestedModelsMergerTest {
    private final NestedModelsMerger merger = new NestedModelsMerger(null);
    private final Artifact testArtifact;
    private final boolean expectNestedModel;
    
    @Parameters(name="{0}/{1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {   
                { "txt", "slingfeature", true },
                { "txt", "slingstart", true },
                { "slingfeature", null, true },
                { "slingstart", null, true },
                { "txt", null, false }
        });
    }
    
    public NestedModelsMergerTest(String aType, String classifier, boolean expectNestedModel) {
        testArtifact = new Artifact("GID", "AId", "VERSION", classifier, aType);
        this.expectNestedModel = expectNestedModel;
    }
    
    @Test
    public void testNestedModelOrNot() {
        if(expectNestedModel) {
            assertTrue("Expecting a nested model for " + testArtifact, merger.isNestedModel(testArtifact));
        } else {
            assertFalse("Not expecting a nested model for " + testArtifact, merger.isNestedModel(testArtifact));
        }
    }
}