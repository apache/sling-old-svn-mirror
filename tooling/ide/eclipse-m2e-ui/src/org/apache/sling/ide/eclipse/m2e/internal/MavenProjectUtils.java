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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
    
    /**
     * Returns a set of candidate locations for the project's model directory
     * 
     * <p>The heuristics are based on the logic from <tt>org.apache.sling.maven.slingstart.ModelPreprocessor</tt>.
     * The returned values may or may not exist on the filesystem, it is up to the client to check that.
     * The values are ordered from the most likely to the least likely, so clients should iterate
     * the returned candidates in order and pick the first one that exists.</p>
     * 
     * @param mavenProject the project, must not be null
     * @return an ordered set of candidates, never empty
     */
    public static Set<String> getModelDirectoryCandidateLocations(MavenProject mavenProject) {
    	
    	List<String> candidates = new ArrayList<>();
    	candidates.add("src/main/provisioning");
    	candidates.add("src/test/provisioning");
    	
    	Plugin slingstartPlugin = mavenProject.getPlugin("org.apache.sling:slingstart-maven-plugin");
    	if ( slingstartPlugin != null && slingstartPlugin.getConfiguration() instanceof Xpp3Dom ) {
			Xpp3Dom config = (Xpp3Dom) slingstartPlugin.getConfiguration();
			Xpp3Dom modelDir = config.getChild("modelDirectory");
			if ( modelDir != null ) {
				 candidates.add(0, modelDir.getValue());
			}    		
    	}
    	
    	return new LinkedHashSet<>(candidates);
    	
    }
    
    private MavenProjectUtils() {
        
    }
}
