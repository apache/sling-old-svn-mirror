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
package org.apache.sling.maven.javaversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets Maven project properties in accordance to the specified <tt>sling.java.version</tt> property
 * 
 * <p>The following versions are set:
 * <ol>
 *   <li><tt>sling.bree</tt> - the value of the OSGi Bundle-RequiredExecutionEnvironment header.
 *   <li><tt>sling.animalSignatures.version</tt> - the value 
 * </ol>
 * </p>
 *
 */
@Mojo(name = "set-java-version", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetJavaVersionMojo extends AbstractMojo {

    /**
     * Java version, without the leading '1.', e.g. <em>8</em>
     */
    @Parameter(property = "sling.java.version")
    private String javaVersion;

    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;
    
    private final Map<String, String> javaVersionToBree = new HashMap<String,String>();
    {
        javaVersionToBree.put("6", "JavaSE-1.6");
        javaVersionToBree.put("7", "JavaSE-1.7");
        javaVersionToBree.put("8", "JavaSE-1.8");
    }

    public void execute() throws MojoExecutionException {

        String bree = javaVersionToBree.get(javaVersion);

        if ( bree == null )
            throw new MojoExecutionException("Invalid javaVersion: " + javaVersion + ". Expected one of " + javaVersionToBree.keySet());
        
        project.getProperties().setProperty("sling.bree", bree);
        project.getProperties().setProperty("sling.animalSignatures.version", javaVersion);
        
        getLog().info("Setting Bundle-RequiredExecutionEnvironment=" + bree + " from sling.java.version=" + javaVersion);
    }
}
