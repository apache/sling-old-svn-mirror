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
package org.apache.sling.crankstart;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class MavenUrlTest {
    @Test
    public void testResolveArtifactl() throws IOException {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        
        //TODO does pax url use the default local repo? 
        //System.setProperty( "org.ops4j.pax.url.mvn.localRepository", localRepoPath );

       final URL url = new URL( "mvn:commons-lang/commons-lang/1.0" );
       url.openStream().close();
       }
}
