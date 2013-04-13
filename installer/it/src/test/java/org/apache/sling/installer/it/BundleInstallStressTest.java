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
package org.apache.sling.installer.it;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.sling.installer.api.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.log.LogService;

/** Repeatedly install/remove/reinstall semi-random sets
 * 	of bundles, to stress-test the installer and framework.
 *
 *  Randomly selects bundles to remove and reinstall in a folder
 *  containing from 4 to N bundles - by supplying a folder with many
 *  bundles, and increasing the number of cycles executed (via
 *  system properties, see pom.xml) the test can be turned into a
 *  long-running stress test.
 */
@RunWith(PaxExam.class)
public class BundleInstallStressTest extends OsgiInstallerTestBase {

	public static final String PROP_BUNDLES_FOLDER = "osgi.installer.BundleInstallStressTest.bundles.folder";
	public static final String PROP_CYCLE_COUNT = "osgi.installer.BundleInstallStressTest.cycle.count";
	public static final String PROP_EXPECT_TIMEOUT_SECONDS = "osgi.installer.BundleInstallStressTest.expect.timeout.seconds";
	public static final int MIN_TEST_BUNDLES = 4;

	/** Folder where test bundles are found */
	private File bundlesFolder;

	/** How many cycles to run */
	private int cycleCount;

	/** List of available test bundles */
	private List<File> testBundles;

	/** Always use the same random sequence */
	private Random random;

	/** Timeout for expectBundles() */
	private long expectBundlesTimeoutMsec;

	/** Synchronize (somewhat) with OSGi operations, to be fair */
	private EventsDetector eventsDetector;
	public static final long MSEC_WITHOUT_EVENTS = 1000L;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        setupInstaller();

        final String bf = System.getProperty(PROP_BUNDLES_FOLDER);
        if(bf == null) {
        	fail("Missing system property: " + PROP_BUNDLES_FOLDER);
        }
        bundlesFolder = new File(bf);
        if(!bundlesFolder.isDirectory()) {
        	fail("Bundles folder '" + bundlesFolder.getAbsolutePath() + "' not found");
        }

        final String cc = System.getProperty(PROP_CYCLE_COUNT);
        if(cc == null) {
        	fail("Missing system property:" + PROP_CYCLE_COUNT);
        }
        cycleCount = Integer.parseInt(cc);

        final String et = System.getProperty(PROP_EXPECT_TIMEOUT_SECONDS);
        if(et == null) {
        	fail("Missing system property:" + PROP_EXPECT_TIMEOUT_SECONDS);
        }
        expectBundlesTimeoutMsec = Integer.parseInt(et) * 1000L;

        log(LogService.LOG_INFO, getClass().getSimpleName()
        		+ ": cycle count=" + cycleCount
        		+ ", expect timeout (msec)=" + expectBundlesTimeoutMsec
        		+ ", test bundles folder=" + bundlesFolder.getAbsolutePath());

        testBundles = new LinkedList<File>();
        final String [] files = bundlesFolder.list();
        for(String filename : files) {
        	if(filename.endsWith(".jar")) {
        		testBundles.add(new File(bundlesFolder, filename));
        	}
        }

        if(testBundles.size() < MIN_TEST_BUNDLES) {
        	fail("Found only " + testBundles.size()
        			+ " bundles in test folder, expected at least " + MIN_TEST_BUNDLES
        			+ " (test folder=" + bundlesFolder.getAbsolutePath() + ")"
        			);
        }

