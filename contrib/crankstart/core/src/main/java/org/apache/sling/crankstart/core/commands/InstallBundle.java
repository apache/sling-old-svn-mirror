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
package org.apache.sling.crankstart.core.commands;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that installs a bundle */
public class InstallBundle implements CrankstartCommand {
    public static final String I_BUNDLE = "bundle";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_BUNDLE.equals(commandLine.getVerb());
    }

    public String getDescription() {
        return I_BUNDLE + ": install a bundle, without starting it";
    }
    
    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final String bundleRef = commandLine.getQualifier();
        final BundleContext ctx = crankstartContext.getOsgiFramework().getBundleContext();
        final int level = getStartLevel(crankstartContext);
        installBundle(ctx, log, bundleRef, level);
    }
    
    static void installBundle(BundleContext ctx, Logger log, String urlString, int startLevel) throws IOException, BundleException {
        final URL url = new URL(urlString);
        final InputStream bundleStream = url.openStream();
        try {
            final Bundle b = ctx.installBundle(urlString, url.openStream());
            if(startLevel > 0) {
                final BundleStartLevel bsl = (BundleStartLevel)b.adapt(BundleStartLevel.class);
                if(bsl == null) {
                    log.warn("Bundle does not adapt to BundleStartLevel, cannot set start level", urlString);
                }
                bsl.setStartLevel(startLevel);
            }
            
            log.info("bundle installed at start level {}: {}", startLevel, urlString);
        } finally {
            bundleStream.close();
        }
    }
    
    private int getStartLevel(CrankstartContext ctx) {
        int result = 0;
        final String str = ctx.getDefaults().get(CrankstartContext.DEFAULT_BUNDLE_START_LEVEL);
        if(str == null) {
            log.debug("{} default value is not set, using default bundle start level", CrankstartContext.DEFAULT_BUNDLE_START_LEVEL);
        } else {
            try {
                result = Integer.valueOf(str);
            } catch(NumberFormatException nfe) {
                log.warn("Invalid {} value [{}], will use default bundle start level", CrankstartContext.DEFAULT_BUNDLE_START_LEVEL, str);
            }
        }
        return result;
    }
}
