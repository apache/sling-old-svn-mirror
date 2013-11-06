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
package org.apache.sling.paxexam.util;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.sling.maven.projectsupport.BundleListUtils;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.options.CompositeOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pax exam options and utilities to test Sling applications
 *  The basic idea is to get a vanilla Sling launchpad instance
 *  setup with a minimal amount of boilerplate code.
 *  See {@link SlingSetupTest} for an example.
 * */
public class SlingPaxOptions {
    private static final Logger log = LoggerFactory.getLogger(SlingPaxOptions.class);
    public static final int DEFAULT_SLING_START_LEVEL = 30;
    public static final String PROP_TELNET_PORT = "osgi.shell.telnet.port";
    public static final String PROP_HTTP_PORT = "org.osgi.service.http.port";
    public static final String DEFAULT_RUN_MODES = "jackrabbit";
    
    private static int getAvailablePort() {
        int result = Integer.MIN_VALUE;
        try {
            final ServerSocket s = new ServerSocket(0);
            result = s.getLocalPort();
            s.close();
        } catch(IOException ignore) {
        }
        return result;
    }
    
    /** Get run modes to use for our tests, as set by the sling.run.modes property */
    public static Collection<String> getTestRunModes() {
        final String runModes = System.getProperty("sling.run.modes", DEFAULT_RUN_MODES);
        return Arrays.asList(runModes.split(","));
    }
    
    /** @param launchpadVersion null means use the latest */
    public static CompositeOption defaultLaunchpadOptions(String launchpadVersion) {
        final String paxLogLevel = System.getProperty("pax.exam.log.level", "INFO");
        
        final int slingStartLevel = DEFAULT_SLING_START_LEVEL;
        final String telnetPort = System.getProperty(PROP_TELNET_PORT, String.valueOf(getAvailablePort()));
        final String httpPort = System.getProperty(PROP_HTTP_PORT, String.valueOf(getAvailablePort()));
        
        log.info("{}={}", PROP_TELNET_PORT, telnetPort);
        log.info("{}={}", PROP_HTTP_PORT, httpPort);
                
        return new DefaultCompositeOption(
                junitBundles(),
                systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value(paxLogLevel),
                SlingPaxOptions.felixRemoteShellBundles(),
                SlingPaxOptions.slingBootstrapBundles(),
                SlingPaxOptions.slingLaunchpadBundles(launchpadVersion),
                CoreOptions.frameworkStartLevel(slingStartLevel),
                CoreOptions.frameworkProperty(PROP_TELNET_PORT).value(telnetPort),
                CoreOptions.frameworkProperty(PROP_HTTP_PORT).value(httpPort)
        );
    }
    
    public static CompositeOption slingBundleList(String groupId, String artifactId, String version, String type, String classifier) {
        
        final DefaultCompositeOption result = new DefaultCompositeOption();
        
        final String paxUrl = new StringBuilder()
        .append("mvn:")
        .append(groupId)
        .append("/")
        .append(artifactId)
        .append("/")
        .append(version == null ? "" : version)
        .append("/")
        .append(type == null ? "" : type)
        .append("/")
        .append(classifier == null ? "" : classifier)
        .toString();
        
        // TODO BundleList should take an InputStream - for now copy to a tmp file for parsing
        log.info("Getting bundle list {}", paxUrl);
        File tmp = null;
        final Collection<String> testRunModes = getTestRunModes();
        try {
            tmp = dumpMvnUrlToTmpFile(paxUrl);
            final BundleList list = BundleListUtils.readBundleList(tmp);
            int counter = 0;
            for(StartLevel s : list.getStartLevels()) {
                
                // Start level < 0 means bootstrap in our bundle lists
                final int startLevel = s.getStartLevel() < 0 ? 1 : s.getStartLevel();
                
                for(Bundle b : s.getBundles()) {
                    counter++;
                    
                    // TODO need better fragment detection
                    // (but pax exam should really detect that by itself?)
                    final List<String> KNOWN_FRAGMENTS = new ArrayList<String>();
                    KNOWN_FRAGMENTS.add("org.apache.sling.extensions.webconsolebranding");
                    final boolean isFragment = b.getArtifactId().contains("fragment") || KNOWN_FRAGMENTS.contains(b.getArtifactId());
                    
                    // Ignore bundles with run modes that do not match ours 
                    final String bundleRunModes = b.getRunModes();
                    if(bundleRunModes != null && bundleRunModes.length() > 0) {
                        boolean active = false;
                        for(String m : bundleRunModes.split(",")) {
                            if(testRunModes.contains(m)) {
                                active = true;
                                break;
                            }
                        }
                        if(!active) {
                            log.info("Ignoring bundle {} as none of its run modes [{}] are active in this test run {}", 
                                    new Object[] { b.getArtifactId(), bundleRunModes, testRunModes} );
                            continue;
                        }
                    }
                    
                    if(isFragment) {
                        result.add(mavenBundle(b.getGroupId(), b.getArtifactId(), b.getVersion()).noStart());
                    } else if(startLevel == 0){
                        result.add(mavenBundle(b.getGroupId(), b.getArtifactId(), b.getVersion()));
                    } else {
                        result.add(mavenBundle(b.getGroupId(), b.getArtifactId(), b.getVersion()).startLevel(startLevel));
                    }
                    
                    log.info("Bundle added: {}/{}/{}", new Object [] { b.getGroupId(), b.getArtifactId(), b.getVersion()});
                }
            }
            log.info("Got {} bundles from {}", counter, paxUrl);
        } catch(Exception e) {
            throw new RuntimeException("Error getting bundle list " + paxUrl, e);
        } finally {
            if(tmp != null) {
                tmp.delete();
            }
        }
        
        return result;
    }

    public static CompositeOption slingBootstrapBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "2.2.0"),
                
                // TODO: why is this needed?
                mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0")
        );
    }
    
    public static CompositeOption slingLaunchpadBundles(String version) {
        return slingBundleList("org.apache.sling", "org.apache.sling.launchpad", version, "xml", "bundlelist");
    }
    
    /** @param version can be null, to use default */ 
    public static CompositeOption felixRemoteShellBundles() {
        final String gogoVersion = "0.10.0";
        return new DefaultCompositeOption(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.gogo.runtime").version(gogoVersion),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.gogo.shell").version(gogoVersion),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.gogo.command").version(gogoVersion),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.shell.remote").version("1.1.2")
        );
    }
    
    private static File dumpMvnUrlToTmpFile(String mvnUrl) throws IOException {
        final URL url = new URL(mvnUrl);
        final InputStream is = new BufferedInputStream(url.openStream());
        
        final File tmp = File.createTempFile(SlingPaxOptions.class.getName(), "xml");
        log.debug("Copying bundle list contents to {}", tmp.getAbsolutePath());
        tmp.deleteOnExit();
        final OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp));
        try {
            final byte [] buffer = new byte[16384];
            int len = 0;
            while( (len = is.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } finally {
            os.close();
            is.close();
        }
        
        return tmp;
    }
}