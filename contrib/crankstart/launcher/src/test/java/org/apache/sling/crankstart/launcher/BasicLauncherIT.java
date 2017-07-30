package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/** Basic tests of the launcher, verify that we 
 *  can start the Felix HTTP service and a few
 *  other things. 
 */
public class BasicLauncherIT {
    
    @ClassRule
    public static CrankstartSetup C = new CrankstartSetup().withModelResources(U.DEFAULT_MODELS);
    
    private DefaultHttpClient client;
    private static WebconsoleClient osgiConsole;
    private static final String uniqueText = "Unique text for tests run " + UUID.randomUUID();
    
    // The Launcher.VARIABLE_OVERRIDE_PREFIX must be used for system properties that
    // are meant to provide values for the provisioning model
    private static final String PROP_UNIQUE_TEXT = Launcher.VARIABLE_OVERRIDE_PREFIX + "single.servlet.text";
    
    static {
        // BeforeClass would be too late for this as it's
        // the CrankstartSetup rule that needs this.
        System.setProperty(PROP_UNIQUE_TEXT, uniqueText);
    }
    
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    @BeforeClass
    public static void setupClass() throws Exception {
        osgiConsole = new WebconsoleClient(C.getBaseUrl(), U.ADMIN, U.ADMIN);
    }
    
    @Before
    public void setup() throws IOException {
        System.getProperties().remove(PROP_UNIQUE_TEXT);
        client = new DefaultHttpClient();
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl());
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting page not found at " + get.getURI(), 404, response.getStatusLine().getStatusCode());
        } finally {
            U.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testSingleConfigServlet() throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + "/single");
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            final String content = U.getContent(response);
            final String expected = "SingleConfigServlet:test content is " + uniqueText;
            assertEquals(expected, content);
        } finally {
            U.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testConfigFactoryServlet() throws Exception {
        final String [] paths = { "/foo", "/bar/test" };
        for(String path : paths) {
            final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                U.closeConnection(response);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testJUnitServlet() throws Exception {
        final String path = "/system/sling/junit";
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting JUnit servlet to be installed via sling extension command, at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            U.closeConnection(response);
        }
    }
    
    @Test
    public void testAdditionalBundles() throws Exception {
        final String [] addBundles = {
                "org.apache.sling.commons.mime",
                "org.apache.sling.settings"
        };
        
        for(String name : addBundles) {
            try {
                osgiConsole.checkBundleInstalled(name, U.LONG_TIMEOUT_SECONDS);
            } catch(AssertionError ae) {
                fail("Expected bundle to be present:" + name);
            }
        }
    }
    
    @Test
    public void testBundlesFromNestedModel() throws Exception {
        final String [] addBundles = {
                "org.apache.sling.commons.threads",
                "org.apache.sling/org.apache.sling.discovery.api"
        };
        
        for(String name : addBundles) {
            try {
                osgiConsole.checkBundleInstalled(name, U.LONG_TIMEOUT_SECONDS);
            } catch(AssertionError ae) {
                fail("Expected bundle to be present:" + name);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testSpecificStartLevel() throws Exception {
        // This bundle should only be installed, as it's set to start level 99
        final String symbolicName = "org.apache.commons.collections";
        
        assertEquals("Expecting bundle " + symbolicName + " to be installed", 
                "Installed", 
                osgiConsole.getBundleState(symbolicName));
        
        // Start level is in the props array, with key="Start Level"
        final JsonObject status = U.getBundleData(C, client, symbolicName);
        final JsonArray props = status.getJsonArray("data").getJsonObject(0).getJsonArray("props");
        final String KEY = "key";
        final String SL = "Start Level";
        boolean found = false;
        for(int i=0; i < props.size(); i++) {
            final JsonObject o = props.getJsonObject(i);
            if(o.containsKey(KEY) && SL.equals(o.getString(KEY))) {
                found = true;
                assertEquals("Expecting the start level that's set in provisioning model", 99, o.getInt("value"));
            }
        }
        assertTrue("Expecting start level to be found in JSON output", found);
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testEmptyConfig() throws Exception {
        U.setAdminCredentials(client);
        U.assertHttpGet(C, client,
            "/test/config/empty.config.should.work", 
            "empty.config.should.work#service.pid=(String)empty.config.should.work##EOC#");
    }
        
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testFelixFormatConfig() throws Exception {
        U.setAdminCredentials(client);
        U.assertHttpGet(C, client,
                "/test/config/felix.format.test", 
                "felix.format.test#array=(String[])[foo, bar.from.launcher.test]#mongouri=(String)mongodb://localhost:27017#service.pid=(String)felix.format.test#service.ranking.launcher.test=(Integer)54321##EOC#");
    }
}