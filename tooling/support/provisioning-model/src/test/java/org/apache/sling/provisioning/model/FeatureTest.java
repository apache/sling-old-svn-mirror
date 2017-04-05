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
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;
import org.junit.Test;

public class FeatureTest {

    @Test
    public void testFeatureType() {
        Feature f = new Feature("blah");
        assertEquals(FeatureTypes.PLAIN, f.getType());

        f.setType(FeatureTypes.SUBSYSTEM_APPLICATION);
        assertEquals(FeatureTypes.SUBSYSTEM_APPLICATION, f.getType());
    }

    @Test
    public void testFeatureVersion() {
        Feature f = new Feature("blah");
        assertNull(f.getVersion());

        f.setVersion("1.0.0");
        assertEquals("1.0.0", f.getVersion());
    }

    @Test
    public void testFeatureVersions() throws Exception {
        final Model m = U.readTestModel("feature.txt");

        assertEquals(3, m.getFeatures().size());
        Feature a = m.getFeature("a");
        assertNotNull(a);
        assertEquals("1.0", a.getVersion());

        Feature b = m.getFeature("b");
        assertNotNull(b);
        assertNull(b.getVersion());

        Feature c = m.getFeature("c");
        assertNotNull(c);
        assertEquals("2.0", c.getVersion());

        // Write the model
        StringWriter writer = new StringWriter();
        try {
            ModelWriter.write(writer, m);
        } finally {
            writer.close();
        }

        // read it again
        StringReader reader = new StringReader(writer.toString());
        final Model readModel = ModelReader.read(reader, "memory");
        reader.close();

        assertEquals(3, readModel.getFeatures().size());
        a = readModel.getFeature("a");
        assertNotNull(a);
        assertEquals("1.0", a.getVersion());

        b = readModel.getFeature("b");
        assertNotNull(b);
        assertNull(b.getVersion());

        c = readModel.getFeature("c");
        assertNotNull(c);
        assertEquals("2.0", c.getVersion());
    }
}
