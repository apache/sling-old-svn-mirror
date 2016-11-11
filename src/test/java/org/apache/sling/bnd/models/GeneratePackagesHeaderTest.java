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
package org.apache.sling.bnd.models;

import static org.apache.sling.bnd.models.ModelsScannerPlugin.MODELS_CLASSES_HEADER;
import static org.apache.sling.bnd.models.ModelsScannerPlugin.MODELS_PACKAGES_HEADER;
import static org.apache.sling.bnd.models.ModelsScannerPlugin.PROPERTY_GENERATE_PACKAGES_HEADER;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import aQute.bnd.osgi.Jar;

public class GeneratePackagesHeaderTest extends AbstractModelsScannerPluginTest {

    @Override
    protected Map<String, String> getProperties() {
        Map<String,String> props = new HashMap<>();
        props.put(PROPERTY_GENERATE_PACKAGES_HEADER, "true");
        return props;
    }

    @Test
    public void testBuild() throws Exception {
        Jar jar = builder.build();
        
        assertHeader(jar, MODELS_PACKAGES_HEADER, 
                "dummy.example.pkg1",
                "dummy.example.pkg2");        

        assertHeaderMissing(jar, MODELS_CLASSES_HEADER);
        
    }

}
