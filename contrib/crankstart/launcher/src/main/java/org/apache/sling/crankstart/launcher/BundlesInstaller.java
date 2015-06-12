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
    
    public void installBundles(BundleContext ctx, FeatureFilter filter) throws IOException, BundleException {
        for(Feature f : model.getFeatures()) {
            if(filter.ignoreFeature(f)) {
                log.info("Ignoring feature: {}", f.getName());
                continue;
            }
            
            log.info("Processing feature: {}", f.getName());
            for(RunMode rm : f.getRunModes()) {
                if(!rmFilter.runModeActive(rm)) {
                    log.info("RunMode is not active, ignored: {}", Arrays.asList(rm.getNames()));
                    continue;
                }
                for(ArtifactGroup g : rm.getArtifactGroups()) {
                    final int startLevel = g.getStartLevel();
                    for(Artifact a : g) {
                        // TODO for now, naively assume a is a bundle, and mvn: protocol
                        final String url = "mvn:" + a.getGroupId() + "/" + a.getArtifactId() + "/" + a.getVersion();
                        installBundle(ctx, url, startLevel);
                    }
                }
            }
        }
    }
    
    protected boolean ignoreFeature(Feature f) {
        return false;
    }
    
    public void installBundle(BundleContext ctx, String bundleUrl, int startLevel) throws IOException, BundleException {
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