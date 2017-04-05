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

import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

/** Test the bundles ignoring feature */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class IgnoredBundlesTest {
    /** Use a released launchpad for this example */
    public static final String SLING_LAUNCHPAD_VERSION = "7";
    
    public static final String MIME_BUNDLE_SN = "org.apache.sling.commons.mime";
    public static final String JSON_BUNDLE_SN = "org.apache.sling.commons.json";
    
    @Inject
    private BundleContext bundleContext;
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        SlingPaxOptions.setIgnoredBundles(MIME_BUNDLE_SN, JSON_BUNDLE_SN);
        try {
            return SlingPaxOptions.defaultLaunchpadOptions(SLING_LAUNCHPAD_VERSION).getOptions();
        } finally {
            SlingPaxOptions.setIgnoredBundles();
        }
    }
    
    @Test
    public void testMimeBundle() {
        assertNull(SlingSetupTest.getBundle(bundleContext, MIME_BUNDLE_SN));
    }
    
    @Test
    public void testJsonBundle() {
        assertNull(SlingSetupTest.getBundle(bundleContext, JSON_BUNDLE_SN));
    }
 }