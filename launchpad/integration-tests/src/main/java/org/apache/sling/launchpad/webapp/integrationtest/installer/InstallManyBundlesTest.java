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
package org.apache.sling.launchpad.webapp.integrationtest.installer;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.launchpad.webapp.integrationtest.util.JsonUtil;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.AssertionFailedError;

/** Test the installation and update of several (optionally many)
 *  bundles via the JCR installer
 */
public class InstallManyBundlesTest {
    /** How many test cycles to run - can be set to a higher
     *  value manually for heavier testing.
     */
    private static final int HOW_MANY = Integer.getInteger("InstallManyBundlesTest.howMany", 10);

    private static final long WAIT_ACTIVE_TIMEOUT_MSEC = 10000;

    private static final String TEST_ID = UUID.randomUUID().toString();

    private static final String BASE_BSN = "org.apache.sling.testbundle."
        + InstallManyBundlesTest.class.getSimpleName()
        + "." + TEST_ID;

    private static final String INSTALL_PATH = "/apps/" + InstallManyBundlesTest.class.getSimpleName() + "/" + TEST_ID + "/install";

    private static HttpTest H = new HttpTest();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String toDelete;

    @Before
    public void setup() throws Exception {
        H.setUp();
        H.getTestClient().mkdirs(HttpTest.HTTP_BASE_URL, INSTALL_PATH);
        toDelete = HttpTest.HTTP_BASE_URL + INSTALL_PATH;
    }

    @After
    public void cleanup() throws IOException {
        H.getTestClient().delete(toDelete);
    }

    private InputStream getBundleStream(String bsn, String version) {
        return TinyBundles.bundle()
                .set(Constants.BUNDLE_VERSION, version)
                .set(Constants.BUNDLE_SYMBOLICNAME, bsn)
                .build();
    }

    private void assertActiveBundle(String bsn, String expectedState, String expectedVersion) throws IOException, JsonException {
        final String consoleUrl = HttpTest.HTTP_BASE_URL + "/system/console/bundles/" + bsn + ".json";

        String state = null;
        String version = null;

        final long timeoutMsec = TimeoutsProvider.getInstance().getTimeout(WAIT_ACTIVE_TIMEOUT_MSEC);
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < endTime) {
            try {
                final String jsonStr = H.getContent(consoleUrl, HttpTest.CONTENT_TYPE_JSON);
                final JsonObject json = JsonUtil.parseObject(jsonStr);
                state = json.getJsonArray("data").getJsonObject(0).getString("state");
                version = json.getJsonArray("data").getJsonObject(0).getString("version");
                if(expectedState.equalsIgnoreCase(state) && expectedVersion.equalsIgnoreCase(version)) {
                    return;
                }
                Thread.sleep(100);
            } catch(AssertionFailedError dontCare) {
                // Thrown by getContent - might happen before the
                // bundle is installed
            } catch(InterruptedException ignore) {
            }
        }

        fail("Did not get state="
                + expectedState + " and version=" + expectedVersion
                + " within " + timeoutMsec + " msec"
                + ", got " + state + " / " + version);
    }

    private boolean waitNoBundles(String bsn) throws IOException {
        final String consoleUrl = HttpTest.HTTP_BASE_URL + "/system/console/bundles/" + bsn + ".json";

        final long timeoutMsec = TimeoutsProvider.getInstance().getTimeout(WAIT_ACTIVE_TIMEOUT_MSEC);
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < endTime) {
            try {
                H.getContent(consoleUrl, HttpTest.CONTENT_TYPE_DONTCARE, null, HttpServletResponse.SC_NOT_FOUND );
                return true;
            } catch(AssertionFailedError dontCare) {
                // Thrown by getContent - might happen if the bundle is still installed
            }
            try {
                Thread.sleep(100);
            } catch(InterruptedException ignore) {
            }
        }

        return false;
    }

    private void installAndCheckBundle(String bsn, int filenameVariant, String version) throws IOException, JsonException {
        final String filename = bsn + "." + filenameVariant + ".jar";
        final String url = HttpTest.HTTP_BASE_URL + INSTALL_PATH + "/" + filename;
        H.getTestClient().upload(url, getBundleStream(bsn, version));
        assertActiveBundle(bsn, "Active", version);
    }

    @Test
    public void installAndUpgradeBundleManyTimes() throws IOException, JsonException {
        final String bsn = BASE_BSN + "_upgradetest";
        int i = 0;
        try {
            for(i=0; i < HOW_MANY; i++) {
                final String version = "42.0." + i;
                installAndCheckBundle(bsn, i, version);
            }
            log.info("Test bundle successfully installed, upgraded and started {} times", HOW_MANY);
        } finally {
            log.info("installAndUpgradeBundleManyTimes exiting with i={}", i);
            // we should wait until the OSGi installer has removed everything
            H.getTestClient().delete(toDelete);
            waitNoBundles(bsn);
        }
    }

    @Test
    public void installManyBundles() throws IOException, JsonException {
        final List<String> bsns = new ArrayList<String>();
        int i = 0;
        try {
            final String version = "42.42.42";
            for(i=0; i < HOW_MANY; i++) {
                final String bsn = BASE_BSN + "_manybundles_" + i;
                bsns.add(bsn);
                installAndCheckBundle(bsn, 0, version);
            }
            log.info("{} different bundles successfully installed and started", HOW_MANY);
        } finally {
            log.info("installManyBundles exiting with i={}", i);
            // we should wait until the OSGi installer has removed everything
            H.getTestClient().delete(toDelete);
            while ( !bsns.isEmpty() ) {
                if ( waitNoBundles(bsns.get(0)) ) {
                    bsns.remove(0);
                } else {
                    final String bsn = bsns.remove(0);
                    bsns.add(bsn);
                }
            }
        }
    }
}