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

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that logs a message */
public class StartBundles implements CrankstartCommand {
    public static final String I_START_ALL_BUNDLES = "start.all.bundles";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_START_ALL_BUNDLES.equals(commandLine.getVerb());
    }

    public String getDescription() {
        return I_START_ALL_BUNDLES + ": start all currently loaded bundles";
    }
    
    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        int count = 0;
        int failures = 0;
        for (Bundle bundle : crankstartContext.getOsgiFramework().getBundleContext().getBundles()) {
            if(isFragment(bundle)) {
                log.debug("Ignoring fragment bundle {}", bundle.getSymbolicName());
                continue;
            }
            if(bundle.getState() != Bundle.ACTIVE) {
                log.info("Starting bundle {}", bundle.getSymbolicName());
                try {
                    bundle.start();
                    count++;
                } catch(Exception e) {
                    log.warn("Failed to start bundle {}", bundle.getSymbolicName(), e);
                    failures++;
                }
            } else {
                log.debug("Bundle {} is already active", bundle.getSymbolicName());
            }
        }
        
        log.info("{} bundles started, {} failures", count, failures);
    }
    
    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }
}
