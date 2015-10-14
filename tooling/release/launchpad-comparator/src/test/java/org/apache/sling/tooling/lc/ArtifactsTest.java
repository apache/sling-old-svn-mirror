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
package org.apache.sling.tooling.lc;

import static org.apache.sling.tooling.lc.aether.Artifacts.launchpadCoordinates;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ArtifactsTest {
    
    @Test
    public void launchpadV7() {
        
        assertThat(launchpadCoordinates("7"), equalTo("org.apache.sling:org.apache.sling.launchpad:xml:bundlelist:7"));
    }
    
    @Test
    public void launchpadV7Snapshot() {
        
        assertThat(launchpadCoordinates("7-SNAPSHOT"), equalTo("org.apache.sling:org.apache.sling.launchpad:xml:bundlelist:7-SNAPSHOT"));
    }

    @Test
    public void launchpadV8() {
        
        assertThat(launchpadCoordinates("8"), equalTo("org.apache.sling:org.apache.sling.launchpad:txt:slingfeature:8"));
    }
    
    @Test
    public void launchpadV8Snapshot() {
        
        assertThat(launchpadCoordinates("8-SNAPSHOT"), equalTo("org.apache.sling:org.apache.sling.launchpad:txt:slingfeature:8-SNAPSHOT"));
    }

}
