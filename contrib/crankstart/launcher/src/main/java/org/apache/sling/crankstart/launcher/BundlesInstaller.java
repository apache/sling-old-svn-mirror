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
import java.net.URL;
import java.util.Arrays;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Install bundles from a provisioning model */
public class BundlesInstaller {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Model model;
    private final RunModeFilter rmFilter;
    
    public BundlesInstaller(Model m, RunModeFilter rmFilter) {
        model = m;
        this.rmFilter = rmFilter;
    }
    
    private boolean isBundle(Artifact a) {
        final String aType = a.getType();
        return "jar".equals(aType) || "war".equals(aType);
    }
    
    public void installBundles(final BundleContext ctx, final FeatureFilter filter) throws Exception {
        
        final ArtifactsVisitor v = new ArtifactsVisitor(model) {

            @Override
            protected void visitArtifact(Feature f, RunMode rm, ArtifactGroup g, Artifact a) throws Exception {
                if(isBundle(a)) {
                    installBundle(ctx, a, g.getStartLevel());
                } else {
                    log.info("Ignoring Artifact, not a bundle: {}", a);
                }
            }

            @Override
            protected boolean acceptFeature(Feature f) {
                final boolean accept = !filter.ignoreFeature(f);
                if(!accept) {
                    log.info("Ignoring feature: {}", f.getName());
                }
                return accept;
            }

            @Override
            protected boolean acceptRunMode(RunMode rm) {
                final boolean accept = rmFilter.runModeActive(rm);
                if(!accept) {
                    log.info("RunMode is not active, ignored: {}", Arrays.asList(rm.getNames()));
                }
                return accept;
            }
        };
        
        v.visit();
    }
    
    public void installBundle(BundleContext ctx, Artifact a, int startLevel) throws IOException, BundleException {
        final String bundleUrl = "mvn:" + a.getGroupId() + "/" + a.getArtifactId() + "/" + a.getVersion() + "/" + a.getType();
        final URL url = new URL(bundleUrl);
        final InputStream bundleStream = url.openStream();
        try {
            final Bundle b = ctx.installBundle(bundleUrl, url.openStream());
            if(startLevel > 0) {
                final BundleStartLevel bsl = (BundleStartLevel)b.adapt(BundleStartLevel.class);
                if(bsl == null) {
                    log.warn("Bundle does not adapt to BundleStartLevel, cannot set start level: {}", bundleUrl);
                }
                bsl.setStartLevel(startLevel);
            }
            
            log.info("bundle installed at start level {}: {}", startLevel, bundleUrl);
        } finally {
            bundleStream.close();
        }
    }
}