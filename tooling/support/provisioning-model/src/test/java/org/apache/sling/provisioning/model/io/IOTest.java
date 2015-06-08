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
package org.apache.sling.provisioning.model.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.U;
import org.junit.Test;

/** Read and merge our test models, write and read them again
 *  and verify the result at various stages.
 */
public class IOTest {

    @Test public void testReadWrite() throws Exception {
        final Model result = U.readCompleteTestModel();

        U.verifyTestModel(result, false);

        // Write the merged raw model
        StringWriter writer = new StringWriter();
        try {
            ModelWriter.write(writer, result);
        } finally {
            writer.close();
        }

        // read it again
        StringReader reader = new StringReader(writer.toString());
        final Model readModel = ModelReader.read(reader, "memory");
        reader.close();
        final Map<Traceable, String> readErrors = ModelUtility.validate(readModel);
        if (readErrors != null ) {
            throw new Exception("Invalid read model : " + readErrors);
        }

        // and verify the result
        U.verifyTestModel(readModel, false);

        // Resolve variables and verify the result
        final Model effective = ModelUtility.getEffectiveModel(readModel, null);
        U.verifyTestModel(effective, true);

        // write effective model
        writer = new StringWriter();
        ModelWriter.write(writer, effective);
        writer.close();

        reader = new StringReader(writer.toString());
        final Model readModel2 = ModelReader.read(reader, "memory");
        reader.close();
        final Map<Traceable, String> readErrors2 = ModelUtility.validate(readModel2);
        if (readErrors2 != null ) {
            throw new Exception("Invalid read model : " + readErrors2);
        }
        // and verify the result
        U.verifyTestModel(readModel2, true);
    }

    @Test public void testMultilineConfiguration() throws Exception {
        final Model m = ModelUtility.getEffectiveModel(U.readCompleteTestModel(new String[] {"configadmin.txt"}), null);

        final List<Configuration> configs = new ArrayList<Configuration>();
        for(final Configuration c :  m.getFeature("configadmin").getRunMode().getConfigurations()) {
            configs.add(c);
        }

        assertEquals(3, configs.size());

        final Configuration cfgA = configs.get(0);
        assertEquals("org.apache.test.A", cfgA.getPid());
        assertNull(cfgA.getFactoryPid());
        assertEquals(1, cfgA.getProperties().size());
        assertEquals("A", cfgA.getProperties().get("name"));

        final Configuration cfgB = configs.get(1);
        assertEquals("org.apache.test.B", cfgB.getPid());
        assertNull(cfgB.getFactoryPid());
        assertEquals(2, cfgB.getProperties().size());
        assertEquals("B", cfgB.getProperties().get("name"));
        assertArrayEquals(new String[] {"one", "two", "three"}, (String[])cfgB.getProperties().get("array"));

        final Configuration cfgC = configs.get(2);
        assertEquals("org.apache.test.C", cfgC.getPid());
        assertNull(cfgC.getFactoryPid());
        assertEquals(2, cfgC.getProperties().size());
        assertEquals("C", cfgC.getProperties().get("name"));
        assertArrayEquals(new Integer[] {1,2,3}, (Integer[])cfgC.getProperties().get("array"));
    }
}
