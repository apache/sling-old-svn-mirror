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
package org.apache.sling.ide.eclipse.m2e;

import java.util.concurrent.TimeUnit;

import org.apache.sling.ide.eclipse.m2e.impl.helpers.MavenProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.DefaultJavaVMInstall;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.Poller;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.Rule;
import org.junit.Test;

/**
 * This tests ensures that a legacy project has the correct error marker attached.
 * 
 * <p>By legacy projects we understand those that use the <tt>maven-bundle-plugin</tt> with a version older than 3.2.0.</p>
 *
 */
public class LegacyMavenBundleProjectTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();
    
    @Rule
    public DefaultJavaVMInstall jvm = new DefaultJavaVMInstall();
    
    @Test
    public void testLegacyMavenBundleProjectHasErrorMarker() throws Exception {
        
        // create project
        final IProject bundleProject = projectRule.getProject();

        MavenProjectAdapter project = new MavenProjectAdapter(bundleProject);
        project.createOrUpdateFile(Path.fromPortableString("pom.xml"), getClass().getResourceAsStream("legacy-pom.xml"));
        
        project.convertToMavenProject();

        // wait up to 1 minute for the build to succeed due to time needed to retrieve dependencies
        Poller markerPoller = new Poller(TimeUnit.MINUTES.toMillis(1));
        markerPoller.pollUntilSuccessful(new Runnable() {
            @Override
            public void run() {
                try {
                    IMarker[] markers = bundleProject.findMarkers(null, true, IResource.DEPTH_ONE);
                    
                    for ( IMarker marker : markers ) {
                        if ( marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) != IMarker.SEVERITY_ERROR ) {
                            continue;
                        }
                        
                        if ( marker.getAttribute(IMarker.MESSAGE, "").startsWith("Missing m2e incremental build support")) {
                            return;
                        }
                    }
                    
                    throw new RuntimeException("Did not find error message starting with 'Missing m2e incremental support'");
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
