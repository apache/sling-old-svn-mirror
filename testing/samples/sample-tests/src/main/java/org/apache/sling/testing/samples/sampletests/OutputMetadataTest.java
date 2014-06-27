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
package org.apache.sling.testing.samples.sampletests;

import static org.junit.Assert.assertNotNull;
import org.apache.sling.junit.SlingTestContext;
import org.apache.sling.junit.SlingTestContextProvider;
import org.junit.Test;

/** Test that adds metadata to the SlingTestContext */
public class OutputMetadataTest {
    
    @Test
    public void addMetadata() {
        final SlingTestContext c = SlingTestContextProvider.getContext();
        assertNotNull("Expecting a SlingTestContext", c);
        
        c.output().put("the_quick", "brown fox!");
        c.output().put("the_answer", 42);
    }
}
