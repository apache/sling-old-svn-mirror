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
package org.apache.sling.ide.osgi.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.sling.ide.osgi.MavenSourceReference;
import org.apache.sling.ide.osgi.SourceReference;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;

public class HttpOsgiClientTest {

    @Test
    public void testGetBundleVersionFromReader() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("bundles.json");
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Assert.assertEquals(new Version("3.4.0"), HttpOsgiClient.getBundleVersionFromReader("org.apache.commons.lang3", reader));
        }
    }
    
    @Test
    public void parseSourceReferences() throws IOException {
        
        try ( InputStream input = getClass().getClassLoader().getResourceAsStream("sourceReferences.json")) {
            List<SourceReference> references = HttpOsgiClient.parseSourceReferences(input);
            assertThat("references.size", references.size(), equalTo(241));
            
            SourceReference first = references.get(0);
            assertThat(first.getType(), equalTo(SourceReference.Type.MAVEN));
            
            MavenSourceReference mavenSR = (MavenSourceReference) first;
            assertThat(mavenSR.getGroupId(), equalTo("org.apache.felix"));
            assertThat(mavenSR.getArtifactId(), equalTo("org.apache.felix.framework"));
            assertThat(mavenSR.getVersion(), equalTo("5.6.6"));
        }
    }
}
