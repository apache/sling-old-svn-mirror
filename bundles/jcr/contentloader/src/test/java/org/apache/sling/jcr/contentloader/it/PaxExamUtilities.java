/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.contentloader.it;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.File;

import org.apache.sling.paxexam.util.SlingPaxOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for Pax Exam testing */
public final class PaxExamUtilities {
    private static final Logger log = LoggerFactory.getLogger(PaxExamUtilities.class);
    
    public static Option[] paxConfig() {
        final File thisProjectsBundle = new File(System.getProperty( "bundle.file.name", "BUNDLE_FILE_NOT_SET" ));
        final String launchpadVersion = System.getProperty("sling.launchpad.version", "LAUNCHPAD_VERSION_NOT_SET");
        log.info("Sling launchpad version: {}", launchpadVersion);
        
        SlingPaxOptions.setIgnoredBundles("org.apache.sling.jcr.contentloader");
        try {
            return new DefaultCompositeOption(
                    SlingPaxOptions.defaultLaunchpadOptions(launchpadVersion),
                    provision(bundle(thisProjectsBundle.toURI().toString())),
                    wrappedBundle(mavenBundle("org.apache.sling", "org.apache.sling.commons.testing").versionAsInProject()),
                    wrappedBundle(mavenBundle("org.ops4j.pax.tinybundles", "tinybundles").versionAsInProject()),
                    mavenBundle("biz.aQute.bnd", "bndlib").versionAsInProject()
            ).getOptions();
        } finally {
            SlingPaxOptions.setIgnoredBundles();
        }
    }
    
    public static Bundle findBundle(BundleContext ctx, String symbolicName) {
        for(Bundle b : ctx.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }
}
