/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.bundlesupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class IntermediateUrisExtractorTest {
    
    @Test
    public void extractPaths() {

        doTest("http://localhost:8080/apps/slingshot/install", 
                Arrays.asList("http://localhost:8080/apps/slingshot/install", "http://localhost:8080/apps/slingshot", "http://localhost:8080/apps" ));
    }
    
    private void doTest(String input, List<String> expectedOutput) {

        List<String> paths = IntermediateUrisExtractor.extractIntermediateUris(input);
        
        assertThat(paths, equalTo(expectedOutput));
    }

    @Test
    public void extractPaths_trailingSlash() {
        
        doTest("http://localhost:8080/apps/slingshot/install/", 
                Arrays.asList("http://localhost:8080/apps/slingshot/install", "http://localhost:8080/apps/slingshot", "http://localhost:8080/apps" ));
    }

    @Test
    public void extractPaths_empty() {
        
        doTest("http://localhost:8080", Collections.<String> emptyList());
    }

}
