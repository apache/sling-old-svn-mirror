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
package org.apache.sling.maven.projectsupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Miscellaneous helper methods for working with bundle lists.
 */
public class BundleListUtils {
    
    private BundleListUtils() {}
    
    /**
     * Initialize the artifact definitions using defaults inside the plugin JAR.
     *
     * @throws IOException if the default properties can't be read
     * @throws XmlPullParserException
     * @throws MojoExecutionException
     */
    public static final void initArtifactDefinitions(ClassLoader classLoader, ArtifactDefinitionsCallback callback) throws IOException {
        Properties dependencies = new Properties();
        dependencies.load(classLoader.getResourceAsStream(
                "org/apache/sling/maven/projectsupport/dependencies.properties"));

        callback.initArtifactDefinitions(dependencies);
    }
    
    public static boolean isCurrentArtifact(MavenProject project, ArtifactDefinition def) {
        return (def.getGroupId().equals(project.getGroupId()) && def.getArtifactId().equals(project.getArtifactId()));
    }
    
    public static BundleList readBundleList(File file) throws IOException, XmlPullParserException {
        BundleListXpp3Reader reader = new BundleListXpp3Reader();
        FileInputStream fis = new FileInputStream(file);
        try {
            return reader.read(fis);
        } finally {
            fis.close();
        }
    }
    
    public static int nodeValue(Xpp3Dom config, String name, int defaultValue) {
        Xpp3Dom node = config.getChild(name);
        if (node != null) {
            return Integer.parseInt(node.getValue());
        } else {
            return defaultValue;
        }
    }
    
    public static boolean nodeValue(Xpp3Dom config, String name, boolean defaultValue) {
        Xpp3Dom node = config.getChild(name);
        if (node != null) {
            return Boolean.parseBoolean(node.getValue());
        } else {
            return defaultValue;
        }
    }

    public static String nodeValue(Xpp3Dom config, String name, String defaultValue) {
        Xpp3Dom node = config.getChild(name);
        if (node != null) {
            return node.getValue();
        } else {
            return defaultValue;
        }
    }
    
    public static void interpolateProperties(BundleList bundleList, MavenProject project, MavenSession mavenSession) throws MojoExecutionException {
        Interpolator interpolator = createInterpolator(project, mavenSession);
        for (final StartLevel sl : bundleList.getStartLevels()) {
            for (final Bundle bndl : sl.getBundles()) {
                try {
                    bndl.setArtifactId(interpolator.interpolate(bndl.getArtifactId()));
                    bndl.setGroupId(interpolator.interpolate(bndl.getGroupId()));
                    bndl.setVersion(interpolator.interpolate(bndl.getVersion()));
                    bndl.setClassifier(interpolator.interpolate(bndl.getClassifier()));
                    bndl.setType(interpolator.interpolate(bndl.getType()));
                } catch (InterpolationException e) {
                    throw new MojoExecutionException("Unable to interpolate properties for bundle " + bndl.toString(), e);
                }
            }
        }

    }
    
    public static Interpolator createInterpolator(MavenProject project, MavenSession mavenSession) {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();

        final Properties props = new Properties();
        props.putAll(project.getProperties());
        props.putAll(mavenSession.getSystemProperties());
        props.putAll(mavenSession.getUserProperties());
        
        interpolator.addValueSource(new PropertiesBasedValueSource(props));

        // add ${project.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource(Arrays.asList("project", "pom"), project, true));

        // add ${session.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource("session", mavenSession));

        // add ${settings.foo}
        final Settings settings = mavenSession.getSettings();
        if (settings != null) {
            interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        }

        return interpolator;
    }

    /**
     * Callback interface for use with initArtifactDefinitions.
     */
    public static interface ArtifactDefinitionsCallback {
        void initArtifactDefinitions(Properties dependencies);
    }

}
