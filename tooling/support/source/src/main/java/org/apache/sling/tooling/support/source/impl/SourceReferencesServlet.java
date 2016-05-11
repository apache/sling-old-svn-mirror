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
package org.apache.sling.tooling.support.source.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <tt>SourceReferencesServlet</tt> infers and outputs source information about bundles running a Sling instance
 */
@Component
@Service(value = Servlet.class)
@Property(name="alias", value="/system/sling/tooling/sourceReferences.json")
public class SourceReferencesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String KEY_TYPE = "__type__";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_ARTIFACT_ID = "artifactId";
    private static final String KEY_VERSION = "version";

    static final String VALUE_TYPE_MAVEN = "maven";

    private static final String FELIX_FW_GROUP_ID = "org.apache.felix";
    private static final String FELIX_FW_ARTIFACT_ID = "org.apache.felix.framework";

    private ComponentContext ctx;
    private List<SourceReferenceFinder> finders;

    protected void activate(ComponentContext ctx) {
        this.ctx = ctx;
        
        finders = new ArrayList<SourceReferenceFinder>();
        finders.add(new FelixJettySourceReferenceFinder());
    }    
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.setContentType("application/json");
            
            final JSONWriter w = new JSONWriter(response.getWriter());
            w.array();
            
            for ( Bundle bundle : ctx.getBundleContext().getBundles() ) {

                Object bundleVersion = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
                
                w.object();
                w.key(Constants.BUNDLE_SYMBOLICNAME);
                w.value(bundle.getSymbolicName());
                w.key(Constants.BUNDLE_VERSION);
                w.value(bundleVersion);
                
                w.key("sourceReferences");
                w.array();

                // the system bundle is embedded by the launchpad jar so we need special handling
                // since the pom.properties file is not located in the bundle
                if ( bundle.getBundleId() == 0 && bundle.getSymbolicName().equals(FELIX_FW_ARTIFACT_ID) ) {
                    writeMavenGav(w, FELIX_FW_GROUP_ID, FELIX_FW_ARTIFACT_ID, (String) bundleVersion);
                }
                
                // look for pom.properties in the bundle ( the original bundle, fragments )
                collectMavenSourceReferences(w, bundle);
                
                // look for pom.properties in jars embedded in the bundle
                for ( String jar : getEmbeddedJars(bundle)) {
                    URL entry = bundle.getEntry(jar);
                    // incorrect or inaccessible entry
                    if ( entry == null ) {
                        continue;
                    }
                    collectMavenSourceRerefences(w, entry);
                }
                
                // query custom finders for source references
                for ( SourceReferenceFinder finder : finders ) {
                    try {
                        for ( SourceReference reference : finder.findSourceReferences(bundle)) {
                            log.debug("{} found reference {}:{}:{} in {}", new Object[] { finder, reference.getGroupId(), reference.getArtifactId(), reference.getVersion(), bundle});
                            writeMavenGav(w, reference.getGroupId(), reference.getArtifactId(), reference.getVersion());
                        }
                    } catch (SourceReferenceException e) {
                        log.warn(finder + " execution did not complete normally for " + bundle, e);
                    }
                }
                
                w.endArray();
                w.endObject();
            }
            
            w.endArray();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    private void collectMavenSourceReferences(JSONWriter w, Bundle bundle) throws IOException, JSONException {
        
        Enumeration<?> entries = bundle.findEntries("/META-INF/maven", "pom.properties", true);
        
        while ( entries != null && entries.hasMoreElements()) {
            URL entry = (URL) entries.nextElement();
            
            InputStream in = entry.openStream();
            try {
                writeMavenGav(w, in);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
    
    private void writeMavenGav(JSONWriter w, String groupId, String artifactId, String version) throws JSONException {
        
        w.object();
        w.key(KEY_TYPE).value(VALUE_TYPE_MAVEN);
        w.key(KEY_GROUP_ID).value(groupId);
        w.key(KEY_ARTIFACT_ID).value(artifactId);
        w.key(KEY_VERSION).value(version);
        w.endObject();
    }
    
    private void writeMavenGav(JSONWriter w, InputStream in) throws IOException, JSONException {
        
        Properties p = new Properties();
        p.load(in);
        w.object();
        w.key(KEY_TYPE).value(VALUE_TYPE_MAVEN);
        for ( String prop : new String[] { KEY_GROUP_ID, KEY_ARTIFACT_ID, KEY_VERSION} ) {
            w.key(prop).value(p.getProperty(prop));
        }
        w.endObject();
    }
    
    private List<String> getEmbeddedJars(Bundle bundle) {
        
        String classPath = (String) bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if ( classPath == null ) {
            return Collections.emptyList();
        }

        List<String> embeddedJars = new ArrayList<String>();
        
        String[] classPathEntryNames = classPath.split("\\,");
        for ( String classPathEntry : classPathEntryNames ) {
            if ( classPathEntry.endsWith(".jar")) {
                embeddedJars.add(classPathEntry);
            }
        }
        
        return embeddedJars;
    }
    
    private void collectMavenSourceRerefences(JSONWriter w, URL entry) throws IOException, JSONException {
        
        InputStream wrappedIn = entry.openStream();
        try {
            JarInputStream jarIs = new JarInputStream(wrappedIn);
            JarEntry jarEntry;
            while ( ( jarEntry = jarIs.getNextJarEntry()) != null ) {
                String entryName = jarEntry.getName();
                if ( entryName.startsWith("META-INF/maven/") && entryName.endsWith("/pom.properties")) {
                    writeMavenGav(w, jarIs);
                }
            }
        } finally {
            IOUtils.closeQuietly(wrappedIn);
        }
    }    

}
