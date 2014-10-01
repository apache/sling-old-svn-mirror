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

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.Traceable;
import org.junit.Test;

public class IOTest {

    /**
     * Not really a unit test...but better than nothing
     */
    @Test public void testReadWrite() throws Exception {
        final Model result = new Model();
        final String[] candidates = new String[] {"boot.txt", "example.txt", "main.txt", "oak.txt"};

        for(final String name : candidates) {
            final Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("/" + name), "UTF-8");
            try {
                final Model current = ModelReader.read(reader, name);
                final Map<Traceable, String> errors = ModelUtility.validate(current);
                if (errors != null ) {
                    throw new Exception("Invalid model at " + name + " : " + errors);
                }
                ModelUtility.merge(result, current);
            } finally {
                reader.close();
            }
        }

        final Map<Traceable, String> errors = ModelUtility.validate(result);
        if (errors != null ) {
            throw new Exception("Invalid assembled model : " + errors);
        }

        // write the complete model
        StringWriter writer = new StringWriter();
        try {
            ModelWriter.write(writer, result);
        } finally {
            writer.close();
        }

        // and read it again
        StringReader reader = new StringReader(writer.toString());
        final Model readModel = ModelReader.read(reader, "memory");
        reader.close();
        final Map<Traceable, String> readErrors = ModelUtility.validate(readModel);
        if (readErrors != null ) {
            throw new Exception("Invalid read model : " + readErrors);
        }
    }
}
