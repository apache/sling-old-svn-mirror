package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/** Test our run modes support */ 
public class RunModeAIT {
    
    @ClassRule
    public static CrankstartSetup C = new CrankstartSetup().withModels(U.DEFAULT_MODELS);
    
    private WebconsoleClient osgiConsole;
    private DefaultHttpClient client;
    private static final String RUN_MODES = "foo,bar,A";
    
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    @BeforeClass
    public static void setupClass() throws Exception {
        System.setProperty(RunModeFilter.SLING_RUN_MODES, RUN_MODES);
    }
    
    @Before
    public void setup() throws IOException {
        osgiConsole = new WebconsoleClient(C.getBaseUrl(), U.ADMIN, U.ADMIN);
        client = new DefaultHttpClient();
    }
    
    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(RunModeFilter.SLING_RUN_MODES);
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testSlingApiVersionA() throws Exception {
        assertEquals("2.9.0", osgiConsole.getBundleVersion(U.SLING_API_BUNDLE));
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testConfigA() throws Exception {
        U.setAdminCredentials(client);
        U.assertHttpGet(C, client,
                "/test/config/runmode.test", 
                "runmode.test#mode=(String)This is A#service.pid=(String)runmode.test##EOC#");
    }
    
}