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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.provisioning.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolve artifacts using Maven URLs - assumes Pax URL is installed */ 
public class MavenResolver {
    public static final String SLINGFEATURE_ARTIFACT_TYPE = "slingfeature";
    public static final String SLINGSTART_ARTIFACT_TYPE = "slingstart";
    public static final String TXT_ARTIFACT_TYPE = "txt";
    
    private static final Logger log = LoggerFactory.getLogger(MavenResolver.class);
    
    @SuppressWarnings("serial")
    private static final Map <String, String> ARTIFACT_TYPES_MAP = new HashMap<String,String>() {
        {
            put(SLINGFEATURE_ARTIFACT_TYPE, TXT_ARTIFACT_TYPE);
            put(SLINGSTART_ARTIFACT_TYPE, TXT_ARTIFACT_TYPE);
        }
    };
    
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
            sb.append("/").append(mapArtifactType(a.getType()));
        }
        
        if(a.getClassifier() != null) {
            sb.append("/").append(a.getClassifier());
        }
        
        return sb.toString();
    }
    
    /** Maven plugins can map artifact types to well-known
     *  extensions during deployment - this implements the
     *  same mapping.
     */   
    public static String mapArtifactType(String artifactType) {
        final String mapped = ARTIFACT_TYPES_MAP.get(artifactType);
        if(mapped != null) {
            log.info("artifact type '{}' mapped to '{}' for resolution", artifactType, mapped);
            return mapped;
        }
        return artifactType;
    }
    
    public static InputStream resolve(Artifact a) throws MalformedURLException, IOException {
        return new URL(mvnUrl(a)).openStream();
    }
}