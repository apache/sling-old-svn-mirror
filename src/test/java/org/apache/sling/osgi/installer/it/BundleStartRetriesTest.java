package org.apache.sling.osgi.installer.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

/** Test the bundle start retries logic of SLING-1042 */
@RunWith(JUnit4TestRunner.class)
public class BundleStartRetriesTest extends OsgiInstallerTestBase {
    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        setupInstaller();
    }
    
    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testBundleStartRetries() throws Exception {
        final String testB = "osgi-installer-testB";
        final String needsB = "osgi-installer-needsB";
        
        assertNull("TestB bundle must not be present at beginning of test", findBundle(testB));
        
        // without testB, needsB must not start
        resetCounters();
        final long nOps = installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER];
        installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")));
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        assertBundle(needsB + " must not be started, testB not present", needsB, null, Bundle.INSTALLED);
        
        // the bundle start task must be retried immediately
        // (== 3 tasks since last counters reset)
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 3);
        
        // and no more retries must happen before receiving a bundle event
        sleep(1000L);
        assertEquals("Exactly 3 OSGi tasks must have been executed after a few installer cycles",
                nOps + 3, installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER]);

        // generate a bundle event -> must trigger just one retry
        resetCounters();
        generateBundleEvent();
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        sleep(1000L);
        assertEquals("Exactly 5 OSGi tasks total must have been executed after bundle event received",
                nOps + 5, installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER]);
    }

}
