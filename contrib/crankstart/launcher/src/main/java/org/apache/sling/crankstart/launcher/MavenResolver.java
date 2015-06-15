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
package org.apache.sling.crankstart.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.sling.provisioning.model.Artifact;

/** Resolve artifacts using Maven URLs - assumes Pax URL is installed */ 
public class MavenResolver {
    public static void setup() {
        // Enable pax URL for mvn: protocol
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
    }
    
    public static String mvnUrl(Artifact a) {
        final StringBuilder sb = new StringBuilder();
        sb
        .append("mvn:")
        .append(a.getGroupId())
        .append("/")
        .append(a.getArtifactId())
        .append("/")
        .append(a.getVersion());
        
        if(a.getType() != null) {
            sb.append("/").append(a.getType());
        }
        
        if(a.getClassifier() != null) {
            sb.append("/").append(a.getClassifier());
        }
        
        return sb.toString();
    }
    
    public static InputStream resolve(Artifact a) throws MalformedURLException, IOException {
        return new URL(mvnUrl(a)).openStream();
    }
}