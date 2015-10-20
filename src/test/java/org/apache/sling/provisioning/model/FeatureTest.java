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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FeatureTest {
    @Test
    public void testTypeEnum() {
        assertEquals(Feature.Type.SUBSYSTEM_APPLICATION,
                Feature.Type.fromTextRepresentation("osgi.subsystem.application"));
        assertEquals(Feature.Type.SUBSYSTEM_COMPOSITE,
                Feature.Type.fromTextRepresentation("osgi.subsystem.composite"));
        assertEquals(Feature.Type.SUBSYSTEM_FEATURE,
                Feature.Type.fromTextRepresentation("osgi.subsystem.feature"));
        assertEquals(Feature.Type.PLAIN, Feature.Type.fromTextRepresentation(null));

        assertEquals("osgi.subsystem.application",
                Feature.Type.SUBSYSTEM_APPLICATION.getTextRepresentation());
        assertEquals("osgi.subsystem.composite",
                Feature.Type.SUBSYSTEM_COMPOSITE.getTextRepresentation());
        assertEquals("osgi.subsystem.feature",
                Feature.Type.SUBSYSTEM_FEATURE.getTextRepresentation());
    }

    @Test
    public void testFeatureType() {
        Feature f = new Feature("blah");
        assertEquals(Feature.Type.PLAIN, f.getType());

        f.setType(Feature.Type.SUBSYSTEM_APPLICATION);
        assertEquals(Feature.Type.SUBSYSTEM_APPLICATION, f.getType());
    }
}
