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

import java.io.InputStream;
import java.net.URL;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that installs a bundle */
public class InstallBundle implements CrankstartCommand {
    public static final String I_BUNDLE = "bundle ";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(String commandLine) {
        return commandLine.startsWith(I_BUNDLE);
    }

    @Override
    public void execute(CrankstartContext crankstartContext, String commandLine) throws Exception {
        final String bundleRef = U.removePrefix(I_BUNDLE, commandLine);
        final URL url = new URL(bundleRef);
        final BundleContext ctx = crankstartContext.getOsgiFramework().getBundleContext();
        final InputStream bundleStream = url.openStream();
        try {
            ctx.installBundle(bundleRef, url.openStream());
            log.info("bundle installed: {}", bundleRef);
        } finally {
            bundleStream.close();
        }
    }
}
