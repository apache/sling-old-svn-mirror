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
package org.apache.sling.ide.eclipse.m2e.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

public class MavenProjectUtils {

    private static final String DEFAULT_SERVLET_API_VERSION = "2.5";
    private static final Pattern SERVLET_API_VERSION_MATCHER = Pattern.compile("^(\\d\\.\\d)");

    public static Resource guessJcrRootFolder(MavenProject project) {
        
        for ( Resource resource : project.getBuild().getResources() ) {
            if ( resource.getDirectory().endsWith("jcr_root")) {
                return resource;
            }
        }
        
        return project.getBuild().getResources().get(0);
    }
    
    public static String guessServletApiVersion(MavenProject project) {
        
        for ( Dependency dependency :  project.getDependencies() ) {
            
            if ( "servlet-api".equals(dependency.getArtifactId()) || "javax.servlet-api".equals(dependency.getArtifactId())) {
                Matcher matcher = SERVLET_API_VERSION_MATCHER.matcher(dependency.getVersion());
                if ( matcher.matches() ) {
                    return matcher.group(1);
                }
            }
        }
        
        return DEFAULT_SERVLET_API_VERSION;
    }
    
    private MavenProjectUtils() {
        
    }
}