        random = new Random(42 + cycleCount);
        eventsDetector = new EventsDetector(bundleContext);
    }

    @After
    public void tearDown() {
        super.tearDown();
        eventsDetector.close();
    }

    @Test
    public void testSemiRandomInstall() throws Exception {
    	if (cycleCount < 1) {
    		fail("Cycle count (" + cycleCount + ") should be >= 1");
    	}

    	final int initialBundleCount = bundleContext.getBundles().length;
    	log(LogService.LOG_INFO,"Initial bundle count=" + initialBundleCount);
    	logInstalledBundles();

    	// Start by installing all bundles
    	Object listener = this.startObservingBundleEvents();
    	log(LogService.LOG_INFO,"Registering all test bundles, " + testBundles.size() + " resources");
    	install(testBundles);
    	BundleEvent[] installedEvents = new BundleEvent[testBundles.size()];
    	for(int i=0; i<installedEvents.length; i++) {
    	    installedEvents[i] = new BundleEvent(null, null, org.osgi.framework.BundleEvent.INSTALLED);
    	}
    	this.waitForBundleEvents("All bundles should be installed", listener, expectBundlesTimeoutMsec, installedEvents);
    	expectBundleCount("After installing all test bundles", initialBundleCount + testBundles.size());

    	// And run a number of cycles where randomly selected bundles are removed and reinstalled
    	final List<File> currentInstallation = new ArrayList<File>(testBundles);
    	for(int i=0; i < cycleCount; i++) {
            final long start = System.currentTimeMillis();
            log(LogService.LOG_INFO, "Test cycle " + i + ", semi-randomly selecting a subset of our test bundles");

            for(final File f : currentInstallation) {
    	        log(LogService.LOG_DEBUG, "Installed bundle: " + f);
    	    }

    		final List<File> toInstall = selectRandomBundles();
        	log(LogService.LOG_INFO,"Re-registering " + toInstall.size() + " randomly selected resources (other test bundles should be uninstalled)");
            for(final File f : toInstall) {
                log(LogService.LOG_DEBUG, "Re-Registering bundle: " + f);
            }
        	int updates = 0;
        	int installs = 0;
        	for(final File f : toInstall ) {
        	    if ( currentInstallation.contains(f) ) {
        	        updates++;
        	    } else {
        	        installs++;
        	    }
        	}
            final int removes = currentInstallation.size() - updates;
            installedEvents = new BundleEvent[removes + installs];
            for(int m=0; m<installedEvents.length; m++) {
                if ( m < removes ) {
                    installedEvents[m] = new BundleEvent(null, null, org.osgi.framework.BundleEvent.UNINSTALLED);
                } else {
                    installedEvents[m] = new BundleEvent(null, null, org.osgi.framework.BundleEvent.INSTALLED);
                }
            }
            log(LogService.LOG_DEBUG, "Cycle results in " + removes + " removed bundles, " + updates + " updated bundles, " + installs + " installed bundles");

            listener = this.startObservingBundleEvents();
    		install(toInstall);
            this.waitForBundleEvents("All bundles should be installed in cycle " + i, listener, expectBundlesTimeoutMsec, installedEvents);

            eventsDetector.waitForNoEvents(MSEC_WITHOUT_EVENTS, expectBundlesTimeoutMsec);
        	expectBundleCount("At cycle " + i, initialBundleCount + toInstall.size());
        	log(LogService.LOG_INFO,"Test cycle " + i + " successful, "
        			+ toInstall.size() + " bundles, "
        			+ (System.currentTimeMillis() - start) + " msec");

        	// update for next cycle
            currentInstallation.clear();
            currentInstallation.addAll(toInstall);
            try {
               Thread.sleep(500);
            } catch (final InterruptedException ie) { }
    	}
    }

    private void install(List<File> bundles) throws IOException {
    	final List<InstallableResource> toInstall = new LinkedList<InstallableResource>();
    	for(File f : bundles) {
    		toInstall.add(getInstallableResource(f, f.getAbsolutePath() + f.lastModified())[0]);
    	}
    	installer.registerResources(URL_SCHEME, toInstall.toArray(new InstallableResource[toInstall.size()]));
    }

    private void expectBundleCount(String info, final int nBundles) throws Exception {
    	log(LogService.LOG_INFO,"Expecting " + nBundles + " bundles to be installed");
    	final Condition c = new Condition() {
    		int actualCount = 0;
			public boolean isTrue() throws Exception {
				actualCount = bundleContext.getBundles().length;
				return actualCount == nBundles;
			}

			@Override
			String additionalInfo() {
				return "Expected " + nBundles + " installed bundles, got " + actualCount;
			}

			@Override
			void onFailure() {
				log(LogService.LOG_INFO, "Failure: " + additionalInfo());
				logInstalledBundles();
			}

			@Override
	    	long getMsecBetweenEvaluations() {
				return 1000L;
			}
    	};
    	waitForCondition(info, expectBundlesTimeoutMsec, c);
    }

    private List<File> selectRandomBundles() {
    	final List<File> result = new LinkedList<File>();
    	for(File f : testBundles) {
    		if(random.nextBoolean()) {
    			log(LogService.LOG_DEBUG, "Test bundle selected: " + f.getName());
    			result.add(f);
    		}
    	}

    	if(result.size() == 0) {
    		result.add(testBundles.get(0));
    	}

    	return result;
    }
}
