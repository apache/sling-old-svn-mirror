package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/** Verify that we can start the Felix HTTP service
 *  with a {@link CrankstartBootstrap}. 
 */
public class CrankstartBootstrapTest {
    
    private static final int port = getAvailablePort();
    private static DefaultHttpClient client;
    private static Thread crankstartThread;
    private static String baseUrl = "http://localhost:" + port;
    public static final int LONG_TIMEOUT = 10000;
    public static final int STD_INTERVAL = 250;
    
    public static final String [] MODEL_PATHS = {
        "/crankstart-model.txt",
        "/provisioning-model/base.txt",
        "/provisioning-model/sling-extensions.txt",
        "/provisioning-model/start-level-99.txt",
        "/provisioning-model/crankstart-tests.txt"
    };
            
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    private static int getAvailablePort() {
        int result = -1;
        ServerSocket s = null;
        try {
            try {
                s = new ServerSocket(0);
                result = s.getLocalPort();
            } finally {
                if(s != null) {
                    s.close();
                }
            }
        } catch(Exception e) {
            throw new RuntimeException("getAvailablePort failed", e);
        }
        return result;
    }
    
    @Before
    public void setupHttpClient() {
        client = new DefaultHttpClient(); 
    }
    
    private void setAdminCredentials() {
        client.getCredentialsProvider().setCredentials(
                AuthScope.ANY, 
                new UsernamePasswordCredentials("admin", "admin"));
        client.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
    }
    
    private static void mergeModelResource(Launcher launcher, String path) throws IOException {
        final InputStream is = CrankstartBootstrapTest.class.getResourceAsStream(path);
        assertNotNull("Expecting test resource to be found:" + path, is);
        final Reader input = new InputStreamReader(is);
        try {
            launcher.mergeModel(input, path);
        } finally {
            input.close();
        }
    }
     
    @BeforeClass
    public static void setup() throws IOException {
        client = new DefaultHttpClient(); 
        final HttpUriRequest get = new HttpGet(baseUrl);
        System.setProperty("crankstart.model.http.port", String.valueOf(port));
        System.setProperty("crankstart.model.osgi.storage.path", getOsgiStoragePath());
        
        try {
            client.execute(get);
            fail("Expecting connection to " + port + " to fail before starting HTTP service");
        } catch(IOException expected) {
        }
        
        final Launcher launcher = new Launcher();
        for(String path : MODEL_PATHS) {
            mergeModelResource(launcher, path);
        }
        
        crankstartThread = new Thread() {
            public void run() {
                try {
                    launcher.launch();
                } catch(Exception e) {
                    e.printStackTrace();
                    fail("Launcher exception:" + e);
                }
            }
        };
        crankstartThread.setDaemon(true);
        crankstartThread.start();
    }
    
    @AfterClass
    public static void cleanup() throws InterruptedException {
        crankstartThread.interrupt();
        crankstartThread.join();
    }
    
    private void closeConnection(HttpResponse r) throws IOException {
        if(r != null && r.getEntity() != null) {
            EntityUtils.consume(r.getEntity());
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(baseUrl);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting page not found at " + get.getURI(), 404, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testSingleConfigServlet() throws Exception {
        final HttpUriRequest get = new HttpGet(baseUrl + "/single");
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testConfigFactoryServlet() throws Exception {
        final String [] paths = { "/foo", "/bar/test" };
        for(String path : paths) {
            final HttpUriRequest get = new HttpGet(baseUrl + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                closeConnection(response);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testJUnitServlet() throws Exception {
        final String path = "/system/sling/junit";
        final HttpUriRequest get = new HttpGet(baseUrl + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting JUnit servlet to be installed via sling extension command, at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testAdditionalBundles() throws Exception {
        setAdminCredentials();
        final String basePath = "/system/console/bundles/";
        final String [] addBundles = {
                "org.apache.sling.commons.mime",
                "org.apache.sling.settings"
        };
        
        for(String name : addBundles) {
            final String path = basePath + name;
            final HttpUriRequest get = new HttpGet(baseUrl + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting additional bundle to be present at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                closeConnection(response);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testSpecificStartLevel() throws Exception {
        // Verify that this bundle is only installed, as it's set to start level 99
        setAdminCredentials();
        final String path = "/system/console/bundles/org.apache.commons.collections.json";
        final HttpUriRequest get = new HttpGet(baseUrl + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting bundle status to be available at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            
            // Start level is in the props array, with key="Start Level"
            final JSONObject status = new JSONObject(content);
            final JSONArray props = status.getJSONArray("data").getJSONObject(0).getJSONArray("props");
            final String KEY = "key";
            final String SL = "Start Level";
            boolean found = false;
            for(int i=0; i < props.length(); i++) {
                final JSONObject o = props.getJSONObject(i);
                if(o.has(KEY) && SL.equals(o.getString(KEY))) {
                    found = true;
                    assertEquals("Expecting the start level that we set", "99", o.getString("value"));
                }
            }
            assertTrue("Expecting start level to be found in JSON output", found);
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testEmptyConfig() throws Exception {
        setAdminCredentials();
        assertHttpGet(
            "/test/config/empty.config.should.work", 
            "empty.config.should.work#service.pid=(String)empty.config.should.work##EOC#");
    }
        
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testFelixFormatConfig() throws Exception {
        setAdminCredentials();
        assertHttpGet(
                "/test/config/felix.format.test", 
                "felix.format.test#array=(String[])[foo, bar.from.launcher.test]#mongouri=(String)mongodb://localhost:27017#service.pid=(String)felix.format.test#service.ranking.launcher.test=(Integer)54321##EOC#");
    }
    
    private void assertHttpGet(String path, String expectedContent) throws Exception {
        final HttpUriRequest get = new HttpGet(baseUrl + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting 200 response at " + path, 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            assertEquals(expectedContent, content);
        } finally {
            closeConnection(response);
        }
    }
    
    private static String getOsgiStoragePath() {
        final File tmpRoot = new File(System.getProperty("java.io.tmpdir"));
        final Random random = new Random();
        final File tmpFolder = new File(tmpRoot, System.currentTimeMillis() + "_" + random.nextInt());
        if(!tmpFolder.mkdir()) {
            fail("Failed to create " + tmpFolder.getAbsolutePath());
        }
        tmpFolder.deleteOnExit();
        return tmpFolder.getAbsolutePath();
    }
}