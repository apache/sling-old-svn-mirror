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
package org.apache.sling.acldef.provisioning;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.MergeUtility;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.junit.Before;
import org.junit.Test;

public class AclModelTest {
    
    private Model model;
    
    private void readModel(String resourcePath) throws IOException {
        final InputStream is = getClass().getResourceAsStream(resourcePath);
        assertNotNull("Expecting resource at " + resourcePath, is);
        final Reader r = new InputStreamReader(is, "UTF-8");
        try {
            MergeUtility.merge(model, ModelReader.read(r, resourcePath));
        } finally {
            r.close();
        }
    }
    
    @Before
    public void setup() throws IOException {
        model = new Model();
        model.setLocation(getClass().getSimpleName());
    }
    
    private void assertConfig(Configuration c, int index, String ... expected) {
        final String key = AclConfigsProvider.PROP_ACLDEFTEXT_PREFIX + (index);
        final Object obj = c.getProperties().get(key);
        assertNotNull("Expecting property " + key, obj);
        final String content = obj.toString();
        for(String exp : expected) {
            assertTrue("Expected " + exp + " in property " + key + " = "+ content, content.contains(exp));
        }
    }
    
    @Test
    public void singleFeature() throws IOException {
        readModel("/models/aclmodel.txt");
        final Configuration cfg = new AclConfigsProvider().getAclDefConfigs(model);
        
        assertConfig(cfg, 1, 
                "feature testfeature", "model AclModelTest", 
                "\nset ACL for someuser", "allow line1", "\nend"); 
        assertConfig(cfg, 2, 
                "feature testfeature", "model AclModelTest", 
                "\nset ACL for another\nend"); 
    }
    
    @Test
    public void noAcl() throws IOException {
        readModel("/models/noacl.txt");
        assertNull("Expecting no Configuration", new AclConfigsProvider().getAclDefConfigs(model));
    }
    
    @Test
    public void mergedModels() throws IOException {
        readModel("/models/aclmodel.txt");
        readModel("/models/moreacls.txt");
        final Configuration cfg = new AclConfigsProvider().getAclDefConfigs(model);
        
        // TODO make test more robust against section indexes
        assertConfig(cfg, 3, 
                "feature testfeature", "model AclModelTest", 
                "\nset ACL for someuser", "allow line1", "\nend"); 
        assertConfig(cfg, 4, 
                "feature testfeature", "model AclModelTest", 
                "\nset ACL for another\nend"); 
        assertConfig(cfg, 1, 
                "feature otherfeature", "model AclModelTest", 
                "\nset ACL for bob\nend\n\ncreate service user P"); 
        assertConfig(cfg, 2, 
                "feature otherfeature", "model AclModelTest", 
                "\ncreate service user S\ncreate service user T"); 
    }
    
}
