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
package org.apache.sling.resource.path;

import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.path.PathBuilder;
import org.hamcrest.Matchers;
import org.junit.Test;

public class PathBuilderTest {

    @Test
    public void noChangeNeeded_root() {
        
        assertThat(new PathBuilder("/").append("path").toString(), Matchers.equalTo("/path"));
    }

    @Test
    public void noChangeNeeded_intermediate() {
        
        assertThat(new PathBuilder("/parent").append("/child").toString(), Matchers.equalTo("/parent/child"));
    }
    
    @Test
    public void removeSlash_root() {
        
        assertThat(new PathBuilder("/").append("/path").toString(), Matchers.equalTo("/path"));
    }

    @Test
    public void removeSlash_intermediate() {
        
        assertThat(new PathBuilder("/parent/").append("/child").toString(), Matchers.equalTo("/parent/child"));
    }
    
    @Test
    public void addSlash() {
        
        assertThat(new PathBuilder("/parent").append("child").toString(), Matchers.equalTo("/parent/child"));
    }
    
    @Test(expected = IllegalArgumentException.class) 
    public void relativeInitialPaths() {
        new PathBuilder("relative");
    }

    @Test(expected = IllegalArgumentException.class) 
    public void nullInitialPath() {
        new PathBuilder(null);
    }

    @Test(expected = IllegalArgumentException.class) 
    public void emptyInitialPath() {
        new PathBuilder("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emptyAppendedPath() {
        new PathBuilder("/parent").append("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullAppendedPath() {
        new PathBuilder("/parent").append(null);
    }
}
